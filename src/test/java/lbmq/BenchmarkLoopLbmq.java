package lbmq;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class BenchmarkLoopLbmq {

    private final long benchmarkSize;
    private final int writerThreads;
    private final int readerThreads;
    private final int queues;

    private final Object element = new Object();
    private final int individualQueueCapacity;

    private final LongAdder readElements = new LongAdder();

    private List<Thread> readers = null;
    private List<Thread> writers = null;

    public BenchmarkLoopLbmq(
            int benchmarkSize, int totalQueueCapacity, int queues, int writerThreads, int readerThreads) {
        this.benchmarkSize = benchmarkSize;
        this.writerThreads = writerThreads;
        this.readerThreads = readerThreads;
        this.queues = queues;
        this.individualQueueCapacity = (totalQueueCapacity + queues - 1) / queues;
        System.out.println("Size per queue: " + individualQueueCapacity);
    }

    Duration start() {
        LinkedBlockingMultiQueue<Integer, Object> queue = new LinkedBlockingMultiQueue<>();
        Stream<LinkedBlockingMultiQueue<Integer, Object>.SubQueue> subQueues = IntStream.range(0, queues)
                .mapToObj(i -> {
                    queue.addSubQueue(i, 1, individualQueueCapacity);
                    return queue.getSubQueue(i);
                });
        writers = subQueues
                .flatMap(subQueue -> IntStream.range(0, writerThreads)
                        .mapToObj(i -> new Thread(() -> writer(subQueue), String.format("writer-%d", i))))
                .collect(Collectors.toList());
        readers = IntStream.range(0, readerThreads)
                .mapToObj(i -> new Thread(() -> reader(queue), String.format("reader-%d", i)))
                .collect(Collectors.toList());
        long start = System.nanoTime();
        writers.forEach(w -> w.start());
        readers.forEach(w -> w.start());
        writers.forEach(w -> Benchmark.join(w));
        readers.forEach(w -> Benchmark.join(w));
        return Duration.ofNanos(System.nanoTime() - start);
    }

    private void writer(LinkedBlockingMultiQueue<Integer, Object>.SubQueue offerable) {
        try {
            for (int i = 0; ; i++) {
                offerable.put(element);
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

    private void reader(Pollable<Object> pollable) {
        try {
            for (int i = 0; ; i++) {
                Object element = pollable.take();
                if (i % 5_000_000 == 0) {
                    System.out.printf(
                            "[%s] Read %d elements.\n", Thread.currentThread().getName(), i);
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
