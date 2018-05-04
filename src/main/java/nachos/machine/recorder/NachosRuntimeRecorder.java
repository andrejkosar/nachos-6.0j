// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.recorder;

import nachos.machine.Machine;
import nachos.machine.config.Config;
import nachos.machine.lib.Lib;
import nachos.machine.processor.Processor;
import nachos.machine.security.Privilege;
import nachos.threads.KThread;
import nachos.threads.ThreadedKernel;
import nachos.userprog.UThread;
import nachos.userprog.UserKernel;
import nachos.userprog.UserProcess;

import java.io.Serializable;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

/**
 * The nachos runtime recorder. Collects nachos runtime statistics and
 * provides methods to read them with privileged access.
 * <p>
 * Widely used when checking implementation in unit tests.
 */
public class NachosRuntimeRecorder {
    /**
     * Flag to indicate, if syscalls should be recorded
     */
    private final boolean recordSyscalls;
    /**
     * Syscalls called by user processes.
     */
    private final HashMap<Integer, ArrayList<SyscallCallRecord>> syscallRecords;
    /**
     * Files from nachos home directory deleted by user process immediately using unlink.
     */
    private final HashMap<Integer, String> deletedFilesByProcessWithUnlink;
    /**
     * Files from nachos home directory previously marked for deletion with unlink,
     * deleted by user process on file close.
     */
    private final HashMap<Integer, String> deletedFilesByProcessWithClose;
    /**
     * Files from nachos home directory previously marked for deletion with unlink,
     * deleted by user process on process exit.
     */
    private final HashMap<Integer, String> deletedFilesOnProcessExit;
    /**
     * Security manager privilege.
     */
    private Privilege privilege;
    /**
     * The root thread of the nachos machine.
     */
    private KThread mainThread;
    /**
     * The idle thread used when there is no available thread to be run.
     */
    private KThread idleThread;
    /**
     * The thread that has recently moved to ready state.
     */
    private KThread readyThread;
    /**
     * The thread that is currently in execution.
     */
    private KThread currentThread;
    /**
     * The process ID of the process, that successfully called {@link UserKernel#terminate()}
     */
    private Integer userKernelTerminationSuccessfulCallerPid;

    /**
     * Allocate a new nachos runtime recorder.
     */
    public NachosRuntimeRecorder() {
        syscallRecords = new HashMap<>();
        deletedFilesByProcessWithUnlink = new HashMap<>();
        deletedFilesByProcessWithClose = new HashMap<>();
        deletedFilesOnProcessExit = new HashMap<>();
        recordSyscalls = Config.getBoolean("NachosRuntimeRecorder.recordSyscalls");
    }

    /**
     * Init this nachos runtime recorder.
     *
     * @param privilege encapsulates privileged access to the Nachos
     *                  machine.
     */
    public void init(Privilege privilege) {
        Lib.assertTrue(this.privilege == null, "init() called multiple times");
        this.privilege = privilege;
        this.privilege.recorder = new NachosRuntimeRecorderPrivilege();

        System.out.print(" nachos-runtime-recorder");
        System.out.print("\n");
    }

    /**
     * Notify the recorder that the specified thread is the root thread.
     * {@link ThreadedKernel#initialize(String[])} <i>must</i> call this
     * method when it starts threading.
     *
     * @param thread the root thread.
     */
    public void reportMainThread(KThread thread) {
        Lib.assertTrue(this.mainThread == null);
        this.mainThread = thread;
    }

    /**
     * Notify the recorder that the specified thread is the idle thread.
     * {@link KThread#createIdleThread()} <i>must</i> call this method before
     * forking the idle thread.
     *
     * @param thread the idle thread.
     */
    public void reportIdleThread(KThread thread) {
        Lib.assertTrue(this.idleThread == null);
        this.idleThread = thread;
    }

    /**
     * Notify the recorder that the specified thread has moved to the ready
     * state. {@link KThread#ready()} <i>must</i> call this method before
     * returning.
     *
     * @param thread the thread that has been added to the ready set.
     */
    public void reportReadyThread(KThread thread) {
        this.readyThread = thread;
    }

    /**
     * Notify the recorder that the specified thread is now running.
     * {@link KThread#restoreState()} <i>must</i> call this method before
     * returning.
     *
     * @param thread the thread that is now running.
     */
    public void reportRunningThread(KThread thread) {
        privilege.tcb.associateThread(thread);
        currentThread = thread;
    }

    /**
     * Notify the recorder that the current thread has finished.
     * {@link KThread#finish()} <i>must</i> call this method before putting
     * the thread to sleep and scheduling its TCB to be destroyed.
     */
    public void reportFinishingCurrentThread() {
        privilege.tcb.authorizeDestroy(currentThread);
    }

