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
 * Tests for exit system call.
 * <p>
 * N.B. All tests in this class depend on working multiprogramming support!
 */
public class Phase2Task3ExitSystemCallTests extends NachosUserProgramTestsSuite {
    public Phase2Task3ExitSystemCallTests() {
        super("phase2/phase2.round.robin.conf");
    }

    /**
     * Tests if calling exit terminates current process with specified exit status.
     */
    @Test
    public void testIfExitTerminatesProcessWithProvidedExitStatus() throws Throwable {
        List<SyscallCallRecord> records = runUserProgram("call_syscall", new String[]{"exit", "666"}).get(UserProcess.rootPid);

        threadAssertEquals(1, records.size());
        threadAssertLastExitSyscallStatusEquals(666, records);
    }

    /**
     * Tests if last call to exit terminates nachos machine.
     * <p>
     * See test_exit_1.c for more detailed description on how this was achieved.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleExit implementation is correct!
     */
    @Test
    public void testIfLastCallToExitTerminatesNachosMachine() throws Throwable {
        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_exit_1", new String[]{"first", "666"});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertEquals(2, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(0).syscall);
        threadAssertNotEquals(-1, rootRecords.get(0).valueReturnedBySyscall);
        int childPid = rootRecords.get(0).valueReturnedBySyscall;
        threadAssertLastExitSyscallStatusEquals(666, rootRecords);

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertEquals(1, childRecords.size());

        threadAssertLastExitSyscallStatusEquals(666, childRecords);

        threadAssertEquals(childPid, getSuccessfulUserKernelTerminateCallerPid());
    }

    /**
     * Tests if calling exit deletes file marked for deletion, if the file is
     * not opened by other process.
     * <p>
     * See test_exit_2.c for more detailed description on how this was achieved.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleExit implementation is correct!
     */
    @Test
    public void testIfExitDeletesFileMarkedForDeletionNotOpenedByOtherProcess() throws Throwable {
        final String file = "test_file.txt";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));

        List<SyscallCallRecord> records = runUserProgram("test_exit_2", new String[]{file, "666"}).get(UserProcess.rootPid);

        threadAssertEquals(3, records.size());
        threadAssertLastExitSyscallStatusEquals(666, records);

        threadAssertFalse(doesFileExistsInNachosHomeDirectory(file));

        threadAssertEquals(UserProcess.rootPid, getSuccessfulUserKernelTerminateCallerPid());
        threadAssertNull(getDeletedFilesByProcessWithUnlink().get(UserProcess.rootPid));
        threadAssertNull(getDeletedFilesByProcessWithClose().get(UserProcess.rootPid));
        threadAssertEquals(file, getDeletedFilesOnProcessExit().get(UserProcess.rootPid));
    }

    /**
     * Tests if calling exit does not delete file marked for deletion, if the file
     * still remains opened by some other process.
     * <p>
     * See test_exit_3.c for more detailed description on how this was achieved.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleExit implementation is correct!
     */
    @Test
    public void testIfExitDoesNotDeleteFileMarkedForDeletionStillOpenedByOtherProcess() throws Throwable {
        final String file = "test_file.txt";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_exit_3",
                new String[]{"first", file, "666"});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertEquals(4, rootRecords.size());
        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(1).syscall);
        threadAssertNotEquals(-1, rootRecords.get(1).valueReturnedBySyscall);
        int childPid = rootRecords.get(1).valueReturnedBySyscall;
        threadAssertLastExitSyscallStatusEquals(666, rootRecords);

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertEquals(3, childRecords.size());
        threadAssertLastExitSyscallStatusEquals(666, childRecords);

        threadAssertFalse(doesFileExistsInNachosHomeDirectory(file));

        threadAssertNull(getDeletedFilesOnProcessExit().get(childPid));
        threadAssertEquals(UserProcess.rootPid, getSuccessfulUserKernelTerminateCallerPid());
        threadAssertEquals(file, getDeletedFilesOnProcessExit().get(UserProcess.rootPid));
    }
}
