package nachos.threads;

import nachos.machine.Machine;
import nachos.machine.lib.Lib;
import nachos.machine.timer.Timer;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    private static final char dbgInt = 'i';

    //TODO(1.3) store all threads waiting for timer interrupt.
    // Remember, that there exists possibility, that two (or more)
    // threads will be mapped onto same wake time


    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     * <p>
     * <b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
        //TODO(1.3) initialize variable that holds threads waiting for timer interrupt

        Machine.timer().setInterruptHandler(new Runnable() {
            @Override
            public void run() {
                timerInterrupt();
            }
        });
    }

    public static void selfTest() {
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        //TODO(1.3) replace call to yield() below with correct implementation of timerInterrupt()
        KThread.yield();

        // Get the machine timer current time


        // If there are any waiting threads on this Alarm instance, wake all threads,
        // whose wake time is lower than or equal to current machine time.







    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     * <p>
     * <blockquote>
     * (current time) {@literal >=} (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param x the minimum number of clock ticks to wait.
     * @see Timer#getTime()
     */
    public void waitFor(long x) {
        Lib.debug(dbgInt, KThread.currentThread().toString() + " going to wait on alarm for " + x + " clock ticks");

        //TODO(1.3) imeplement waitFor() method without using busy waiting as below
        long wakeTime = Machine.timer().getTime() + x;
        while ( wakeTime > Machine.timer().getTime() ) {
            KThread.yield();
        }
        // Calculate wake time for current thread and put it in this alarm
        // waiting map with interrupts disabled to prevent state corruption













    }
}
