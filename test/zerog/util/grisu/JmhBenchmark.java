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
 * Benchmark                         Mode  Cnt         Score         Error  Units
 * JmhBenchmark.test_lowp_doubleto  thrpt   20  11439027.798 ± 2677191.952  ops/s
 * JmhBenchmark.test_lowp_grisubuf  thrpt   20  11540289.271 ±  237842.768  ops/s
 * JmhBenchmark.test_lowp_grisustr  thrpt   20   5038077.637 ±  754272.267  ops/s
 * 
 * JmhBenchmark.test_rand_doubleto  thrpt   20   1841031.602 ±  219147.330  ops/s
 * JmhBenchmark.test_rand_grisubuf  thrpt   20   2609354.822 ±   57551.153  ops/s
 * JmhBenchmark.test_rand_grisustr  thrpt   20   2078684.828 ±  298474.218  ops/s
 * 
 * This doens't account for any garbage costs either since the benchmarks
 * aren't generating enough to trigger GC, and Java internally uses per-thread
 * objects to avoid some allocations.
 * 
 * Don't call Grisu.doubleToString() except for testing. I think the extra
 * allocations and copying are killing it. I'll fix that.
 */

public class JmhBenchmark {
    
    static final int nmask = 1024*1024 - 1;
    static final double[] random_values = new double[nmask + 1];
    static final double[] lowp_values = new double[nmask + 1];
    
    static final byte[] buffer = new byte[30];
    static final byte[] bresults = new byte[30];
    
    static int i = 0;
    static final Grisu g = Grisu.fmt;
    
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
    public String test_rand_doubleto() {
        String s = Double.toString( random_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
    @Benchmark
    public String test_lowp_doubleto() {
        String s = Double.toString( lowp_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
    @Benchmark
    public String test_rand_grisustr() {
        String s =  g.doubleToString( random_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
    @Benchmark
    public String test_lowp_grisustr() {
        String s =  g.doubleToString( lowp_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
    @Benchmark
    public byte[] test_rand_grisubuf() {
        g.doubleToBytes( bresults, 0, random_values[i] );
        i = (i + 1) & nmask;
        return bresults;
    }
    
    @Benchmark
    public byte[] test_lowp_grisubuf() {
        g.doubleToBytes( bresults, 0, lowp_values[i] );
        i = (i + 1) & nmask;
        return bresults;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JmhBenchmark.class.getSimpleName() + ".*")
                .warmupIterations(20)
                .measurementIterations(20)
                .forks(1)
                .build();

        new Runner(opt).run();
    }


}