    /**
     * Notify the recorder that last user thread is finishing.
     * {@link UserKernel#terminate()} <i>must</i> call this method.
     */
    public void reportFinishingLastUserThread() {
        userKernelTerminationSuccessfulCallerPid = privilege.doPrivileged(new PrivilegedAction<Integer>() {
            @Override
            public Integer run() {
                return Lib.getInstanceField(((UThread) KThread.currentThread()).process, int.class, "pid");
            }
        });
        boolean intStatus = Machine.interrupt().disable();
        mainThread.ready();
        Machine.interrupt().restore(intStatus);
        KThread.finish();
    }

    /**
     * Notify the recorder that syscall was called.
     *
     * @param pid         caller process pid.
     * @param returnValue value returned by syscall.
     * @param syscall     syscall id.
     * @param a0          first syscall parameter.
     * @param a1          second syscall parameter.
     * @param a2          third syscall parameter.
     * @param a3          fourth syscall parameter.
     * @param extraData   extra data for the syscall (like bytes read by read syscall) if any.
     * @see UserProcess
     */
    private void reportSyscallCalledByUserProcess0(Integer pid, Integer returnValue, Integer syscall, Integer a0, Integer a1, Integer a2, Integer a3, Object extraData) {
        if ( recordSyscalls ) {
            ArrayList<SyscallCallRecord> processRecords = syscallRecords.computeIfAbsent(pid, new Function<Integer, ArrayList<SyscallCallRecord>>() {
                @Override
                public ArrayList<SyscallCallRecord> apply(Integer k) {
                    return new ArrayList<>();
                }
            });
            processRecords.add(new SyscallCallRecord(pid, returnValue, syscall, a0, a1, a2, a3, extraData));
        }
    }

    /**
     * Notify the recorder that syscall was called.
     *
     * @param pid         caller process pid.
     * @param returnValue value returned by syscall.
     * @param syscall     syscall id.
     * @param a0          first syscall parameter.
     * @param a1          second syscall parameter.
     * @param a2          third syscall parameter.
     * @param a3          fourth syscall parameter.
     * @param extraData   extra data for the syscall (e.g. bytes read by read syscall) if any.
     * @see UserProcess
     */
    public void reportSyscallCalledByUserProcess(int pid, int returnValue, int syscall, int a0, int a1, int a2, int a3, Object extraData) {
        reportSyscallCalledByUserProcess0(pid, returnValue, syscall, a0, a1, a2, a3, extraData);
    }

    /**
     * Notify the recorder that syscall was called.
     *
     * @param pid         caller process pid.
     * @param returnValue value returned by syscall.
     * @param syscall     syscall id.
     * @param a0          first syscall parameter.
     * @param a1          second syscall parameter.
     * @param a2          third syscall parameter.
     * @param a3          fourth syscall parameter.
     * @see UserProcess
     */
    public void reportSyscallCalledByUserProcess(int pid, int returnValue, int syscall, int a0, int a1, int a2, int a3) {
        reportSyscallCalledByUserProcess0(pid, returnValue, syscall, a0, a1, a2, a3, null);
    }

    /**
     * Notify the recorder that syscall was called.
     *
     * @param pid     caller process pid.
     * @param syscall syscall id.
     * @param a0      first syscall parameter.
     * @param a1      second syscall parameter.
     * @param a2      third syscall parameter.
     * @param a3      fourth syscall parameter.
     * @see UserProcess
     */
    public void reportSyscallCalledByUserProcess(int pid, int syscall, int a0, int a1, int a2, int a3) {
        reportSyscallCalledByUserProcess0(pid, null, syscall, a0, a1, a2, a3, null);
    }

    /**
     * Notify the recorder about filename of the file deleted by user
     * process from nachos home directory immediately using unlink.
     *
     * @param pid  deleting process pid.
     * @param file filename of the file deleted.
     */
    public void reportFileDeletedByProcessWithUnlink(int pid, String file) {
        deletedFilesByProcessWithUnlink.put(pid, file);
    }

    /**
     * Notify the recorder about filename of the file deleted by user
     * process from nachos home directory when file was closed by process.
     *
     * @param pid  deleting process pid.
     * @param file filename of the file deleted.
     */
    public void reportFileDeletedByProcessWithClose(int pid, String file) {
        deletedFilesByProcessWithClose.put(pid, file);
    }

    /**
     * Notify the recorder about filename of the file deleted by user
     * process from nachos home directory when process exited.
     *
     * @param pid  deleting process pid.
     * @param file filename of the file deleted.
     */
    public void reportFileDeletedByProcessOnExit(int pid, String file) {
        deletedFilesOnProcessExit.put(pid, file);
    }

    /**
     * Notify the recorder that a timer interrupt occurred and was handled by
     * software if a timer interrupt handler was installed. Called by the
     * hardware timer.
     *
     * @param privilege proves the authenticity of this call.
     * @param time      the actual time at which the timer interrupt was
     *                  issued.
     */
    public void timerInterrupt(Privilege privilege, long time) {
        Lib.assertTrue(privilege == this.privilege, "Security violation");
    }

