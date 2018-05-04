// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase1;

import nachos.test.NachosKernelTestsSuite;
import nachos.threads.Condition;
import nachos.threads.InterruptsCondition;
import nachos.threads.KThread;
import nachos.threads.Lock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for condition variable implementation.
 */
public class Phase1Task2ConditionVariableTests extends NachosKernelTestsSuite {
    private final String expectedLoggerOutput =
            "Incrementing:\n" +
                    "1, 2, 3, 4, 5, \n" +
                    "Decrementing:\n" +
                    "4, 3, \n" +
                    "Decrementing:\n" +
                    "2, 1, \n" +
                    "Incrementing:\n" +
                    "2, 3, 4, 5, 6, \n" +
                    "Decrementing:\n" +
                    "5, 4, \n" +
                    "Decrementing:\n" +
                    "3, 2, \n" +
                    "Incrementing:\n" +
                    "3, 4, 5, 6, 7, \n" +
                    "Decrementing:\n" +
                    "6, 5, \n" +
                    "Decrementing:\n" +
                    "4, 3, \n";
    private final StringBuilder logger = new StringBuilder();
    private int value;

    public Phase1Task2ConditionVariableTests() {
        super("phase1/phase1.round.robin.conf");
    }

    /**
     * Tests if calling of wakeAll() wakes correctly all threads
     * sleeping on condition variable.
     */
    @Test
    public void testConditionWakeAll() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                value = 0;

