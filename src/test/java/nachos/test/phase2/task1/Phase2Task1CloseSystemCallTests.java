// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase2.task1;

import nachos.test.NachosUserProgramTestsSuite;
import nachos.userprog.UserProcess;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static nachos.machine.recorder.NachosRuntimeRecorder.SyscallCallRecord;

/**
 * Tests for close system call.
 */
public class Phase2Task1CloseSystemCallTests extends NachosUserProgramTestsSuite {
    public Phase2Task1CloseSystemCallTests() {
        super("phase2/phase2.round.robin.conf");
    }

    /**
     * Tests if calling close successfully closes opened file without modifying it.
     * <p>
     * See test_close_1.c for more detailed description on how it is done.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleClose implementation is correct!
     */
    @Test
    public void testIfCloseSuccessfullyClosesFile() throws Throwable {
        final String file = "test_file.txt";
        final String fileContens = readContentsOfFileInNachosHomeDirectory(file);
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));

        List<SyscallCallRecord> records = runUserProgram("test_close_1", new String[]{file}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(3, records.size());

        threadAssertEquals(UserProcess.syscallClose, records.get(1).syscall);
        threadAssertEquals(0, records.get(1).valueReturnedBySyscall);

        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));
        threadAssertEquals(fileContens, readContentsOfFileInNachosHomeDirectory(file));
    }

    /**
     * Tests if calling close deletes file, which has been previously marked
     * for deletion and is not opened by other process than current process.
     * <p>
     * See test_close_2.c for more detailed description on how it is done.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleClose implementation is correct!
     * <p>
     * N.B.2 Depends on working multiprogramming support!
     */
    @Test
    public void testIfCloseDeletesLastReferenceToFileMarkedForDeletion() throws Throwable {
        final String file = "new_file";
        threadAssertFalse(doesFileExistsInNachosHomeDirectory(file));

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_close_2", new String[]{"first", file});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(0, rootRecords);
        threadAssertEquals(5, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(1).syscall);
        threadAssertNotEquals(-1, rootRecords.get(1).valueReturnedBySyscall);
        int childPid = rootRecords.get(1).valueReturnedBySyscall;
        threadAssertEquals(UserProcess.syscallClose, rootRecords.get(3).syscall);
        threadAssertEquals(0, rootRecords.get(3).valueReturnedBySyscall);

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertLastExitSyscallStatusEquals(0, childRecords);
        threadAssertEquals(2, recordsMap.get(childPid).size());

        threadAssertEquals(UserProcess.syscallUnlink, childRecords.get(0).syscall);
        threadAssertEquals(0, childRecords.get(0).valueReturnedBySyscall);

        threadAssertEquals(file, getDeletedFilesByProcessWithClose().get(UserProcess.rootPid));

        threadAssertFalse(doesFileExistsInNachosHomeDirectory(file));
    }

    /**
     * Tests if calling close does not delete file, which has been previously marked
     * for deletion, but still is opened by process other than current process.
     * <p>
     * See test_close_3.c for more detailed description on how it is done.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleClose implementation is correct!
     * <p>
     * N.B.2 Depends on working multiprogramming support!
     */
    @Test
    public void testIfCloseDoesNotDeleteFileMarkedForDeletionWhenFileIsStillOpenedByOtherProcess() throws Throwable {
        final String file = "new_file";
        threadAssertFalse(doesFileExistsInNachosHomeDirectory(file));

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_close_3", new String[]{"first", file});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(0, rootRecords);
        threadAssertEquals(5, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(1).syscall);
        threadAssertNotEquals(-1, rootRecords.get(1).valueReturnedBySyscall);
        int childPid = rootRecords.get(1).valueReturnedBySyscall;
        threadAssertEquals(UserProcess.syscallClose, rootRecords.get(3).syscall);
        threadAssertEquals(0, rootRecords.get(3).valueReturnedBySyscall);

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertLastExitSyscallStatusEquals(0, childRecords);
        threadAssertEquals(4, childRecords.size());

        threadAssertEquals(UserProcess.syscallClose, childRecords.get(2).syscall);
        threadAssertEquals(0, childRecords.get(2).valueReturnedBySyscall);

        HashMap<Integer, String> deletedFilesRecords = getDeletedFilesByProcessWithClose();
        threadAssertNull(deletedFilesRecords.get(childPid));
        threadAssertEquals(file, deletedFilesRecords.get(UserProcess.rootPid));
    }

    /**
     * Tests if calling close returns -1 when provided file descriptor is
     * out of range defined by number of concurrently open files per process,
     * which is 16 including stdin and stdout.
     */
    @Test
    public void testIfCloseFailsWhenProvidedFileDescriptorIsOutOfRange() throws Throwable {
        List<SyscallCallRecord> records = runUserProgram("call_syscall",
                new String[]{"close", Integer.toString(UserProcess.maxFilesPerProcess + 1)}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallClose, records.get(0).syscall);
        threadAssertEquals(UserProcess.maxFilesPerProcess + 1, records.get(0).a0);
        threadAssertEquals(-1, records.get(0).valueReturnedBySyscall);
    }

    /**
     * Tests if calling close returns -1 when provided file descriptor has
     * no open file present.
     */
    @Test
    public void testIfCloseFailsWhenNoOpenFileIsPresentOnProvidedFileDescriptor() throws Throwable {
        List<SyscallCallRecord> records = runUserProgram("call_syscall",
                new String[]{"close", "2"}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallClose, records.get(0).syscall);
        threadAssertEquals(2, records.get(0).a0);
        threadAssertEquals(-1, records.get(0).valueReturnedBySyscall);
    }

    /**
     * Tests if calling close on standard input file descriptor successfully
     * closes standard input opened at file descriptor 0 for each process by default.
     */
    @Test
    public void testIfCloseSuccessfullyClosesStdIn() throws Throwable {
        List<SyscallCallRecord> records = runUserProgram("call_syscall",
                new String[]{"close", Integer.toString(UserProcess.defaultStdInFd)}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallClose, records.get(0).syscall);
        threadAssertEquals(UserProcess.defaultStdInFd, records.get(0).a0);
        threadAssertEquals(0, records.get(0).valueReturnedBySyscall);
    }

    /**
     * Tests if calling close on standard output file descriptor successfully
     * closes standard output opened at file descriptor 1 for each process by default.
     */
    @Test
    public void testIfCloseSuccessfullyClosesStdOut() throws Throwable {
        List<SyscallCallRecord> records = runUserProgram("call_syscall",
                new String[]{"close", Integer.toString(UserProcess.defaultStdOutFd)}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallClose, records.get(0).syscall);
        threadAssertEquals(UserProcess.defaultStdOutFd, records.get(0).a0);
        threadAssertEquals(0, records.get(0).valueReturnedBySyscall);
    }
}
