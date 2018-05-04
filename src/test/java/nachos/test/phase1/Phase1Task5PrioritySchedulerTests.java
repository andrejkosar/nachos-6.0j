// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase1;

import nachos.machine.Machine;
import nachos.machine.lib.Lib;
import nachos.test.NachosKernelTestsSuite;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.ThreadedKernel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;

/**
 * Tests for priority scheduling implementation.
 */
public class Phase1Task5PrioritySchedulerTests extends NachosKernelTestsSuite {
    public Phase1Task5PrioritySchedulerTests() {
        super("phase1/phase1.priority.conf");
    }

    /**
     * Simple test for basic priority scheduler implementation. Creates
     * multiple threads with random priority, which is higher than main thread priority.
     * Yields from main thread and after main thread gets scheduled again expects, that
     * all other threads have finished their execution. Also checks that their schedule
     * order was correct.
     */
    @Test
    public void testPrioritySchedulingWithoutPriorityDonation() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                List<KThread> threads = new ArrayList<>(50);

                // Lock and list to record order in which threads were scheduled by
                // priority scheduler.
                Lock lock = new Lock();
                List<KThread> finishedThreads = new ArrayList<>(50);

                // Disable interrupts so we can directly set thread's priority
                // from main thread.
                boolean intStatus = Machine.interrupt().disable();

                // Get main thread priority. Should be default priority = 1.
                final long mainThreadPriority = ThreadedKernel.scheduler.getPriority();