                final Lock lock = new Lock();
                final Condition condition = new InterruptsCondition(lock);
                // Create 10 threads and store them in ArrayList
                List<KThread> threads = new ArrayList<>(10);
                for ( int i = 0; i < 10; i++ ) {
                    value++;
                    threads.add(new KThread(new Runnable() {
                        @Override
                        public void run() {
                            // Each thread will just go to sleep on same condition instance
                            lock.acquire();
                            condition.sleep();
                            // If all threads are correctly woken up, they should
                            // decrement our helper value variable and finish
                            // their execution. Decrementing is protected with condition
                            // lock so there is no possibility for multiple threads to
                            // corrupt state of our value.
                            value--;
                            lock.release();
                        }
                    }).setName("thread" + i));
                }
                // Fork all threads
                for ( KThread t : threads ) {
                    t.fork();
                }
                // Yield from main thread to begin every thread execution
                // Because we are using RoundRobinScheduler it is guaranteed, that
                // all threads get called before calling main thread again
                KThread.yield();
                // Wake all threads sleeping on condition variable while holding lock
                lock.acquire();
                condition.wakeAll();
                lock.release();
                // Continue yielding main thread until all threads finish their work
                while ( value != 0 ) {
                    KThread.yield();
                }
                for ( KThread thread : threads ) {
                    threadAssertEquals(KThread.Status.Finished, privilegedGetInstanceFieldDeepClone(thread, KThread.Status.class, "status"));
                }
            }
        });
    }

    /**
     * Tests if condition variable implementation works by using condition variable
     * to synchronize incrementing and decrementing single instance of counter using
     * multiple threads. At the end expects correct order of operations called on
     * counter. Test solely relies on round robin scheduler when testing logger output.
     *
     * @see nachos.threads.RoundRobinScheduler
     */
    @Test
    public void testCounterSynchronizationUsingCondition() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                final Counter counter = new Counter(0);
                final Lock lock = new Lock();
                final Condition condition = new InterruptsCondition(lock);

                // Create three threads. One will be incrementing our counter
                // and the other two will be decrementing it
                KThread incrementingThread = new KThread(
                        new IncrementingRunnable(counter, lock, condition)
                ).setName("incrementing thread");
                KThread decrementingThread1 = new KThread(
                        new DecrementingRunnable(counter, lock, condition)
                ).setName("decrementing thread 1");
                KThread decrementingThread2 = new KThread(
                        new DecrementingRunnable(counter, lock, condition)
                ).setName("decrementing thread 2");

                // This is our initialization phase.
                // Here we will fork all three threads in the correct order.
                // We rely solely on the RoundRobinScheduler used in this test config
                // which will start threads in order we've forked them.
                // All three threads will go to sleep on our condition variable again
                // in correct order.
                incrementingThread.fork();
                decrementingThread1.fork();
                decrementingThread2.fork();

                // Yield from main thread in the favor of incrementing thread
                KThread.yield();

                // After all threads went to sleep on condition variable
                // wake first thread (incrementing thread) while holding lock
                lock.acquire();
                // Waking incrementing thread here is last step of our initialization thread.
                // From here onwards, threads should synchronize theirselves.
                condition.wake();
                lock.release();

                // Let main thread wait for all threads to finish. We could've used join() here
                // but we do not want our test to depend on it's correct implementation.
                while ( !areAllThreadsFinished(incrementingThread, decrementingThread1, decrementingThread2) ) {
                    KThread.yield();
                }
                threadAssertEquals(expectedLoggerOutput, logger.toString());
            }
        });
    }

    /**
     * Simple counter object.
     */
    private class Counter {
        private int value;

        private Counter(int value) {
            this.value = value;
        }

        private void increment() {
            value++;
        }

        private void decrement() {
            value--;
        }

        private int getValue() {
            return value;
        }
    }

    /**
     * Incrementing runnable executed by thread, that increments counter.
     */
    private class IncrementingRunnable implements Runnable {
        private final Counter counter;
        private final Lock lock;
        private final Condition condition;

        private IncrementingRunnable(final Counter counter,
                                     final Lock lock,
                                     final Condition condition) {
            this.counter = counter;
            this.condition = condition;
            this.lock = lock;
        }

        @Override
        public void run() {
            // Acquire lock before calling any operations on condition variable
            lock.acquire();
            // This is part of the initialization phase
            condition.sleep();
            // We will make 3 computation cycles. After each incrementation cycle
            // there will be two decrementation cycles
            for ( int i = 0; i < 3; i++ ) {
                logger.append("Incrementing:\n");
                // Increment counter 5 times
                for ( int j = 0; j < 5; j++ ) {
                    counter.increment();
                    logger.append(counter.getValue()).append(", ");
                    // Yield current thread, just to test that even after calling
                    // yield, this thread gets planned again
                    KThread.yield();
                }
                logger.append("\n");
                // Wake decrementing thread sleeping on condition variable
                // and go to sleep
                condition.wake();
                condition.sleep();
            }
            // Wake next decrementing thread to finish it's runnable
            condition.wake();
            // Release lock after calling operations on condition variable
            lock.release();
        }
    }

    /**
     * Decrementing runnable executed by two other threads, that decrement
     * value of the counter.
     */
    private class DecrementingRunnable implements Runnable {
        private final Counter counter;
        private final Lock lock;
        private final Condition condition;

        private DecrementingRunnable(final Counter counter,
                                     final Lock lock,
                                     final Condition condition) {
            this.counter = counter;
            this.lock = lock;
            this.condition = condition;
        }

        @Override
        public void run() {
            // Acquire lock before calling any operations on condition variable
            lock.acquire();
            // This call to sleep() is part of the initialization phase
            condition.sleep();

            // We will make 3 computation cycles. After each incrementation cycle
            // there will be two decrementation cycles
            for ( int i = 0; i < 3; i++ ) {
                logger.append("Decrementing:\n");
                // Decrementing counter 2 times
                for ( int j = 0; j < 2; j++ ) {
                    counter.decrement();
                    logger.append(counter.getValue()).append(", ");
                    // Yield current thread, just to test that even after calling
                    // yield, this thread gets planned again
                    KThread.yield();
                }
                logger.append("\n");
                // Wake next thread sleeping on condition variable. It can be next
                // decrementing thread or incrementing thread based on which thread is caller
                // and go to sleep
                condition.wake();
                condition.sleep();
            }
            // Wake next thread to finish it's runnable (if there is one, otherwise nothing happens)
            condition.wake();
            // Release lock after calling operations on condition variable
            lock.release();
        }
    }
}
