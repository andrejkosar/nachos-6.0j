// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase2.task4;

import nachos.machine.Machine;
import nachos.test.NachosKernelTestsSuite;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.ThreadedKernel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.hamcrest.core.Is.is;

/**
 * Tests for lottery scheduling implementation.
 */
@SuppressWarnings("Duplicates")
@RunWith(Parameterized.class)
public class Phase2Task4LotterySchedulerTests extends NachosKernelTestsSuite {
    /**
     * Constructs lottery scheduling tests class. We are running each test three times,
     * each time with different seed value used in Lib.random(), to be sure that
     * it is not just a coincidence, that tests passed.
     */
    public Phase2Task4LotterySchedulerTests(long randomSeed) {
        super("phase2/phase2.lottery.conf", "-s " + randomSeed);
    }

    @Parameterized.Parameters(name = "randomSeed={0}")
    public static Iterable<Long> randomSeeds() {
        return Arrays.asList(9830833L, 1116643687L, 1588825123L);
    }

    /**
     * Simple test for lottery scheduler implementation, which purpose is just to
     * test if lottery scheduler implementation does not deadlock.
     * <p>
     * Creates 50 threads. Each of the threads yields 10 times. Test passes
     * if no exception is thrown, or code does not deadlock (timeout).
     */
    @Test
    public void testIfLotterySchedulingDoesNotDeadlockWhenThreadTicketsAreEqual() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                List<KThread> threads = new ArrayList<>(50);