                // Create 50 threads with random, but higher priority than main thread.
                for ( int i = 0; i < 50; i++ ) {
                    final long priority = mainThreadPriority + 1 + Lib.random(ThreadedKernel.scheduler.getMaximumPriority() - mainThreadPriority);
                    KThread thread = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            // Record current (this) thread. Use Lock to synchronize multiple
                            // threads adding itself to list.
                            lock.acquire();
                            finishedThreads.add(KThread.currentThread());
                            lock.release();
                        }
                    }).setName("thread" + i);
                    // Set calculated random priority, store thread and fork it.
                    ThreadedKernel.scheduler.setPriority(thread, priority);
                    threads.add(thread);
                    thread.fork();
                }

                // After restoring interrupts, yield from main thread to give other threads chance to run.
                Machine.interrupt().restore(intStatus);
                KThread.yield();

                // When main thread gets scheduled again all other threads (having higher priority) should be finished.
                for ( KThread thread : threads ) {
                    threadAssertEquals(KThread.Status.Finished, privilegedGetInstanceFieldDeepClone(thread, KThread.Status.class, "status"));
                }

                // Check schedule order of all threads.
                long previousThreadPriority = ThreadedKernel.scheduler.getMaximumPriority();
                long previousThreadEnqueueTime = Long.MIN_VALUE;
                for ( KThread thread : finishedThreads ) {
                    // Thread that has been scheduled before this thread should have higher or
                    // equal priority to this thread.
                    long priority = privilegedGetInstanceFieldDeepClone(thread.schedulingState, long.class, "priority");
                    threadAssertThat(previousThreadPriority, is(greaterThanOrEqualTo(priority)));

                    // If previous thread had equal priority to this thread, it's enqueue time
                    // should be smaller than this thread enqueue time.
                    long threadEnqueueTime = privilegedGetInstanceFieldDeepClone(thread.schedulingState, long.class, "enqueuedTime");
                    if ( previousThreadPriority == priority ) {
                        threadAssertThat(threadEnqueueTime, is(greaterThan(previousThreadEnqueueTime)));
                    }
                    previousThreadEnqueueTime = threadEnqueueTime;
                    previousThreadPriority = priority;
                }
            }
        });
    }

    /**
     * Creates two threads. First with higher initial priority and second with
     * lower initial priority. After thread with higher initial priority gets scheduled,
     * it decreases it's priority to be lower than second thread and yields. Expects that
     * second thread (with lower initial priority) ends before first thread.
     */
    @Test
    public void testPrioritySchedulingWithoutPriorityDonationWhileAlteringPriorities() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                // Lock and list to record order in which threads were scheduled by
                // priority scheduler.
                Lock lock = new Lock();
                List<KThread> finishedThreads = new ArrayList<>();

                boolean intStatus = Machine.interrupt().disable();

                // Create first  thread with initially higher (maximum) priority.
                KThread initiallyHigher = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // First thing, that this thread does is that it decreases it's priority twice and yields.
                        ThreadedKernel.scheduler.decreasePriority();
                        ThreadedKernel.scheduler.decreasePriority();
                        KThread.yield();
                        // Record that this (current) thread finished.
                        lock.acquire();
                        finishedThreads.add(KThread.currentThread());
                        lock.release();
                    }
                }).setName("initiallyHigher");
                ThreadedKernel.scheduler.setPriority(initiallyHigher, ThreadedKernel.scheduler.getMaximumPriority());

                // Create second thread with initially lower (maximum - 1) priority, but still higher
                // than priority of the main thread.
                KThread initiallyLower = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // Yield. After yield, it should get rescheduled again immediately.
                        KThread.yield();
                        // Record that this (current) thread finished.
                        lock.acquire();
                        finishedThreads.add(KThread.currentThread());
                        lock.release();
                    }
                }).setName("initiallyLower");
                ThreadedKernel.scheduler.setPriority(initiallyLower, ThreadedKernel.scheduler.getMaximumPriority() - 1);
                Machine.interrupt().restore(intStatus);

                // Fork both threads and yield from the main thread. Note: we forked initially lower priority
                // thread first, but this should have no ill effect if priority scheduler implementation is correct.
                initiallyLower.fork();
                initiallyHigher.fork();
                KThread.yield();

                // Both threads should be finished and finished in correct order. Thread with initially lower
                // priority should have finished before thread with initially higher priority.
                threadAssertEquals(KThread.Status.Finished, privilegedGetInstanceFieldDeepClone(initiallyLower, KThread.Status.class, "status"));
                threadAssertEquals(KThread.Status.Finished, privilegedGetInstanceFieldDeepClone(initiallyHigher, KThread.Status.class, "status"));
                threadAssertEquals(2, finishedThreads.size());
                threadAssertEquals(initiallyLower, finishedThreads.get(0));
                threadAssertEquals(initiallyHigher, finishedThreads.get(1));
            }
        });
    }

    /**
     * Tests if priority donation works in priority scheduler implementation. Creates
     * two threads. First with higher priority and second with lower priority (but still
     * higher than main thread). Main thread with lowest priority of all acquires the lock
     * before yielding. After highest priority thread will be scheduled, it should go to sleep
     * on lock because the lock is held by main thread. Next scheduled thread should be main
     * thread even though it has lower priority, because of priority donation. At the end
     * expects correct finishing order of threads.
     */
    @Test
    public void testPrioritySchedulingWithPriorityDonation() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                List<KThread> finishedThreads = new ArrayList<>();
                Lock lock = new Lock();

                boolean intStatus2 = Machine.interrupt().disable();
                ThreadedKernel.scheduler.setPriority(ThreadedKernel.scheduler.getMinimumPriority());

                // Create thread with highest priority. Thread will try to acquire
                // lock held by main thread, thus going to sleep. Only main thread
                // with lowest priority is able to wake it up again.
                KThread highestPriorityThread = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        lock.acquire();
                        lock.release();
                        finishedThreads.add(KThread.currentThread());
                    }
                }).setName("highestPriorityThread");
                ThreadedKernel.scheduler.setPriority(highestPriorityThread, 7);

                // Create thread with medium priority. This thread should be scheduled
                // as last one, because main thread will get it's priority donation from
                // highestPriorityThread after highestPriorityThread tries to acquire lock
                // held by main thread.
                KThread mediumPriorityThread = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        finishedThreads.add(KThread.currentThread());
                    }
                }).setName("mediumPriorityThread");
                ThreadedKernel.scheduler.setPriority(mediumPriorityThread, 6);

                mediumPriorityThread.fork();
                highestPriorityThread.fork();

                // Acquire lock by main thread, which has lowest priority.
                lock.acquire();
                Machine.interrupt().restore(intStatus2);

                // Set main thread name and yield.
                KThread.currentThread().setName("lowestPriorityThread");
                KThread.yield();

                // Thread with highest priority tried to acquire the lock held by
                // main (current) thread. Release the lock and yield again from main thread.
                // Next scheduled thread should be again thread with highest priority.
                threadAssertEquals(ThreadedKernel.scheduler.getMaximumPriority(), privilegedGetInstanceFieldDeepClone(KThread.currentThread().schedulingState, long.class, "effectivePriority"));
                lock.release();

                finishedThreads.add(KThread.currentThread());

                // Wait for all threads to finish. Highest priority thread should be already finished, so
                // wait for medium priority thread to finish it's execution.
                while ( !areAllThreadsFinished(highestPriorityThread, mediumPriorityThread) ) {
                    KThread.yield();
                }

                threadAssertEquals(3, finishedThreads.size());
                threadAssertEquals(KThread.currentThread(), finishedThreads.get(0));
                threadAssertEquals(highestPriorityThread, finishedThreads.get(1));
                threadAssertEquals(mediumPriorityThread, finishedThreads.get(2));
            }
        });
    }

    /**
     * Tests if transitive priority donation works by creating two threads. These
     * threads together with main thread and in assistance of two locks effectively
     * reverse order in which threads will finish their execution parts.
     */
    @Test
    public void testPrioritySchedulingWithTransitivePriorityDonationWhileAlteringPriorities() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                // Create two locks and list to store order in which threads have finished.
                Lock lockInitiallyHeldByHigherMediumPriorityThread = new Lock();
                Lock lockInitiallyHeldByLowestPriorityThread = new Lock();
                List<KThread> finishedThreads = new ArrayList<>();

                // Create thread with highest priority. Initially it has medium
                // priority, but that will change right after higher medium priority thread
                // acquires it's lock.
                boolean intStatus = Machine.interrupt().disable();
                KThread highestPriorityThread = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // At first it should sleep on lock already held by medium priority thread.
                        lockInitiallyHeldByHigherMediumPriorityThread.acquire();
                        lockInitiallyHeldByHigherMediumPriorityThread.release();
                        finishedThreads.add(KThread.currentThread());
                    }
                }).setName("highestPriorityThread");
                ThreadedKernel.scheduler.setPriority(highestPriorityThread, 6);

                // Create thread with higher medium priority. Initially it has highest
                // priority, but that will change right after higher medium priority thread
                // acquires it's lock.
                KThread higherMediumPriorityThread = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // Should successfully acquire lock.
                        lockInitiallyHeldByHigherMediumPriorityThread.acquire();
                        // Decrease it's priority.
                        ThreadedKernel.scheduler.decreasePriority();
                        ThreadedKernel.scheduler.decreasePriority();
                        KThread.yield();
                        // At first should sleep on lock already held by lowest priority thread.
                        lockInitiallyHeldByLowestPriorityThread.acquire();
                        lockInitiallyHeldByLowestPriorityThread.release();
                        lockInitiallyHeldByHigherMediumPriorityThread.release();
                        finishedThreads.add(KThread.currentThread());
                    }
                }).setName("higherMediumPriorityThread");
                ThreadedKernel.scheduler.setPriority(higherMediumPriorityThread, 7);

                // Create thread with lower medium priority. Should finish as the last one.
                KThread lowerMediumPriorityThread = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        finishedThreads.add(KThread.currentThread());
                    }
                }).setName("lowerMediumPriorityThread");
                ThreadedKernel.scheduler.setPriority(lowerMediumPriorityThread, 4);

                // Set main thread priority to lowest.
                KThread.currentThread().setName("lowestPriorityThread");
                ThreadedKernel.scheduler.setPriority(ThreadedKernel.scheduler.getMinimumPriority());
                Machine.interrupt().restore(intStatus);

                highestPriorityThread.fork();
                higherMediumPriorityThread.fork();
                lowerMediumPriorityThread.fork();

                // Acquire lock for lowest priority thread and yield.
                lockInitiallyHeldByLowestPriorityThread.acquire();
                KThread.yield();

                // Should be scheduled again before both highest and medium priority
                // threads, as highest priority thread waits for lock held by higher medium priority
                // thread, which in turn waits for lock held by lowest priority thread (main).
                // Should be scheduled also before lower medium priority thread, because it will
                // get it's priority transitively donated from highest priority thread.
                threadAssertEquals(6L, privilegedGetInstanceFieldDeepClone(KThread.currentThread().schedulingState, long.class, "effectivePriority"));
                lockInitiallyHeldByLowestPriorityThread.release();

                finishedThreads.add(KThread.currentThread());

                while ( !areAllThreadsFinished(highestPriorityThread, higherMediumPriorityThread) ) {
                    KThread.yield();
                }

                threadAssertEquals(4, finishedThreads.size());
                threadAssertEquals(KThread.currentThread(), finishedThreads.get(0));
                threadAssertEquals(higherMediumPriorityThread, finishedThreads.get(1));
                threadAssertEquals(highestPriorityThread, finishedThreads.get(2));
                threadAssertEquals(lowerMediumPriorityThread, finishedThreads.get(3));
            }
        });
    }
}
