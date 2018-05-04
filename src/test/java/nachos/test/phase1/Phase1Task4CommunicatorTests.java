// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase1;

import nachos.test.NachosKernelTestsSuite;
import nachos.threads.Communicator;
import nachos.threads.KThread;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for Communicator class implementation.
 */
public class Phase1Task4CommunicatorTests extends NachosKernelTestsSuite {
    private final String expectedLoggerOutput =
            "Speaker 1 speaks a word 11\n" +
                    "Listener 1 heard a word 11\n" +
                    "Speaker 1 speaks a word 12\n" +
                    "Listener 2 heard a word 12\n" +
                    "Speaker 2 speaks a word 21\n" +
                    "Listener 3 heard a word 21\n" +
                    "Speaker 1 speaks a word 13\n" +
                    "Listener 4 heard a word 13\n" +
                    "Speaker 2 speaks a word 22\n" +
                    "Listener 5 heard a word 22\n" +
                    "Speaker 1 speaks a word 14\n" +
                    "Listener 6 heard a word 14\n" +
                    "Speaker 2 speaks a word 23\n" +
                    "Listener 7 heard a word 23\n" +
                    "Speaker 1 speaks a word 15\n" +
                    "Listener 8 heard a word 15\n" +
                    "Speaker 2 speaks a word 24\n" +
                    "Listener 9 heard a word 24\n" +
                    "Speaker 2 speaks a word 25\n" +
                    "Listener 10 heard a word 25\n";
    private final StringBuilder logger = new StringBuilder();
    private boolean speakerHasSpoken = false;
    private boolean listenerHasListened = false;
    private int spokenWord;
    private int heardWord;

    public Phase1Task4CommunicatorTests() {
        super("phase1/phase1.round.robin.conf");
    }

    /**
     * Does simple test with one speaker and one listener thread. Speaker
     * sends one message to listener using communicator instance.
     */
    @Test
    public void testOneMessageTransferWithOneSpeakerAndOneListener() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                Communicator communicator = new Communicator();

                // Create speaker thread, which will speak random word
                // using communicator.
                KThread speaker = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        threadAssertFalse(speakerHasSpoken);
                        threadAssertFalse(listenerHasListened);
                        spokenWord = (int) (Math.random() * 666);
                        communicator.speak(spokenWord);
                        speakerHasSpoken = true;
                    }
                }).setName("speaker");

                // Create listener thread, whom will be random word
                // delivered.
                KThread listener = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        threadAssertFalse(speakerHasSpoken);
                        threadAssertFalse(listenerHasListened);
                        heardWord = communicator.listen();
                        listenerHasListened = true;
                    }
                }).setName("listener");

                // Fork both threads. Notice that we are forking listener
                // thread sooner than speaker thread. As we are using round
                // robin scheduler, it will get called first, but it should
                // block, because there is no word yet to be heard.
                listener.fork();
                speaker.fork();

                // Wait will both threads finish.
                while ( !areAllThreadsFinished(listener, speaker) ) {
                    KThread.yield();
                }

                // Check that both speaker and listener ran and that spoken
                // and heard word are equal.
                threadAssertTrue(speakerHasSpoken);
                threadAssertTrue(listenerHasListened);
                threadAssertEquals(spokenWord, heardWord);
            }
        });
    }

    /**
     * Tests sending multiple messages through communicator using
     * multiple speakers and multiple listeners. At the end expects
     * correct logger output based on the expected scheduling from
     * round robin scheduler.
     *
     * @see nachos.threads.RoundRobinScheduler
     */
    @Test
    public void testMultipleMessagesTransferUsingMultipleSpeakersAndListeners() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                // Initialize communicator instance and helper variables.
                // We will have 2 speakers and every speaker will speak 5
                // words. Listeners count will be 10, so there will be 1
                // listener for every word spoken by a speaker.
                final int wordsSpokenBySpeaker = 5;
                final int speakersCount = 2;
                final int wordsHeardByListener = 1;
                final int listenersCount = 10;
                final Communicator communicator = new Communicator();

                // Create and fork speaker threads.
                List<KThread> threads = new ArrayList<>(12);
                for ( int i = 1; i < speakersCount + 1; i++ ) {
                    final int speakerIndex = i;
                    KThread speaker = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            for ( int j = 1; j < wordsSpokenBySpeaker + 1; j++ ) {
                                // Word spoken by speaker will computed by simple
                                // algorithm, that ensures every word uniqueness.
                                int wordToSpeak = speakerIndex * 10 + j;
                                communicator.speak(wordToSpeak);
                                logger.append("Speaker ").append(speakerIndex).append(" speaks a word ").append(wordToSpeak).append("\n");
                            }
                        }
                    }).setName("speaker " + speakerIndex);
                    speaker.fork();
                    threads.add(speaker);
                }

                // Create and fork listener threads.
                for ( int i = 1; i < listenersCount + 1; i++ ) {
                    final int listenerIndex = i;
                    KThread listener = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            for ( int j = 1; j < wordsHeardByListener + 1; j++ ) {
                                int wordHeard = communicator.listen();
                                logger.append("Listener ").append(listenerIndex).append(" heard a word ").append(wordHeard).append("\n");
                            }
                        }
                    }).setName("listener " + listenerIndex);
                    listener.fork();
                    threads.add(listener);
                }

                // Wait for all threads to finish using yielding main thread.
                while ( !areAllThreadsFinished(threads) ) {
                    KThread.yield();
                }

                // Check if output is correct.
                threadAssertEquals(expectedLoggerOutput, logger.toString());
            }
        });
    }
}
