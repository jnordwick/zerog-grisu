package zerog.util.grisu;

import static zerog.util.grisu.DiyFp.doubleExponentBias;
import static zerog.util.grisu.DiyFp.doubleMantissaSize;
import static zerog.util.grisu.DiyFp.u_doubleExponentMask;
import static zerog.util.grisu.DiyFp.u_doubleHiddenBit;
import static zerog.util.grisu.DiyFp.u_doubleMantissaMask;

// TODO: add rounding to number of decimals
// TODO: add fast path for small precision numbers
// TODO: add JMH benchmarking code to git
// TODO: doubleToString() make scratch buffer thread local.

public class Grisu {

    /**
     * The default formatter with 16 {@code max_int_digits}, 10 {@code max_frac_digits},
     * and using little 'e' for the {@code exp_char}.
     */
    public static final Grisu fmt = new Grisu( 16, 10, 'e' );

    // NOTE: get rid of these and pass them into the method? Or maybe passing
    // them in should override these?
    public final int max_int_digits;
    public final int max_frac_digits;
    public final byte exp_char;

    /**
     * Grisu2 is capable of printing out {@value #max_grisu_precision} digits
     * of total precision with the 64 but longs. A longer long would allow more.
     */
    static final int max_grisu_precision = 17;

    /**
     * The longest printed representation is {@value #longest_double_output}. As
     * long as {@code max_int_digits} is less than {@value #max_grisu_precision}
     * this holds, plus 1 for minus sign, plus 1 for the decimal point, plus 5
     * for the exponent (+/-, e, 3 digits).
     */
    public static final int longest_double_output = max_grisu_precision + 1 + 1 + 5;

    protected static final byte[] nan_text = "NaN".getBytes();
    protected static final byte[] inf_text = "Infinity".getBytes();
    protected static final byte[] zero_text = "0.0".getBytes();

    /**
     * Create a formatter with a set of defaults.
     * 
     * @param max_int_digits
     * @param max_frac_digits
     * @param exp_char
     */
    public Grisu( int max_int_digits, int max_frac_digits, char exp_char ) {

        this.max_int_digits = max_int_digits;
        this.max_frac_digits = max_frac_digits;
        this.exp_char = (byte)exp_char;
    }

    /**
     * Prints the double floating point value into a {@link #String}. Underneath it
     * calls {@link #doubleToBytes(byte[], int, double)} with a newly allocated
     * temporary buffer and then news off a String from that.
     * 
     * @param value The double value
     * @return The printed representation
     */
    public String doubleToString( double value ) {

        byte[] buf = new byte[ longest_double_output ];
        int len = doubleToBytes( buf, 0, value );
        
        return new String( buf, 0, len );
    }

    /**
     * Will print the specific double value to the buffer starting at offset using
     * the Grisu2 algorithm described by Florian Loitsch in
     * <a href="http://florian.loitsch.com/publications"><i>Printing Floating-Point
     * Numbers Quickly and Accurately with Integers</i></a>.
     * It gives the shortest correct result that will round trip about 99.8% of the
     * time and a correct one, but not the shortest, the rest of the time.
     * <p>
     * No garbage is generate in the call.
     * 
     * @param buffer A buffer that has at least {@value #longest_double_output}
     * more bytes allocated. This isn't checked so you can get away with less,
     * if you are sure if will fit.
     * 
     * @param boffset Where to begin writing.
     */
    public int doubleToBytes( byte[] buffer, int boffset, double value ) {
        
        // TODO: unpack here and test for special cases myself.

        // Get the special cases out of the way: NaN, infinities, zero(s)
        if( Double.isNaN( value )) {
            // TESTME: isNan() hopefully catches all the NaN values, not just the canonical
            System.arraycopy( nan_text, 0, buffer, boffset, nan_text.length );
            return nan_text.length;
        }
        else if( value == Double.POSITIVE_INFINITY ) {

            System.arraycopy( inf_text, 0, buffer, boffset, inf_text.length );
            return inf_text.length;
        }
        else if( value == Double.NEGATIVE_INFINITY ) {

            buffer[boffset] = '-';
            System.arraycopy( inf_text, 0, buffer, boffset + 1, inf_text.length );
            return 1 + inf_text.length;
        }
        else if( value == 0 ) {
            // NOTE: Should I test for +/-0.0 separately?
            System.arraycopy( zero_text, 0, buffer, boffset, zero_text.length );
            return zero_text.length;
        }

        // So we have a number to stringify now
        int pos = 0;
        if( value < 0 ) {
            buffer[boffset + pos] = '-';
            pos += 1;
            value = -value;
        }

        long u_lenpow = u_grisu2( buffer, boffset + pos, value );
        return pos + formatBuffer( buffer, boffset + pos, StuffedPair.car( u_lenpow ), StuffedPair.cdr( u_lenpow ));
    }

