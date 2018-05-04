package nachos.userprog;

import nachos.machine.Machine;
import nachos.machine.lib.Lib;
import nachos.machine.processor.Processor;
import nachos.threads.KThread;
import nachos.threads.ThreadedKernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Globally accessible reference to the synchronized console.
     */
    public static SynchConsole console;
    /**
     * Global FIFO queue of all free physical pages indexes.
     */
    private static Queue<Integer> freePhysicalPages;
    /**
     * All processes created within this kernel.
     */
    private static Map<Integer, UserProcess> processes;
    /**
     * Map of filenames opened by processes used to identify if file is opened by any
     * process, which is used when deleting files in close system call handle method.
     */
    private static Map<String, List<UserProcess>> filenameProcessesMap;
    /**
     * Set of filenames that has been marked for deletion by unlink system call.
     */
    private static Set<String> filesToDelete;
    /**
     * Counter of how many processes has been created. Used when creating unique
     * pid for a new process.
     */
    private static int processesCreated;

    static {
        initializeStaticFields();
    }

    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
        super();
    }

    private static void initializeStaticFields() {
        freePhysicalPages = new LinkedList<>();
        processes = new HashMap<>();
        filenameProcessesMap = new HashMap<>();
        filesToDelete = new HashSet<>();
        console = null;
        processesCreated = UserProcess.rootPid;
    }

    /**
     * Returns the current process.
     *
     * @return the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
        if ( !(KThread.currentThread() instanceof UThread) ) {
            return null;
        }

        return ((UThread) KThread.currentThread()).process;
    }

    /**
     * Generates and returns new unique pid.
     *
     * @return new unique process id.
     */
    public static int getNextPid() {
        boolean intStatus = Machine.interrupt().disable();
        int pid = processesCreated++;
        Machine.interrupt().restore(intStatus);
        return pid;
    }

    /**
     * Retrieves and removes free page number from free physical pages queue.
     *
     * @return free physical page number.
     */
    public static int pollFreePage() {
        Integer page;
        boolean intStatus = Machine.interrupt().disable();
        if ( (page = freePhysicalPages.poll()) == null ) {
            page = -1;
        }
        Machine.interrupt().restore(intStatus);
        return page;
    }

    /**
     * Inserts specified physical page number back to free physical pages queue.
     *
     * @param page physical page number to be inserted.
     */
    public static void addFreePage(int page) {
        Lib.assertTrue(page >= 0 && page < Machine.processor().getNumPhysPages());
        boolean intStatus = Machine.interrupt().disable();
        freePhysicalPages.add(page);
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Registers newly created user process.
     *
     * @param pid     process id for the new process.
     * @param process {@link UserProcess} instance
     */
    public static void registerProcess(int pid, UserProcess process) {
        boolean intStatus = Machine.interrupt().disable();
        processes.put(pid, process);
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Unregisters process on given pid.
     *
     * @param pid process id of the process to be unregistered.
     */
    public static void unregisterProcess(int pid) {
        boolean intStatus = Machine.interrupt().disable();
        processes.remove(pid);
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Retrieves {@link UserProcess} instance with specified pid.
     *
     * @param pid process id for process to be retrieved.
     * @return {@link UserProcess} instance with specified pid.
     */
    public static UserProcess getProcessByPid(int pid) {
        return processes.get(pid);
    }

    /**
     * Returns how many processes are currently running within the kernel.
     *
     * @return number of processes currently running within the kernel.
     */
    public static int getProcessesSize() {
        return processes.size();
    }

    /**
     * Checks if file with specified filename is opened by any process.
     *
     * @param filename filename of the file to check.
     * @return true if file is opened by any process, otherwise false.
     */
    public static boolean isFileOpenedByOtherThanCurrentProcess(String filename) {
        boolean intStatus = Machine.interrupt().disable();
        List<UserProcess> processes = filenameProcessesMap.get(filename);
        boolean isOpened = processes != null && !processes.isEmpty();
        if ( isOpened && processes.size() == 1 ) {
            isOpened = !processes.get(0).equals(currentProcess());
        }
        Machine.interrupt().restore(intStatus);
        return isOpened;
    }

    public static boolean isFileOpenedBySomeProcess(String filename) {
        boolean intStatus = Machine.interrupt().disable();
        List<UserProcess> processes = filenameProcessesMap.get(filename);
        boolean isOpened = processes != null && !processes.isEmpty();
        Machine.interrupt().restore(intStatus);
        return isOpened;
    }

    /**
     * Registers that file has been opened by specified process.
     *
     * @param filename filename of the file opened.
     * @param process  {@link UserProcess} instance that opened the file.
     */
    public static void registerFileOpenedByProcess(String filename, UserProcess process) {
        boolean intStatus = Machine.interrupt().disable();
        List<UserProcess> processes = filenameProcessesMap.get(filename);
        if ( processes == null ) {
            processes = new ArrayList<>();
            processes.add(process);
            filenameProcessesMap.put(filename, processes);
        }
        else {
            processes.add(process);
        }
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Registers that file has been closed by specified process.
     *
     * @param filename filename of the file closed.
     * @param process  {@link UserProcess} instance that closed the file.
     */
    public static void registerFileClosedByProcess(String filename, UserProcess process) {
        boolean intStatus = Machine.interrupt().disable();
        List<UserProcess> processes = filenameProcessesMap.get(filename);
        if ( processes != null ) {
            processes.remove(process);
        }
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Marks file as to be deleted.
     *
     * @param filename filename of the file marked for deletion.
     */
    public static void registerFileForDeletion(String filename) {
        boolean intStatus = Machine.interrupt().disable();
        filesToDelete.add(filename);
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Unegisters file from filesToDelete set.
     *
     * @param filename filename of the file to be unregistered.
     */
    public static void unregisterFileForDeletion(String filename) {
        boolean intStatus = Machine.interrupt().disable();
        filesToDelete.remove(filename);
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Checks if the specified is marked for deletion.
     *
     * @param filename filename of the file to check.
     * @return true if file was marked for deletion by any process, false otherwise.
     */
    public static boolean isFileRegisteredForDeletion(String filename) {
        boolean intStatus = Machine.interrupt().disable();
        boolean isRegistered = filesToDelete.contains(filename);
        Machine.interrupt().restore(intStatus);
        return isRegistered;
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    @Override
    public void initialize(String[] args) {
        super.initialize(args);

        console = new SynchConsole(Machine.console());

        Machine.processor().setExceptionHandler(new Runnable() {
            @Override
            public void run() {
                exceptionHandler();
            }
        });

        for ( int i = 0; i < Machine.processor().getNumPhysPages(); i++ ) {
            freePhysicalPages.add(i);
        }
    }

    /**
     * Test the console device.
     */
    @Override
    public void selfTest() {
        super.selfTest();
    }

    /**
     * <p>
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     * </p>
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     * </p>
     */
    public void exceptionHandler() {
        Lib.assertTrue(KThread.currentThread() instanceof UThread);

        UserProcess process = ((UThread) KThread.currentThread()).process;
        int cause = Machine.processor().readRegister(Processor.regCause);
        process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see nachos.machine.Machine#getShellProgramName
     */
    @Override
    public void run() {
        super.run();

        UserProcess process = UserProcess.newUserProcess();

        String shellProgram = Machine.getShellProgramName();
        Lib.assertTrue(process.syncExecute(shellProgram, new String[]{}),
                "Execution of \"" + shellProgram + "\" failed.");
    }

    /**
     * Terminate this kernel. Never returns.
     */
    @Override
    public void terminate() {
        Machine.nachosRuntimeRecorder().reportFinishingLastUserThread();
    }
}
