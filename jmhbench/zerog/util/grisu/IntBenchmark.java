package zerog.util.grisu;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class IntBenchmark {
    
    private static final Grisu g = Grisu.fmt;
        
    public static byte[] bresults = new byte[30];
    private static int i = -1024 * 1024;
        
    @Benchmark
    public String ints_doubleto() {
        String s = Double.toString( i + 1 );
        i = i > 1024 * 1024 ? -1024 * 1024 : i + 1;
        return s;
    }

    @Benchmark
    public String ints_grisustr() {
        String s = g.doubleToString( i + 1 );
        i = i > 1024 * 1024 ? -1024 * 1024 : i + 1;
        return s;
    }
    
    @Benchmark
    public byte[] ints_grisubuf() {
        g.doubleToBytes( bresults, 0, i + 1 );
        i = i > 1024 * 1024 ? -1024 * 1024 : i + 1;
        return bresults;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + IntBenchmark.class.getSimpleName() + ".*")
                .warmupIterations(10)
                .measurementIterations(10)
                .forks(1)
                .build();

        new Runner(opt).run();
    }


}
