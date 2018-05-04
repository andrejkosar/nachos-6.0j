// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase2.task1;

import nachos.test.NachosUserProgramTestsSuite;
import nachos.userprog.UserProcess;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static nachos.machine.recorder.NachosRuntimeRecorder.SyscallCallRecord;

/**
 * Tests for unlink system call.
 */
public class Phase2Task1UnlinkSystemCallTests extends NachosUserProgramTestsSuite {
    public Phase2Task1UnlinkSystemCallTests() {
        super("phase2/phase2.round.robin.conf");
    }

    /**
     * Tests if calling unlink immediately deletes provided file, if the file is not
     * opened by some process.
     */
    @Test
    public void testIfUnlinkDeletesFileNotOpenedBySomeProcess() throws Throwable {
        final String file = "test_file.txt";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));

        List<SyscallCallRecord> records = runUserProgram("call_syscall", new String[]{"unlink", file}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallUnlink, records.get(0).syscall);
        threadAssertEquals(0, records.get(0).valueReturnedBySyscall);

        threadAssertFalse(doesFileExistsInNachosHomeDirectory(file));

        threadAssertEquals(file, getDeletedFilesByProcessWithUnlink().get(UserProcess.rootPid));
    }

    /**
     * Tests if calling unlink does not delete file immediately, if the file
     * is opened by current process only.
     * <p>
     * See test_unlink_1.c for more detailed description on how it is done.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleClose implementation is correct!
     */
    @Test
    public void testIfUnlinkDoesNotDeleteFileOpenedByCurrentProcess() throws Throwable {
        final String file = "test_file.txt";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));

        List<SyscallCallRecord> records = runUserProgram("test_unlink_1", new String[]{file}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(4, records.size());

        threadAssertEquals(UserProcess.syscallUnlink, records.get(1).syscall);
        threadAssertEquals(0, records.get(1).valueReturnedBySyscall);

        threadAssertFalse(doesFileExistsInNachosHomeDirectory(file));

        threadAssertNull(getDeletedFilesByProcessWithUnlink().get(UserProcess.rootPid));
        threadAssertEquals(file, getDeletedFilesByProcessWithClose().get(UserProcess.rootPid));
    }

    /**
     * Tests if calling unlink does not delete file immediately, if the file
     * is opened by other than current process.
     * <p>
     * See test_unlink_2.c for more detailed description on how it is done.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleClose implementation is correct!
     * <p>
     * N.B.2 Depends on working multiprogramming support!
     */
    @Test
    public void testIfUnlinkDoesNotDeleteFileOpenedByOtherThanCurrentProcess() throws Throwable {
        final String file = "test_file.txt";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_unlink_2", new String[]{"first", file});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(0, rootRecords);
        threadAssertEquals(5, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(1).syscall);
        threadAssertNotEquals(-1, rootRecords.get(1).valueReturnedBySyscall);
        int childPid = rootRecords.get(1).valueReturnedBySyscall;

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertLastExitSyscallStatusEquals(0, childRecords);
        threadAssertEquals(2, childRecords.size());

        threadAssertEquals(UserProcess.syscallUnlink, childRecords.get(0).syscall);
        threadAssertEquals(0, childRecords.get(0).valueReturnedBySyscall);

        threadAssertFalse(doesFileExistsInNachosHomeDirectory(file));

        threadAssertNull(getDeletedFilesByProcessWithUnlink().get(childPid));
        threadAssertEquals(file, getDeletedFilesByProcessWithClose().get(UserProcess.rootPid));
    }

    /**
     * Tests if calling unlink returns -1 when provided file name exceeds
     * max string length (256), because readVirtualMemoryString() is
     * expected to return null.
     */
    @Test
    public void testIfUnlinkFailsWhenProvidedFilenameExceedsMaxStringLength() throws Throwable {
        char[] chars = new char[UserProcess.maxStrLength + 10];
        Arrays.fill(chars, 'a');
        final String file = new String(chars);
        threadAssertThat(file.length(), Is.is(Matchers.greaterThan(UserProcess.maxStrLength)));

        List<SyscallCallRecord> records = runUserProgram("call_syscall", new String[]{"unlink", file}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallUnlink, records.get(0).syscall);
        threadAssertEquals(-1, records.get(0).valueReturnedBySyscall);
    }
}
