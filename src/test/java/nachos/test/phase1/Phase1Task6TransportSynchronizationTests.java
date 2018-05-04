// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase1;

import nachos.test.NachosKernelTestsSuite;
import nachos.threads.Boat;
import org.junit.Test;

/**
 * Tests if Oahu to Molokai transportation synchronization
 * problem is correctly implemented.
 */
public class Phase1Task6TransportSynchronizationTests extends NachosKernelTestsSuite {
    public Phase1Task6TransportSynchronizationTests() {
        super("phase1/phase1.round.robin.conf");
    }

    /**
     * Helper method to do a boat test.
     */
    private void doParametrizedBoatTest(int adults, int children) {
        Boat boat = new Boat();
        boat.begin(adults, children);
        threadAssertEquals(Boat.Location.Molokai, privilegedGetInstanceFieldDeepClone(boat, Boat.Location.class, "boatLocation"));
        threadAssertEquals(0, privilegedGetInstanceFieldDeepClone(boat, int.class, "childrenOnOahu"));
        threadAssertEquals(0, privilegedGetInstanceFieldDeepClone(boat, int.class, "adultsOnOahu"));
        threadAssertEquals(0, privilegedGetInstanceFieldDeepClone(boat, int.class, "childrenOnBoard"));
        threadAssertEquals(children, privilegedGetInstanceFieldDeepClone(boat, int.class, "childrenOnMolokai"));
        threadAssertEquals(adults, privilegedGetInstanceFieldDeepClone(boat, int.class, "adultsOnMolokai"));
    }

    /**
     * Tests Boat implementation with two children and no adults threads.
     */
    @Test
    public void testWithTwoChildren() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                doParametrizedBoatTest(0, 2);
            }
        });

    }

    /**
     * Tests boat implementation with two children and one adult threads.
     */
    @Test
    public void testWithTwoChildrenAndOneAdult() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                doParametrizedBoatTest(1, 2);
            }
        });
    }

    /**
     * Tests boat implementation with three children and three adult threads.
     */
    @Test
    public void testWithThreeChildrenAndThreeAdults() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                doParametrizedBoatTest(3, 3);
            }
        });
    }

    /**
     * Tests boat implementation with 57 children and 43 adult threads.
     */
    @Test
    public void testWithMultipleChildrenAndMultipleAdults() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                doParametrizedBoatTest(43, 57);
            }
        });
    }

    /**
     * Tests boat implementation with no children and 5 adult threads.
     * If the implementation is correct this should timeout (deadlock).
     */
    @Test
    public void testWithNoChildrenAndFiveAdults() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                threadAssertActionTimeout(LIVELOCK_TIMEOUT, new Runnable() {
                    @Override
                    public void run() {
                        doParametrizedBoatTest(5, 0);
                    }
                });
            }
        });
    }

    /**
     * Tests boat implementation with 1 child and 5 adult threads.
     * If the implementation is correct this should timeout (deadlock).
     */
    @Test
    public void testWithOneChildAndFiveAdults() throws Throwable {
        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                threadAssertActionTimeout(LIVELOCK_TIMEOUT, new Runnable() {
                    @Override
                    public void run() {
                        doParametrizedBoatTest(5, 1);
                    }
                });
            }
        });
    }
}
