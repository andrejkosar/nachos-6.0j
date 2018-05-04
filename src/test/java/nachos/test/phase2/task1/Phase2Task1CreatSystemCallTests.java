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
 * Tests for creat system call.
 */
public class Phase2Task1CreatSystemCallTests extends NachosUserProgramTestsSuite {
    public Phase2Task1CreatSystemCallTests() {
        super("phase2/phase2.round.robin.conf");
    }

    /**
     * Tests if calling creat on absent file actually creates specified file.
     */
    @Test
    public void testIfCreatDoesCreatesAbsentFile() throws Throwable {
        final String file = "new_file";
        threadAssertFalse(doesFileExistsInNachosHomeDirectory(file));

        List<SyscallCallRecord> records = runUserProgram("call_syscall", new String[]{"creat", file}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallCreat, records.get(0).syscall);
        threadAssertNotEquals(-1, records.get(0).valueReturnedBySyscall);

        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));
    }

    /**
     * Tests if calling creat on existing file opens file as expected.
     */
    @Test
    public void testIfCreatOpensExistingFile() throws Throwable {
        final String file = "test_file.txt";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));

        List<SyscallCallRecord> records = runUserProgram("call_syscall", new String[]{"creat", file}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallCreat, records.get(0).syscall);
        threadAssertNotEquals(-1, records.get(0).valueReturnedBySyscall);

        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));
    }

    /**
     * Tests if calling creat returns -1 when provided file name exceeds
     * max string length (256), because readVirtualMemoryString() is
     * expected to return null.
     */
    @Test
    public void testIfCreatFailsWhenProvidedFilenameExceedsMaxStringLength() throws Throwable {
        char[] chars = new char[UserProcess.maxStrLength + 10];
        Arrays.fill(chars, 'a');
        final String file = new String(chars);
        threadAssertThat(file.length(), Is.is(Matchers.greaterThan(UserProcess.maxStrLength)));

        List<SyscallCallRecord> records = runUserProgram("call_syscall", new String[]{"creat", file}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(2, records.size());

        threadAssertEquals(UserProcess.syscallCreat, records.get(0).syscall);
        threadAssertEquals(-1, records.get(0).valueReturnedBySyscall);
    }

    /**
     * Tests if creat returns -1 when no free file descriptor exists.
     * <p>
     * See test_creat_1.c for more detailed description on how it is done.
     */
    @Test
    public void testIfCreatFailsWhenNoFreeFileDescriptorExists() throws Throwable {
        final String file = "test_file.txt";
        final String newFile = "new_file";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));
        threadAssertFalse(doesFileExistsInNachosHomeDirectory(newFile));

        List<SyscallCallRecord> records = runUserProgram("test_creat_1", new String[]{file, newFile}).get(UserProcess.rootPid);

        threadAssertLastExitSyscallStatusEquals(0, records);
        threadAssertEquals(17, records.size());

        for ( int i = 0; i < 14; i++ ) {
            threadAssertEquals(UserProcess.syscallCreat, records.get(i).syscall);
            threadAssertNotEquals(-1, records.get(i).valueReturnedBySyscall);
        }

        threadAssertEquals(UserProcess.syscallCreat, records.get(14).syscall);
        threadAssertEquals(-1, records.get(14).valueReturnedBySyscall);

        threadAssertEquals(UserProcess.syscallCreat, records.get(15).syscall);
        threadAssertEquals(-1, records.get(15).valueReturnedBySyscall);

        threadAssertTrue(doesFileExistsInNachosHomeDirectory(file));
        threadAssertFalse(doesFileExistsInNachosHomeDirectory(newFile));
    }

    /**
     * Tests if creat returns -1 when called on file registered for deletion.
     * <p>
     * See test_creat_2.c for more detailed description on how it is done.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleCreat implementation is correct!
     * <p>
     * N.B.2 Depends on working multiprogramming support!
     */
    @Test
    public void testIfCreatFailsWhenCalledOnFileRegisteredForDeletion() throws Throwable {
        final String newFile = "new_file";
        threadAssertFalse(doesFileExistsInNachosHomeDirectory(newFile));

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_creat_2", new String[]{"first", newFile});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(0, rootRecords);
        threadAssertEquals(4, rootRecords.size());

        threadAssertEquals(UserProcess.syscallCreat, rootRecords.get(0).syscall);
        threadAssertNotEquals(-1, rootRecords.get(0).valueReturnedBySyscall);
        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(1).syscall);
        threadAssertNotEquals(-1, rootRecords.get(1).valueReturnedBySyscall);
        int childPid = rootRecords.get(1).valueReturnedBySyscall;

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertLastExitSyscallStatusEquals(0, childRecords);
        threadAssertEquals(3, childRecords.size());

        threadAssertEquals(UserProcess.syscallCreat, childRecords.get(1).syscall);
        threadAssertEquals(-1, childRecords.get(1).valueReturnedBySyscall);

        threadAssertFalse(doesFileExistsInNachosHomeDirectory(newFile));
    }

    /**
     * Tests if creat returns -1 when StubFileSystem open files count exceeds.
     * <p>
     * See test_creat_3.c for more detailed description on how it is done.
     * <p>
     * N.B. Depends on another syscalls so it's possible, that this test will
     * fail even if handleCreat implementation is correct!
     * <p>
     * N.B.2 Depends on working multiprogramming support!
     */
    @Test
    public void testIfCreatFailsWhenStubFileSystemOpenCountExceeds() throws Throwable {
        final String newFile = "new_file";
        threadAssertFalse(doesFileExistsInNachosHomeDirectory(newFile));

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("test_creat_3", new String[]{"first", newFile});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(0, rootRecords);
        threadAssertEquals(17, rootRecords.size());

        for ( int i = 0; i < 14; i++ ) {
            threadAssertEquals(UserProcess.syscallCreat, rootRecords.get(i).syscall);
            threadAssertNotEquals(-1, rootRecords.get(i).valueReturnedBySyscall);
        }
        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(14).syscall);
        threadAssertNotEquals(-1, rootRecords.get(14).valueReturnedBySyscall);
        int childPid = rootRecords.get(14).valueReturnedBySyscall;

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertLastExitSyscallStatusEquals(0, childRecords);
        threadAssertEquals(2, childRecords.size());

        threadAssertEquals(UserProcess.syscallCreat, childRecords.get(0).syscall);
        threadAssertEquals(-1, childRecords.get(0).valueReturnedBySyscall);

        threadAssertTrue(doesFileExistsInNachosHomeDirectory(newFile));
    }
}
