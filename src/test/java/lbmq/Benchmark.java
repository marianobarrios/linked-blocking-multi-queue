package lbmq;

import java.time.Duration;

public class Benchmark {

    static boolean warmup = true;

    static boolean testAlt = true;
    static boolean testLbmq = true;

    static int totalQueueCapacity = 1_000_000;

    static int warmupSize = 50_000_000;
    static int warmupQueues = 1;
    static int warmupWritersPerQueue = 1;
    static int warmupReaders = 1;

    static int benchmarkSize = 20_000_000;
    static int queues = 5;
    static int writersPerQueue = 50;
    static int readers = 1;

    public static void main(String[] args) {
        if (warmup) {
            System.out.println("Warm up");
            if (testAlt) {
                BenchmarkLoopAlt warmupAlt = new BenchmarkLoopAlt(
                        warmupSize, totalQueueCapacity, warmupQueues, warmupWritersPerQueue, warmupReaders);
                warmupAlt.start();
            }
            if (testLbmq) {
                BenchmarkLoopLbmq warmupLbmq = new BenchmarkLoopLbmq(
                        warmupSize, totalQueueCapacity, warmupQueues, warmupWritersPerQueue, warmupReaders);
                warmupLbmq.start();
            }
        }
        // test
        System.out.println("Benchmark");
        Duration timeAlt = null;
        Duration timeLbmq = null;
        if (testAlt) {
            BenchmarkLoopAlt testAlt =
                    new BenchmarkLoopAlt(benchmarkSize, totalQueueCapacity, queues, writersPerQueue, readers);
            timeAlt = testAlt.start();
        }
        if (testLbmq) {
            BenchmarkLoopLbmq testLbmq =
                    new BenchmarkLoopLbmq(benchmarkSize, totalQueueCapacity, queues, writersPerQueue, readers);
            timeLbmq = testLbmq.start();
        }
        if (testAlt) {
            System.out.printf("Time Alternative: %s\n", timeAlt);
        }
        if (testLbmq) {
            System.out.printf("Time LBMQ: %s\n", timeLbmq);
        }
    }

    static void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