                // Create 50 threads and add to collection.
                boolean intStatus = Machine.interrupt().disable();
                for ( int i = 0; i < 50; i++ ) {
                    KThread thread = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            for ( int j = 0; j < 10; j++ ) {
                                KThread.yield();
                            }
                        }
                    }).setName("thread" + i);
                    threads.add(thread);
                    thread.fork();
                }
                Machine.interrupt().restore(intStatus);

                // Main thread waits till all threads are finished.
                while ( !areAllThreadsFinished(threads) ) {
                    KThread.yield();
                }
            }
        });
    }

    /**
     * Test similar to the test above. This test's purpose is just to
     * test if lottery scheduler implementation does not deadlock, when tickets
     * for each thread are different.
     * <p>
     * Creates 50 threads and sets their priorities. Each of the threads yields 10 times.
     * Test passes if no exception is thrown, or code does not deadlock (timeout).
     */
    @Test
    public void testIfLotterySchedulingDoesNotDeadlockWhenThreadTicketsAreDifferent() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                List<KThread> threads = new ArrayList<>(50);

                // Creates 50 threads, set their priorities and add each
                // of them to collection.
                boolean intStatus = Machine.interrupt().disable();
                for ( int i = 0; i < 50; i++ ) {
                    final long priority = i * 10 + 500;
                    KThread thread = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            for ( int j = 0; j < 10; j++ ) {
                                KThread.yield();
                            }
                        }
                    }).setName("thread" + priority);
                    ThreadedKernel.scheduler.setPriority(thread, priority);
                    threads.add(thread);
                    thread.fork();
                }
                Machine.interrupt().restore(intStatus);

                // Main thread waits for other thread to finish.
                while ( !areAllThreadsFinished(threads) ) {
                    KThread.yield();
                }
            }
        });
    }

    private boolean wereThereTenThousandSchedules(int[] schedules) {
        int sum = 0;
        for ( int v : schedules ) {
            sum += v;
        }
        return sum >= 10000;
    }

    /**
     * Tests if lottery scheduler implementation works, when no tickets
     * donation is needed.
     * <p>
     * As lottery scheduling is based on probability and random numbers
     * generation, this test repeats itself multiple times and only
     * average results of those repeats combined are tested.
     */
    @Test
    public void testLotterySchedulingWithoutTicketsDonationUsingStatisticalAlgorithm() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                // Here we hold probabilities of how often was each of the threads scheduled.
                final double[][] percentages = new double[20][4];

                // We repeat this test 20 times and then calculate average results.
                for ( int k = 0; k < 20; k++ ) {
                    // In this array we hold how many times were each of thread
                    // scheduled in this cycle. When sum of the array reaches 10000, we
                    // finish each thread execution.
                    // There will be 4 threads with different tickets amount.
                    final int[] schedules = new int[4];

                    // We need to disable interrupts, so we can set ticket amount (priority) for
                    // each thread.
                    boolean intStatus = Machine.interrupt().disable();
                    // Create thread with most tickets.
                    KThread mostTicketsThread = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    return;
                                }
                                schedules[0]++;
                                KThread.yield();
                            }
                        }
                    }).setName("mostTicketsThread");
                    ThreadedKernel.scheduler.setPriority(mostTicketsThread, 50);

                    // Create two medium threads with equal medium amount of the tickets.
                    KThread mediumTicketsThread1 = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    return;
                                }
                                schedules[1]++;
                                KThread.yield();
                            }
                        }
                    }).setName("mediumTicketsThread1");
                    ThreadedKernel.scheduler.setPriority(mediumTicketsThread1, 20);

                    KThread mediumTicketsThread2 = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    return;
                                }
                                schedules[2]++;
                                KThread.yield();
                            }
                        }
                    }).setName("mediumTicketsThread2");
                    ThreadedKernel.scheduler.setPriority(mediumTicketsThread2, 20);

                    // Create last thread with least amount of the tickets.
                    KThread leastTicketsThread = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    return;
                                }
                                schedules[3]++;
                                KThread.yield();
                            }
                        }
                    }).setName("leastTicketsThread");
                    ThreadedKernel.scheduler.setPriority(leastTicketsThread, 10);

                    // Fork each thread. And restore interrupts.
                    mostTicketsThread.fork();
                    mediumTicketsThread1.fork();
                    mediumTicketsThread2.fork();
                    leastTicketsThread.fork();
                    Machine.interrupt().restore(intStatus);

                    // Main threads waits till all other threads are finished.
                    while ( !areAllThreadsFinished(mostTicketsThread, mediumTicketsThread1, mediumTicketsThread2, leastTicketsThread) ) {
                        KThread.yield();
                    }

                    // After all of the tests are finished, meaning that there were 10000 schedules
                    // we can calculate scheduling probabilities of each thread.
                    percentages[k][0] = schedules[0] / 10000.0 * 100.0;
                    percentages[k][1] = schedules[1] / 10000.0 * 100.0;
                    percentages[k][2] = schedules[2] / 10000.0 * 100.0;
                    percentages[k][3] = schedules[3] / 10000.0 * 100.0;
                }

                // After we repeat scheduling test 20 times, calculate average probabilities.
                double mostTicketsPercentage = 0.0;
                for ( double[] p : percentages ) {
                    mostTicketsPercentage += p[0];
                }
                mostTicketsPercentage /= 20;

                double mediumTickets1Percentage = 0.0;
                for ( double[] p : percentages ) {
                    mediumTickets1Percentage += p[1];
                }
                mediumTickets1Percentage /= 20;

                double mediumTickets2Percentage = 0.0;
                for ( double[] p : percentages ) {
                    mediumTickets2Percentage += p[2];
                }
                mediumTickets2Percentage /= 20;

                double leastTicketsPercentage = 0.0;
                for ( double[] p : percentages ) {
                    leastTicketsPercentage += p[3];
                }
                leastTicketsPercentage /= 20;

                // Ticket amounts for threads were deliberately specified in the way,
                // that their sum is 100. Meaning that if lottery scheduler implementation
                // is correct, we should have probabilities as defined by ticket amounts.
                // This won't be definitely exact, so we allow error deviation of 1%.
                threadAssertThat(mostTicketsPercentage, is(both(greaterThan(49.0)).and(lessThan(51.0))));
                threadAssertThat(mediumTickets1Percentage, is(both(greaterThan(19.0)).and(lessThan(21.0))));
                threadAssertThat(mediumTickets2Percentage, is(both(greaterThan(19.0)).and(lessThan(21.0))));
                threadAssertThat(leastTicketsPercentage, is(both(greaterThan(9.0)).and(lessThan(11.0))));
            }
        });
    }

    /**
     * Tests if lottery scheduler implementation works, when tickets donation is present.
     * <p>
     * Test is very similar to the test above. It is also based on statistical
     * computation, but the only difference is that we use lock to make thread
     * with most tickets donate tickets to the thread with least tickets.
     */
    @Test
    public void testLotterySchedulingWithTicketsDonationUsingStatisticalAlgorithm() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                final double[][] percentages = new double[20][4];

                for ( int k = 0; k < 20; k++ ) {
                    final int[] schedules = new int[4];
                    final Lock lock = new Lock();

                    boolean intStatus = Machine.interrupt().disable();
                    KThread mostTicketsThread = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            // We will try to acquire the lock, which is already held by
                            // the thread with least tickets.
                            lock.acquire();
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    lock.release();
                                    return;
                                }
                                schedules[0]++;
                                KThread.yield();
                            }
                            lock.release();
                        }
                    }).setName("mostTicketsThread");
                    ThreadedKernel.scheduler.setPriority(mostTicketsThread, 50);

                    KThread mediumTicketsThread1 = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    return;
                                }
                                schedules[1]++;
                                KThread.yield();
                            }
                        }
                    }).setName("mediumTicketsThread1");
                    ThreadedKernel.scheduler.setPriority(mediumTicketsThread1, 20);

                    KThread mediumTicketsThread2 = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    return;
                                }
                                schedules[2]++;
                                KThread.yield();
                            }
                        }
                    }).setName("mediumTicketsThread2");
                    ThreadedKernel.scheduler.setPriority(mediumTicketsThread2, 20);

                    KThread leastTicketsThread = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            // Successfully acquire the lock as it is not held by any thread yet
                            // and start the thread with most tickets.
                            lock.acquire();
                            mostTicketsThread.fork();
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    lock.release();
                                    return;
                                }
                                schedules[3]++;
                                KThread.yield();
                            }
                            lock.release();
                        }
                    }).setName("leastTicketsThread");
                    ThreadedKernel.scheduler.setPriority(leastTicketsThread, 10);

                    // Start every thread, except the thread with most tickets, because we
                    // do not want it to acquire the lock before the thread with least ticket
                    // amount. The thread with most ticket amount will be started from the
                    // thread with least ticket amount.
                    mediumTicketsThread1.fork();
                    mediumTicketsThread2.fork();
                    leastTicketsThread.fork();
                    Machine.interrupt().restore(intStatus);

                    while ( !areAllThreadsFinished(mostTicketsThread, mediumTicketsThread1, mediumTicketsThread2, leastTicketsThread) ) {
                        KThread.yield();
                    }

                    percentages[k][0] = schedules[0] / 10000.0 * 100.0;
                    percentages[k][1] = schedules[1] / 10000.0 * 100.0;
                    percentages[k][2] = schedules[2] / 10000.0 * 100.0;
                    percentages[k][3] = schedules[3] / 10000.0 * 100.0;
                }

                double mostTicketsPercentage = 0.0;
                for ( double[] p : percentages ) {
                    mostTicketsPercentage += p[0];
                }
                mostTicketsPercentage /= 20;

                double mediumTickets1Percentage = 0.0;
                for ( double[] p : percentages ) {
                    mediumTickets1Percentage += p[1];
                }
                mediumTickets1Percentage /= 20;

                double mediumTickets2Percentage = 0.0;
                for ( double[] p : percentages ) {
                    mediumTickets2Percentage += p[2];
                }
                mediumTickets2Percentage /= 20;

                double leastTicketsPercentage = 0.0;
                for ( double[] p : percentages ) {
                    leastTicketsPercentage += p[3];
                }
                leastTicketsPercentage /= 20;

                // In the end we expect correct probabilities similarly to the test above.
                // Notice, that thread with most tickets, should not be scheduled at all,
                // because the lock it needs, is held by the thread with lowest ticket amount
                // all the time.
                threadAssertThat(mostTicketsPercentage, is(0.0));
                threadAssertThat(mediumTickets1Percentage, is(both(greaterThan(19.0)).and(lessThan(21.0))));
                threadAssertThat(mediumTickets2Percentage, is(both(greaterThan(19.0)).and(lessThan(21.0))));
                // The thread with least tickets, should get planned in roughly 60% of the cases.
                // It is because of all the tickets donated from the thread with most tickets (50),
                // added to it's own ticket amount (10).
                threadAssertThat(leastTicketsPercentage, is(both(greaterThan(59.0)).and(lessThan(61.0))));
            }
        });
    }

    /**
     * Tests if lottery scheduler implementation works, when transitive tickets
     * donation is present.
     * <p>
     * Test is again very similar to the both tests above.
     */
    @Test
    public void testLotterySchedulingWithTransitiveTicketsDonationUsingStatisticalAlgorithm() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                final double[][] percentages = new double[20][4];
                // This time we will need to use two lock, to be able to test
                // transitive tickets donation.
                final Lock lockHeldByLeastTicketsThread = new Lock();
                final Lock lockHeldByMediumTickets1Thread = new Lock();

                for ( int k = 0; k < 20; k++ ) {
                    final int[] schedules = new int[4];

                    boolean intStatus = Machine.interrupt().disable();
                    KThread mostTicketsThread = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            // Thread with most tickets tries to acquire the lock already
                            // held by one of the threads with medium ticket amount.
                            lockHeldByMediumTickets1Thread.acquire();
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    lockHeldByMediumTickets1Thread.release();
                                    return;
                                }
                                schedules[0]++;
                                KThread.yield();
                            }
                            lockHeldByMediumTickets1Thread.release();
                        }
                    }).setName("mostTicketsThread");
                    ThreadedKernel.scheduler.setPriority(mostTicketsThread, 50);

                    KThread mediumTicketsThread1 = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            // Successfully acquire the lock. And fork the thread with
                            // most tickets. After that try to acquire the lock already held
                            // by the thread with least ticket amount.
                            lockHeldByMediumTickets1Thread.acquire();
                            mostTicketsThread.fork();
                            lockHeldByLeastTicketsThread.acquire();
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    lockHeldByLeastTicketsThread.release();
                                    lockHeldByMediumTickets1Thread.release();
                                    return;
                                }
                                schedules[1]++;
                                KThread.yield();
                            }
                            lockHeldByLeastTicketsThread.release();
                            lockHeldByMediumTickets1Thread.release();
                        }
                    }).setName("mediumTicketsThread1");
                    ThreadedKernel.scheduler.setPriority(mediumTicketsThread1, 20);

                    KThread mediumTicketsThread2 = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    return;
                                }
                                schedules[2]++;
                                KThread.yield();
                            }
                        }
                    }).setName("mediumTicketsThread2");
                    ThreadedKernel.scheduler.setPriority(mediumTicketsThread2, 20);

                    KThread leastTicketsThread = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            // Successfully acquire the lock and fork both medium threads.
                            lockHeldByLeastTicketsThread.acquire();
                            mediumTicketsThread1.fork();
                            mediumTicketsThread2.fork();
                            for ( int j = 0; j < 10000; j++ ) {
                                if ( wereThereTenThousandSchedules(schedules) ) {
                                    lockHeldByLeastTicketsThread.release();
                                    return;
                                }
                                schedules[3]++;
                                KThread.yield();
                            }
                            lockHeldByLeastTicketsThread.release();
                        }
                    }).setName("leastTicketsThread");
                    ThreadedKernel.scheduler.setPriority(leastTicketsThread, 10);

                    // We will start only thread with least amount of tickets
                    // from the main thread, because we want correct threads to
                    // hold their locks.
                    leastTicketsThread.fork();
                    Machine.interrupt().restore(intStatus);

                    while ( !areAllThreadsFinished(mostTicketsThread, mediumTicketsThread1, mediumTicketsThread2, leastTicketsThread) ) {
                        KThread.yield();
                    }

                    percentages[k][0] = schedules[0] / 10000.0 * 100.0;
                    percentages[k][1] = schedules[1] / 10000.0 * 100.0;
                    percentages[k][2] = schedules[2] / 10000.0 * 100.0;
                    percentages[k][3] = schedules[3] / 10000.0 * 100.0;
                }

                double mostTicketsPercentage = 0.0;
                for ( double[] p : percentages ) {
                    mostTicketsPercentage += p[0];
                }
                mostTicketsPercentage /= 20;

                double mediumTickets1Percentage = 0.0;
                for ( double[] p : percentages ) {
                    mediumTickets1Percentage += p[1];
                }
                mediumTickets1Percentage /= 20;

                double mediumTickets2Percentage = 0.0;
                for ( double[] p : percentages ) {
                    mediumTickets2Percentage += p[2];
                }
                mediumTickets2Percentage /= 20;

                double leastTicketsPercentage = 0.0;
                for ( double[] p : percentages ) {
                    leastTicketsPercentage += p[3];
                }
                leastTicketsPercentage /= 20;

                // In the end we again expect correct probabilities similarly to the tests above.
                // The difference is again, that the thread with most tickets and one of the thread
                // with medium amount of tickets should not be scheduled at all, because locks they
                // need are already held by other threads.
                threadAssertThat(mostTicketsPercentage, is(0.0));
                threadAssertThat(mediumTickets1Percentage, is(0.0));
                threadAssertThat(mediumTickets2Percentage, is(both(greaterThan(19.0)).and(lessThan(21.0))));
                // And similar to the test above, the thread with least tickets, should be
                // planned in roughly 80% of the cases. This time it is combined amounts of
                // the thread with most tickets (50), one of the medium ticket amount (20) thread
                // and the amount it already had (10).
                // Tickets from the thread with most tickets were not transferred directly,
                // but transitively using thread with medium ticket amount.
                threadAssertThat(leastTicketsPercentage, is(both(greaterThan(79.0)).and(lessThan(81.0))));
            }
        });
    }
}