    protected static void round( byte[] buffer, int pos, long u_delta, long u_rest, long  u_onef, long u_winf ) {

        while (Long.compareUnsigned( u_rest, u_winf ) < 0
                && Long.compareUnsigned( u_delta - u_rest, u_onef ) >= 0
                && (Long.compareUnsigned( u_rest + u_onef, u_winf ) < 0
                        || Long.compareUnsigned( u_winf - u_rest, u_rest + u_onef - u_winf ) > 0)) {

            buffer[pos - 1]--;
            u_rest += u_onef;
        }
    }

    // I wonder if this gets compiled to cmov ops? Not sure if it would
    // be beneficial or not...
    protected static int numUnsignedDigits( int u_x ) {
        
        int n = 1;

        if( Integer.compareUnsigned( u_x, 100_000_000 ) >= 0 ) {
            u_x = Integer.divideUnsigned( u_x, 100_000_000 );
            n += 8;
        }
        // after here, we are guaranteed that u_x can no longer have a high bit.
        if( u_x >= 10_000 ) {
            u_x /= 10_000;
            n += 4;
        }
        if( u_x >= 100 ) {
            u_x /= 100;
            n += 2;
        }
        if( u_x >= 10 ) {
            u_x /= 10;
            n += 1;
        }

        return n;
    }

    protected static long u_digitGen( long u_vf, int ve, long u_pf, int pe, long u_delta, byte[] buffer, int boffset, int base10exp ) {

        long u_onef = 1L << -pe;
        long u_fracMask = u_onef - 1;
        long u_winf = u_pf - u_vf;

        // Grab the integral and fractional parts.
        int u_intpart = (int)(u_pf >>> -pe);
        long u_fracpart = u_pf & u_fracMask;

        int digits = numUnsignedDigits( u_intpart );
        int pos = boffset;

        // Write the integer part.
        while( digits > 0 ) {

            int pow10 = (int)CachedPowers.u_pow10[digits - 1];
            int u_dig = Integer.divideUnsigned( u_intpart, pow10 );
            u_intpart = Integer.remainderUnsigned( u_intpart, pow10 );

            // no leading zeros
            if( !(pos == boffset && u_dig == 0) )
                buffer[pos++] = (byte)('0' + u_dig);

            long u_more = (Integer.toUnsignedLong( u_intpart ) << -pe) + u_fracpart;
            digits--;

            // No use going any further, so truncate it off and round.
            if (Long.compareUnsigned( u_more, u_delta ) <= 0) {

                base10exp += digits;
                round( buffer, pos, u_delta, u_more, CachedPowers.u_pow10[digits] << -pe, u_winf );
                return StuffedPair.cons( pos - boffset, base10exp );
            }
        }

        // Write the fractional part
        for (;;) {

            u_fracpart *= 10;
            u_delta *= 10;

            int u_dig = (int)(u_fracpart >>> -pe);

            // no leading zeros
            if( !(pos == boffset && u_dig == 0) )
                buffer[pos++] = (byte)('0' + u_dig);

            u_fracpart &= u_fracMask;
            digits--;

            // no use going any further. Trunc and round as above.
            if (Long.compareUnsigned(u_fracpart, u_delta) <= 0) {

                base10exp += digits;
                round(buffer, pos, u_delta, u_fracpart, u_onef, u_winf * CachedPowers.u_pow10[-digits]);
                //				System.out.println( "cons=" + StuffedPair.toString(StuffedPair.cons(pos-boffset,base10exp)));
                return StuffedPair.cons( pos - boffset, base10exp );
            }
        }
    }

