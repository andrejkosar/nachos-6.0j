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
 * Tests for join system call.
 * <p>
 * N.B. All tests in this class depend on working multiprogramming support!
 */
public class Phase2Task3JoinSystemCallTests extends NachosUserProgramTestsSuite {
    public Phase2Task3JoinSystemCallTests() {
        super("phase2/phase2.round.robin.conf");
    }

    /**
     * Tests if calling join suspends current process until child process
     * finishes it's execution.
     * <p>
     * See test_join_1.c for more detailed description on how this was achieved.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleJoin implementation is correct!
     */
    @Test
    public void testIfJoinSuspendsCurrentProcessUntilChildProcessFinishes() throws Throwable {
        final int number = (int) (Math.random() * 123456) + 10;

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_join_1",
                new String[]{"first", Integer.toString(number)});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(number + 1, rootRecords);
        threadAssertEquals(3, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(0).syscall);
        threadAssertNotEquals(-1, rootRecords.get(0).valueReturnedBySyscall);
        int childPid = rootRecords.get(0).valueReturnedBySyscall;
        threadAssertEquals(UserProcess.syscallJoin, rootRecords.get(1).syscall);
        threadAssertEquals(childPid, rootRecords.get(1).a0);
        threadAssertEquals(1, rootRecords.get(1).valueReturnedBySyscall);

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertLastExitSyscallStatusEquals(number + 1, childRecords);
        threadAssertEquals(1, childRecords.size());
    }

    /**
     * Tests if calling join returns -1 when provided process pid does not exist.
     */
    @Test
    public void testIfJoinFailsWhenProvidedProcessPidDoesNotExist() throws Throwable {
        int nonExistentPid = UserProcess.rootPid + 3;

        List<SyscallCallRecord> records = runUserProgram("call_syscall",
                new String[]{"join", Integer.toString(nonExistentPid)}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallJoin, records.get(0).syscall);
        threadAssertEquals(nonExistentPid, records.get(0).a0);
        threadAssertEquals(-1, records.get(0).valueReturnedBySyscall);
    }

    /**
     * Tests if calling join returns -1 when provided child process pid
     * does not belong to process, which is child process of the current
     * process.
     * <p>
     * See test_join_2.c for more detailed description on how this was achieved.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleJoin implementation is correct!
     */
    @Test
    public void testIfJoinFailsWhenProvidedProcessPidIsNotCurrentProcessChildProcessPid() throws Throwable {
        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_join_2", new String[]{"first"});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(0, rootRecords);

        threadAssertEquals(3, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(0).syscall);
        threadAssertNotEquals(-1, rootRecords.get(0).valueReturnedBySyscall);
        int childPid = rootRecords.get(0).valueReturnedBySyscall;

        threadAssertEquals(UserProcess.syscallJoin, rootRecords.get(1).syscall);
        threadAssertEquals(childPid, rootRecords.get(1).a0);
        threadAssertEquals(1, rootRecords.get(1).valueReturnedBySyscall);

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertLastExitSyscallStatusEquals(0, childRecords);
        threadAssertEquals(2, childRecords.size());

        threadAssertEquals(UserProcess.syscallJoin, childRecords.get(0).syscall);
        threadAssertEquals(UserProcess.rootPid, childRecords.get(0).a0);
        threadAssertEquals(-1, childRecords.get(0).valueReturnedBySyscall);
    }

    /**
     * Tests if second call to join returns -1 when called on the same child
     * for the second time.
     * <p>
     * See test_join_3.c for more detailed description on how this was achieved.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleJoin implementation is correct!
     */
    @Test
    public void testIfSecondCallToJoinFailsWhenCalledOnChildProcessTwice() throws Throwable {
        final int number = 25;
        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_join_3",
                new String[]{"first", Integer.toString(number)});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(number, rootRecords);
        threadAssertEquals(4, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(0).syscall);
        threadAssertNotEquals(-1, rootRecords.get(0).valueReturnedBySyscall);
        int childPid = rootRecords.get(0).valueReturnedBySyscall;

        threadAssertEquals(UserProcess.syscallJoin, rootRecords.get(1).syscall);
        threadAssertEquals(1, rootRecords.get(1).valueReturnedBySyscall);

        threadAssertEquals(UserProcess.syscallJoin, rootRecords.get(2).syscall);
        threadAssertEquals(-1, rootRecords.get(2).valueReturnedBySyscall);

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertLastExitSyscallStatusEquals(number, childRecords);
        threadAssertEquals(1, childRecords.size());
    }
}
