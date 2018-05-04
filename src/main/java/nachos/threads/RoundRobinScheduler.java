package nachos.threads;

import nachos.machine.Machine;
import nachos.machine.lib.Lib;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A round-robin scheduler tracks waiting threads in FIFO queues, implemented
 * with linked lists. When a thread begins waiting for access, it is appended
 * to the end of a list. The next thread to receive access is always the first
 * thread in the list. This causes access to be given on a first-come
 * first-serve basis.
 */
public class RoundRobinScheduler extends Scheduler {
    /**
     * Allocate a new round-robin scheduler.
     */
    public RoundRobinScheduler() {
    }

    /**
     * Allocate a new FIFO thread queue.
     *
     * @param transferPriority ignored. Round robin schedulers have
     *                         no priority.
     * @return a new FIFO thread queue.
     */
    @Override
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new FifoQueue();
    }

    private class FifoQueue extends ThreadQueue {
        private Queue<KThread> waitQueue = new LinkedList<>();

        /**
         * Add a thread to the end of the wait queue.
         *
         * @param thread the thread to append to the queue.
         */
        @Override
        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());

            waitQueue.add(thread);
        }

        /**
         * Remove a thread from the beginning of the queue.
         *
         * @return the first thread on the queue, or <tt>null</tt> if the
         * queue is
         * empty.
         */
        @Override
        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());

            return waitQueue.poll();
        }

        /**
         * The specified thread has received exclusive access, without using
         * <tt>waitForAccess()</tt> or <tt>nextThread()</tt>. Assert that no
         * threads are waiting for access.
         */
        @Override
        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());

            Lib.assertTrue(waitQueue.isEmpty());
        }

        /**
         * Print out the contents of the queue.
         */
        @Override
        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());

            for ( KThread thread : waitQueue ) {
                System.out.print(thread + " ");
            }
        }
    }
}
