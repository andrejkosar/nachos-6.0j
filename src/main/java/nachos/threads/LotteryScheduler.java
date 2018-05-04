package nachos.threads;

import nachos.machine.Machine;
import nachos.machine.lib.Lib;

/**
 * <p>
 * A scheduler that chooses threads using a lottery.
 * </p>
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 * </p>
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 * </p>
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 * </p>
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * The default priority for a new thread. Do not change this value.
     */
    private static final long priorityDefault = 1L;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    private static final long priorityMinimum = 1L;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    private static final long priorityMaximum = Integer.MAX_VALUE;

    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
        super();
    }

    public static void selfTest() {
    }

    /**
     * Allocate a new lottery thread queue.
     *
     * @param transferPriority <tt>true</tt> if this queue should
     *                         transfer tickets from waiting threads
     *                         to the owning thread.
     * @return a new lottery thread queue.
     */
    @Override
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        boolean intStatus = Machine.interrupt().disable();
        LotteryQueue queue = new LotteryQueue(transferPriority);
        Machine.interrupt().restore(intStatus);

        return queue;
    }

    @Override
    protected ThreadState getThreadState(KThread thread) {
        if ( thread.schedulingState == null ) {
            thread.schedulingState = new LotteryThreadState(thread);
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
     * A <tt>ThreadQueue</tt> that holds lottery when selecting next thread
     * to be run.
     */
    protected class LotteryQueue extends PriorityQueue {
        protected LotteryQueue(boolean transferPriority) {
            super(transferPriority);
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return the next thread that <tt>nextThread()</tt> would return.
         */
        @Override
        protected ThreadState pickNextThread() {
            if ( queue.isEmpty() ) {
                return null;
            }
            //TODO(2.4) Implement pickNextThread() in LotteryScheduler. Base your work on pickNextThread() from PriorityScheduler.

            // Set helper variable to this lottery queue.
            originalCallerQueue = this;

            // Hold the lottery.
            // First calculate the sum of the tickets of all threads.





            // Select random ticket from the heap.


            // Find the thread state of the thread which is owner of the
            // winning ticket.











            Lib.assertNotReached();
            return null;
        }
    }

    /**
     * The scheduling state of a thread. Number of tickets held by
     * this thread is stored in priority field. The number of tickets
     * including donations is stored in effectivePriority field.
     *
     * @see nachos.threads.KThread#schedulingState
     */
    protected class LotteryThreadState extends ThreadState {
        protected LotteryThreadState(KThread thread) {
            super(thread);
        }

        @Override
        protected long getEffectivePriority() {
            //TODO(2.4) Implement effective priority calculation for LotteryScheduler. Look at implementation from PriorityScheduler.
            // Only recalculate tickets sum with donations if required.


            // Loop through all thread resources.

            // Change effective priority only if transferPriority is true,
            // there are some values in queue and the queue is
            // not the queue, which called this method initially.



            // Get thread state of thread owner with all it's donated tickets.

            // If this is not this thread.

            // Add tickets to this thread tickets sum.








            return -1;
        }
    }
}
