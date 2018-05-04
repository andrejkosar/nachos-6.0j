package nachos.threads;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    //TODO(1.4) add required variables for the communicator to function






    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        //TODO(1,4) initialize required variables




    }

    public static void selfTest() {
    }

    /**
     * <p>
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     * </p>
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     * </p>
     *
     * @param word the integer to transfer.
     */
    public void speak(int word) {
        //TODO(1.4) implement speak() method
        // Acquire lock so we can use condition variables


        // Put current thread to sleep on speakers condition if
        // there is word, that hasn't been heard yet




        // Word has been heard, so we can speak


        // Wake one listener thread (there can be 0 listening threads)


        // Release lock so other threads can continue

    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return the integer transferred.
     */
    public int listen() {
        //TODO(1.4) implement listen() method
        // Acquire lock so we can use condition variables


        // Put current thread to sleep on listeners condition if
        // there isn't word to be heard




        // There is word which hasn't been heard yet

        // Wake one speaker thread


        // Store word into temporary variable, release the lock
        // and return word that has been heard


        return 0;
    }
}
