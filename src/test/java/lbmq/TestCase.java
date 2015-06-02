/*
 * Derived from work made by Doug Lea with assistance from members of JCP JSR-166 Expert Group
 * (https://jcp.org/en/jsr/detail?id=166). The original work is in the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package lbmq;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;

import junit.framework.AssertionFailedError;

/**
 * Base class for the LinkedBlockingMultiQueue tests. Defines some constants, utility methods and classes, as well as a
 * simple framework for helping to make sure that assertions failing in generated threads cause the associated test that
 * generated them to itself fail (which JUnit does not otherwise arrange). The rules for creating such tests are:
 *
 * <ol>
 *
 * <li>All delays and timeouts must use one of the constants {@code SHORT_DELAY_MS}, {@code SMALL_DELAY_MS},
 * {@code MEDIUM_DELAY_MS}, {@code LONG_DELAY_MS}. The idea here is that a SHORT is always discriminable from zero time,
 * and always allows enough time for the small amounts of computation (creating a thread, calling a few methods, etc)
 * needed to reach a timeout point. Similarly, a SMALL is always discriminable as larger than SHORT and smaller than
 * MEDIUM. And so on. These constants are set to conservative values, but even so, if there is ever any doubt, they can
 * all be increased in one spot to rerun tests on slower platforms.</li>
 *
 * <li>All threads generated must be joined inside each test case method (or {@code fail} to do so) before returning
 * from the method. The {@code joinPool} method can be used to do this when using Executors.</li>
 *
 * </ol>
 *
 * <p>
 * <b>Other notes</b>
 * <ul>
 *
 * <li>These tests are "conformance tests", and do not attempt to test throughput, latency, scalability or other
 * performance factors (see the separate "jtreg" tests for a set intended to check these for the most central aspects of
 * functionality.) So, most tests use the smallest sensible numbers of threads, collection sizes, etc needed to check
 * basic conformance.</li>
 *
 * </ul>
 */
public class TestCase {

    // Delays for timing-dependent tests, in milliseconds.

    public static long SHORT_DELAY_MS = 50;
    public static long SMALL_DELAY_MS = SHORT_DELAY_MS * 5;
    public static long MEDIUM_DELAY_MS = SHORT_DELAY_MS * 10;
    public static long LONG_DELAY_MS = SHORT_DELAY_MS * 200;

    /**
     * Returns a timeout in milliseconds to be used in tests that verify that operations block or time out.
     */
    long timeoutMillis() {
        return SHORT_DELAY_MS / 4;
    }

    /**
     * The first exception encountered if any threadAssertXXX method fails.
     */
    private final AtomicReference<Throwable> threadFailure = new AtomicReference<Throwable>(null);

    /**
     * Records an exception so that it can be rethrown later in the test harness thread, triggering a test case failure.
     * Only the first failure is recorded; subsequent calls to this method from within the same test have no effect.
     */
    public void threadRecordFailure(Throwable t) {
        threadFailure.compareAndSet(null, t);
    }

    /**
     * Extra checks that get done for all test cases.
     *
     * Triggers test case failure if any thread assertions have failed, by rethrowing, in the test harness thread, any
     * exception recorded earlier by threadRecordFailure.
     *
     * Triggers test case failure if interrupt status is set in the main thread.
     */
    @After
    public void tearDown() throws Exception {
        Throwable t = threadFailure.getAndSet(null);
        if (t != null) {
            if (t instanceof Error)
                throw (Error) t;
            else if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            else if (t instanceof Exception)
                throw (Exception) t;
            else {
                AssertionFailedError afe = new AssertionFailedError(t.toString());
                afe.initCause(t);
                throw afe;
            }
        }
        if (Thread.interrupted())
            throw new AssertionFailedError("interrupt status set in main thread");
    }

