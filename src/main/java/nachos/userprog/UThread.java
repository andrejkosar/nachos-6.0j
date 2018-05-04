package nachos.userprog;

import nachos.machine.Machine;
import nachos.machine.lib.Lib;
import nachos.machine.processor.Processor;
import nachos.threads.KThread;

/**
 * A UThread is KThread that can execute user program code inside a user
 * process, in addition to Nachos kernel code.
 */
public class UThread extends KThread {
    /**
     * The process to which this thread belongs.
     */
    public UserProcess process;
    /**
     * <p>
     * Storage for the user register set.
     * </p>
     * <p>
     * A thread capable of running user code actually has <i>two</i> sets of
     * CPU registers: one for its state while executing user code, and one for
     * its state while executing kernel code. While this thread is not running,
     * its user state is stored here.
     * </p>
     */
    private int userRegisters[] = new int[Processor.numUserRegisters];

    /**
     * Allocate a new UThread.
     *
     * @param process UserProcess instance
     */
    public UThread(UserProcess process) {
        super();

        setTarget(new Runnable() {
            @Override
            public void run() {
                runProgram();
            }
        });

        this.process = process;
    }

    private void runProgram() {
        process.initRegisters();
        process.restoreState();

        Machine.processor().run();

        Lib.assertNotReached();
    }

    /**
     * Save state before giving up the processor to another thread.
     */
    @Override
    protected void saveState() {
        process.saveState();

        for ( int i = 0; i < Processor.numUserRegisters; i++ ) {
            userRegisters[i] = Machine.processor().readRegister(i);
        }

        super.saveState();
    }

    /**
     * Restore state before receiving the processor again.
     */
    @Override
    protected void restoreState() {
        super.restoreState();

        for ( int i = 0; i < Processor.numUserRegisters; i++ ) {
            Machine.processor().writeRegister(i, userRegisters[i]);
        }

        process.restoreState();
    }
}
