// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase2.task3;

import nachos.test.NachosUserProgramTestsSuite;
import nachos.userprog.UserProcess;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static nachos.machine.recorder.NachosRuntimeRecorder.SyscallCallRecord;

/**
 * Tests for halt system call.
 * <p>
 * N.B. All tests in this class depend on working multiprogramming support!
 */
public class Phase2Task3HaltSystemCallTests extends NachosUserProgramTestsSuite {
    public Phase2Task3HaltSystemCallTests() {
        super("phase2/phase2.round.robin.conf");
    }

    /**
     * Tests if calling halt actually stops nachos machine execution.
     */
    @Test
    public void testIfHaltStopsNachosExecution() throws Throwable {
        List<SyscallCallRecord> records = runUserProgram("call_syscall", new String[]{"halt"}).get(UserProcess.rootPid);

        threadAssertEquals(1, records.size());
        threadAssertEquals(UserProcess.syscallHalt, records.get(0).syscall);
        threadAssertNull(records.get(0).valueReturnedBySyscall);

        threadAssertEquals(UserProcess.rootPid, getSuccessfulUserKernelTerminateCallerPid());
    }

    /**
     * Tests if calling halt actually stops nachos machine execution and
     * prevents any created child process to start it's execution.
     * <p>
     * See test_halt_1.c for more detailed description on how this was achieved.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleHalt implementation is correct!
     */
    @Test
    public void testIfHaltStopsNachosExecutionAlongWithChildProcess() throws Throwable {
        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_halt_1", new String[]{"first"});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertEquals(2, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(0).syscall);
        threadAssertNotEquals(-1, rootRecords.get(0).valueReturnedBySyscall);
        int childPid = rootRecords.get(0).valueReturnedBySyscall;
        threadAssertEquals(UserProcess.syscallHalt, rootRecords.get(1).syscall);
        threadAssertNull(rootRecords.get(1).valueReturnedBySyscall);

        threadAssertNull(recordsMap.get(childPid));

        threadAssertEquals(UserProcess.rootPid, getSuccessfulUserKernelTerminateCallerPid());
    }

    /**
     * Tests if calling halt does not stop nachos machine execution, if
     * called by other than root process.
     * <p>
     * See test_halt_2.c for more detailed description on how this was achieved.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleHalt implementation is correct!
     */
    @Test
    public void testIfHaltDoesNotStopsNachosExecutionWhenCalledByNonRootProcess() throws Throwable {
        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_halt_2", new String[]{"first"});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertEquals(3, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(0).syscall);
        threadAssertNotEquals(-1, rootRecords.get(0).valueReturnedBySyscall);
        int childPid = rootRecords.get(0).valueReturnedBySyscall;
        threadAssertEquals(UserProcess.syscallHalt, rootRecords.get(2).syscall);
        threadAssertNull(rootRecords.get(2).valueReturnedBySyscall);

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertEquals(1, childRecords.size());
        threadAssertEquals(UserProcess.syscallHalt, childRecords.get(0).syscall);
        threadAssertNull(childRecords.get(0).valueReturnedBySyscall);

        threadAssertEquals(UserProcess.rootPid, getSuccessfulUserKernelTerminateCallerPid());
    }
}
