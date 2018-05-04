package nachos.threads;

import nachos.machine.lib.Lib;

import java.util.LinkedList;
import java.util.Queue;

/**
 * <p>
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * </p>
 *
 * @see SemaphoresCondition
 */
public class InterruptsCondition extends Condition {
    private static final char dbgThread = 't';

    //TODO(1.2) use FIFO queue to store references to threads sleeping on this condition variable
    /**
     * FIFO queue of threads sleeping on this condition variable.
     */
    private Queue<KThread> waitQueue;

    /**
     * Allocate a new condition variable.
     *
     * @param conditionLock the lock associated with this condition
     *                      variable. The current thread must hold this
     *                      lock whenever it uses <tt>sleep()</tt>,
     *                      <tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public InterruptsCondition(Lock conditionLock) {
        super(conditionLock);
        this.waitQueue = new LinkedList<>();
    }

    public static void selfTest() {
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    @Override
    public void sleep() {
        Lib.debug(dbgThread, "Sleep on condition variable called by " + KThread.currentThread().toString());
        //TODO(1.2) implement sleep() method
        // Calling thread must hold lock associated with this condition variable

        // Disable interrupts

        // Release the lock

        // Insert calling (current) thread to wait queue

        // And go to sleep
        KThread.sleep();
        // After this thread gets awaken by some other thread
        // restore interrupt status

        // Automatically reacquire lock before returning from sleep()

    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    @Override
    public void wake() {
        Lib.debug(dbgThread, KThread.currentThread().toString() + " is going to wake at most up one thread " +
                "sleeping on condition variable");
        //TODO(1.2) implement sleep() method
        // Calling thread must hold lock associated with this condition variable

        // Disable interrupts

        // Remove one thread sleeping on this condition variable



        // Restore interrupts status

    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    @Override
    public void wakeAll() {
        Lib.debug(dbgThread, KThread.currentThread().toString() + " called wakeAll() on condition variable");
        //TODO(1.2) implement sleep() method
        // Calling thread must hold lock associated with this condition variable

        // Wake up all threads sleeping on this condition variable waiting queue



    }
}