    /**
     * Just like assertTrue(b), but additionally recording (using threadRecordFailure) any AssertionFailedError thrown,
     * so that the current testcase will fail.
     */
    public void threadAssertTrue(boolean b) {
        try {
            assertTrue(b);
        } catch (AssertionFailedError t) {
            threadRecordFailure(t);
            throw t;
        }
    }

    /**
     * Records the given exception using {@link #threadRecordFailure}, then rethrows the exception, wrapping it in an
     * AssertionFailedError if necessary.
     */
    public void threadUnexpectedException(Throwable t) {
        threadRecordFailure(t);
        t.printStackTrace();
        if (t instanceof RuntimeException)
            throw (RuntimeException) t;
        else if (t instanceof Error)
            throw (Error) t;
        else {
            AssertionFailedError afe = new AssertionFailedError("unexpected exception: " + t);
            afe.initCause(t);
            throw afe;
        }
    }

    /**
     * Delays, via Thread.sleep, for the given millisecond delay, but if the sleep is shorter than specified, may
     * re-sleep or yield until time elapses.
     */
    static void delay(long millis) throws InterruptedException {
        long startTime = System.nanoTime();
        long ns = millis * 1000 * 1000;
        for (;;) {
            if (millis > 0L)
                Thread.sleep(millis);
            else
                // too short to sleep
                Thread.yield();
            long d = ns - (System.nanoTime() - startTime);
            if (d > 0L)
                millis = d / (1000 * 1000);
            else
                break;
        }
    }

    /**
     * Waits out termination of a thread pool or fails doing so.
     */
    void joinPool(ExecutorService exec) {
        try {
            exec.shutdown();
            if (!exec.awaitTermination(2 * LONG_DELAY_MS, MILLISECONDS))
                fail("ExecutorService " + exec + " did not terminate in a timely manner");
        } catch (SecurityException ok) {
            // Allowed in case test doesn't have privs
        } catch (InterruptedException fail) {
            fail("Unexpected InterruptedException");
        }
    }

    /**
     * Checks that thread does not terminate within the default millisecond delay of {@code timeoutMillis()}.
     */
    void assertThreadStaysAlive(Thread thread) {
        assertThreadStaysAlive(thread, timeoutMillis());
    }

    /**
     * Checks that thread does not terminate within the given millisecond delay.
     */
    void assertThreadStaysAlive(Thread thread, long millis) {
        try {
            // No need to optimize the failing case via Thread.join.
            delay(millis);
            assertTrue(thread.isAlive());
        } catch (InterruptedException fail) {
            fail("Unexpected InterruptedException");
        }
    }

    /**
     * Fails with message "should throw exception".
     */
    public void shouldThrow() {
        fail("Should throw exception");
    }

    /**
     * Fails with message "should throw " + exceptionName.
     */
    public void shouldThrow(String exceptionName) {
        fail("Should throw " + exceptionName);
    }

    /**
     * The number of elements to place in collections, arrays, etc.
     */
    public static final int SIZE = 20;

    // Some convenient Integer constants

    public static final Integer zero = new Integer(0);
    public static final Integer one = new Integer(1);
    public static final Integer two = new Integer(2);
    public static final Integer three = new Integer(3);
    public static final Integer four = new Integer(4);
    public static final Integer five = new Integer(5);
    public static final Integer six = new Integer(6);
    public static final Integer seven = new Integer(7);
    public static final Integer eight = new Integer(8);
    public static final Integer nine = new Integer(9);

    /**
     * Sleeps until the given time has elapsed. Throws AssertionFailedError if interrupted.
     */
    void sleep(long millis) {
        try {
            delay(millis);
        } catch (InterruptedException fail) {
            AssertionFailedError afe = new AssertionFailedError("Unexpected InterruptedException");
            afe.initCause(fail);
            throw afe;
        }
    }

