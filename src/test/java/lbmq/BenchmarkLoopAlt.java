package lbmq;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class BenchmarkLoopAlt {

    private final long benchmarkSize;
    private final int writerThreadsPerQueue;
    private final int readerThreads;
    private final int queues;

    private final Object element = new Object();
    private final int individualQueueCapacity;

    private final LongAdder readElements = new LongAdder();

    private List<Thread> readers = null;
    private List<Thread> writers = null;

    BenchmarkLoopAlt(
            int benchmarkSize, int totalQueueCapacity, int queues, int writerThreadsPerQueue, int readerThreads) {
        this.queues = queues;
        this.benchmarkSize = benchmarkSize;
        this.writerThreadsPerQueue = writerThreadsPerQueue;
        this.readerThreads = readerThreads;
        int divisor = queues + 1;
        this.individualQueueCapacity = (totalQueueCapacity + divisor - 1) / divisor;
        System.out.println("Size per queue: " + individualQueueCapacity);
    }

    Duration start() {
        LinkedBlockingQueue<BlockingQueue<Object>> notifier = new LinkedBlockingQueue<>(individualQueueCapacity);
        Stream<LinkedBlockingQueue<Object>> subQueues =
                IntStream.range(0, queues).mapToObj(i -> new LinkedBlockingQueue<>(individualQueueCapacity));
        writers = subQueues
                .flatMap(subQueue -> IntStream.range(0, writerThreadsPerQueue)
                        .mapToObj(i -> new Thread(() -> writer(subQueue, notifier), String.format("writer-%d", i))))
                .collect(Collectors.toList());
        readers = IntStream.range(0, readerThreads)
                .mapToObj(i -> new Thread(() -> reader(notifier), String.format("reader-%d", i)))
                .collect(Collectors.toList());
        long start = System.nanoTime();
        writers.forEach(w -> w.start());
        readers.forEach(w -> w.start());
        writers.forEach(w -> Benchmark.join(w));
        readers.forEach(w -> Benchmark.join(w));
        return Duration.ofNanos(System.nanoTime() - start);
    }

    private void writer(BlockingQueue<Object> offerable, LinkedBlockingQueue<BlockingQueue<Object>> notifier) {
        try {
            for (int i = 0; ; i++) {
                offerable.put(element);
                notifier.put(offerable);
                if (i % 5_000_000 == 0) {
                    System.out.printf(
                            "[%s] Wrote %d elements. Queue size: %d\n",
                            Thread.currentThread().getName(), i, offerable.size());
                }
            }
        } catch (InterruptedException e) {
            // ok
        }
    }

    private void reader(BlockingQueue<BlockingQueue<Object>> notifier) {
        int queueSize;
        try {
            for (int i = 0; ; i++) {
                BlockingQueue<Object> queue = notifier.take();
                queue.take();
                queueSize = queue.size();
                if (i % 5_000_000 == 0) {
                    System.out.printf(
                            "[%s] Read %d elements. Queue size: %d\n",
                            Thread.currentThread().getName(), i, queueSize);
                }
                readElements.increment();
                if (readElements.longValue() >= benchmarkSize) {
                    interruptOthers();
                }
            }
        } catch (InterruptedException e) {
            // ok
        }
    }

    private void interruptOthers() {
        readers.forEach(t -> t.interrupt());
        writers.forEach(t -> t.interrupt());
    }
}
