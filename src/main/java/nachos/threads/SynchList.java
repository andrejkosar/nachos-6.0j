package nachos.threads;

import nachos.machine.lib.Lib;

import java.util.LinkedList;

/**
 * A synchronized queue.
 */
public class SynchList<T> {
    private LinkedList<T> list;
    private Lock lock;
    private Condition listEmpty;

    /**
     * Allocate a new synchronized queue.
     */
    public SynchList() {
        list = new LinkedList<>();
        lock = new Lock();
        listEmpty = new SemaphoresCondition(lock);
    }

    /**
     * Test that this module is working.
     */
    public static void selfTest() {
        SynchList<Integer> ping = new SynchList<>();
        SynchList<Integer> pong = new SynchList<>();

        new KThread(new PingTest(ping, pong)).setName("ping").fork();

        for ( int i = 0; i < 10; i++ ) {
            Integer o = i;
            ping.add(i);
            Lib.assertTrue(pong.removeFirst().equals(o));
        }
    }

    /**
     * Add the specified object to the end of the queue. If another thread is
     * waiting in <tt>removeFirst()</tt>, it is woken up.
     *
     * @param o the object to add. Must not be <tt>null</tt>.
     */
    public void add(T o) {
        Lib.assertTrue(o != null);

        lock.acquire();
        list.add(o);
        listEmpty.wake();
        lock.release();
    }

    /**
     * Remove an object from the front of the queue, blocking until the queue
     * is non-empty if necessary.
     *
     * @return the element removed from the front of the queue.
     */
    public T removeFirst() {
        T o;

        lock.acquire();
        while ( list.isEmpty() ) {
            listEmpty.sleep();
        }
        o = list.removeFirst();
        lock.release();

        return o;
    }

    private static class PingTest implements Runnable {
        private SynchList<Integer> ping;
        private SynchList<Integer> pong;

        PingTest(SynchList<Integer> ping, SynchList<Integer> pong) {
            this.ping = ping;
            this.pong = pong;
        }

        @Override
        public void run() {
            for ( int i = 0; i < 10; i++ ) {
                pong.add(ping.removeFirst());
            }
        }
    }
}