    protected static long u_grisu2( byte[] buffer, int boffset, double value ) {

        // copy and paste from DiyFp(), I admit it.
        long u_vbits = Double.doubleToLongBits( value );
        int ve = (int)(( u_vbits & u_doubleExponentMask ) >>> doubleMantissaSize);
        long u_vf = u_vbits & u_doubleMantissaMask;

        if( ve != 0 ) {
            // normalized
            u_vf |= u_doubleHiddenBit;
            ve -= doubleExponentBias;
        }
        else {
            // denormalized
            ve =  1 - doubleExponentBias;
        }

        // calculate the lower and upper bounds
        int shiftval = u_vf == u_doubleHiddenBit ? 2 : 1;
        long u_mf = (u_vf << shiftval) - 1;
        int me = ve - shiftval;

        long u_pf = (u_vf << 1) + 1;
        int pe = ve - 1;

        // normalize the three ersatz floats
        shiftval = Long.numberOfLeadingZeros( u_vf );
        u_vf <<= shiftval;
        ve -= shiftval;

        shiftval = Long.numberOfLeadingZeros( u_mf );
        u_mf <<= shiftval;
        me -= shiftval;

        shiftval = Long.numberOfLeadingZeros( u_pf );
        u_pf <<= shiftval;
        pe -= shiftval;

        // Find the correct cached power of 10 in ersatz float representation
        int index = CachedPowers.cacheIndexFrom2Exp( ve );
        int base10exp = CachedPowers.exponentFromIndex( index );
        long u_powf = CachedPowers.u_f[index];
        int powe = CachedPowers.e[index];

        // multiple the value and the window by the cached approximation
        u_vf = DiyFp.u_multiplySignificands( u_powf, u_vf );
        ve = DiyFp.multiplyExponents( powe, ve );

        u_mf = DiyFp.u_multiplySignificands( u_powf, u_mf );
        me = DiyFp.multiplyExponents( powe, me );

        u_pf = DiyFp.u_multiplySignificands( u_powf, u_pf );
        pe = DiyFp.multiplyExponents( powe, pe );

        return u_digitGen(u_vf, ve, u_pf, pe, u_pf - u_mf, buffer, boffset, base10exp);
    }

    protected int appendExponent(byte[] buffer, int boffset, int base10exp ) {

        int pos = boffset;

        buffer[pos++] = exp_char;

        if (base10exp < 0) {

            buffer[pos++] = '-';
            base10exp = -base10exp;
        }
        else {

            buffer[pos++] = '+';
        }

        if (base10exp >= 100) {

            buffer[pos] = (byte)('0' + (base10exp / 100));
            base10exp %= 100;
            buffer[pos + 1] = (byte)('0' + (base10exp / 10));
            buffer[pos + 2] = (byte)('0' + (base10exp % 10));
            pos += 3;
        }
        else if (base10exp >= 10) {

            buffer[pos] = (byte)('0' + (base10exp / 10));
            buffer[pos + 1] = (byte)('0' + (base10exp % 10));
            pos += 2;
        }
        else {

            buffer[pos] = (byte)('0' + base10exp);
            pos += 1;
        }

        return pos - boffset;
    }

