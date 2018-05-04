// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase2.task1;

import nachos.test.NachosUserProgramTestsSuite;
import nachos.userprog.UserProcess;
import org.junit.Test;

import java.util.List;

import static nachos.machine.recorder.NachosRuntimeRecorder.SyscallCallRecord;

/**
 * Tests for read system call.
 */
public class Phase2Task1ReadSystemCallTests extends NachosUserProgramTestsSuite {
    public Phase2Task1ReadSystemCallTests() {
        super("phase2/phase2.round.robin.conf");
    }

    /**
     * Tests if calling read on existing file returns expected file contents with
     * expected length. Also tests if read does not modify file somehow while
     * reading it.
     * <p>
     * See test_read_1.c for more detailed description on how it is done.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleRead implementation is correct!
     */
    @Test
    public void testIfReadReadsSpecifiedCountOfBytes() throws Throwable {
        final String file = "test_file.txt";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));
        final String fileContents = readContentsOfFileInNachosHomeDirectory(file);

        List<SyscallCallRecord> records = runUserProgram("test_read_1",
                new String[]{file, Integer.toString(fileContents.length())}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(3, records.size());

        threadAssertEquals(UserProcess.syscallRead, records.get(1).syscall);
        threadAssertEquals(fileContents.length(), records.get(1).a2);
        threadAssertEquals(fileContents.length(), records.get(1).valueReturnedBySyscall);
        threadAssertEquals(fileContents, records.get(1).extraData);

        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));
        threadAssertEquals(fileContents, readContentsOfFileInNachosHomeDirectory(file));
    }

    /**
     * Tests if calling read on existing file with count of bytes larger than actual
     * file contents returns expected file contents with expected length. Also tests
     * if read does not modify file somehow while reading it.
     * <p>
     * See test_read_1.c for more detailed description on how it is done.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleRead implementation is correct!
     */
    @Test
    public void testIfReadReadsUpToSpecifiedCountOfBytes() throws Throwable {
        final String file = "test_file.txt";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));
        final String fileContents = readContentsOfFileInNachosHomeDirectory(file);

        // We will deliberately invoke read with count larger than file contents
        // length, to see if handleRead behaves as expected.
        List<SyscallCallRecord> records = runUserProgram("test_read_1",
                new String[]{file, Integer.toString(fileContents.length() + 10)}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(3, records.size());

        threadAssertEquals(UserProcess.syscallRead, records.get(1).syscall);
        threadAssertEquals(fileContents.length() + 10, records.get(1).a2);
        threadAssertEquals(fileContents.length(), records.get(1).valueReturnedBySyscall);
        threadAssertEquals(fileContents, records.get(1).extraData);

        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));
        threadAssertEquals(fileContents, readContentsOfFileInNachosHomeDirectory(file));
    }

    /**
     * Tests if calling read returns -1 when provided file descriptor is
     * out of range defined by number of concurrently open files per process,
     * which is 16 including stdin and stdout.
     */
    @Test
    public void testIfReadFailsWhenProvidedFileDescriptorIsOutOfRange() throws Throwable {
        List<SyscallCallRecord> records = runUserProgram("call_syscall",
                new String[]{"read", Integer.toString(UserProcess.maxFilesPerProcess + 1), "666"}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallRead, records.get(0).syscall);
        threadAssertEquals(UserProcess.maxFilesPerProcess + 1, records.get(0).a0);
        threadAssertEquals(666, records.get(0).a2);
        threadAssertEquals(-1, records.get(0).valueReturnedBySyscall);
    }

    /**
     * Tests if calling read returns -1 when provided file descriptor has
     * file position undefined.
     * <p>
     * File descriptor gets file position undefined when
     * some other call to read (or write) on the same file descriptor returned -1
     * (in this test caused by calling read on file descriptor, which has no open
     * file present).
     * <p>
     * See test_read_2.c for more detailed description on how this was achieved.
     */
    @Test
    public void testIfReadFailsWhenProvidedFileDescriptorHasUndefinedFilePosition() throws Throwable {
        List<SyscallCallRecord> records = runUserProgram("test_read_2",
                new String[]{"2", "666"}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(3, records.size());

        threadAssertEquals(UserProcess.syscallRead, records.get(0).syscall);
        threadAssertEquals(2, records.get(0).a0);
        threadAssertEquals(666, records.get(0).a2);
        threadAssertEquals(-1, records.get(0).valueReturnedBySyscall);

        threadAssertEquals(UserProcess.syscallRead, records.get(1).syscall);
        threadAssertEquals(2, records.get(1).a0);
        threadAssertEquals(666, records.get(1).a2);
        threadAssertEquals(-1, records.get(1).valueReturnedBySyscall);
    }

    /**
     * Tests if calling read returns -1 when provided file descriptor has
     * no open file present.
     */
    @Test
    public void testIfReadFailsWhenNoOpenFileIsPresentOnProvidedFileDescriptor() throws Throwable {
        List<SyscallCallRecord> records = runUserProgram("call_syscall",
                new String[]{"read", "2", "666"}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallRead, records.get(0).syscall);
        threadAssertEquals(2, records.get(0).a0);
        threadAssertEquals(666, records.get(0).a2);
        threadAssertEquals(-1, records.get(0).valueReturnedBySyscall);
    }

    /**
     * Tests if calling read on standard input file descriptor does not
     * return -1. Standard input file descriptor should be opened at file
     * descriptor 0 for each process by default.
     */
    @Test
    public void testIfReadDoesNotFailWhenTryingToReadFromStdIn() throws Throwable {
        List<SyscallCallRecord> records = runUserProgram("call_syscall",
                new String[]{"read", Integer.toString(UserProcess.defaultStdInFd), "5"}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallRead, records.get(0).syscall);
        threadAssertEquals(UserProcess.defaultStdInFd, records.get(0).a0);
        threadAssertNotEquals(-1, records.get(0).valueReturnedBySyscall);
    }
}
