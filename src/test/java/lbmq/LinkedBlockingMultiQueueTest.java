/*
 * Derived from work made by Doug Lea with assistance from members of JCP JSR-166 Expert Group
 * (https://jcp.org/en/jsr/detail?id=166). The original work is in the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package lbmq;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

public class LinkedBlockingMultiQueueTest extends TestCase {

    public enum QueueKey {
        A,
        B,
        C
    }

    public static <T> LinkedBlockingMultiQueue<QueueKey, T> createSingleQueue(int n) {
        LinkedBlockingMultiQueue<QueueKey, T> q = new LinkedBlockingMultiQueue<>();
        q.addSubQueue(QueueKey.A /* key*/, 1 /* priority */, n /* capacity */);
        return q;
    }

    public static <T> LinkedBlockingMultiQueue<QueueKey, T> createSingleQueue() {
        LinkedBlockingMultiQueue<QueueKey, T> q = new LinkedBlockingMultiQueue<>();
        q.addSubQueue(QueueKey.A /* key*/, 1 /* priority */);
        return q;
    }

    public static <T> LinkedBlockingMultiQueue<QueueKey, T> createMultiQueue() {
        LinkedBlockingMultiQueue<QueueKey, T> q = new LinkedBlockingMultiQueue<>();
        q.addSubQueue(QueueKey.A /* key*/, 1 /* priority */);
        q.addSubQueue(QueueKey.B /* key*/, 2 /* priority */);
        q.addSubQueue(QueueKey.C /* key*/, 2 /* priority */);
        return q;
    }

    /** Returns a new queue of given size containing consecutive Integers 0 ... n. */
    public static LinkedBlockingMultiQueue<QueueKey, Integer> populatedSingleQueue(int n) {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(n);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertTrue(q.isEmpty());
        assertTrue(sq.isEmpty());
        for (int i = 0; i < n; i++) {
            assertTrue(sq.offer(i));
        }
        assertFalse(q.isEmpty());
        assertFalse(sq.isEmpty());
        assertEquals(0, sq.remainingCapacity());
        assertEquals(n, q.totalSize());
        assertEquals(n, sq.size());
        return q;
    }

    /** Returns a new queue of given size containing consecutive Integers 0 ... n. */
    public static LinkedBlockingMultiQueue<QueueKey, Integer> populatedMultiQueue() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createMultiQueue();
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qa = q.getSubQueue(QueueKey.A);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qb = q.getSubQueue(QueueKey.B);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qc = q.getSubQueue(QueueKey.C);
        assertTrue(q.isEmpty());
        assertTrue(qa.isEmpty());
        assertTrue(qb.isEmpty());
        assertTrue(qc.isEmpty());
        assertTrue(qa.offer(zero));
        assertTrue(qa.offer(one));
        assertTrue(qa.offer(two));
        assertTrue(qb.offer(three));
        assertTrue(qc.offer(four));
        assertTrue(qb.offer(five));
        assertTrue(qc.offer(six));
        assertTrue(qb.offer(seven));
        assertTrue(qc.offer(eight));
        assertFalse(q.isEmpty());
        assertFalse(qa.isEmpty());
        assertFalse(qb.isEmpty());
        assertFalse(qc.isEmpty());
        assertEquals(9, q.totalSize());
        assertEquals(3, qa.size());
        assertEquals(3, qb.size());
        assertEquals(3, qc.size());
        return q;
    }

    /** A new queue has the indicated capacity, or Integer.MAX_VALUE if none given */
    @Test
    public void testConstructor1() {
        assertEquals(SIZE, createSingleQueue(SIZE).getSubQueue(QueueKey.A).remainingCapacity());
        assertEquals(
                Integer.MAX_VALUE, createSingleQueue().getSubQueue(QueueKey.A).remainingCapacity());
    }

    /** Constructor throws IllegalArgumentException if capacity argument nonpositive */
    @Test
    public void testConstructor2() {
        assertThrows(IllegalArgumentException.class, () -> createSingleQueue(0));
        ;
    }

    /**
     * Returns an element suitable for insertion in the collection. Override for collections with
     * unusual element types.
     */
    protected Integer makeElement(int i) {
        return i;
    }

    /** offer(null) throws NullPointerException */
    @Test
    public void testOfferNull() {
        final LinkedBlockingMultiQueue<QueueKey, ?> q = createSingleQueue();
        LinkedBlockingMultiQueue<QueueKey, ?>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertThrows(NullPointerException.class, () -> sq.offer(null));
    }

    /** add(null) throws NullPointerException */
    @Test
    public void testAddNull() {
        final LinkedBlockingMultiQueue<QueueKey, ?> q = createSingleQueue();
        LinkedBlockingMultiQueue<QueueKey, ?>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertThrows(NullPointerException.class, () -> sq.add(null));
    }

    /** timed offer(null) throws NullPointerException */
    @Test
    public void testTimedOfferNull() throws InterruptedException {
        final LinkedBlockingMultiQueue<QueueKey, ?> q = createSingleQueue();
        LinkedBlockingMultiQueue<QueueKey, ?>.SubQueue sq = q.getSubQueue(QueueKey.A);
        long startTime = System.nanoTime();
        try {
            sq.offer(null, LONG_DELAY_MS, MILLISECONDS);
            shouldThrow();
        } catch (NullPointerException success) {
            // expected
        }
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
    }

    /** put(null) throws NullPointerException */
    @Test
    public void testPutNull() {
        final LinkedBlockingMultiQueue<QueueKey, ?> q = createSingleQueue();
        LinkedBlockingMultiQueue<QueueKey, ?>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertThrows(NullPointerException.class, () -> sq.put(null));
    }

    /** put(null) throws NullPointerException */
    @Test
    public void testAddAllNull() {
        final LinkedBlockingMultiQueue<QueueKey, ?> q = createSingleQueue();
        LinkedBlockingMultiQueue<QueueKey, ?>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertThrows(NullPointerException.class, () -> sq.addAll(null));
    }

    /** addAll of a collection with null elements throws NullPointerException */
    @Test
    public void testAddAllNullElements() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        final Collection<Integer> elements = Arrays.asList(new Integer[SIZE]);
        assertThrows(NullPointerException.class, () -> sq.addAll(elements));
    }

    /** toArray(null) throws NullPointerException */
    @Test
    public void testToArray_NullArray() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertThrows(NullPointerException.class, () -> sq.toArray((Object[]) null));
    }

    /** drainTo(null) throws NullPointerException */
    @Test
    public void testDrainToNull() {
        LinkedBlockingMultiQueue<QueueKey, ?> q = createSingleQueue();
        assertThrows(NullPointerException.class, () -> q.drainTo(null));
    }

    /** drainTo(null, n) throws NullPointerException */
    @Test
    public void testDrainToNullN() {
        LinkedBlockingMultiQueue<QueueKey, ?> q = createSingleQueue();
        assertThrows(NullPointerException.class, () -> q.drainTo(null, 0));
    }

    /** drainTo(c, n) returns 0 and does nothing when n <= 0 */
    @Test
    public void testDrainToNonPositiveMaxElements() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        final int[] ns = {0, -1, -42, Integer.MIN_VALUE};
        for (int n : ns) {
            assertEquals(0, q.drainTo(new ArrayList<>(), n));
        }
        if (sq.remainingCapacity() > 0) {
            // Not SynchronousQueue, that is
            Integer one = makeElement(1);
            sq.add(one);
            for (int n : ns) {
                assertEquals(0, q.drainTo(new ArrayList<>(), n));
            }
            assertEquals(1, sq.size());
            assertEquals(1, q.totalSize());
            assertSame(one, q.poll());
        }
    }

    /** timed poll before a delayed offer times out; after offer succeeds; on interruption throws */
    @Test
    public void testTimedPollWithOffer() throws InterruptedException {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        final CheckedBarrier barrier = new CheckedBarrier(2);
        final Integer zero = makeElement(0);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertNull(q.poll(timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
                barrier.await();
                assertSame(zero, q.poll(LONG_DELAY_MS, MILLISECONDS));

                Thread.currentThread().interrupt();
                try {
                    q.poll(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
                assertFalse(Thread.interrupted());

                barrier.await();
                try {
                    q.poll(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
                assertFalse(Thread.interrupted());
            }
        });

        barrier.await();
        long startTime = System.nanoTime();
        assertTrue(sq.offer(zero, LONG_DELAY_MS, MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);

        barrier.await();
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /** timed poll before a delayed offer times out; after offer succeeds; on interruption throws */
    @Test
    public void testTimedPollWithOfferMultiDisabled() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        final CheckedBarrier barrier = new CheckedBarrier(2);
        final Integer zero = makeElement(0);
        assertTrue(sq.offer(zero));
        sq.enable(false);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime1 = System.nanoTime();
                assertNull(q.poll(timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime1) >= timeoutMillis());
                barrier.await();
                long startTime2 = System.nanoTime();
                assertSame(zero, q.poll(LONG_DELAY_MS, MILLISECONDS));
                assertTrue(millisElapsedSince(startTime2) < SHORT_DELAY_MS);
            }
        });
        barrier.await();
        sq.enable(true);
        awaitTermination(t);
    }

    /** timed poll before a delayed offer times out, after offer succeeds; on interruption throws */
    @Test
    public void testTimedPollWithOfferMultiAdded() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qa = q.getSubQueue(QueueKey.A);
        final CheckedBarrier barrier = new CheckedBarrier(2);
        final Integer zero = makeElement(0);
        assertTrue(qa.offer(zero));
        qa.enable(false);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime1 = System.nanoTime();
                assertNull(q.poll(timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime1) >= timeoutMillis());
                barrier.await();
                long startTime2 = System.nanoTime();
                assertSame(zero, q.poll(LONG_DELAY_MS, MILLISECONDS));
                assertTrue(millisElapsedSince(startTime2) < SHORT_DELAY_MS);
            }
        });
        barrier.await();
        q.addSubQueue(QueueKey.B, 100);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qb = q.getSubQueue(QueueKey.B);
        assertTrue(qb.offer(zero));
        awaitTermination(t);
    }

    /** take() blocks interruptibly when empty */
    @Test
    public void testTakeFromEmptyBlocksInterruptibly() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                threadStarted.countDown();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
                assertFalse(Thread.interrupted());
            }
        });

        await(threadStarted);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /** take() throws InterruptedException immediately if interrupted before waiting */
    @Test
    public void testTakeFromEmptyAfterInterrupt() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                Thread.currentThread().interrupt();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
                assertFalse(Thread.interrupted());
            }
        });

        awaitTermination(t);
    }

    /** timed poll() blocks interruptibly when empty */
    @Test
    public void testTimedPollFromEmptyBlocksInterruptibly() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                threadStarted.countDown();
                try {
                    q.poll(2 * LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
                assertFalse(Thread.interrupted());
            }
        });

        await(threadStarted);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /** timed poll() throws InterruptedException immediately if interrupted before waiting */
    @Test
    public void testTimedPollFromEmptyAfterInterrupt() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                Thread.currentThread().interrupt();
                try {
                    q.poll(2 * LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
                assertFalse(Thread.interrupted());
            }
        });

        awaitTermination(t);
    }

    /**
     * remove(x) removes x and returns true if present TODO: move to superclass CollectionTest.java
     */
    @Test
    public void testRemoveElement() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        final int size = Math.min(sq.remainingCapacity(), SIZE);
        final Integer[] elts = new Integer[size];
        assertFalse(sq.contains(makeElement(99)));
        assertFalse(sq.remove(makeElement(99)));
        checkEmpty(q);
        for (int i = 0; i < size; i++) {
            sq.add(elts[i] = makeElement(i));
        }
        for (int i = 1; i < size; i += 2) {
            for (int pass = 0; pass < 2; pass++) {
                assertEquals((pass == 0), sq.contains(elts[i]));
                assertEquals((pass == 0), sq.remove(elts[i]));
                assertFalse(sq.contains(elts[i]));
                assertTrue(sq.contains(elts[i - 1]));
                if (i < size - 1) assertTrue(sq.contains(elts[i + 1]));
            }
        }
        if (size > 0) assertTrue(sq.contains(elts[0]));
        for (int i = size - 2; i >= 0; i -= 2) {
            assertTrue(sq.contains(elts[i]));
            assertFalse(sq.contains(elts[i + 1]));
            assertTrue(sq.remove(elts[i]));
            assertFalse(sq.contains(elts[i]));
            assertFalse(sq.remove(elts[i + 1]));
            assertFalse(sq.contains(elts[i + 1]));
        }
        checkEmpty(q);
        checkEmpty(sq);
    }

    /** Queue transitions from empty to full when elements added */
    @Test
    public void testEmptyFull() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(2);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertTrue(q.isEmpty());
        assertTrue(sq.isEmpty());
        assertEquals(2, sq.remainingCapacity());
        assertTrue(sq.offer(one));
        assertFalse(q.isEmpty());
        assertFalse(sq.isEmpty());
        assertTrue(sq.offer(two));
        assertFalse(q.isEmpty());
        assertFalse(sq.isEmpty());
        assertEquals(0, sq.remainingCapacity());
        assertFalse(sq.offer(three));
    }

    /** remainingCapacity decreases on add, increases on remove */
    @Test
    public void testRemainingCapacity() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, sq.remainingCapacity());
            assertEquals(SIZE, sq.size() + sq.remainingCapacity());
            assertEquals(i, q.remove().intValue());
        }
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(SIZE - i, sq.remainingCapacity());
            assertEquals(SIZE, sq.size() + sq.remainingCapacity());
            assertTrue(sq.offer(i));
        }
    }

    /** Offer succeeds if not full; fails if full */
    @Test
    public void testOffer() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(1);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertTrue(sq.offer(zero));
        assertFalse(sq.offer(one));
    }

    /** add succeeds if not full; throws IllegalStateException if full */
    @Test
    public void testAdd() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(SIZE);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(sq.add(i));
        }
        assertEquals(0, sq.remainingCapacity());
        assertThrows(IllegalStateException.class, () -> sq.add(SIZE));
    }

    /** addAll(this) throws IllegalArgumentException */
    @Test
    public void testAddAllSelf() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertThrows(IllegalArgumentException.class, () -> sq.addAll(sq));
    }

    /**
     * addAll of a collection with any null elements throws NPE after possibly adding some elements
     */
    @Test
    public void testAddAll3() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(SIZE);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE - 1; ++i) {
            ints[i] = i;
        }
        Collection<Integer> elements = Arrays.asList(ints);
        assertThrows(NullPointerException.class, () -> sq.addAll(elements));
    }

    /** addAll throws IllegalStateException if not enough room */
    @Test
    public void testAddAll4() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(SIZE - 1);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE; ++i) {
            ints[i] = i;
        }
        Collection<Integer> elements = Arrays.asList(ints);
        assertThrows(IllegalStateException.class, () -> sq.addAll(elements));
    }

    /** Queue contains all elements, in traversal order, of successful addAll */
    @Test
    public void testAddAll5() {
        Integer[] empty = new Integer[0];
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE; ++i) {
            ints[i] = i;
        }
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(SIZE);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertFalse(sq.addAll(Arrays.asList(empty)));
        assertTrue(sq.addAll(Arrays.asList(ints)));
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(ints[i], q.poll());
        }
    }

    /** all elements successfully put are contained */
    @Test
    public void testPut() throws InterruptedException {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(SIZE);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        for (int i = 0; i < SIZE; ++i) {
            Integer x = i;
            sq.put(x);
            assertTrue(sq.contains(x));
        }
        assertEquals(0, sq.remainingCapacity());
    }

    /** put blocks interruptibly if full */
    @Test
    public void testBlockingPut() throws InterruptedException {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(SIZE);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i) {
                    sq.put(i);
                }
                assertEquals(SIZE, sq.size());
                assertEquals(0, sq.remainingCapacity());
                Thread.currentThread().interrupt();
                try {
                    sq.put(99);
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    sq.put(99);
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
                assertFalse(Thread.interrupted());
            }
        });
        await(pleaseInterrupt);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
        assertEquals(SIZE, q.totalSize());
        assertEquals(SIZE, sq.size());
        assertEquals(0, sq.remainingCapacity());
    }

    /** put blocks interruptibly waiting for take when full */
    @Test
    public void testPutWithTake() throws InterruptedException {
        final int capacity = 2;
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(2);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        final CountDownLatch pleaseTake = new CountDownLatch(1);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < capacity; i++) {
                    sq.put(i);
                }
                pleaseTake.countDown();
                sq.put(86);
                pleaseInterrupt.countDown();
                try {
                    sq.put(99);
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
                assertFalse(Thread.interrupted());
            }
        });
        await(pleaseTake);
        assertEquals(0, sq.remainingCapacity());
        assertEquals(0, q.take().intValue());
        await(pleaseInterrupt);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
        assertEquals(0, sq.remainingCapacity());
    }

    /** timed offer times out if full and elements not taken */
    @Test
    public void testTimedOffer() {
        final LinkedBlockingMultiQueue<QueueKey, Object> q = createSingleQueue(2);
        final LinkedBlockingMultiQueue<QueueKey, Object>.SubQueue sq = q.getSubQueue(QueueKey.A);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                sq.put(new Object());
                sq.put(new Object());
                long startTime = System.nanoTime();
                assertFalse(sq.offer(new Object(), timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
                pleaseInterrupt.countDown();
                try {
                    sq.offer(new Object(), 2 * LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
            }
        });
        await(pleaseInterrupt);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /** take retrieves elements in FIFO order */
    @Test
    public void testTake() throws InterruptedException {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.take().intValue());
        }
    }

    @Test
    public void testTakeMulti() throws InterruptedException {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedMultiQueue();
        for (int i = 0; i < nine; ++i) {
            assertEquals(i, q.take().intValue());
        }
    }

    @Test
    public void testTakeMultiDisabled() throws InterruptedException {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedMultiQueue();
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qa = q.getSubQueue(QueueKey.A);
        qa.enable(false);
        for (int i = three; i < nine; ++i) {
            assertEquals(i, q.take().intValue());
        }
        qa.enable(true);
        for (int i = 0; i < three; ++i) {
            assertEquals(i, q.take().intValue());
        }
    }

    /** Take removes existing elements until empty, then blocks interruptibly */
    @Test
    public void testBlockingTake() throws InterruptedException {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i) {
                    assertEquals(i, q.take().intValue());
                }
                Thread.currentThread().interrupt();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
                assertFalse(Thread.interrupted());
                pleaseInterrupt.countDown();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {
                    // expected
                }
                assertFalse(Thread.interrupted());
            }
        });
        await(pleaseInterrupt);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /** poll succeeds unless empty */
    @Test
    public void testPoll() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.poll().intValue());
        }
        assertNull(q.poll());
    }

    @Test
    public void testPollMulti() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedMultiQueue();
        for (int i = 0; i < nine; ++i) {
            assertEquals(i, q.poll().intValue());
        }
        assertNull(q.poll());
    }

    @Test
    public void testPollMultiDisabled() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedMultiQueue();
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qa = q.getSubQueue(QueueKey.A);
        qa.enable(false);
        for (int i = three; i < nine; ++i) {
            assertEquals(i, q.poll().intValue());
        }
        assertNull(q.poll());
        qa.enable(true);
        for (int i = 0; i < three; ++i) {
            assertEquals(i, q.poll().intValue());
        }
        assertNull(q.poll());
    }

    /** timed poll with zero timeout succeeds when non-empty, else times out */
    @Test
    public void testTimedPoll0() throws InterruptedException {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.poll(0, MILLISECONDS).intValue());
        }
        assertNull(q.poll(0, MILLISECONDS));
    }

    /** timed poll with nonzero timeout succeeds when non-empty, else times out */
    @Test
    public void testTimedPoll() throws InterruptedException {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            long startTime = System.nanoTime();
            assertEquals(i, (int) q.poll(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
        long startTime = System.nanoTime();
        assertNull(q.poll(timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        checkEmpty(q);
    }

    /** Interrupted timed poll throws InterruptedException instead of returning timeout status */
    @Test
    public void testInterruptedTimedPoll() throws InterruptedException {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        final CountDownLatch aboutToWait = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i) {
                    long t0 = System.nanoTime();
                    assertEquals(i, (int) q.poll(LONG_DELAY_MS, MILLISECONDS));
                    assertTrue(millisElapsedSince(t0) < SMALL_DELAY_MS);
                }
                long t0 = System.nanoTime();
                aboutToWait.countDown();
                try {
                    q.poll(MEDIUM_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {
                    assertTrue(millisElapsedSince(t0) < MEDIUM_DELAY_MS);
                }
            }
        });
        aboutToWait.await();
        waitForThreadToEnterWaitState(t, SMALL_DELAY_MS);
        t.interrupt();
        awaitTermination(t, MEDIUM_DELAY_MS);
        checkEmpty(q);
    }

    /** peek returns next element, or null if empty */
    @Test
    public void testPeek() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.peek().intValue());
            assertEquals(i, q.poll().intValue());
            assertTrue(q.peek() == null || !q.peek().equals(i));
        }
        assertNull(q.peek());
    }

    /** peek returns next element, or null if empty */
    @Test
    public void testPeekMulti() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedMultiQueue();
        for (int i = 0; i < nine; ++i) {
            assertEquals(i, q.peek().intValue());
            assertEquals(i, q.poll().intValue());
            assertTrue(q.peek() == null || !q.peek().equals(i));
        }
        assertNull(q.peek());
    }

    /** peek returns next element, or null if empty */
    @Test
    public void testPeekMultiDisabled() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedMultiQueue();
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qa = q.getSubQueue(QueueKey.A);
        qa.enable(false);
        for (int i = three; i < nine; ++i) {
            assertEquals(i, q.peek().intValue());
            assertEquals(i, q.poll().intValue());
            assertTrue(q.peek() == null || !q.peek().equals(i));
        }
        assertNull(q.peek());
        qa.enable(true);
        for (int i = 0; i < three; ++i) {
            assertEquals(i, q.peek().intValue());
            assertEquals(i, q.poll().intValue());
            assertTrue(q.peek() == null || !q.peek().equals(i));
        }
        assertNull(q.peek());
    }

    /** element returns next element, or throws NSEE if empty */
    @Test
    public void testElement() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.element().intValue());
            assertEquals(i, q.poll().intValue());
        }
        assertThrows(NoSuchElementException.class, () -> q.element());
    }

    /** element returns next element, or throws NSEE if empty */
    @Test
    public void testElementMulti() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedMultiQueue();
        for (int i = 0; i < nine; ++i) {
            assertEquals(i, q.element().intValue());
            assertEquals(i, q.poll().intValue());
        }
        assertThrows(NoSuchElementException.class, () -> q.element());
    }

    /** element returns next element, or throws NSEE if empty */
    @Test
    public void testElementMultiDisabled() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedMultiQueue();
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qa = q.getSubQueue(QueueKey.A);
        qa.enable(false);
        for (int i = three; i < nine; ++i) {
            assertEquals(i, q.element().intValue());
            assertEquals(i, q.poll().intValue());
        }
        try {
            q.element();
            shouldThrow();
        } catch (NoSuchElementException success) {
            // expected
        }
        qa.enable(true);
        for (int i = 0; i < three; ++i) {
            assertEquals(i, q.element().intValue());
            assertEquals(i, q.poll().intValue());
        }
        try {
            q.element();
            shouldThrow();
        } catch (NoSuchElementException success) {
            // expected
        }
    }

    /** removes next element, or throws NSEE if empty */
    @Test
    public void testRemove() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.remove().intValue());
        }
        assertThrows(NoSuchElementException.class, () -> q.remove());
    }

    /** removes next element, or throws NSEE if empty */
    @Test
    public void testRemoveMulti() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedMultiQueue();
        for (int i = 0; i < nine; ++i) {
            assertEquals(i, q.remove().intValue());
        }
        assertThrows(NoSuchElementException.class, () -> q.remove());
    }

    /** An add following remove(x) succeeds */
    @Test
    public void testRemoveElementAndAdd() throws InterruptedException {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertTrue(sq.add(1));
        assertTrue(sq.add(2));
        assertTrue(sq.remove(1));
        assertTrue(sq.remove(2));
        assertTrue(sq.add(3));
        assertNotNull(q.take());
    }

    /** contains(x) reports true when elements added but not yet removed */
    @Test
    public void testContains() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(sq.contains(i));
            q.poll();
            assertFalse(sq.contains(i));
        }
    }

    /** clear removes all elements */
    @Test
    public void testClear() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        sq.clear();
        assertTrue(q.isEmpty());
        assertTrue(sq.isEmpty());
        assertEquals(0, q.totalSize());
        assertEquals(0, sq.size());
        assertEquals(SIZE, sq.remainingCapacity());
        sq.add(one);
        assertFalse(q.isEmpty());
        assertFalse(sq.isEmpty());
        assertTrue(sq.contains(one));
        sq.clear();
        assertTrue(q.isEmpty());
        assertTrue(sq.isEmpty());
    }

    /** containsAll(c) is true when c contains a subset of elements */
    @Test
    public void testContainsAll() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q1 = populatedSingleQueue(SIZE);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq1 = q1.getSubQueue(QueueKey.A);
        final LinkedBlockingMultiQueue<QueueKey, Integer> q2 = createSingleQueue();
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq2 = q2.getSubQueue(QueueKey.A);
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(sq1.containsAll(sq2));
            assertFalse(sq2.containsAll(sq1));
            sq2.add(i);
        }
        assertTrue(sq2.containsAll(sq1));
    }

    /** retainAll(c) retains only those elements of c and reports true if changed */
    @Test
    public void testRetainAll() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q1 = populatedSingleQueue(SIZE);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq1 = q1.getSubQueue(QueueKey.A);
        final LinkedBlockingMultiQueue<QueueKey, Integer> q2 = populatedSingleQueue(SIZE);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq2 = q2.getSubQueue(QueueKey.A);
        for (int i = 0; i < SIZE; ++i) {
            boolean changed = sq1.retainAll(sq2);
            if (i == 0) {
                assertFalse(changed);
            } else {
                assertTrue(changed);
            }
            assertTrue(sq1.containsAll(sq2));
            assertEquals(SIZE - i, q1.totalSize());
            assertEquals(SIZE - i, sq1.size());
            q2.remove();
        }
    }

    /** removeAll(c) removes only those elements of c and reports true if changed */
    @Test
    public void testRemoveAll() {
        for (int i = 1; i < SIZE; ++i) {
            final LinkedBlockingMultiQueue<QueueKey, Integer> q1 = populatedSingleQueue(SIZE);
            final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq1 = q1.getSubQueue(QueueKey.A);
            final LinkedBlockingMultiQueue<QueueKey, Integer> q2 = populatedSingleQueue(i);
            final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq2 = q2.getSubQueue(QueueKey.A);
            assertTrue(sq1.removeAll(sq2));
            assertEquals(SIZE - i, q1.totalSize());
            assertEquals(SIZE - i, sq1.size());
            for (int j = 0; j < i; ++j) {
                Integer x = q2.remove();
                assertFalse(sq1.contains(x));
            }
        }
    }

    /** toArray contains all elements in FIFO order */
    @Test
    public void testToArray() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        Object[] o = sq.toArray();
        for (int i = 0; i < o.length; i++) {
            assertSame(o[i], q.poll());
        }
    }

    /** toArray(a) contains all elements in FIFO order */
    @Test
    public void testToArray2() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        Integer[] ints = new Integer[SIZE];
        Integer[] array = sq.toArray(ints);
        assertSame(ints, array);
        for (int i = 0; i < ints.length; i++) {
            assertSame(ints[i], q.poll());
        }
    }

    /** toArray(incompatible array type) throws ArrayStoreException */
    @Test
    public void testToArray1_BadArg() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        assertThrows(ArrayStoreException.class, () -> sq.toArray(new String[10]));
    }

    /** iterator iterates through all elements */
    @Test
    public void testIterator() throws InterruptedException {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        Iterator<Integer> it = sq.iterator();
        int i;
        for (i = 0; it.hasNext(); i++) {
            assertTrue(sq.contains(it.next()));
        }
        assertEquals(i, SIZE);
        assertIteratorExhausted(it);

        it = sq.iterator();
        for (i = 0; it.hasNext(); i++) {
            assertEquals(it.next(), q.take());
        }
        assertEquals(i, SIZE);
        assertIteratorExhausted(it);
    }

    /** iterator of empty collection has no elements */
    @Test
    public void testEmptyIterator() {
        assertIteratorExhausted(createSingleQueue().getSubQueue(QueueKey.A).iterator());
    }

    /** iterator.remove removes current element */
    @Test
    public void testIteratorRemove() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(3);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        sq.add(two);
        sq.add(one);
        sq.add(three);

        Iterator<Integer> it = sq.iterator();
        it.next();
        it.remove();

        it = sq.iterator();
        assertSame(it.next(), one);
        assertSame(it.next(), three);
        assertFalse(it.hasNext());
    }

    /** iterator ordering is FIFO */
    @Test
    public void testIteratorOrdering() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(3);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        sq.add(one);
        sq.add(two);
        sq.add(three);
        assertEquals(0, sq.remainingCapacity());
        int k = 0;
        for (int value : sq) {
            assertEquals(++k, value);
        }
        assertEquals(3, k);
    }

    /** Modifications do not cause iterators to fail */
    @Test
    public void testWeaklyConsistentIteration() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(3);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        sq.add(one);
        sq.add(two);
        sq.add(three);
        for (int ignored : sq) {
            q.remove();
        }
        assertEquals(0, sq.size());
        assertEquals(0, q.totalSize());
    }

    /** toString contains toStrings of elements */
    @Test
    public void testToString() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        String s = sq.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /** offer transfers elements across Executor tasks */
    @Test
    public void testOfferInExecutor() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(2);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        sq.add(one);
        sq.add(two);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertFalse(sq.offer(three));
                threadsStarted.await();
                assertTrue(sq.offer(three, LONG_DELAY_MS, MILLISECONDS));
                assertEquals(0, sq.remainingCapacity());
            }
        });
        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                threadsStarted.await();
                assertSame(one, q.take());
            }
        });
        joinPool(executor);
    }

    /** timed poll retrieves elements across Executor threads */
    @Test
    public void testPollInExecutor() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue(2);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertNull(q.poll());
                threadsStarted.await();
                assertSame(one, q.poll(LONG_DELAY_MS, MILLISECONDS));
                checkEmpty(q);
            }
        });
        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                threadsStarted.await();
                sq.put(one);
            }
        });
        joinPool(executor);
    }

    /** drainTo(c) empties queue into another collection c */
    @Test
    public void testDrainTo() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        ArrayList<Integer> l = new ArrayList<>();
        q.drainTo(l);
        assertEquals(0, q.totalSize());
        assertEquals(0, sq.size());
        assertEquals(SIZE, l.size());
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(l.get(i), Integer.valueOf(i));
        }
        sq.add(zero);
        sq.add(one);
        assertFalse(q.isEmpty());
        assertTrue(sq.contains(zero));
        assertTrue(sq.contains(one));
        l.clear();
        q.drainTo(l);
        assertEquals(0, q.totalSize());
        assertEquals(0, sq.size());
        assertEquals(2, l.size());
        for (int i = 0; i < 2; ++i) {
            assertEquals(l.get(i), Integer.valueOf(i));
        }
    }

    /** drainTo(c) empties queue into another collection c */
    @Test
    public void testDrainToMulti() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedMultiQueue();
        ArrayList<Integer> l = new ArrayList<>();
        q.drainTo(l);
        assertEquals(0, q.totalSize());
        assertEquals(nine.intValue(), l.size());
        for (int i = 0; i < nine; ++i) {
            assertEquals(l.get(i), Integer.valueOf(i));
        }
    }

    /** drainTo(c) empties queue into another collection c */
    @Test
    public void testDrainToMultiDisabled() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedMultiQueue();
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qa = q.getSubQueue(QueueKey.A);
        ArrayList<Integer> l1 = new ArrayList<>();
        qa.enable(false);
        q.drainTo(l1);
        assertEquals(0, q.totalSize());
        assertEquals(six.intValue(), l1.size());
        for (int i = three; i < nine; ++i) {
            assertEquals(l1.get(i - three), Integer.valueOf(i));
        }
        qa.enable(true);
        assertEquals(3, q.totalSize());
        ArrayList<Integer> l2 = new ArrayList<>();
        q.drainTo(l2);
        assertEquals(0, q.totalSize());
        assertEquals(three.intValue(), l2.size());
        for (int i = 0; i < three; ++i) {
            assertEquals(l2.get(i), Integer.valueOf(i));
        }
    }

    /** drainTo empties full queue, unblocking a waiting put. */
    @Test
    public void testDrainToWithActivePut() throws InterruptedException {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = populatedSingleQueue(SIZE);
        LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                sq.put(SIZE + 1);
            }
        });
        t.start();
        ArrayList<Integer> l = new ArrayList<>();
        q.drainTo(l);
        assertTrue(l.size() >= SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(l.get(i), Integer.valueOf(i));
        }
        t.join();
        assertTrue(q.totalSize() + l.size() >= SIZE);
        assertTrue(sq.size() + l.size() >= SIZE);
    }

    /** drainTo(c, n) empties first min(n, size) elements of queue into c */
    @Test
    public void testDrainToN() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q = createSingleQueue();
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq = q.getSubQueue(QueueKey.A);
        for (int i = 0; i < SIZE + 2; ++i) {
            for (int j = 0; j < SIZE; j++) {
                assertTrue(sq.offer(j));
            }
            ArrayList<Integer> l = new ArrayList<>();
            q.drainTo(l, i);
            int k = Math.min(i, SIZE);
            assertEquals(k, l.size());
            assertEquals(SIZE - k, q.totalSize());
            assertEquals(SIZE - k, sq.size());
            for (int j = 0; j < k; ++j) {
                assertEquals(l.get(j), Integer.valueOf(j));
            }
            do {
                // empty
            } while (q.poll() != null);
        }
    }

    /** remove(null), contains(null) always return false */
    @Test
    public void testNeverContainsNull() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> q1 = createSingleQueue();
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq1 = q1.getSubQueue(QueueKey.A);
        assertFalse(sq1.contains(null));
        assertFalse(sq1.remove(null));
        final LinkedBlockingMultiQueue<QueueKey, Integer> q2 = populatedSingleQueue(2);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue sq2 = q2.getSubQueue(QueueKey.A);
        assertFalse(sq2.contains(null));
        assertFalse(sq2.remove(null));
    }

    @Test
    public void testAddRemoveSubQueues() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = new LinkedBlockingMultiQueue<>();
        assertEquals(0, q.totalSize());
        q.addSubQueue(QueueKey.A, 1 /* priority */);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qa = q.getSubQueue(QueueKey.A);
        qa.offer(one);
        assertEquals(1, q.totalSize());
        q.removeSubQueue(QueueKey.A);
        assertEquals(0, q.totalSize());
        q.addSubQueue(QueueKey.B, 1 /* priority */);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue qb = q.getSubQueue(QueueKey.B);
        qb.offer(one);
        assertEquals(1, q.totalSize());
        qb.enable(false);
        assertEquals(0, q.totalSize());
        assertEquals(qb, q.removeSubQueue(QueueKey.B));
        assertEquals(0, q.totalSize());
    }

    @Test
    public void testRemoveQueueOfTheSamePriorityDecrementsNextSubQueuePointer() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = new LinkedBlockingMultiQueue<>();
        q.addSubQueue(QueueKey.A, 0);
        q.addSubQueue(QueueKey.B, 0);

        q.getSubQueue(QueueKey.A).offer(1);
        q.getSubQueue(QueueKey.B).offer(2);

        Integer firstPoll = q.poll();
        q.removeSubQueue(QueueKey.A);
        Integer secondPoll = q.poll();

        assertEquals(1, (long) firstPoll);
        assertEquals(2, (long) secondPoll);
    }

    @Test
    public void testThatPriorityGroupIsRemovedWhenItDoesNotContainAnySubQueue() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = new LinkedBlockingMultiQueue<>();
        q.addSubQueue(QueueKey.A, 0);
        q.addSubQueue(QueueKey.B, 1);

        q.getSubQueue(QueueKey.B).offer(2);

        q.removeSubQueue(QueueKey.A);

        Integer poll = q.poll();

        assertEquals(2, (long) poll);
    }

    @Test
    public void testAddPrioritySubQueueWhenLowerPriorityQueueExists() {
        LinkedBlockingMultiQueue<QueueKey, Integer> q = new LinkedBlockingMultiQueue<>();
        q.addSubQueue(QueueKey.A, 1);
        q.addSubQueue(QueueKey.B, 0);
        assertEquals(2, q.getPriorityGroupsCount());
    }

    /** {@link lbmq.LinkedBlockingMultiQueue.SubQueue#enable(boolean)} must be idempotent */
    @Test
    public void testThatTotalSizeMustNotChangeEnablingAlreadyEnabledQueues() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> subject = new LinkedBlockingMultiQueue<>();
        subject.addSubQueue(QueueKey.A, 0);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue subQueue = subject.getSubQueue(QueueKey.A);
        subQueue.add(100);

        assertEquals(1, subQueue.size());
        assertEquals(1, subject.totalSize());

        subQueue.enable(true);

        assertEquals(1, subQueue.size());
        assertEquals(1, subject.totalSize());
    }

    /** {@link lbmq.LinkedBlockingMultiQueue.SubQueue#enable(boolean)} must be idempotent */
    @Test
    public void testThatTotalSizeMustNotChangeDisablingAlreadyDisabledQueues() {
        final LinkedBlockingMultiQueue<QueueKey, Integer> subject = new LinkedBlockingMultiQueue<>();
        subject.addSubQueue(QueueKey.A, 0);
        final LinkedBlockingMultiQueue<QueueKey, Integer>.SubQueue subQueue = subject.getSubQueue(QueueKey.A);
        subQueue.add(100);

        assertTrue(subQueue.isEnabled());
        assertEquals(1, subQueue.size());
        assertEquals(1, subject.totalSize());

        subQueue.enable(false);

        assertEquals(1, subQueue.size());
        assertEquals(0, subject.totalSize());

        subQueue.enable(false);

        assertEquals(1, subQueue.size());
        assertEquals(0, subject.totalSize());
    }

    void checkEmpty(LinkedBlockingMultiQueue<?, ?> q) {
        try {
            assertTrue(q.isEmpty());
            assertEquals(0, q.totalSize());

            assertNull(q.peek());

            assertNull(q.poll());
            assertNull(q.poll(0, MILLISECONDS));

            try {
                q.element();
                shouldThrow();
            } catch (NoSuchElementException success) {
                // expected
            }
            try {
                q.remove();
                shouldThrow();
            } catch (NoSuchElementException success) {
                // expected
            }
        } catch (InterruptedException fail) {
            threadUnexpectedException(fail);
        }
    }

    void checkEmpty(LinkedBlockingMultiQueue<?, ?>.SubQueue q) {
        assertTrue(q.isEmpty());
        assertEquals(0, q.size());

        assertEquals(q.toString(), "[]");

        assertArrayEquals(q.toArray(), new Object[0]);
        assertFalse(q.iterator().hasNext());

        try {
            q.iterator().next();
            shouldThrow();
        } catch (NoSuchElementException success) {
            // expected
        }
    }
}