    /**
     * 
     * Grisu just gives you a string of numbers and a base 10 exponent.
     * This makes it easier to read (e.g, 0.1 isn't rendered as 1 * 10^-1)
     * This is conceptually simple, but intricate in practice, especially when
     * trying to write into a buffer at a offset.
     * This only write positive values. It is assumed the negative sign will
     * be taken care of elsewhere.
     * returns the final length of the formatted number

     * @param buffer The buffer {@link #u_grisu2(byte[], int, double)} printed into.
     * @param boffset The start position of the digits. The same argument to {@link #u_grisu2(byte[], int, double)}.
     * @param blen The length of the digit string returned from {@link #u_grisu2(byte[], int, double)}
     * @param exp The exponent returned from {@link #u_grisu2(byte[], int, double)}
     * @return The final length of the formatted number
     */
    protected int formatBuffer( byte[] buffer, int boffset, int blen, int exp ) {

        int givendigits = blen;

        // This is going to get ugly. SESE-minded people should avert their eyes.
        if( exp >= 0 ) {

            int totaldigits = givendigits + exp;

            if( totaldigits <= max_int_digits ) {

                // easiest, just extend zeros, if any, and append zero fraction
                // 12e0 -> 12.0 and 432e1 -> 4320.0
                for( ; givendigits < totaldigits; ++givendigits )
                    buffer[boffset + givendigits] = '0';

                buffer[boffset + totaldigits] = '.';
                buffer[boffset + totaldigits + 1] = '0';

                return totaldigits + 2;
            }

            else {
                // These will all be large numbers:
                // 123456789e0 -> 1.23456789e8 and 123e10 -> 1.23e12
                int elen;
                if( givendigits == 1 ) {
                    // 1e10 -> 1e10
                    elen = appendExponent( buffer, boffset + givendigits, exp );
                }
                else {
                    // 123e10 -> 1.23e12
                    System.arraycopy( buffer, boffset + 1, buffer, boffset + 2, givendigits );
                    buffer[boffset + 1] = '.';
                    blen += 1; // now we have a '.' in the number
                    elen = appendExponent( buffer, boffset + blen, exp + givendigits - 1 );
                }

                return blen + elen;
            }
        }
        else {
            // From here on down, we have negative exponents

            int fracdigits = -exp;
            int intdigits = Math.max( givendigits - fracdigits, 0 );
            int totaldigits = intdigits + fracdigits;

            if( intdigits > 0 && intdigits <= max_int_digits ) {

                // 12345e-2 -> 123.45
                System.arraycopy( buffer, boffset + intdigits, buffer, boffset + intdigits + 1, fracdigits );
                buffer[boffset + intdigits] = '.';

                return givendigits + 1; // just inserted a dot
            }

            else if( totaldigits <= max_frac_digits ) {

                // 12345e-5 -> 0.12345 and 12e-3 -> 0.012
                int leadingzeros = fracdigits - givendigits;
                int leadingspace = leadingzeros + 2;

                System.arraycopy( buffer, boffset, buffer, boffset + leadingspace, givendigits );

                buffer[boffset] = '0';
                buffer[boffset + 1] = '.';
                for( int i = 0; i < leadingzeros; ++i )
                    buffer[boffset + 2 + i] = '0';

                return leadingspace + givendigits;
            }
            else {

                if( givendigits == 1 ) {
                    int explen = appendExponent( buffer, boffset + givendigits, exp );
                    return boffset + givendigits + explen;
                }
                else {

                    // These are all exponential form at least `precision` digits long or
                    // with at least `precision` integer digits.
                    // 123456789e-2 -> 1.23456789e+6 and 1234e-20 -> 1.234e-17
                    System.arraycopy( buffer, boffset + 1, buffer, boffset + 2, givendigits - 1 );
                    buffer[boffset + 1] = '.';

                    int newexp = exp + givendigits - 1;
                    int explen = appendExponent( buffer, boffset + givendigits + 1, newexp );

                    return 1 + givendigits + explen;
                }
            }
        }
        // Why is there no way to suppress unreachable code ERRORS? Sometimes it is needed
        //assert false : "Unreachable";
    }

}