    /**
     * Notify the recorder that a user program executed a syscall instruction.
     *
     * @param privilege proves the authenticity of this call.
     * @return <tt>true</tt> if the kernel exception handler should be called.
     */
    public boolean exceptionHandler(Privilege privilege) {
        Lib.assertTrue(privilege == this.privilege, "Security violation");
        return true;
    }

    /**
     * Notify the recorder that {@link Processor#run()} was invoked. This
     * can be used to simulate user programs.
     *
     * @param privilege proves the authenticity of this call.
     */
    public void runProcessor(Privilege privilege) {
        Lib.assertTrue(privilege == this.privilege, "Security violation");
    }

    /**
     * Request permission to send a packet. The recorder can use this to drop
     * packets very selectively.
     *
     * @param privilege proves the authenticity of this call.
     * @return <tt>true</tt> if the packet should be sent.
     */
    public boolean canSendPacket(Privilege privilege) {
        Lib.assertTrue(privilege == this.privilege, "Security violation");
        return true;
    }

    /**
     * Request permission to receive a packet. The recorder can use this to
     * drop packets very selectively.
     *
     * @param privilege proves the authenticity of this call.
     * @return <tt>true</tt> if the packet should be delivered to the kernel.
     */
    public boolean canReceivePacket(Privilege privilege) {
        Lib.assertTrue(privilege == this.privilege, "Security violation");
        return true;
    }

    /**
     * Class containing syscall record called by user process.
     */
    public static class SyscallCallRecord implements Serializable {
        public final Integer callerPid;
        public final Integer valueReturnedBySyscall;
        public final Integer syscall;
        public final Integer a0;
        public final Integer a1;
        public final Integer a2;
        public final Integer a3;
        public final Object extraData;

        private SyscallCallRecord(Integer callerPid, Integer valueReturnedBySyscall, Integer syscall, Integer a0, Integer a1,
                                  Integer a2, Integer a3, Object extraData) {
            this.callerPid = callerPid;
            this.valueReturnedBySyscall = valueReturnedBySyscall;
            this.syscall = syscall;
            this.a0 = a0;
            this.a1 = a1;
            this.a2 = a2;
            this.a3 = a3;
            this.extraData = extraData;
        }

        @Override
        public String toString() {
            return "SyscallCallRecord{" +
                    "callerPid=" + callerPid +
                    ", valueReturnedBySyscall=" + valueReturnedBySyscall +
                    ", syscall=" + syscall + " (" + syscallToString(syscall) + ")" +
                    ", a0=" + a0 +
                    ", a1=" + a1 +
                    ", a2=" + a2 +
                    ", a3=" + a3 +
                    ", extraData=" + extraData +
                    '}';
        }

        private String syscallToString(int syscall) {
            switch ( syscall ) {
                case UserProcess.syscallHalt:
                    return "halt";
                case UserProcess.syscallExit:
                    return "exit";
                case UserProcess.syscallExec:
                    return "exec";
                case UserProcess.syscallJoin:
                    return "join";
                case UserProcess.syscallCreat:
                    return "creat";
                case UserProcess.syscallOpen:
                    return "open";
                case UserProcess.syscallRead:
                    return "read";
                case UserProcess.syscallWrite:
                    return "write";
                case UserProcess.syscallClose:
                    return "close";
                case UserProcess.syscallUnlink:
                    return "unlink";
                default:
                    return "UNKNOWN";
            }
        }
    }

    /**
     * @see Privilege.NachosRuntimeRecorderPrivilege
     */
    private class NachosRuntimeRecorderPrivilege implements Privilege.NachosRuntimeRecorderPrivilege {
        private NachosRuntimeRecorderPrivilege() {
        }

        @Override
        public KThread mainThread() {
            return mainThread;
        }

        @Override
        public KThread idleThread() {
            return idleThread;
        }

        @Override
        public KThread readyThread() {
            return readyThread;
        }

        @Override
        public KThread currentThread() {
            return currentThread;
        }

        @Override
        public Integer userKernelTerminationSuccessfulCallerPid() {
            return userKernelTerminationSuccessfulCallerPid;
        }

        @Override
        public HashMap<Integer, ArrayList<SyscallCallRecord>> syscallRecords() {
            return syscallRecords;
        }

        @Override
        public HashMap<Integer, String> deletedFilesByProcessWithUnlink() {
            return deletedFilesByProcessWithUnlink;
        }

        @Override
        public HashMap<Integer, String> deletedFilesByProcessWithClose() {
            return deletedFilesByProcessWithClose;
        }

        @Override
        public HashMap<Integer, String> deletedFilesOnProcessExit() {
            return deletedFilesOnProcessExit;
        }
    }
}
