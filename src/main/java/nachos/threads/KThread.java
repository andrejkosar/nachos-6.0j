package nachos.threads;

import nachos.machine.Machine;
import nachos.machine.lib.Lib;
import nachos.machine.tcb.TCB;

import java.io.Serializable;

/**
 * <p>
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 * </p>
 * <p>
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 * </p>
 * <blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>
 * The following code would then create a thread and start it running:
 * </p>
 * <blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {
    private static final char dbgThread = 't';

    /**
     * Number of times the KThread constructor was called.
     */
    private static int numCreated;

    /**
     * Current scheduler thread ready queue.
     */
    private static ThreadQueue readyQueue;

    /**
     * Reference to currently running thread.
     */
    private static KThread currentThread;

    /**
     * Reference to thread that will be destroyed in next thread
     * call to restoreState().
     */
    private static KThread toBeDestroyed;

    /**
     * Reference to idle thread. Nachos context switches to idle thread, when
     * readyQueue.nextThread() backed by current scheduler returns null.
     */
    private static KThread idleThread;

    static {
        initializeStaticFields();
    }

    /**
     * Unique identifer for this thread. Used to deterministically compare
     * threads.
     */
    private final int id = numCreated++;

    /**
     * Additional state used by schedulers.
     *
     * @see nachos.threads.PriorityScheduler.ThreadState
     * @see nachos.threads.LotteryScheduler.LotteryThreadState
     */
    public Object schedulingState = null;

    /**
     * The status of this thread. A thread can either be new (not yet forked),
     * ready (on the ready queue but not running), running, or blocked (not
     * on the ready queue and not running).
     */
    private Status status = Status.New;

    private String name = "(unnamed thread-" + id + ")";
    private Runnable target;
    private TCB tcb;

    //TODO(1.1) We need to remember reference to the thread currently waiting

    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.
     */
    public KThread() {
        if ( currentThread != null ) {
            tcb = new TCB();
        }
        else {
            readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
            readyQueue.acquire(this);

            currentThread = this;
            tcb = TCB.currentTCB();
            name = "main";
            restoreState();

            createIdleThread();
        }
    }

    /**
     * Allocate a new KThread.
     *
     * @param target the object whose <tt>run</tt> method is called.
     */
    public KThread(Runnable target) {
        this();
        this.target = target;
    }

    private static void initializeStaticFields() {
        numCreated = 0;
        readyQueue = null;
        currentThread = null;
        toBeDestroyed = null;
        idleThread = null;
    }

    /**
     * Get the current thread.
     *
     * @return the current thread.
     */
    public static KThread currentThread() {
        Lib.assertTrue(currentThread != null);
        return currentThread;
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is
     * safe to do so. This method is automatically called when a thread's
     * <tt>run</tt> method returns, but it may also be called directly.
     * <p>
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to
     * delete this thread.
     */
    public static void finish() {
        Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());

        // Disable interrupts so we can do what we need without any
        // other thread getting processor and hence corrupting state
        Machine.interrupt().disable();

        //TODO(1.1) If we have thread waiting for us (the current thread) in join() operation, wake it up

        // Notify recorder that current thread is finishing it's
        // execution and to authorize TCB to destroy it
        Machine.nachosRuntimeRecorder().reportFinishingCurrentThread();

        // Save reference to current thread to be destroyed in restoreState()
        // of the next thread scheduled to run
        Lib.assertTrue(toBeDestroyed == null);
        toBeDestroyed = currentThread;

        // Set current thread status to finished
        currentThread.status = Status.Finished;

        // Go to sleep and start execution of another thread
        sleep();
    }

    /**
     * <p>
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be
     * rescheuled.
     * </p>
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise
     * returns when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     * </p>
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add
     * itself to the ready queue and switch to the next thread. On return,
     * restores interrupts to the previous state, in case <tt>yield()</tt> was
     * called with interrupts disabled.
     * </p>
     */
    public static void yield() {
        Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());

        Lib.assertTrue(currentThread.status == Status.Running);

        boolean intStatus = Machine.interrupt().disable();

        currentThread.ready();

        runNextThread();

        Machine.interrupt().restore(intStatus);
    }

    /**
     * <p>
     * Relinquish the CPU, because the current thread has either finished or it
     * is blocked. This thread must be the current thread.
     * </p>
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e.
     * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
     * some thread will wake this thread up, putting it back on the ready queue
     * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
     * scheduled this thread to be destroyed by the next thread to run.
     * </p>
     */
    public static void sleep() {
        Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());

        Lib.assertTrue(Machine.interrupt().disabled());

        if ( currentThread.status != Status.Finished ) {
            currentThread.status = Status.Blocked;
        }

        runNextThread();
    }

    /**
     * <p>
     * Create the idle thread. Whenever there are no threads ready to be run,
     * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
     * idle thread must never block, and it will only be allowed to run when
     * all other threads are blocked.
     * </p>
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     * </p>
     */
    private static void createIdleThread() {
        Lib.assertTrue(idleThread == null);

        idleThread = new KThread(new Runnable() {
            @Override
            public void run() {
                while ( true ) {
                    yield();
                }
            }
        });
        idleThread.setName("idle");

        Machine.nachosRuntimeRecorder().reportIdleThread(idleThread);

        idleThread.fork();
    }

    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    private static void runNextThread() {
        KThread nextThread = readyQueue.nextThread();
        if ( nextThread == null ) {
            nextThread = idleThread;
        }

        nextThread.run();
    }

    public static void selfTest() {
    }

    /**
     * Set the target of this thread.
     *
     * @param target the object whose <tt>run</tt> method is called.
     * @return this thread.
     */
    public KThread setTarget(Runnable target) {
        Lib.assertTrue(status == Status.New);

        this.target = target;
        return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @return the name given to this thread.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @param name the name to give to this thread.
     * @return this thread.
     */
    public KThread setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return the full name given to this thread.
     */
    @Override
    public String toString() {
        return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another
     * thread.
     *
     * @param o object to compare against.
     * @return result of the comparison against current thread. Returns 0 if same.
     */
    public int compareTo(Object o) {
        KThread thread = (KThread) o;

        if ( id < thread.id ) {
            return -1;
        }
        else if ( id > thread.id ) {
            return 1;
        }
        else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        KThread thread = (KThread) o;

        return id == thread.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads
     * are running concurrently: the current thread (which returns from the
     * call to the <tt>fork</tt> method) and the other thread (which executes
     * its target's <tt>run</tt> method).
     */
    public void fork() {
        Lib.assertTrue(status == Status.New);
        Lib.assertTrue(target != null);

        Lib.debug(dbgThread,
                "Forking thread: " + toString() + " Runnable: " + target);

        boolean intStatus = Machine.interrupt().disable();

        tcb.start(new Runnable() {
            @Override
            public void run() {
                runThread();
            }
        });

        ready();

        Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
        begin();
        target.run();
        finish();
    }

    private void begin() {
        Lib.debug(dbgThread, "Beginning thread: " + toString());

        Lib.assertTrue(this == currentThread);

        restoreState();

        Machine.interrupt().enable();
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's
     * ready queue.
     */
    public void ready() {
        Lib.debug(dbgThread, "Ready thread: " + toString());

        Lib.assertTrue(Machine.interrupt().disabled());
        Lib.assertTrue(status != Status.Ready);

        status = Status.Ready;
        if ( this != idleThread ) {
            readyQueue.waitForAccess(this);
        }

        Machine.nachosRuntimeRecorder().reportReadyThread(this);
    }

    /**
     * Waits for this thread to finish. If this thread is already finished,
     * return immediately. This method must only be called once; the second
     * call is not guaranteed to return. This thread must not be the current
     * thread.
     */
    public void join() {
        Lib.debug(dbgThread, "Joining to thread: " + toString());

        //TODO(1.1) Finally we need to implement join method

        // This thread must not be current thread

        // We disable interrupts, so we are not awaken before we call sleep()

        // Return immediately if this thread is already finished

        // The result of calling join() a second time on the same thread
        // before it finishes is undefined, even if the second
        // caller is a different thread than the first caller.


        // We save reference to running thread inside this thread


        // Sleep current thread and let scheduler choose another one to
        // run. Thread will be woken up when this thread finishes it's
        // execution in finish() method.



        // Restore interrupts after returning from sleep() method
    }

    /**
     * <p>
     * Dispatch the CPU to this thread. Save the state of the current thread,
     * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
     * load the state of the new thread. The new thread becomes the current
     * thread.
     * </p>
     * <p>
     * If the new thread and the old thread are the same, this method must
     * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     * </p>
     * <p>
     * The state of the previously running thread must already have been
     * changed from running to blocked or ready (depending on whether the
     * thread is sleeping or yielding).
     */
    private void run() {
        Lib.assertTrue(Machine.interrupt().disabled());

        Machine.yield();

        currentThread.saveState();

        Lib.debug(dbgThread, "Switching from: " + currentThread.toString() + " to: " + toString());

        currentThread = this;

        tcb.contextSwitch();

        currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to
     * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
     */
    protected void restoreState() {
        Lib.debug(dbgThread, "Running thread: " + currentThread.toString());

        Lib.assertTrue(Machine.interrupt().disabled());
        Lib.assertTrue(this == currentThread);
        Lib.assertTrue(tcb == TCB.currentTCB());

        Machine.nachosRuntimeRecorder().reportRunningThread(this);

        status = Status.Running;

        if ( toBeDestroyed != null ) {
            Lib.debug(dbgThread, "Destroying thread [" + toBeDestroyed + "] in restoreState of [" + currentThread + "]");
            toBeDestroyed.tcb.destroy();
            toBeDestroyed.tcb = null;
            toBeDestroyed = null;
        }
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not
     * need to do anything here.
     */
    protected void saveState() {
        Lib.assertTrue(Machine.interrupt().disabled());
        Lib.assertTrue(this == currentThread);
    }

    public enum Status implements Serializable {
        New, Ready, Running, Blocked, Finished
    }

    private static class PingTest implements Runnable {
        private int which;

        PingTest(int which) {
            this.which = which;
        }

        @Override
        public void run() {
            for ( int i = 0; i < 5; i++ ) {
                System.out.println("*** thread " + which + " looped " + i + " times");
                yield();
            }
        }
    }
}
