package zerog.util.grisu;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RandomBenchmark {
    
    private static final Grisu g = Grisu.fmt;
    
    private static final int nmask = 1024*1024 - 1;
    private static final double[] random_values = new double[nmask + 1];
    
    public static byte[] bresults;
    public static int i;
        
    @Setup()
    public void setup() {
        i = 0;
        bresults = new byte[30];
        Random r = new Random();
        
        for( int i = 0; i < random_values.length; ++i ) {
            random_values[i] = r.nextDouble();
        }
    }
    
    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public String rand_doubleto() {
        String s = Double.toString( random_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
   @Benchmark
   @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public String rand_grisustr() {
        String s =  g.doubleToString( random_values[i] );
        i = (i + 1) & nmask;
        return s;
    }
    
   @Benchmark
   @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public byte[] rand_grisubuf() {
        g.doubleToBytes( bresults, 0, random_values[i] );
        i = (i + 1) & nmask;
        return bresults;
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + RandomBenchmark.class.getSimpleName() + ".*")
                .addProfiler( org.openjdk.jmh.profile.LinuxPerfAsmProfiler.class )
                .warmupIterations(30)
                .measurementIterations(10)
                .forks(1)
                .build();

        new Runner(opt).run();
    }


}
