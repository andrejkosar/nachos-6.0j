package nachos.threads;

import nachos.machine.lib.Lib;

import java.util.LinkedList;
import java.util.Queue;

/**
 * <p>
 * An implementation of condition variables built upon semaphores.
 * </p>
 * <p>
 * A condition variable is a synchronization primitive that does not have
 * a value (unlike a semaphore or a lock), but threads may still be queued.
 * </p>
 * <ul>
 * <li>
 * <tt>sleep()</tt>: atomically release the lock and relinquish the CPU
 * until woken; then reacquire the lock.
 * </li>
 * <li>
 * <tt>wake()</tt>: wake up a single thread sleeping in this condition
 * variable, if possible.
 * </li>
 * <li>
 * <tt>wakeAll()</tt>: wake up all threads sleeping in this condition
 * variable.
 * </li>
 * </ul>
 * <p>
 * Every condition variable is associated with some lock. Multiple condition
 * variables may be associated with the same lock. All three condition variable
 * operations can only be used while holding the associated lock.
 * </p>
 * <p>
 * In Nachos, condition variables are summed to obey <i>Mesa-style</i>
 * semantics. When a <tt>wake()</tt> or <tt>wakeAll()</tt> wakes up another
 * thread, the woken thread is simply put on the ready list, and it is the
 * responsibility of the woken thread to reacquire the lock (this reacquire is
 * taken core of in <tt>sleep()</tt>).
 * </p>
 * <p>
 * By contrast, some implementations of condition variables obey
 * <i>Hoare-style</i> semantics, where the thread that calls <tt>wake()</tt>
 * gives up the lock and the CPU to the woken thread, which runs immediately
 * and gives the lock and CPU back to the waker when the woken thread exits the
 * critical section.
 * </p>
 * <p>
 * The consequence of using Mesa-style semantics is that some other thread
 * can acquire the lock and change data structures, before the woken thread
 * gets a chance to run. The advance to Mesa-style semantics is that it is a
 * lot easier to implement.
 * </p>
 */
public class SemaphoresCondition extends Condition {
    private static final char dbgThread = 't';
    /**
     * FIFO queue of semaphores representing waiting threads on
     * this condition variable. It's size is equal to number of
     * threads currently sleeping on this condition variable.
     */
    private Queue<Semaphore> waitQueue;

    /**
     * Allocate a new condition variable.
     *
     * @param conditionLock the lock associated with this condition
     *                      variable. The current thread must hold this
     *                      lock whenever it uses <tt>sleep()</tt>,
     *                      <tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public SemaphoresCondition(Lock conditionLock) {
        super(conditionLock);
        waitQueue = new LinkedList<>();
    }

    public static void selfTest() {
    }

    /**
     * <p>
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     * </p>
     * <p>
     * This implementation uses semaphores to implement this, by allocating a
     * semaphore for each waiting thread. The waker will <tt>V()</tt> this
     * semaphore, so thre is no chance the sleeper will miss the wake-up, even
     * though the lock is released before caling <tt>P()</tt>.
     * </p>
     */
    @Override
    public void sleep() {
        Lib.debug(dbgThread, "Sleep on condition variable called by " + KThread.currentThread().toString());
        // Calling thread must hold lock associated with this condition variable
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        // Create new semaphore instance with initial value set to 0
        // for calling thread and add it to this condition waiting queue
        Semaphore waiter = new Semaphore(0);
        waitQueue.add(waiter);

        // Release the lock
        conditionLock.release();
        // Call P() on newly created semaphore object which causes
        // thread to go to sleep (as initial value was set to 0)
        waiter.P();
        // Some other thread called V() on semaphore object and woke
        // this thread. It will now automatically reacquire lock before
        // returning from sleep().
        // For Mesa-style semantics time between calls to waiter.P()
        // and conditionLock.acquire() is dangerous, because some other
        // thread can acquire lock before this thread if it gets planned
        // by scheduler before this one.
        conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    @Override
    public void wake() {
        Lib.debug(dbgThread, KThread.currentThread().toString() + " is going to wake at most up one thread " +
                "sleeping on condition variable");
        // Calling thread must hold lock associated with this condition variable
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        // Remove one sleeping thread and call V() on the semaphore
        // associated with it
        if ( !waitQueue.isEmpty() ) {
            waitQueue.remove().V();
        }
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    @Override
    public void wakeAll() {
        Lib.debug(dbgThread, KThread.currentThread().toString() + " called wakeAll() on condition variable");
        // Calling thread must hold lock associated with this condition variable
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        // Wake up all threads sleeping on this condition variable waiting queue
        while ( !waitQueue.isEmpty() ) {
            wake();
        }
    }
}
