// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase2.task1;

import nachos.test.NachosUserProgramTestsSuite;
import nachos.userprog.UserProcess;
import org.junit.Test;

import java.util.List;

import static nachos.machine.recorder.NachosRuntimeRecorder.SyscallCallRecord;

/**
 * Tests for write system call.
 */
public class Phase2Task1WriteSystemCallTests extends NachosUserProgramTestsSuite {
    public Phase2Task1WriteSystemCallTests() {
        super("phase2/phase2.round.robin.conf");
    }

    /**
     * Tests if calling write on existing file writes provided bytes into file
     * as expected.
     * <p>
     * See test_write_1.c for more detailed description on how it is done.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleWrite implementation is correct!
     */
    @Test
    public void testIfWriteSuccessfullyWritesSpecifiedBytes() throws Throwable {
        final String file = "newfile.txt";
        final String fileContents = "Testing writing into file. It works!";
        threadAssertFalse(doesFileExistsInNachosHomeDirectory(file));

        List<SyscallCallRecord> records = runUserProgram("test_write_1",
                new String[]{file, fileContents, Integer.toString(fileContents.length())}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(3, records.size());

        threadAssertEquals(UserProcess.syscallWrite, records.get(1).syscall);
        threadAssertEquals(fileContents.length(), records.get(1).a2);
        threadAssertEquals(fileContents.length(), records.get(1).valueReturnedBySyscall);
        threadAssertEquals(fileContents, records.get(1).extraData);

        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));
        threadAssertEquals(fileContents, readContentsOfFileInNachosHomeDirectory(file));
    }


    /**
     * Tests if calling write returns -1 when provided file descriptor is
     * out of range defined by number of concurrently open files per process,
     * which is 16 including stdin and stdout.
     */
    @Test
    public void testIfWriteFailsWhenProvidedFileDescriptorIsOutOfRange() throws Throwable {
        final String text = "Not stored bytes";

        List<SyscallCallRecord> records =
                runUserProgram("call_syscall",
                        new String[]{"write",
                                Integer.toString(UserProcess.maxFilesPerProcess + 1),
                                text,
                                Integer.toString(text.length())}
                ).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallWrite, records.get(0).syscall);
        threadAssertEquals(UserProcess.maxFilesPerProcess + 1, records.get(0).a0);
        threadAssertEquals(text.length(), records.get(0).a2);
        threadAssertEquals(-1, records.get(0).valueReturnedBySyscall);
        threadAssertEquals(text, records.get(0).extraData);
    }

    /**
     * Tests if calling write returns -1 when provided file descriptor has
     * file position undefined.
     * <p>
     * File descriptor gets file position undefined when
     * some other call to write (or read) on the same file descriptor returned -1
     * (in this test caused by calling write on file descriptor, which has no open
     * file present).
     * <p>
     * See test_write_2.c for more detailed description on how this was achieved.
     */
    @Test
    public void testIfWriteFailsWhenProvidedFileDescriptorHasUndefinedFilePosition() throws Throwable {
        final String text = "Not stored bytes";

        List<SyscallCallRecord> records = runUserProgram("test_write_2",
                new String[]{"2", text, Integer.toString(text.length())}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(3, records.size());

        threadAssertEquals(UserProcess.syscallWrite, records.get(0).syscall);
        threadAssertEquals(2, records.get(0).a0);
        threadAssertEquals(text.length(), records.get(0).a2);
        threadAssertEquals(-1, records.get(0).valueReturnedBySyscall);
        threadAssertEquals(text, records.get(0).extraData);

        threadAssertEquals(UserProcess.syscallWrite, records.get(1).syscall);
        threadAssertEquals(2, records.get(1).a0);
        threadAssertEquals(text.length(), records.get(1).a2);
        threadAssertEquals(-1, records.get(1).valueReturnedBySyscall);
        threadAssertEquals(text, records.get(1).extraData);
    }

    /**
     * Tests if calling write returns -1 when provided file descriptor has
     * no open file present.
     */
    @Test
    public void testIfWriteFailsWhenNoOpenFileIsPresentOnProvidedFileDescriptor() throws Throwable {
        final String text = "Not stored bytes";

        List<SyscallCallRecord> records = runUserProgram("call_syscall",
                new String[]{"write", "2", text, Integer.toString(text.length())}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallWrite, records.get(0).syscall);
        threadAssertEquals(2, records.get(0).a0);
        threadAssertEquals(text.length(), records.get(0).a2);
        threadAssertEquals(text, records.get(0).extraData);
        threadAssertEquals(-1, records.get(0).valueReturnedBySyscall);
    }

    /**
     * Tests if calling write successfully writes specified bytes into standard
     * output. Standard output file descriptor should be opened at file
     * descriptor 1 for each process by default.
     */
    @Test
    public void testIfWriteSuccessfullyWritesSpecifiedBytesIntoStdOut() throws Throwable {
        final String text = "Text to write into standard output";

        List<SyscallCallRecord> records =
                runUserProgram("call_syscall",
                        new String[]{"write",
                                Integer.toString(UserProcess.defaultStdOutFd),
                                text,
                                Integer.toString(text.length())}
                ).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallWrite, records.get(0).syscall);
        threadAssertEquals(UserProcess.defaultStdOutFd, records.get(0).a0);
        threadAssertEquals(text, records.get(0).extraData);
        threadAssertEquals(text.length(), records.get(0).valueReturnedBySyscall);
    }
}
