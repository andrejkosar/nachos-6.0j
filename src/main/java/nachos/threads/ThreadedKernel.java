package nachos.threads;

import nachos.machine.Kernel;
import nachos.machine.Machine;
import nachos.machine.config.Config;
import nachos.machine.io.FileSystem;
import nachos.machine.lib.Lib;

/**
 * A multi-threaded OS kernel.
 */
public class ThreadedKernel extends Kernel {
    /**
     * Globally accessible reference to the scheduler.
     */
    public static Scheduler scheduler = null;
    /**
     * Globally accessible reference to the alarm.
     */
    public static Alarm alarm = null;
    /**
     * Globally accessible reference to the file system.
     */
    public static FileSystem fileSystem = null;

    /**
     * Allocate a new multi-threaded kernel.
     */
    public ThreadedKernel() {
        super();
    }

    /**
     * Initialize this kernel. Creates a scheduler, the first thread, and an
     * alarm, and enables interrupts. Creates a file system if necessary.
     */
    @Override
    public void initialize(String[] args) {
        // set scheduler
        scheduler = Lib.constructObject(Config.getString("ThreadedKernel.scheduler"), Scheduler.class);

        // set fileSystem
        String fileSystemName = Config.getString("ThreadedKernel.fileSystem", null);
        if ( fileSystemName != null ) {
            fileSystem = Lib.constructObject(Config.getString("ThreadedKernel.fileSystem"), FileSystem.class);
        }
        else if ( Machine.stubFileSystem() != null ) {
            fileSystem = Machine.stubFileSystem();
        }
        else {
            fileSystem = null;
        }

        // start threading
        Machine.nachosRuntimeRecorder().reportMainThread(new KThread(null));

        alarm = new Alarm();

        Machine.interrupt().enable();
    }

    /**
     * Test this kernel. Test the <tt>KThread</tt>, <tt>Semaphore</tt>,
     * <tt>SynchList</tt>, and <tt>ElevatorBank</tt> classes.
     */
    @Override
    public void selfTest() {
        KThread.selfTest();
        Semaphore.selfTest();
        SynchList.selfTest();
        InterruptsCondition.selfTest();
        SemaphoresCondition.selfTest();
        Alarm.selfTest();
        Communicator.selfTest();
        PriorityScheduler.selfTest();
        Boat.selfTest();
    }

    /**
     * A threaded kernel does not run user programs, so this method does
     * nothing.
     */
    @Override
    public void run() {
    }

    /**
     * Terminate this kernel. Never returns.
     */
    @Override
    public void terminate() {
        Machine.halt();
    }
}
