package zerog.util.grisu;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class LowPrecBenchmark {
    
    private static final Grisu g = Grisu.fmt;
    
    private static final int nmask = 1024*1024 - 1;
    
    public double[] lowp_values;
    public static byte[] bresults;
    public static int i;
        
    @Setup
    public void setup() {
        lowp_values =  new double[nmask + 1];
        bresults = new byte[30];
        i = 0;

        Random r = new Random();
        int[] pows = new int[] { 1, 10, 100, 1000, 10000, 100000, 1000000 };
        for(int i = 0; i < lowp_values.length; ++i ) {
            lowp_values[i] = (double)(1 + r.nextInt( 10000 )) / (double)pows[r.nextInt( pows.length )];
        }
    }
    
    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public String lowp_doubleto() {
        String s = Double.toString( lowp_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public String lowp_grisustr() {
        String s =  g.doubleToString( lowp_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
   public byte[] lowp_grisubuf() {
        g.doubleToBytes( bresults, 0, lowp_values[i] );
        i = (i + 1) & nmask;
        return bresults;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + LowPrecBenchmark.class.getSimpleName() + ".*")
                .addProfiler( org.openjdk.jmh.profile.LinuxPerfAsmProfiler.class )
                .warmupIterations(30)
                .measurementIterations(10)
                .forks(1)
                .build();

        new Runner(opt).run();
    }


}
