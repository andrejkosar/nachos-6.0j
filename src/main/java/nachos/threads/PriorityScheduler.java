package nachos.threads;

import nachos.machine.Machine;
import nachos.machine.lib.Lib;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * A scheduler that chooses threads based on their priorities.
 * </p>
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 * </p>
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to starve a thread if there's always a thread waiting with
 * higher priority.
 * </p>
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 * </p>
 */

public class PriorityScheduler extends Scheduler {
    /**
     * The default priority for a new thread. Do not change this value.
     */
    private static final long priorityDefault = 1L;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    private static final long priorityMinimum = 0L;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    private static final long priorityMaximum = 7L;
    /**
     * Thread safe atomic integer that is used as unique id
     * generator for priority queues ids.
     */
    private final AtomicInteger queuesCreated = new AtomicInteger(0);

    //TODO(1.5) you will probably need to remember which thread called pickNextThread() when trying to calculate effective priority
    /**
     * Helper variable to prevent stack overflow when calling {@link PriorityQueue#pickNextThread()}
     * inside of {@link ThreadState#getEffectivePriority()}
     */
    protected PriorityQueue originalCallerQueue = null;

    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
        super();
    }

    public static void selfTest() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param transferPriority <tt>true</tt> if this queue should
     *                         transfer priority from waiting threads
     *                         to the owning thread.
     * @return a new priority thread queue.
     */
    @Override
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        boolean intStatus = Machine.interrupt().disable();
        PriorityQueue queue = new PriorityQueue(transferPriority);
        Machine.interrupt().restore(intStatus);

        return queue;
    }

    @Override
    public long getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    @Override
    public long getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getEffectivePriority();
    }

    @Override
    public void setPriority(KThread thread, long priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= getMinimumPriority() && priority <= getMaximumPriority());

        getThreadState(thread).setPriority(priority);
    }

    @Override
    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        long priority = getPriority();
        if ( priority == getMaximumPriority() ) {
            return false;
        }

        setPriority(priority + 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    @Override
    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        long priority = getPriority();
        if ( priority == getMinimumPriority() ) {
            return false;
        }

        setPriority(priority - 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param thread the thread whose scheduling state to return.
     * @return the scheduling state of the specified thread.
     */
    @Override
    protected ThreadState getThreadState(KThread thread) {
        if ( thread.schedulingState == null ) {
            thread.schedulingState = new ThreadState(thread);
        }

        return (ThreadState) thread.schedulingState;
    }

    @Override
    public long getDefaultPriority() {
        return priorityDefault;
    }

    @Override
    public long getMinimumPriority() {
        return priorityMinimum;
    }

    @Override
    public long getMaximumPriority() {
        return priorityMaximum;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
        /**
         * Flag that is set to <tt>true</tt> if this queue should
         * transfer priority from waiting threads to the owning thread.
         */
        protected boolean transferPriority;

        /**
         * Set that holds all threads inside this priority queue.
         */
        protected Set<KThread> queue;

        /**
         * Unique priority queue identifier. It only needs to be unique
         * within this priority queue.
         */
        protected int id;

        //TODO(1.5) You will also need to somehow remember which thread came sooner, than the other.
        // This is required because PriorityScheduler works as FIFO queue (e.g. RoundRobin Scheduler)
        // for threads with equal priorities.

        //TODO(1.5) When recalculating effective priority you need to know thread, which holds resource
        // represented by this priority queue


        /**
         * Allocate a new priority thread queue.
         *
         * @param transferPriority <tt>true</tt> if this queue should
         *                         transfer priority from waiting threads
         *                         to the owning thread.
         */
        protected PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
            this.id = queuesCreated.getAndAdd(1);

            queue = new HashSet<>();
        }

        /**
         * Helper method to set dirty flag to true for every
         * thread that should recalculate it's effective priority at the next call.
         */
        private void makeDirty() {
            //TODO(1.5) Need to recalculate effective priority of thread, which holds resource represented by this queue



            //TODO(1.5) And to recalculate effective priorities of all threads waiting on this resource in queue
            for ( KThread thread : queue ) {
                // Mark thread as in need of effective priority recalculation

            }
        }

        @Override
        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());

            getThreadState(thread).waitForAccess(this);
        }

        @Override
        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());

            getThreadState(thread).acquire(this);
            //TODO(1.5) The thread from parameter now holds resource represented by this priority queue

        }

        @Override
        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());

            ThreadState threadState = pickNextThread();
            if ( threadState == null ) {
                return null;
            }

            queue.remove(threadState.thread);
            return threadState.thread;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return the next thread that <tt>nextThread()</tt> would return.
         */
        protected ThreadState pickNextThread() {
            if ( queue.isEmpty() ) {
                return null;
            }

            //TODO(1.5) Remembering which thread called pickNextThread() make save you from headaches from StackOverflowException when calculating effective priority
            // Set helper variable to this priority queue.
            originalCallerQueue = this;

            //TODO(1.5) You need to come up with algorithm to pick next thread by your self
            // Pick next thread to be run. Pick thread with highest priority.
            // If there are multiple threads with same highest priority, pick
            // thread that waited for longest time (enqueue time is lowest).








            // Read picked thread and current thread effective priorities.


            // Select thread with higher effective priority.




            // Or if priorities are equal, select thread that waits longer
            // (has lower enqueue time)








            return null;
        }

        @Override
        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());

            for ( KThread thread : queue ) {
                System.out.print(thread + " ");
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

            PriorityQueue queue = (PriorityQueue) o;

            return id == queue.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
        /**
         * The priority of the associated thread.
         */
        protected long priority;

        //TODO(1.5) You need to minimize need for effective priority recalculations. Maybe use some variable as cache?


        //TODO(1.5) You will need a flag indicating whether to recalculate effective priority.


        //TODO(1.5) You will need to remember set of all priority queues (resources e.g. Lock, Semaphore etc.)
        // on which thread associated with this thread state called acquire() or waitForAccess().
        // This is important, because you need to know when to transfer higher priority of thread
        // waiting on this thread to this thread.

        /**
         * The thread with which this thread state object is associated.
         */
        protected KThread thread;

        //TODO(1.5) You will also need to somehow remember which thread came sooner, than the other.
        // This is required because PriorityScheduler works as FIFO queue (e.g. RoundRobin Scheduler)
        // for threads with equal priorities.


        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param thread the thread this state belongs to.
         */
        protected ThreadState(KThread thread) {
            this.thread = thread;

            //TODO(1.5) Initialize set of all priority queues

            setPriority(ThreadedKernel.scheduler.getDefaultPriority());
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return the priority of the associated thread.
         */
        private long getPriority() {
            return priority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param priority the new priority.
         */
        private void setPriority(long priority) {
            if ( this.priority == priority ) {
                return;
            }

            this.priority = priority;
            //TODO(1.5) Effective priority of this thread should be recalculated.

        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return the effective priority of the associated thread.
         */
        protected long getEffectivePriority() {
            //TODO(1.5) implement effective priority with donated priorities (along with caching)

            // Only recalculate effective priority if required.


            // Loop through all thread resources.

            // If we have effective priority equal to maximum priority, there is no reason to continue



            // Change effective priority only if transferPriority is true,
            // there are some threads in queue and the current queue is
            // not the queue, which called this method initially.



            // Get thread state of thread with highest priority by calling pickNextThread

            // If this is not this thread and effective priority is higher donate it to this thread.

            // Do a sort of recursive call to getEffectivePriority(). It's not
            // purely recursive, because instance of thread state on which method
            // is called, is different from this thread state.








            return -1;
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param waitQueue the queue that the associated thread is
         *                  now waiting on.
         * @see nachos.threads.ThreadQueue#waitForAccess
         */
        private void waitForAccess(PriorityQueue waitQueue) {
            //TODO(1.5) waitQueue from parameter now becomes one of resources on which this thread waits

            //TODO(1.5) this is a good place to remember which thread came sooner

            waitQueue.queue.add(thread);
            // Effective priority of whole queue should be recalculated.
            waitQueue.makeDirty();
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @param waitQueue the queue that the associated thread is now
         *                  waiting on
         * @see nachos.threads.ThreadQueue#acquire
         * @see nachos.threads.ThreadQueue#nextThread
         */
        private void acquire(PriorityQueue waitQueue) {
            Lib.assertTrue(waitQueue.queue.isEmpty());

            //TODO(1.5) waitQueue from parameter now becomes one of resources on which this thread waits

            // Effective priority of whole queue should be recalculated.
            waitQueue.makeDirty();
        }
    }
}
