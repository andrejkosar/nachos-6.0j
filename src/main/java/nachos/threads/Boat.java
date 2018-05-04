package nachos.threads;

import java.io.Serializable;

/**
 * <p>
 * Contains Oahu to Molokai boat transport synchronization
 * problem solution.
 * </p>
 * <p>
 * Also includes functions to be called to show, that
 * your solution is properly synchronized. Simply prints
 * messages to standard out, so that you can watch it.
 * </p>
 * <p>
 * Note that this file includes all possible variants of how
 * someone can get from one island to another. Inclusion in
 * this class does not imply that any of the indicated actions
 * are a good idea or even allowed.
 * </p>
 */
public class Boat {
    /**
     * Current boat location.
     */
    private Location boatLocation;

    /**
     * Number of children currently on Oahu.
     */
    private int childrenOnOahu;

    /**
     * Number of adults currently on Oahu.
     */
    private int adultsOnOahu;

    /**
     * Number of children currently on Molokai.
     */
    private int childrenOnMolokai;

    /**
     * Number of adults currently on Molokai.
     */
    private int adultsOnMolokai;

    /**
     * Number of children currently on board.
     */
    private int childrenOnBoard;

    //TODO(1.6) Add as many additional variables as you need to implement correct solution






    public Boat() {
    }

    public static void selfTest() {
    }

    /**
     * Mark start of the child itinerary.
     */
    private void initializeChild() {
        System.out.println("A child has forked.");
    }

    /**
     * Mark start of the adult itinerary.
     */
    private void initializeAdult() {
        System.out.println("An adult has forked.");
    }

    /**
     * Should be called when a child pilots the boat from Oahu to Molokai
     */
    private void childRowToMolokai() {
        System.out.println("**Child rowing to Molokai.");
    }

    /**
     * Should be called when a child pilots the boat from Molokai to Oahu
     */
    private void childRowToOahu() {
        System.out.println("**Child rowing to Oahu.");
    }

    /**
     * Should be called when a child not piloting the boat disembarks on Molokai
     */
    private void childRideToMolokai() {
        System.out.println("**Child arrived on Molokai as a passenger.");
    }

    /**
     * Should be called when a child not piloting the boat disembarks on Oahu
     */
    private void childRideToOahu() {
        System.out.println("**Child arrived on Oahu as a passenger.");
    }

    /**
     * Should be called when a adult pilots the boat from Oahu to Molokai
     */
    private void adultRowToMolokai() {
        System.out.println("**Adult rowing to Molokai.");
    }

    /**
     * Should be called when a adult pilots the boat from Molokai to Oahu
     */
    private void adultRowToOahu() {
        System.out.println("**Adult rowing to Oahu.");
    }

    /**
     * Should be called when an adult not piloting the boat disembarks on Molokai
     */
    private void adultRideToMolokai() {
        System.out.println("**Adult arrived on Molokai as a passenger.");
    }

    /**
     * Should be called when an adult not piloting the boat disembarks on Oahu
     */
    private void adultRideToOahu() {
        System.out.println("**Adult arrived on Oahu as a passenger.");
    }

    /**
     * Try to solve transportation synchronization problem with specified
     * amounts of the passengers.
     *
     * @param adults   initial number of adults on Oahu.
     * @param children initial number of children on Oahu.
     */
    public void begin(int adults, int children) {
        //TODO(1.6) Implement this method. It serves as entry point for the Boat synchronization problem simulation.
        // Instantiate required variables.
        boatLocation = Location.Oahu;
        childrenOnOahu = children;
        adultsOnOahu = adults;
        childrenOnMolokai = 0;
        adultsOnMolokai = 0;
        childrenOnBoard = 0;

        // Instantiate additional variables






        // Create threads for every child and every adult.
        // Every child's run method will be childItinerary() and
        // every adult's run method will execute adultItinerary().
        for ( int i = 0; i < children; i++ ) {
            new KThread(new Runnable() {
                @Override
                public void run() {
                    childItinerary(Location.Oahu);
                }
            }).setName("Child - " + i).fork();
        }

        for ( int i = 0; i < adults; i++ ) {
            new KThread(new Runnable() {
                @Override
                public void run() {
                    adultItinerary(Location.Oahu);
                }
            }).setName("Adult - " + i).fork();
        }

        // You will reach a point in your simulation where the adult and child threads
        // believe that everyone is across on Molokai. At this point, you are allowed to
        // do one-way communication from the threads to begin() in order to inform it that
        // the simulation may be over. It may be possible, however, that your adult and
        // child threads are incorrect.






        // You will find condition variables to be the most useful synchronization method
        // for this problem.




        // Yield the main thread to give other threads chance to run.
        KThread.yield();
    }

    /**
     * Helper method to do all necessary operations when rowing adult to Molokai.
     *
     * @return returns {@link Location#Molokai}
     */
    private Location rowAdultToMolokai() {
        //TODO(1.6) Complete implementation of this method.
        // Print what is happening.
        adultRowToMolokai();
        // Correctly change all required variables and return boat location at the end.



        return Location.Molokai;
    }

    /**
     * Helper method to do all necessary operations when rowing child to Molokai.
     *
     * @return returns {@link Location#Molokai}
     */
    private Location rowChildToMolokai() {
        //TODO(1.6) Complete implementation of this method.
        // Print what is happening.
        childRowToMolokai();
        // Correctly change all required variables and return boat location at the end.



        return Location.Molokai;
    }

    /**
     * Helper method to do all necessary operations when riding child to Molokai.
     *
     * @return returns {@link Location#Molokai}
     */
    private Location rideChildToMolokai() {
        //TODO(1.6) Complete implementation of this method.
        // Print what is happening.
        childRideToMolokai();
        // Correctly change all required variables and return boat location at the end.



        return Location.Molokai;
    }

    /**
     * Helper method to do all necessary operations when rowing child to Oahu.
     *
     * @return returns {@link Location#Oahu}
     */
    private Location rowChildToOahu() {
        //TODO(1.6) Complete implementation of this method.
        // Print what is happening.
        childRowToOahu();
        // Correctly change all required variables and return boat location at the end.



        return Location.Oahu;
    }

    /**
     * Represents adult decisions when taking it's itinerary from
     * Oahu to Molokai. It's called as body of the adult thread.
     *
     * @param location itinerary starting location
     *                 (should be equal to {@link Location#Oahu})
     */
    private void adultItinerary(Location location) {
        initializeAdult();

        //TODO(1.6) Implement adult decision logic

















    }

    /**
     * Represents child decisions when taking it's itinerary from
     * Oahu to Molokai. It's called as body of the child thread.
     *
     * @param location itinerary starting location
     *                 (should be equal to {@link Location#Oahu})
     */
    private void childItinerary(Location location) {
        initializeChild();

        //TODO(1.6) Implement child decision logic






















    }

    /**
     * Enum representing current location of the boat or passenger.
     */
    public enum Location implements Serializable {
        Oahu, Molokai
    }
}
