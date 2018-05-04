// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.machine.lib.Lib;

/**
 * An OS kernel.
 */
public abstract class Kernel {
    /**
     * Globally accessible reference to the kernel.
     */
    public static Kernel kernel;

    static {
        initializeStaticFields();
    }

    /**
     * Allocate a new kernel.
     */
    public Kernel() {
        // make sure only one kernel is created
        Lib.assertTrue(kernel == null);
        kernel = this;
    }

    private static void initializeStaticFields() {
        kernel = null;
    }

    /**
     * Initialize this kernel.
     *
     * @param args command line arguments
     */
    public abstract void initialize(String[] args);

    /**
     * Test that this module works.
     */
    public abstract void selfTest();

    /**
     * Begin executing user programs, if applicable.
     */
    public abstract void run();

    /**
     * Terminate this kernel. Never returns.
     */
    public abstract void terminate();
}