    /**
     * Spin-waits up to the specified number of milliseconds for the given thread to enter a wait state: BLOCKED,
     * WAITING, or TIMED_WAITING.
     */
    void waitForThreadToEnterWaitState(Thread thread, long timeoutMillis) {
        long startTime = System.nanoTime();
        for (;;) {
            Thread.State s = thread.getState();
            if (s == Thread.State.BLOCKED || s == Thread.State.WAITING || s == Thread.State.TIMED_WAITING)
                return;
            else if (s == Thread.State.TERMINATED)
                fail("Unexpected thread termination");
            else if (millisElapsedSince(startTime) > timeoutMillis) {
                threadAssertTrue(thread.isAlive());
                return;
            }
            Thread.yield();
        }
    }

    /**
     * Returns the number of milliseconds since time given by startNanoTime, which must have been previously returned
     * from a call to {@link System#nanoTime()}.
     */
    public static long millisElapsedSince(long startNanoTime) {
        return NANOSECONDS.toMillis(System.nanoTime() - startNanoTime);
    }

    /**
     * Returns a new started daemon Thread running the given runnable.
     */
    public Thread newStartedThread(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * Waits for the specified time (in milliseconds) for the thread to terminate (using {@link Thread#join(long)}),
     * else interrupts the thread (in the hope that it may terminate later) and fails.
     */
    void awaitTermination(Thread t, long timeoutMillis) {
        try {
            t.join(timeoutMillis);
        } catch (InterruptedException fail) {
            threadUnexpectedException(fail);
        } finally {
            if (t.getState() != Thread.State.TERMINATED) {
                t.interrupt();
                fail("Test timed out");
            }
        }
    }

    /**
     * Waits for LONG_DELAY_MS milliseconds for the thread to terminate (using {@link Thread#join(long)}), else
     * interrupts the thread (in the hope that it may terminate later) and fails.
     */
    void awaitTermination(Thread t) {
        awaitTermination(t, LONG_DELAY_MS);
    }

    // Some convenient Runnable classes

    public abstract class CheckedRunnable implements Runnable {
        protected abstract void realRun() throws Throwable;

        public final void run() {
            try {
                realRun();
            } catch (Throwable fail) {
                threadUnexpectedException(fail);
            }
        }
    }

    public void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(LONG_DELAY_MS, MILLISECONDS));
        } catch (Throwable fail) {
            threadUnexpectedException(fail);
        }
    }

    /**
     * A CyclicBarrier that uses timed await and fails with AssertionFailedErrors instead of throwing checked
     * exceptions.
     */
    public class CheckedBarrier extends CyclicBarrier {
        public CheckedBarrier(int parties) {
            super(parties);
        }

        public int await() {
            try {
                return super.await(2 * LONG_DELAY_MS, MILLISECONDS);
            } catch (TimeoutException timedOut) {
                throw new AssertionFailedError("timed out");
            } catch (Exception fail) {
                AssertionFailedError afe = new AssertionFailedError("Unexpected exception: " + fail);
                afe.initCause(fail);
                throw afe;
            }
        }
    }

    void assertSerialEquals(Object x, Object y) {
        assertTrue(Arrays.equals(serialBytes(x), serialBytes(y)));
    }

    void assertNotSerialEquals(Object x, Object y) {
        assertFalse(Arrays.equals(serialBytes(x), serialBytes(y)));
    }

    byte[] serialBytes(Object o) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(o);
            oos.flush();
            oos.close();
            return bos.toByteArray();
        } catch (Throwable fail) {
            threadUnexpectedException(fail);
            return new byte[0];
        }
    }

    @SuppressWarnings("unchecked")
    <T> T serialClone(T o) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialBytes(o)));
            T clone = (T) ois.readObject();
            assertSame(o.getClass(), clone.getClass());
            return clone;
        } catch (Throwable fail) {
            threadUnexpectedException(fail);
            return null;
        }
    }

    public void assertIteratorExhausted(Iterator<?> it) {
        try {
            it.next();
            shouldThrow();
        } catch (NoSuchElementException success) {
        }
        assertFalse(it.hasNext());
    }

}