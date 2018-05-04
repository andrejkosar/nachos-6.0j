// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test.phase2.task3;

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
 * Tests for exec system call.
 * <p>
 * N.B. All tests in this class depend on working multiprogramming support!
 */
public class Phase2Task3ExecSystemCallTests extends NachosUserProgramTestsSuite {
    public Phase2Task3ExecSystemCallTests() {
        super("phase2/phase2.round.robin.conf");
    }

    /**
     * Tests if calling exec successfully executes specified user program.
     */
    @Test
    public void testIfExecSuccessfullyExecutesSpecifiedUserProgram() throws Throwable {
        final String executableFile = "test_exec_1.elf";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(executableFile));

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("call_syscall",
                new String[]{"exec", executableFile, "1", "666"});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(0, rootRecords);
        threadAssertEquals(2, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(0).syscall);
        threadAssertEquals(1, rootRecords.get(0).a1);
        threadAssertNotEquals(-1, rootRecords.get(0).valueReturnedBySyscall);
        int childPid = rootRecords.get(0).valueReturnedBySyscall;

        List<SyscallCallRecord> childRecords = recordsMap.get(childPid);
        threadAssertLastExitSyscallStatusEquals(666, childRecords);
        threadAssertEquals(1, childRecords.size());
    }

    /**
     * Tests if calling exec returns -1 when provided executable filename exceeds
     * max string length (256), because readVirtualMemoryString() is
     * expected to return null.
     */
    @Test
    public void testIfExecFailsWhenProvidedExecutableFilenameExceedsMaxStringLength() throws Throwable {
        char[] chars = new char[UserProcess.maxStrLength + 10];
        Arrays.fill(chars, 'a');
        final String executableFile = new String(chars) + ".elf";
        threadAssertThat(executableFile.length(), Is.is(Matchers.greaterThan(UserProcess.maxStrLength)));

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("call_syscall",
                new String[]{"exec", executableFile, "0"});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(0, rootRecords);
        threadAssertEquals(2, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(0).syscall);
        threadAssertEquals(0, rootRecords.get(0).a1);
        threadAssertEquals(-1, rootRecords.get(0).valueReturnedBySyscall);

        threadAssertEquals(1, recordsMap.size());
    }

    /**
     * Tests if calling exec returns -1 when provided executable filename does not
     * end with .elf extension.
     */
    @Test
    public void testIfExecFailsWhenProvidedExecutableFilenameDoesNotEndWithCoffExtension() throws Throwable {
        final String executableFile = "test_exec_1";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(executableFile + ".elf"));

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("call_syscall",
                new String[]{"exec", executableFile, "0"});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(0, rootRecords);
        threadAssertEquals(2, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(0).syscall);
        threadAssertEquals(0, rootRecords.get(0).a1);
        threadAssertEquals(-1, rootRecords.get(0).valueReturnedBySyscall);

        threadAssertEquals(1, recordsMap.size());
    }

    /**
     * Tests if calling exec returns -1 when one of the arguments exceeds max string
     * length (256).
     */
    @Test
    public void testIfExecFailsWhenOneOfTheArgumentsExceedsMaxStringLength() throws Throwable {
        final String executableFile = "test_exec_1.elf";
        threadAssertTrue(doesFileExistsInNachosHomeDirectory(executableFile));

        char[] chars = new char[UserProcess.maxStrLength + 10];
        Arrays.fill(chars, '1');
        final String longArgument = new String(chars);
        threadAssertThat(longArgument.length(), Is.is(Matchers.greaterThan(UserProcess.maxStrLength)));

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("call_syscall",
                new String[]{"exec", executableFile, "2", "666", longArgument});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(0, rootRecords);
        threadAssertEquals(2, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(0).syscall);
        threadAssertEquals(2, rootRecords.get(0).a1);
        threadAssertEquals(-1, rootRecords.get(0).valueReturnedBySyscall);

        threadAssertEquals(1, recordsMap.size());
    }

    /**
     * Tests if calling exec returns -1 when provided executable file does not exist.
     */
    @Test
    public void testIfExecFailsWhenProvidedExecutableFileDoesNotExist() throws Throwable {
        final String executableFile = "absent_executable.elf";
        threadAssertFalse(doesFileExistsInNachosHomeDirectory(executableFile));

        HashMap<Integer, ArrayList<SyscallCallRecord>> recordsMap = runUserProgram("call_syscall",
                new String[]{"exec", executableFile, "0"});

        List<SyscallCallRecord> rootRecords = recordsMap.get(UserProcess.rootPid);
        threadAssertLastExitSyscallStatusEquals(0, rootRecords);
        threadAssertEquals(2, rootRecords.size());

        threadAssertEquals(UserProcess.syscallExec, rootRecords.get(0).syscall);
        threadAssertEquals(0, rootRecords.get(0).a1);
        threadAssertEquals(-1, rootRecords.get(0).valueReturnedBySyscall);

        threadAssertEquals(1, recordsMap.size());
    }
}
