package zerog.util.grisu;

import static org.junit.Assert.*;

import org.junit.Test;

public class GrisuTest {

    @Test
    public void test_zero() {
        double d = 0.0;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals( "0.0", s );
    }

    @Test
    public void test_pinf() {
        double d = 1.0 / 0.0;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals( "Infinity", s );
    }

    @Test
    public void test_ninf() {
        double d = -1.0 / 0.0;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals( "-Infinity", s );
    }

    @Test
    public void test_inf() {
        double d = 0.0 / 0.0;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals( "NaN", s );
    }

    @Test
    public void test_long1() {
        double d = 987654321.123456789;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("987654321.1234568", s);
    }

    @Test
    public void test_nlong1() {
        double d = -987654321.123456789;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("-987654321.1234568", s);
    }

    @Test
    public void test_big1() {
        double d = 123456789.123456789*123123123.123123123*456456456.456456456;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("6.938314078198765e+24", s);
    }

    @Test
    public void test_nbig1() {
        double d = -123456789.123456789*123123123.123123123*456456456.456456456;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("-6.938314078198765e+24", s);
    }

    @Test
    public void test_huge1() {
        double d = 123456789123456789.0 * 1.0e+291;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("1.2345678912345678e+308", s);
    }

    @Test
    public void test_nhuge1() {
        double d = -123456789123456789.0 * 1.0e+291;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("-1.2345678912345678e+308", s);
    }

    @Test
    public void test_small1() {
        double d = 0.00000123456789*0.00000000123123123*0.00000000456456456;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("6.938314057383822e-24", s);
    }

    @Test
    public void test_nsmall1() {
        double d = -0.00000123456789*0.00000000123123123*0.00000000456456456;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("-6.938314057383822e-24", s);
    }

    @Test
    public void test_tiny1() {
        double d = 123456789123456789.0 * 1.0e-323;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("1.2199151650353452e-306", s);
    }

    @Test
    public void test_ntiny1() {
        double d = -123456789123456789.0 * 1.0e-323;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("-1.2199151650353452e-306", s);
    }

    @Test
    public void test_x1() {
        double d = 123.453;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("123.453", s);
    }

    @Test
    public void test_x2() {
        double d = 0.123453;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("0.123453", s);
    }

    @Test
    public void test_x3() {
        double d = 123453.0;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("123453.0", s);
    }

    @Test
    public void test_x4() {
        long n = 5404319552844595L;
        long d = 18014398509481984L;

        String s = Grisu.fmt.doubleToString( (double)n / (double)d);
        assertEquals("0.3", s);
    }

    // When the variable width formatting is added, add this back in
    // @Test
    public void test_x5() {
        double d = 0.2999999999999999;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("0.2999999999999999", s);
    }

    @Test
    public void test_x6() {
        double d = 0.29999999999999999;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("0.3", s);
    }

    @Test
    public void test_max() {
        double d = 1.7976931348623157e+308; // 0x7FEFFFFFFFFFFFFF
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("1.7976931348623157e+308", s);
    }

    @Test
    public void test_nmax() {
        double d = -1.7976931348623157e+308; // 0xFFEFFFFFFFFFFFFF
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("-1.7976931348623157e+308", s);
    }

    @Test
    public void test_min() {
        double d = 5.0e-324; // 0x1
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("5e-324", s);
    }

    @Test
    public void test_border1() {
        double d = 1.125;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("1.125", s);
    }

    @Test
    public void test_border2() {
        double d = 8.0;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("8.0", s);
    }

    @Test
    public void test_border3() {
        double d = 3.9999999999999997;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("3.9999999999999996", s);
    }

    @Test
    public void test_border4() {
        double d = 1e+308;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("1e+308", s);
    }

    @Test
    public void test_border5() {
        double d = 1e-323;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("1e-323", s);
    }

    @Test
    public void test_border6() {
        double d = 1e+307;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("1e+307", s);
    }

    public void test_border7() {
        double d = 1e-322;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("1e-322", s);
    }

    @Test
    public void test_denorm1() {
        double d = 1.23e-321;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("1.23e-321", s);
    }

    @Test
    public void test_denorm2() {
        double d = 1.2345e-321;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("1.235e-321", s);
    }

    @Test
    public void test_pow10s() {

        for(int i = -323; i <= 308; ++i ) {
            if( i >= -20 && i <= 20 ) // The format in here changes
                continue;
            String sd = "1e" + (i > 0 ? "+" : "") + i;
            double d = Double.parseDouble( "1e" + i );
            String s = Grisu.fmt.doubleToString( d );
            assertEquals(sd, s);
        }
    }
    
    @Test
    public void test_smallints() {
        
        for(int i = -10000; i <= 10000; i += 1 ) {
            String sd = i + ".0";
            String s = Grisu.fmt.doubleToString( i );
            assertEquals(sd, s);
        }
    }
    
    @Test
    public void test_ints() {
        
        int step = 23456;
        for(int i = Integer.MIN_VALUE; i <= Integer.MAX_VALUE - step; i += step ) {
            String sd = i + ".0";
            String s = Grisu.fmt.doubleToString( i );
            assertEquals(sd, s);
        }
    }
    
    @Test
    public void test_misc1() {
        
        double d = 502973;
        String s = Grisu.fmt.doubleToString( d );
        assertEquals("502973.0", s);
    }
}
