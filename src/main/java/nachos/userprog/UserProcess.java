package nachos.userprog;

import nachos.machine.Kernel;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;
import nachos.machine.config.Config;
import nachos.machine.elf.ElfLoadingException;
import nachos.machine.elf.Elf;
import nachos.machine.elf.ElfSectionHeader;
import nachos.machine.io.OpenFile;
import nachos.machine.lib.Lib;
import nachos.machine.processor.Processor;
import nachos.threads.KThread;
import nachos.threads.ThreadedKernel;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 * </p>
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * </p>
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
    public static final int rootPid = 1;
    public static final int
            syscallHalt = 0,
            syscallExit = 1,
            syscallExec = 2,
            syscallJoin = 3,
            syscallCreat = 4,
            syscallOpen = 5,
            syscallRead = 6,
            syscallWrite = 7,
            syscallClose = 8,
            syscallUnlink = 9;
    /**
     * The number of pages in the program's stack.
     */
    public static final int stackPages = 8;
    public static final char dbgProcess = 'a';
    public static final int pageSize = Processor.pageSize;
    public static final int maxStrLength = 256;
    public static final int maxFilesPerProcess = 16;
    public static final int defaultStdInFd = 0;
    public static final int defaultStdOutFd = 1;
    /**
     * Value stored in {@link FileDescriptor#position} when error occurred
     * during reading or writing to associated file.
     */
    private static final int undefinedFilePosition = Integer.MAX_VALUE;
    /**
     * Value stored in {@link FileDescriptor#position} for console, because
     * writing to and reading from console is handled little differently
     * than in classic files.
     */
    private static final int synchConsoleFilePosition = Integer.MIN_VALUE;

    /**
     * The program being run by this process.
     */
    protected Elf elf;

    /**
     * This process's page table.
     */
    protected TranslationEntry[] pageTable;
    /**
     * The number of continuous pages occupied by the program.
     */
    protected int numPages;
    private int pid;
    private int initialPC, initialSP;
    private int argc, argv;
    private FileDescriptor[] fileDescriptors;
    private Map<Integer, UserProcess> children;
    private UThread thread;
    private int exitStatus;

    /**
     * Allocate a new process.
     */
    public UserProcess() {
        pid = UserKernel.getNextPid();

        fileDescriptors = new FileDescriptor[maxFilesPerProcess];
        for ( int i = 0; i < maxFilesPerProcess; i++ ) {
            fileDescriptors[i] = new FileDescriptor();
        }

        //TODO(2.1) When any process is started, its file descriptors 0 and 1 must refer to standard input and standard output.
        fileDescriptors[defaultStdInFd].file = UserKernel.console.openForReading();
        fileDescriptors[defaultStdInFd].position = synchConsoleFilePosition;

        fileDescriptors[defaultStdOutFd].file = UserKernel.console.openForWriting();
        fileDescriptors[defaultStdOutFd].position = synchConsoleFilePosition;

        children = new HashMap<>();
    }

    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return Lib.constructObject(Config.getString("Kernel.processClassName"), UserProcess.class);
    }

    /**
     * Execute the specified program with the specified arguments asynchronously. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the program was successfully executed.
     */
    public boolean asyncExecute(String name, String[] args) {
        if ( !load(name, args) ) {
            return false;
        }

        UserKernel.registerProcess(pid, this);

        thread = new UThread(this);
        thread.setName(name).fork();

        return true;
    }

    /**
     * Execute the specified program with specified arguments synchronously. Attempts
     * to load the program, forks a thread to run it and waits for it's completion.
     *
     * @param name name the name of the file containing the executable.
     * @param args args the arguments to pass to the executable.
     * @return <tt>true</tt> if the program was successfully executed.
     */
    public boolean syncExecute(String name, String[] args) {
        if ( !load(name, args) ) {
            return false;
        }

        UserKernel.registerProcess(pid, this);

        thread = new UThread(this);
        thread.setName(name).fork();

        boolean intStatus = Machine.interrupt().disable();
        KThread.sleep();
        Machine.interrupt().restore(intStatus);

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param vaddr     the starting virtual address of the null-terminated
     *                  string.
     * @param maxLength the maximum number of characters in the string,
     *                  not including the null terminator.
     * @return the string read, or <tt>null</tt> if no null terminator was
     * found.
     */
    protected String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] buffer = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(vaddr, buffer);

        for ( int length = 0; length < bytesRead; length++ ) {
            if ( buffer[length] == '\0' ) {
                return new String(buffer, 0, length);
            }
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param dst   the array where the data will be stored.
     * @return the number of bytes successfully transferred.
     */
    protected int readVirtualMemory(int vaddr, byte[] dst) {
        return readVirtualMemory(vaddr, dst, 0, dst.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr     the first byte of virtual memory to read.
     * @param dst       the array where the data will be stored.
     * @param dstOffset the first byte to write in the array.
     * @param length    the number of bytes to transfer from virtual memory to
     *                  the array.
     * @return the number of bytes successfully transferred.
     */
    protected int readVirtualMemory(int vaddr, byte[] dst, int dstOffset, int length) {
        Lib.assertTrue(dstOffset >= 0 && length >= 0 && dstOffset + length <= dst.length);

        byte[] memory = Machine.processor().getMemory();

        //TODO(2.2) Modify this method, so that it works with multiple user processes
        // for now, just assume that virtual addresses equal physical addresses
        if ( vaddr < 0 || vaddr >= memory.length ) {
            return 0;
        }

        int amount = Math.min(length, memory.length - vaddr);
        System.arraycopy(memory, vaddr, dst, dstOffset, amount);

        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data  the array containing the data to be written.
     * @return the number of bytes successfully transferred.
     */
    protected int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr      the first byte of virtual memory to write.
     * @param data       the array containing the data to be written.
     * @param dataOffset the first byte to transfer from the array.
     * @param length     the number of bytes to transfer from the array to
     *                   virtual memory.
     * @return the number of bytes successfully transferred.
     */
    protected int writeVirtualMemory(int vaddr, byte[] data, int dataOffset, int length) {
        Lib.assertTrue(dataOffset >= 0 && length >= 0 && dataOffset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        //TODO(2.2) Modify this method, so that it works with multiple user processes
        // for now, just assume that virtual addresses equal physical addresses
        if ( vaddr < 0 || vaddr >= memory.length ) {
            return 0;
        }

        int amount = Math.min(length, memory.length - vaddr);
        System.arraycopy(data, dataOffset, memory, vaddr, amount);

        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if ( executable == null ) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            elf = new Elf(executable);
        }
        catch ( ElfLoadingException e ) {
            executable.close();
            e.printStackTrace();
            Lib.debug(dbgProcess, "\telf load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for ( int i = 0; i < elf.getNumSections(); i++ ) {
            ElfSectionHeader section = elf.getSection(i);
            if ( section.isLoadable() && section.getFirstVPN() != numPages ) {
                elf.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            // numPages is 0 for an unloadable section
            // this way we just sort of pretend it doesn't exist
            numPages += section.getNumPages();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for ( int i = 0; i < args.length; i++ ) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }

        if ( argsSize > pageSize ) {
            elf.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = elf.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages * pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if ( !loadSections() ) {
            elf.close();
            return false;
        }

        // store arguments in last page
        int entryOffset = (numPages - 1) * pageSize;
        int stringOffset = entryOffset + args.length * 4;

        this.argc = args.length;
        this.argv = entryOffset;

        for ( byte[] arg : argv ) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset, ByteOrder.LITTLE_ENDIAN);
            Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, arg) == arg.length);
            stringOffset += arg.length;
            Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[]{0}) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the ELF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        if ( numPages > Machine.processor().getNumPhysPages() ) {
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

        //TODO(2.2) Set up pagetable structure









        // Load ELF sections.
        for ( int s = 0; s < elf.getNumSections(); s++ ) {
            ElfSectionHeader section = elf.getSection(s);
            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                    + " section (" + section.getNumPages() + " pages)");

            for ( int i = 0; i < section.getNumPages(); i++ ) {
                int vpn = section.getFirstVPN() + i;

                //TODO(2.2) Modify this, so that it allocates the number of pages that it needs
                // for now, just assume virtual addresses=physical addresses
                section.loadPage(i, vpn);
            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        //TODO(2.2) Release any resources allocated in loadSections()




    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for ( int i = 0; i < Processor.numUserRegisters; i++ ) {
            processor.writeRegister(i, 0);
        }

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * <p>
     * Halt the Nachos machine by calling {@link Machine#halt()}.
     * </p>
     * <p>
     * Only the root process
     * (the first process, executed by {@link UserKernel#run()}) should be allowed to
     * execute this syscall. Any other process should ignore the syscall and return
     * immediately.
     * </p>
     */
    private void handleHalt() {
        Lib.debug(dbgProcess, "called handleHalt()");
        if ( pid != rootPid ) {
            Lib.debug(dbgProcess, "handleHalt() called by process " + pid + " which is not the root process");
            Lib.assertTrue(KThread.currentThread() == thread);
            KThread.finish();
            Lib.assertNotReached("handleHalt() did not stop current process!");
        }
        Lib.debug(dbgProcess, "handleHalt() going to halt the Machine");
        Kernel.kernel.terminate();
        Lib.assertNotReached("Kernel.kernel.terminate() did not halt machine!");
    }

    /**
     * <p>
     * Terminate the current process immediately. Any open file descriptors
     * belonging to the process are closed. Any children of the process no longer
     * have a parent process.
     * </p>
     * <p>
     * status is returned to the parent process as this process's exit status and
     * can be collected using the join syscall. A process exiting normally should
     * (but is not required to) set status to 0.
     * </p>
     * <p>
     * exit() never returns.
     * </p>
     *
     * @param status exit status of this process
     */
    private void handleExit(int status) {
        Lib.debug(dbgProcess, "called handleExit()");

        //TODO(2.3) Implement exit syscall


        Lib.assertNotReached("handleExit() did not stop the process!");
    }

    /**
     * <p>
     * Execute the program stored in the specified file, with the specified
     * arguments, in a new child process. The child process has a new unique
     * process ID, and starts with stdin opened as file descriptor 0, and stdout
     * opened as file descriptor 1.
     * </p>
     * <p>
     * exec() returns the child process's process ID, which can be passed to
     * join(). On error, returns -1.
     * </p>
     *
     * @param file a null-terminated string that specifies the name of the file
     *             containing the executable. Note that this string must include the ".elf"
     *             extension.
     * @param argc specifies the number of arguments to pass to the child process. This
     *             number must be non-negative.
     * @param argv argv is an array of pointers to null-terminated strings that represent the
     *             arguments to pass to the child process. argv[0] points to the first
     *             argument, and argv[argc-1] points to the last argument.
     * @return the child process's process ID, which can be passed to
     * join(). On error, returns -1.
     */
    private int handleExec(int file, int argc, int argv) {
        Lib.debug(dbgProcess, "called handleExec()");

        //TODO(2.3) Implement exec syscall





        return -1;
    }

    /**
     * <p>
     * Suspend execution of the current process until the child process specified
     * by the pid argument has exited. If the child has already exited by the
     * time of the call, returns immediately. When the current process resumes, it
     * disowns the child process, so that join() cannot be used on that process
     * again.
     * </p>
     * <p>
     * If the child exited normally, returns 1. If the child exited as a result of
     * an unhandled exception, returns 0. If processID does not refer to a child
     * process of the current process, returns -1.
     * </p>
     *
     * @param pid    process ID of the child process, returned by exec().
     * @param status status points to an integer where the exit status of the child process will
     *               be stored. This is the value the child passed to exit(). If the child exited
     *               because of an unhandled exception, the value stored is not defined.
     * @return 1 if the child exited normally. <br>
     * 0 if the child exited as a result of an unhandled exception. <br>
     * -1 if pid does not refer to a child process of the current process.
     */
    private int handleJoin(int pid, int status) {
        Lib.debug(dbgProcess, "called handleJoin()");

        //TODO(2.3) Implement join syscall





        return 0;
    }

    /**
     * <p>
     * Attempt to open the named disk file, creating it if it does not exist,
     * and return a file descriptor that can be used to access the file.
     * </p>
     * <p>
     * Note that creat() can only be used to create files on disk; creat() will
     * never return a file descriptor referring to a stream.
     * </p>
     * <p>
     * Returns the new file descriptor, or -1 if an error occurred.
     * </p>
     *
     * @param name syscall argument representing the starting virtual address
     *             of the filename (null-terminated string).
     * @return the new file descriptor, or -1 if an error occurred.
     */
    private int handleCreat(int name) {
        Lib.debug(dbgProcess, "called handleCreat()");


        //TODO(2.1) Implement creat syscall


        return -1;
    }

    /**
     * <p>
     * Attempt to open the named file and return a file descriptor.
     * </p>
     * <p>
     * Note that open() can only be used to open files on disk; open() will never
     * return a file descriptor referring to a stream.
     * </p>
     * <p>
     * Returns the new file descriptor, or -1 if an error occurred.
     * </p>
     *
     * @param name syscall argument representing the starting virtual address
     *             of the filename (null-terminated string).
     * @return the new file descriptor, or -1 if an error occurred.
     */
    private int handleOpen(int name) {
        Lib.debug(dbgProcess, "called handleOpen()");

        //TODO(2.1) Implement open syscall




        return -1;
    }

    /**
     * <p>
     * Attempt to read up to count bytes into buffer from the file or stream
     * referred to by fileDescriptor.
     * </p>
     * <p>
     * On success, the number of bytes read is returned. If the file descriptor
     * refers to a file on disk, the file position is advanced by this number.
     * </p>
     * <p>
     * It is not necessarily an error if this number is smaller than the number of
     * bytes requested. If the file descriptor refers to a file on disk, this
     * indicates that the end of the file has been reached. If the file descriptor
     * refers to a stream, this indicates that the fewer bytes are actually
     * available right now than were requested, but more bytes may become available
     * in the future. Note that read() never waits for a stream to have more data;
     * it always returns as much as possible immediately.
     * </p>
     * <p>
     * On error, -1 is returned, and the new file position is undefined. This can
     * happen if fileDescriptor is invalid, if part of the buffer is read-only or
     * invalid, or if a network stream has been terminated by the remote host and
     * no more data is available.
     * </p>
     *
     * @param fileDescriptor file descriptor number (returned by open() or creat())
     * @param buffer         the buffer to store the bytes in
     * @param count          the number of bytes to read
     * @return number of bytes read, or -1 if error occurred
     */
    private int handleRead(int fileDescriptor, int buffer, int count) {
        Lib.debug(dbgProcess, "called handleRead()");

        //TODO(2.1) Implement read syscall



        return -1;
    }

    /**
     * <p>
     * Attempt to write up to count bytes from buffer to the file or stream
     * referred to by fileDescriptor. write() can return before the bytes are
     * actually flushed to the file or stream. A write to a stream can block,
     * however, if kernel queues are temporarily full.
     * </p>
     * <p>
     * On success, the number of bytes written is returned (zero indicates nothing
     * was written), and the file position is advanced by this number. It IS an
     * error if this number is smaller than the number of bytes requested. For
     * disk files, this indicates that the disk is full. For streams, this
     * indicates the stream was terminated by the remote host before all the data
     * was transferred.
     * </p>
     * <p>
     * On error, -1 is returned, and the new file position is undefined. This can
     * happen if fileDescriptor is invalid, if part of the buffer is invalid, or
     * if a network stream has already been terminated by the remote host.
     * </p>
     *
     * @param fileDescriptor file descriptor number (returned by open() or creat())
     * @param buffer         the buffer to get the bytes from
     * @param count          the number of bytes to write
     * @return number of bytes written, or -1 if error occurred
     */
    private int handleWrite(int fileDescriptor, int buffer, int count) {
        Lib.debug(dbgProcess, "called handleWrite()");

        //TODO(2.1) Implement write syscall



        return -1;
    }

    /**
     * <p>
     * Close a file descriptor, so that it no longer refers to any file or stream
     * and may be reused.
     * </p>
     * <p>
     * If the file descriptor refers to a file, all data written to it by write()
     * will be flushed to disk before close() returns.
     * If the file descriptor refers to a stream, all data written to it by write()
     * will eventually be flushed (unless the stream is terminated remotely), but
     * not necessarily before close() returns.
     * </p>
     * <p>
     * The resources associated with the file descriptor are released. If the
     * descriptor is the last reference to a disk file which has been removed using
     * unlink, the file is deleted (this detail is handled by the file system
     * implementation).
     * </p>
     * <p>
     * Returns 0 on success, or -1 if an error occurred.
     * </p>
     *
     * @param fileDescriptor file descriptor number (returned by open() or creat())
     * @return 0 on success, or -1 if an error occurred.
     */
    private int handleClose(int fileDescriptor) {
        Lib.debug(dbgProcess, "called handleClose()");

        //TODO(2.1) Implement close syscall




        return -1;
    }

    /**
     * Delete a file from the file system. If no processes have the file open, the
     * file is deleted immediately and the space it was using is made available for
     * reuse.
     * <p>
     * If any processes still have the file open, the file will remain in existence
     * until the last file descriptor referring to it is closed. However, creat()
     * and open() will not be able to return new file descriptors for the file
     * until it is deleted.
     * <p>
     * Returns 0 on success, or -1 if an error occurred.
     *
     * @param name syscall argument representing the starting virtual address
     *             of the filename (null-terminated string).
     * @return 0 on success, or -1 if an error occurred.
     */
    private int handleUnlink(int name) {
        Lib.debug(dbgProcess, "called handleUnlink()");

        //TODO(2.1) Implement unlink syscall



        return -1;
    }

    /**
     * Finds free file descriptor.
     *
     * @return free file descriptor, or -1 if there isn't free file descriptor
     * currently available.
     */
    private int findFreeFileDescriptor() {
        for ( int i = 0; i < fileDescriptors.length; i++ ) {
            if ( fileDescriptors[i].file == null ) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        UserProcess that = (UserProcess) o;

        return pid == that.pid;
    }

    @Override
    public int hashCode() {
        return pid;
    }

    /**
     * <p>
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     * </p>
     * <table>
     * <caption>Syscall numbers and prototypes</caption>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * </tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     * </tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     * </tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     *
     * @param syscall the syscall number.
     * @param a0      the first syscall argument.
     * @param a1      the second syscall argument.
     * @param a2      the third syscall argument.
     * @param a3      the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        int returnValue;
        switch ( syscall ) {
            case syscallHalt:
                Machine.nachosRuntimeRecorder().reportSyscallCalledByUserProcess(pid, syscall, a0, a1, a2, a3);
                handleHalt();
                Lib.assertNotReached();
                return -1;
            case syscallExit:
                Machine.nachosRuntimeRecorder().reportSyscallCalledByUserProcess(pid, syscall, a0, a1, a2, a3);
                handleExit(a0);
                Lib.assertNotReached();
                return -1;
            case syscallExec:
                returnValue = handleExec(a0, a1, a2);
                Machine.nachosRuntimeRecorder().reportSyscallCalledByUserProcess(pid, returnValue, syscall, a0, a1, a2, a3);
                return returnValue;
            case syscallJoin:
                returnValue = handleJoin(a0, a1);
                Machine.nachosRuntimeRecorder().reportSyscallCalledByUserProcess(pid, returnValue, syscall, a0, a1, a2, a3);
                return returnValue;
            case syscallCreat:
                returnValue = handleCreat(a0);
                Machine.nachosRuntimeRecorder().reportSyscallCalledByUserProcess(pid, returnValue, syscall, a0, a1, a2, a3);
                return returnValue;
            case syscallOpen:
                returnValue = handleOpen(a0);
                Machine.nachosRuntimeRecorder().reportSyscallCalledByUserProcess(pid, returnValue, syscall, a0, a1, a2, a3);
                return returnValue;
            case syscallRead:
                returnValue = handleRead(a0, a1, a2);
                String bytesRead = readVirtualMemoryString(a1, a2);
                Machine.nachosRuntimeRecorder().reportSyscallCalledByUserProcess(pid, returnValue, syscall, a0, a1, a2, a3, bytesRead);
                return returnValue;
            case syscallWrite:
                returnValue = handleWrite(a0, a1, a2);
                String bytesToWrite = readVirtualMemoryString(a1, a2);
                Machine.nachosRuntimeRecorder().reportSyscallCalledByUserProcess(pid, returnValue, syscall, a0, a1, a2, a3, bytesToWrite);
                return returnValue;
            case syscallClose:
                returnValue = handleClose(a0);
                Machine.nachosRuntimeRecorder().reportSyscallCalledByUserProcess(pid, returnValue, syscall, a0, a1, a2, a3);
                return returnValue;
            case syscallUnlink:
                returnValue = handleUnlink(a0);
                Machine.nachosRuntimeRecorder().reportSyscallCalledByUserProcess(pid, returnValue, syscall, a0, a1, a2, a3);
                return returnValue;
            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                return -1;
        }
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch ( cause ) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(
                        processor.readRegister(Processor.regV0),
                        processor.readRegister(Processor.regA0),
                        processor.readRegister(Processor.regA1),
                        processor.readRegister(Processor.regA2),
                        processor.readRegister(Processor.regA3));
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;
            default:
                Lib.debug(dbgProcess, "Unexpected exception: " + Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    /**
     * Wrapper class for file opened on given file descriptor.
     */
    private class FileDescriptor {
        private int position;
        private OpenFile file;
    }
}
