package zerog.util.grisu;

import java.util.Random;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/* 
 * Current JMH bench, similar on small numbers (no fast path code yet)
 * and 40% faster on completely random numbers.
 * 
 * Benchmark w/ int fastpath     Mode  Cnt         Score         Error  Units
 * 
 * JmhBenchmark.small_doubleto  thrpt   12   8735633.334 ± 3739167.110  ops/s
 * JmhBenchmark.small_grisubuf  thrpt   12  14776535.423 ±   48584.322  ops/s
 * JmhBenchmark.small_grisustr  thrpt   12   7799887.379 ± 2142616.641  ops/s
 * 
 * JmhBenchmark.lowp_doubleto   thrpt   12  13573315.245 ± 3955622.162  ops/s
 * JmhBenchmark.lowp_grisubuf   thrpt   12  23346841.830 ±   80295.609  ops/s
 * JmhBenchmark.lowp_grisustr   thrpt   12   6833943.811 ± 1289906.969  ops/s
 * 
 * JmhBenchmark.rand_doubleto   thrpt   12   2425774.325 ±   64280.208  ops/s
 * JmhBenchmark.rand_grisubuf   thrpt   12   2153958.102 ±  293316.158  ops/s
 * JmhBenchmark.rand_grisustr   thrpt   12   2434937.640 ±  496754.763  ops/s
 * 
 * Benchmark                     Mode  Cnt         Score         Error  Units
 * 
 * JmhBenchmark.small_doubleto  thrpt   12   6791720.346 ±   13780.102  ops/s
 * JmhBenchmark.small_grisubuf  thrpt   12   3575878.935 ±  896569.103  ops/s
 * JmhBenchmark.small_grisustr  thrpt   12   2262332.057 ±   23508.600  ops/s
 * 
 * JmhBenchmark.lowp_doubleto   thrpt   12  15553661.357 ±  717270.063  ops/s
 * JmhBenchmark.lowp_grisubuf   thrpt   12   8695800.151 ± 1904169.087  ops/s
 * JmhBenchmark.lowp_grisustr   thrpt   12   4404017.177 ±  977052.119  ops/s
 *
 * JmhBenchmark.rand_doubleto   thrpt   12   1731019.237 ±    1970.826  ops/s
 * JmhBenchmark.rand_grisubuf   thrpt   12   2056287.699 ±    4670.942  ops/s
 * JmhBenchmark.rand_grisustr   thrpt   12   2612992.590 ±   11561.918  ops/s
 *
 * This doens't account for any garbage costs either since the benchmarks
 * aren't generating enough to trigger GC, and Java internally uses per-thread
 * objects to avoid some allocations.
 * 
 * Don't call Grisu.doubleToString() except for testing. I think the extra
 * allocations and copying are killing it. I'll fix that.
 */

public class JmhBenchmark {
    
    private static final Grisu g = Grisu.fmt;
    
    private static final int nmask = 1024*1024 - 1;
    private static final double[] random_values = new double[nmask + 1];
    private static final double[] lowp_values = new double[nmask + 1];
    
    public static byte[] bresults = new byte[30];
    private static int i = 0;
    
    static {
        
        Random r = new Random();
        int[] pows = new int[] { 1, 10, 100, 1000, 10000, 100000, 1000000 };
        
        for( int i = 0; i < random_values.length; ++i ) {
            random_values[i] = r.nextDouble();
        }
        
        for(int i = 0; i < lowp_values.length; ++i ) {
            lowp_values[i] = (1 + r.nextInt( 10000 )) / pows[r.nextInt( pows.length )];
        }
    }
    
    @Benchmark
    public String small_doubleto() {
        String s = Double.toString( i + 1 );
        i = (i + 1) & nmask;
        return s;
    }

    @Benchmark
    public String rand_doubleto() {
        String s = Double.toString( random_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
    @Benchmark
    public String lowp_doubleto() {
        String s = Double.toString( lowp_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
    @Benchmark
    public String small_grisustr() {
        String s = g.doubleToString( i + 1 );
        i = (i + 1) & nmask;
        return s;
    }
    

   @Benchmark
    public String rand_grisustr() {
        String s =  g.doubleToString( random_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
    @Benchmark
    public String lowp_grisustr() {
        String s =  g.doubleToString( lowp_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
    @Benchmark
    public byte[] small_grisubuf() {
        g.doubleToBytes( bresults, 0, i + 1 );
        i = (i + 1) & nmask;
        return bresults;
    }

   @Benchmark
    public byte[] rand_grisubuf() {
        g.doubleToBytes( bresults, 0, random_values[i] );
        i = (i + 1) & nmask;
        return bresults;
    }
    
    @Benchmark
    public byte[] lowp_grisubuf() {
        g.doubleToBytes( bresults, 0, lowp_values[i] );
        i = (i + 1) & nmask;
        return bresults;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JmhBenchmark.class.getSimpleName() + ".*")
                .warmupIterations(6)
                .measurementIterations(12)
                .forks(1)
                .build();

        new Runner(opt).run();
    }


}
