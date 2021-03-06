package nachos.vm;

import nachos.userprog.UserKernel;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    private static final char dbgVM = 'v';

    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
        super();
    }

    /**
     * Initialize this kernel.
     */
    @Override
    public void initialize(String[] args) {
        super.initialize(args);
    }

    /**
     * Test this kernel.
     */
    @Override
    public void selfTest() {
        super.selfTest();
    }

    /**
     * Start running user programs.
     */
    @Override
    public void run() {
        super.run();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    @Override
    public void terminate() {
        super.terminate();
    }
}
