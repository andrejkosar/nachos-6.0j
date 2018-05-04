// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test;

import nachos.machine.lib.Lib;
import nachos.userprog.UserProcess;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static nachos.machine.recorder.NachosRuntimeRecorder.SyscallCallRecord;

public class NachosUserProgramTestsSuite extends NachosKernelTestsSuite {
    /**
     * Pattern for splitting string by spaces except spaces inside double quotation
     * e.g. software term \"on the fly\" and \"synchrony\" => [software, term, on the fly, and, synchrony]
     */
    private static final Pattern commandPattern = Pattern.compile("\"?( |$)(?=(([^\"]*\"){2})*[^\"]*$)\"?");
    private static final Boolean enableMakeCommand;
    private static final List<String> makeCommand;
    private static final Boolean enableMakeTidyCommand;
    private static final List<String> makeTidyCommand;
    private static final Pattern makeTidyCommandPattern = Pattern.
            compile("(^Makefile$)|(^Makefile.template$)|(^script$)|(^script_coff$)|(^start.s$)|(^test_file.txt$)|(^.+(\\.(h|c|o|elf|a))$)");

    static {
        enableMakeCommand = Boolean.parseBoolean(testEnvConfig.get("enable.command.make"));
        makeCommand = commandPattern.splitAsStream(testEnvConfig.get("command.make").trim()).collect(Collectors.toList());
        enableMakeTidyCommand = Boolean.parseBoolean(testEnvConfig.get("enable.command.make.tidy"));
        makeTidyCommand = commandPattern.splitAsStream(testEnvConfig.get("command.make.tidy").trim()).collect(Collectors.toList());
    }

    protected NachosUserProgramTestsSuite(String confFile) {
        super(confFile);
    }

    protected NachosUserProgramTestsSuite(String confFile, String machineArgs) {
        super(confFile, machineArgs);
    }

    protected NachosUserProgramTestsSuite(String confFile, long timeout) {
        super(confFile, timeout);
    }

    protected NachosUserProgramTestsSuite(String confFile, String machineArgs, long timeout) {
        super(confFile, machineArgs, timeout);
    }

    @BeforeClass
    public static void compileUserPrograms() throws IOException, InterruptedException {
        if ( enableMakeCommand ) {
            try {
                int result = new ProcessBuilder(makeCommand)
                        .inheritIO()
                        .start()
                        .waitFor();
                if ( result != 0 ) {
                    System.err.println("Calling \"make\" in nachos home directory returned non 0 status!");
                    System.exit(result);
                }
            }
            catch ( Exception e ) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static void tidyNachosHomeDirectoryInternal() throws IOException, InterruptedException {
        if ( enableMakeTidyCommand ) {
            int result = new ProcessBuilder(makeTidyCommand)
                    .inheritIO()
                    .start()
                    .waitFor();
            if ( result != 0 ) {
                System.err.println("Calling \"make tidy\" in nachos home directory returned non 0 status!");
            }
        }
        else {
            //noinspection ConstantConditions
            for ( File file : nachosHomeDirectory.listFiles((dir, name) -> !makeTidyCommandPattern.matcher(name).matches()) ) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            PrintStream testFileOut = new PrintStream(new File(nachosHomeDirectory, "test_file.txt"));
            testFileOut.print("Test file contents");
            testFileOut.close();
        }
    }

    @AfterClass
    public static void tidyNachosHomeDirectory() throws IOException, InterruptedException {
        tidyNachosHomeDirectoryInternal();
    }

    @Override
    public void setUpNachos() throws IOException, InterruptedException {
        tidyNachosHomeDirectoryInternal();
        super.setUpNachos();
    }

    protected HashMap<Integer, ArrayList<SyscallCallRecord>> runUserProgram(String programName, String[] args) throws Throwable {
        final HashMap[] result = new HashMap[1];

        runKernelSteps(new Runnable() {
            @Override
            public void run() {
                UserProcess process = UserProcess.newUserProcess();
                int pid = privilegedGetInstanceField(process, int.class, "pid");

                threadAssertEquals(UserProcess.rootPid, pid);
                threadAssertTrue(privilege.doPrivileged(new PrivilegedAction<Boolean>() {
                    @Override
                    public Boolean run() {
                        return doesFileExistsInNachosHomeDirectory(programName.concat(".elf"));
                    }
                }));
                threadAssertTrue(process.syncExecute(programName.concat(".elf"), args));

                result[0] = privilegedGetInstanceFieldDeepClone(nachosRuntimeRecorder, HashMap.class, "syscallRecords");
            }
        });

        //noinspection unchecked
        return result[0];
    }

    protected void threadAssertLastExitSyscallStatusEquals(int expected, List<SyscallCallRecord> records) {
        threadAssertEquals(UserProcess.syscallExit, records.get(records.size() - 1).syscall);
        threadAssertNull(records.get(records.size() - 1).valueReturnedBySyscall);
        threadAssertEquals(expected, records.get(records.size() - 1).a0);
    }

    protected void threadAssertLastExitSyscallStatusNotEquals(int unexpected, List<SyscallCallRecord> records) {
        threadAssertNotEquals(unexpected, records.get(records.size() - 1).a0);
    }

    protected boolean doesFileExistsInNachosHomeDirectory(String filename) {
        return Files.exists(Paths.get(nachosHomeDirectory.toString(), filename));
    }

    protected String readContentsOfFileInNachosHomeDirectory(String filename) throws IOException {
        if ( doesFileExistsInNachosHomeDirectory(filename) ) {
            return new String(Files.readAllBytes(Paths.get(nachosHomeDirectory.toString(), filename)), StandardCharsets.UTF_8);
        }
        return null;
    }

    protected HashMap<Integer, String> getDeletedFilesByProcessWithUnlink() {
        return Lib.deepClone(privilege.recorder.deletedFilesByProcessWithUnlink());
    }

    protected HashMap<Integer, String> getDeletedFilesByProcessWithClose() {
        return Lib.deepClone(privilege.recorder.deletedFilesByProcessWithClose());
    }

    protected HashMap<Integer, String> getDeletedFilesOnProcessExit() {
        return Lib.deepClone(privilege.recorder.deletedFilesOnProcessExit());
    }

    protected Integer getSuccessfulUserKernelTerminateCallerPid() {
        return Lib.deepClone(privilege.recorder.userKernelTerminationSuccessfulCallerPid());
    }
}
