// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.security;

import nachos.machine.Stats;
import nachos.machine.io.SerialConsole;
import nachos.threads.KThread;

import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;

import static nachos.machine.recorder.NachosRuntimeRecorder.SyscallCallRecord;

/**
 * <p>
 * A capability that allows privileged access to the Nachos machine.
 * </p>
 * <p>
 * Some privileged operations are guarded by the Nachos security manager:
 * </p>
 * <ol>
 * <li>creating threads</li>
 * <li>writing/deleting files in the test directory</li>
 * <li>exit with specific status code</li>
 * </ol>
 * These operations can only be performed through <tt>doPrivileged()</tt>.
 * <p>
 * Some privileged operations require a capability:
 * </p>
 * <ol>
 * <li>scheduling interrupts</li>
 * <li>advancing the simulated time</li>
 * <li>accessing machine statistics</li>
 * <li>installing a console</li>
 * <li>flushing the simulated processor's pipeline</li>
 * <li>approving TCB operations</li>
 * </ol>
 * <p>
 * These operations can be directly performed using a <tt>Privilege</tt>
 * object.
 * </p>
 * <p>
 * The Nachos kernel should <i>never</i> be able to directly perform any of
 * these privileged operations. If you have discovered a loophole somewhere,
 * notify someone.
 * </p>
 */
public abstract class Privilege {
    /**
     * Nachos runtime statistics.
     */
    public Stats stats;
    /**
     * Provides access to some private <tt>Machine</tt> methods.
     */
    public MachinePrivilege machine;
    /**
     * Provides access to some private <tt>Interrupt</tt> methods.
     */
    public InterruptPrivilege interrupt;
    /**
     * Provides access to some private <tt>Processor</tt> methods.
     */
    public ProcessorPrivilege processor;
    /**
     * Provides access to some private <tt>TCB</tt> methods and fields.
     */
    public TCBPrivilege tcb;
    /**
     * Provides access to some private <tt>NachosMachineRuntime</tt> fields.
     */
    public NachosRuntimeRecorderPrivilege recorder;

    /**
     * Allocate a new <tt>Privilege</tt> object. Note that this object in
     * itself does not encapsulate privileged access until the machine devices
     * fill it in.
     */
    public Privilege() {
    }

    /**
     * Perform the specified action with privilege.
     *
     * @param action the action to perform.
     */
    public abstract void doPrivileged(Runnable action);

    /**
     * Perform the specified <tt>PrivilegedAction</tt> with privilege.
     *
     * @param action the action to perform.
     * @return the return value of the action.
     */
    public abstract <T> T doPrivileged(PrivilegedAction<T> action);

    /**
     * Perform the specified <tt>PrivilegedExceptionAction</tt> with privilege.
     *
     * @param action the action to perform.
     * @return the return value of the action.
     * @throws PrivilegedActionException thrown in default implementation
     */
    public abstract <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException;

    public final void exit() {
        tcb.nachosExit();
    }

    public final void exit(Throwable cause) {
        tcb.nachosExit(cause);
    }

    /**
     * An interface that provides access to some private <tt>Machine</tt>
     * methods.
     */
    public interface MachinePrivilege {
        /**
         * Install a hardware console.
         *
         * @param console the new hardware console.
         */
        void setConsole(SerialConsole console);
    }

    /**
     * An interface that provides access to some private <tt>Interrupt</tt>
     * methods.
     */
    public interface InterruptPrivilege {
        /**
         * Schedule an interrupt to occur at some time in the future.
         *
         * @param when    the number of ticks until the interrupt should
         *                occur.
         * @param type    a name for the type of interrupt being
         *                scheduled.
         * @param handler the interrupt handler to call.
         */
        void schedule(long when, String type, Runnable handler);

        /**
         * Advance the simulated time.
         *
         * @param inKernelMode <tt>true</tt> if the current thread is running kernel
         *                     code, <tt>false</tt> if the current thread is running
         *                     MIPS user code.
         */
        void tick(boolean inKernelMode);
    }

    /**
     * An interface that provides access to some private <tt>Processor</tt>
     * methods.
     */
    public interface ProcessorPrivilege {
        /**
         * Flush the processor pipeline in preparation for switching to kernel
         * mode.
         */
        void flushPipe();
    }

    /**
     * An interface that provides access to some private <tt>NachosRuntimeRecorder</tt> fields.
     * Used for testing.
     */
    public interface NachosRuntimeRecorderPrivilege {
        KThread mainThread();

        KThread idleThread();

        KThread readyThread();

        KThread currentThread();

        Integer userKernelTerminationSuccessfulCallerPid();

        HashMap<Integer, ArrayList<SyscallCallRecord>> syscallRecords();

        HashMap<Integer, String> deletedFilesByProcessWithUnlink();

        HashMap<Integer, String> deletedFilesByProcessWithClose();

        HashMap<Integer, String> deletedFilesOnProcessExit();
    }

    /**
     * An abstract class that provides access to some private <tt>TCB</tt> methods and fields.
     */
    public static abstract class TCBPrivilege {
        /**
         * Associate the current TCB with the specified <tt>KThread</tt>.
         * <tt>NachosRuntimeRecorder.reportRunningThread()</tt> <i>must</i> call this method
         * before returning.
         *
         * @param thread the current thread.
         */
        public abstract void associateThread(KThread thread);

        /**
         * Authorize the TCB associated with the specified thread to be
         * destroyed.
         *
         * @param thread the thread whose TCB is about to be destroyed.
         */
        public abstract void authorizeDestroy(KThread thread);

        /**
         * Sets flag, that is checked in TCB.start() and TCB.destroy(), where it checks
         * if currentTCB.javaThread equals current java thread got by Thread.currentThread().
         *
         * @param value the value to be assigned
         */
        public abstract void setShouldCheckForNonNachosThreads(boolean value);

        /**
         * Exit nachos kindly returning 0 as exit status.
         */
        protected abstract void nachosExit();

        /**
         * Exit nachos on specified exception returning 1 as exit status.
         *
         * @param cause exception causing nachos to stop.
         */
        protected abstract void nachosExit(Throwable cause);
    }
}
