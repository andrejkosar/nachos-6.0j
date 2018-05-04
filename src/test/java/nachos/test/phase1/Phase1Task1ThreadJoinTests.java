// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase1;

import nachos.machine.lib.AssertionFailureException;
import nachos.test.NachosKernelTestsSuite;
import nachos.threads.KThread;
import org.junit.Test;

/**
 * Tests for KThread.join().
 */
public class Phase1Task1ThreadJoinTests extends NachosKernelTestsSuite {
    public Phase1Task1ThreadJoinTests() {
        super("phase1/phase1.round.robin.conf");
    }

    /**
     * Tests if joining on thread works.
     */
    @Test
    public void testJoinOnThread() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                // Create first yielding thread
                KThread thread1 = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        for ( int i = 0; i < 3; i++ ) {
                            KThread.yield();
                        }
                    }
                }).setName("thread1");

                // Create second thread that starts first thread and than calls join() on it
                KThread thread2 = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        threadAssertEquals(KThread.Status.New, privilegedGetInstanceFieldDeepClone(thread1, KThread.Status.class, "status"));
                        thread1.fork();
                        threadAssertEquals(KThread.Status.Ready, privilegedGetInstanceFieldDeepClone(thread1, KThread.Status.class, "status"));
                        thread1.join();
                        threadAssertEquals(KThread.Status.Finished, privilegedGetInstanceFieldDeepClone(thread1, KThread.Status.class, "status"));
                        for ( int i = 0; i < 3; i++ ) {
                            KThread.yield();
                        }
                    }
                }).setName("thread2");

                // Start second thread from the main thread. Second thread does actually runs first
                // except main thread.
                thread2.fork();
                threadAssertEquals(KThread.Status.Ready, privilegedGetInstanceFieldDeepClone(thread2, KThread.Status.class, "status"));

                // Wait for second thread to finish. Transitively meaning wait for also for the first thread to finish.
                // This is place where first context switch occurs actually.
                thread2.join();
                threadAssertEquals(KThread.Status.Finished, privilegedGetInstanceFieldDeepClone(thread2, KThread.Status.class, "status"));
            }
        });
    }

    /**
     * Tests if calling join on not started thread
     * causes livelock in the calling thread.
     */
    @Test
    public void testJoinOnNotStartedThread() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                // Create new thread without starting it
                KThread thread = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        for ( int i = 0; i < 5; i++ ) {
                            KThread.yield();
                        }
                    }
                }).setName("thread");

                threadAssertEquals(KThread.Status.New, privilegedGetInstanceFieldDeepClone(thread, KThread.Status.class, "status"));
                // Assert that thread livelocks by checking if given action
                // lasts at least given number of seconds
                threadAssertActionTimeout(LIVELOCK_TIMEOUT, new Runnable() {
                    @Override
                    public void run() {
                        // Call join on not started thread
                        thread.join();
                    }
                });
            }
        });
    }

    /**
     * Tests if joining on current thread throws expected exception.
     */
    @Test
    public void testJoinOnCurrentThread() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                // Create thread and start it immediately
                new KThread(new Runnable() {
                    @Override
                    public void run() {
                        threadAssertEquals(KThread.Status.Running, privilegedGetInstanceFieldDeepClone(KThread.currentThread(), KThread.Status.class, "status"));
                        // Assert that correct exception is thrown when join() on the current thread is called
                        threadExpectException(AssertionFailureException.class, new Runnable() {
                            @Override
                            public void run() {
                                KThread.currentThread().join();
                            }
                        });
                    }
                }).fork();
                // Yield main thread to start execution of the thread above
                // Note: As we are using RoundRobinScheduler, we are certain that above thread
                //       will be selected by scheduler to run as the next thread
                KThread.yield();
            }
        });
    }

    /**
     * Test calling join on finished thread.
     */
    @Test
    public void testJoinOnFinishedThread() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                // Create thread
                KThread thread1 = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // Does nothing
                    }
                }).setName("thread1");
                threadAssertEquals(KThread.Status.New, privilegedGetInstanceFieldDeepClone(thread1, KThread.Status.class, "status"));

                // Start thread
                thread1.fork();
                threadAssertEquals(KThread.Status.Ready, privilegedGetInstanceFieldDeepClone(thread1, KThread.Status.class, "status"));

                // Yield current (main) thread
                KThread.yield();
                threadAssertEquals(KThread.Status.Finished, privilegedGetInstanceFieldDeepClone(thread1, KThread.Status.class, "status"));

                // Above thread finished it's execution. Test if calling join on it returns immediately.
                thread1.join();
                threadAssertEquals(KThread.Status.Finished, privilegedGetInstanceFieldDeepClone(thread1, KThread.Status.class, "status"));
            }
        });
    }

    /**
     * Tests if calling join on unfinished thread twice throws exception.
     */
    @Test
    public void testJoinOnThreadTwice() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                // Create yielding thread
                KThread thread = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        for ( int i = 0; i < 10; i++ ) {
                            KThread.yield();
                        }
                    }
                }).setName("thread");

                // Create thread that will call join on yielding thread as the first one
                KThread joiningThread1 = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // Make sure yielding threads is not finished
                        threadAssertNotEquals(KThread.Status.Finished, privilegedGetInstanceFieldDeepClone(thread, KThread.Status.class, "status"));
                        thread.join();
                    }
                }).setName("joiningThread1");

                // Create thread that will call join on yielding thread as second
                KThread joiningThread2 = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // Expect assertion error from thread.join()
                        threadExpectException(AssertionFailureException.class, new Runnable() {
                            @Override
                            public void run() {
                                // Make sure yielding threads is not finished
                                threadAssertNotEquals(KThread.Status.Finished, privilegedGetInstanceFieldDeepClone(thread, KThread.Status.class, "status"));
                                thread.join();
                            }
                        });
                    }
                }).setName("joiningThread2");

                // Start all threads. RoundRobinScheduler will schedule threads in first-came first-scheduled manner
                thread.fork();
                joiningThread1.fork();
                joiningThread2.fork();

                // Yield from the main thread until yielding thread finishes it's work
                while ( !areAllThreadsFinished(thread) ) {
                    KThread.yield();
                }
            }
        });
    }
}
