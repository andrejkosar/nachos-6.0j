// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase1;

import nachos.machine.Machine;
import nachos.machine.Stats;
import nachos.test.NachosKernelTestsSuite;
import nachos.threads.KThread;
import nachos.threads.ThreadedKernel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.CombinableMatcher.both;

/**
 * Tests for Alarm implementation.
 */
public class Phase1Task3AlarmTests extends NachosKernelTestsSuite {
    /**
     * List of all threads waiting on Alarm.
     */
    private List<KThread> threads;
    /**
     * List of wake up times of all threads.
     */
    private List<Long> threadsWakeUpTimes;

    public Phase1Task3AlarmTests() {
        super("phase1/phase1.round.robin.conf");
    }

    /**
     * Tests if alarm is working by repeatedly putting thread to sleep
     * and storing machine times before thread goes to sleep and after
     * thread awakes. After that it checks whether thread slept for expected
     * number of ticks.
     */
    @Test
    public void testIfAlarmIsWorking() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                KThread thread = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // Set waiting time for 100 000 ticks and do 50 cycles, because
                        // the timer interrupt is called approximately every 500 clock ticks,
                        // not exactly. See Timer.scheduleInterrupt implementation.
                        long waitingTime = 100000;
                        for ( int i = 0; i < 50; i++ ) {
                            // Store machine time before calling waitFor()
                            long preWaitTime = Machine.timer().getTime();
                            ThreadedKernel.alarm.waitFor(waitingTime);
                            // Store machine time after thread gets awaken
                            long postWaitTime = Machine.timer().getTime();

                            // As there is some randomness when calling timer interrupt
                            // we need to calculate boundaries around time in which thread
                            // should be awaken. See Timer.scheduleInterrupt implementation.
                            long delay = Stats.TimerTicks;
                            delay += (delay / 10) - (delay / 20);

                            // Calculate offset and check if it is within the calculated boundaries.
                            // If there was no randomness, offset would be always equal to 0.
                            long offset = postWaitTime - preWaitTime - waitingTime;
                            threadAssertThat(offset, is(both(lessThanOrEqualTo(delay)).and(greaterThanOrEqualTo(0L))));
                        }
                    }
                }).setName("clock thread");

                thread.fork();
                // Wait for thread to finish it's execution
                while ( privilegedGetInstanceFieldDeepClone(thread, KThread.Status.class, "status") != KThread.Status.Finished ) {
                    KThread.yield();
                }
            }
        });
    }

    private boolean areAllThreadWakeUpTimesEqual() {
        for ( int i = 1; i < threadsWakeUpTimes.size(); i++ ) {
            if ( !threadsWakeUpTimes.get(i - 1).equals(threadsWakeUpTimes.get(i)) ) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests if alarm correctly wakes all threads at the same time
     * planned for exactly same wake up time.
     */
    @Test
    public void testIfAlarmWakesAllThreadsWaitingForSameTime() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                // Store current machine time and set sleeping time
                long time = Machine.timer().getTime();
                long sleepTime = 60000;

                threadsWakeUpTimes = new ArrayList<>();
                threads = new ArrayList<>();
                // We will create multiple threads that should be awaken approximately at the same time.
                for ( int i = 0; i < 60; i++ ) {
                    final int step = i + 1;
                    KThread thread = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            // Put current thread to sleep on alarm for given time.
                            // Each subsequent thread is put to sleep for less amount
                            // of time to ensure that all threads are planned for the same wake up time.
                            // The reason behind this is, that threads scheduled to run later, will have
                            // machine time advanced little more.
                            ThreadedKernel.alarm.waitFor(sleepTime - (Machine.timer().getTime() - time));
                            // When storing thread wake up time, we also need to take into account
                            // time advance of later scheduled threads after they have woken up,
                            // so we subtract product of number of for loop cycles and kernel tick size
                            threadsWakeUpTimes.add(Machine.timer().getTime() - step * Stats.KernelTick);
                        }
                    }).setName("thread" + i);
                    threads.add(thread);
                    thread.fork();
                }

                while ( !areAllThreadsFinished(threads) ) {
                    KThread.yield();
                }

                threadAssertTrue(areAllThreadWakeUpTimesEqual());
                threadAssertEquals(threads.size(), threadsWakeUpTimes.size());
            }
        });
    }
}
