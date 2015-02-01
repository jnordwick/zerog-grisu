/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package zerog.util.grisu;

import java.util.Random;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/* 
 * Current JMH bench, similar on small numbers (no fast path
 * code yet) and 75% faster on completely random numbers.
 * 
 * Benchmark                         Mode  Cnt         Score         Error  Units
 * JmhBenchmark.test_lowp_doubleto  thrpt   40  10337464.294 ± 1515567.984  ops/s
 * JmhBenchmark.test_lowp_grisubuf  thrpt   40  10269674.393 ±  765917.689  ops/s
 * JmhBenchmark.test_lowp_grisustr  thrpt   40   4926455.732 ±  564312.662  ops/s
 * 
 * JmhBenchmark.test_rand_doubleto  thrpt   40   1785882.370 ±  103271.284  ops/s
 * JmhBenchmark.test_rand_grisubuf  thrpt   40   3087466.908 ±  400094.707  ops/s
 * JmhBenchmark.test_rand_grisustr  thrpt   40   2170533.061 ±  266207.143  ops/s
 * 
 * This doens't account for any garbage costs either since the benchmarks
 * aren't generating enough to trigger GC, and Java internally uses per-thread
 * objects to avoid many allcation costs.
 * 
 * Don't call Grisu.doubleToString() except for testing. I think the extra
 * allocations and copying are killing it. I'll fix it.
 */

public class JmhBenchmark {
    
    static double[] random_values = new double[1000000];
    static double[] lowp_values = new double[1000000];
    
    static byte[] buffer = new byte[30];
    static byte[] bresults = new byte[30];
    
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
        i = (i + 1) % lowp_values.length;
        String s = Double.toString( random_values[i] );
        return s;
    }
    
    @Benchmark
    public String test_lowp_doubleto() {
        i = (i + 1) % lowp_values.length;
        String s = Double.toString( lowp_values[i] );
        return s;
    }
    
    @Benchmark
    public String test_rand_grisustr() {
        i = (i + 1) % lowp_values.length;
        String s =  g.doubleToString( random_values[i] );
        return s;
    }
    
    @Benchmark
    public String test_lowp_grisustr() {
        i = (i + 1) % lowp_values.length;
        String s =  g.doubleToString( lowp_values[i] );
        return s;
    }
    
    @Benchmark
    public byte[] test_rand_grisubuf() {
        i = (i + 1) % lowp_values.length;
        g.doubleToBytes( bresults, 0, random_values[i] );
        return bresults;
    }
    
    @Benchmark
    public byte[] test_lowp_grisubuf() {
        i = (i + 1) % lowp_values.length;
        g.doubleToBytes( bresults, 0, lowp_values[i] );
        return bresults;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + JmhBenchmark.class.getSimpleName() + ".*")
                .warmupIterations(20)
                .measurementIterations(20)
                .forks(0)
                .build();

        new Runner(opt).run();
    }


}
