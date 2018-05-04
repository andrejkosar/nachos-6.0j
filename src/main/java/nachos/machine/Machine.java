// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.machine.config.Config;
import nachos.machine.interrupt.Interrupt;
import nachos.machine.io.FileSystem;
import nachos.machine.io.OpenFile;
import nachos.machine.io.SerialConsole;
import nachos.machine.io.StandardConsole;
import nachos.machine.io.StubFileSystem;
import nachos.machine.lib.Lib;
import nachos.machine.net.NetworkLink;
import nachos.machine.processor.Processor;
import nachos.machine.recorder.NachosRuntimeRecorder;
import nachos.machine.security.NachosSecurityManager;
import nachos.machine.security.Privilege;
import nachos.machine.tcb.NachosSystemExitException;
import nachos.machine.tcb.TCB;
import nachos.machine.timer.Timer;
import nachos.threads.Alarm;
import nachos.threads.Boat;
import nachos.threads.Communicator;
import nachos.threads.Condition;
import nachos.threads.InterruptsCondition;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.LotteryScheduler;
import nachos.threads.PriorityScheduler;
import nachos.threads.RoundRobinScheduler;
import nachos.threads.Scheduler;
import nachos.threads.Semaphore;
import nachos.threads.SemaphoresCondition;
import nachos.threads.ThreadQueue;
import nachos.threads.ThreadedKernel;
import nachos.userprog.SynchConsole;
import nachos.userprog.UThread;
import nachos.userprog.UserKernel;
import nachos.userprog.UserProcess;
import nachos.vm.VMKernel;
import nachos.vm.VMProcess;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * The master class of the simulated machine. Processes command line arguments,
 * constructs all simulated hardware devices, and starts the runtime recorder.
 */
public final class Machine {
    private static final String help = "\n"
            + "Options:\n"
            + "\n"
            + "\t-d <debug flags>\n"
            + "\t\tEnable some debug flags, e.g. -d ti\n"
            + "\n"
            + "\t-h\n"
            + "\t\tPrint this help message.\n"
            + "\n"
            + "\t-m <pages>\n"
            + "\t\tSpecify how many physical pages of memory to simulate.\n"
            + "\n"
            + "\t-s <seed>\n"
            + "\t\tSpecify the seed for the random number generator (seed is a\n"
            + "\t\tlong).\n"
            + "\n"
            + "\t-x <program>\n"
            + "\t\tSpecify a program that UserKernel.run() should execute,\n"
            + "\t\tinstead of the value of the configuration variable\n"
            + "\t\tKernel.shellProgram\n"
            + "\n"
            + "\t-z\n"
            + "\t\tprint the copyright message\n"
            + "\n"
            + "\t-[] <config file>\n"
            + "\t\tSpecify a config file to use, instead of nachos.conf\n";

    private static final String copyright = "\n"
            + "Copyright 1992-2001 The Regents of the University of California.\n"
            + "All rights reserved.\n"
            + "\n"
            + "Permission to use, copy, modify, and distribute this software and\n"
            + "its documentation for any purpose, without fee, and without\n"
            + "written agreement is hereby granted, provided that the above\n"
            + "copyright notice and the following two paragraphs appear in all\n"
            + "copies of this software.\n"
            + "\n"
            + "IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY\n"
            + "PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL\n"
            + "DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS\n"
            + "DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN\n"
            + "ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n"
            + "\n"
            + "THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY\n"
            + "WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES\n"
            + "OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE\n"
            + "SOFTWARE PROVIDED HEREUNDER IS ON AN \"AS IS\" BASIS, AND THE\n"
            + "UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE\n"
            + "MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.\n";

    private static final Map<String, String> envConfig;
    private static final File nachosHomeDirectory;

    private static Interrupt interrupt;
    private static Timer timer;
    private static Processor processor;
    private static SerialConsole console;
    private static FileSystem stubFileSystem;
    private static NetworkLink networkLink;
    private static NachosRuntimeRecorder nachosRuntimeRecorder;
    private static String shellProgramName;
    private static NachosSecurityManager securityManager;
    private static Privilege privilege;
    private static String[] args;
    private static Stats stats;
    private static int numPhysPages;
    private static String configFilePath;
    private static long randomSeed;

    static {
        final String envConfigName = "env.conf";
        URL envConfigUrl = Machine.class.getClassLoader().getResource(envConfigName);
        if ( envConfigUrl == null ) {
            throw new RuntimeException(envConfigName + " config not found in main resources");
        }
        envConfig = Config.parseConfigFile(envConfigUrl.getFile());
        nachosHomeDirectory = new File(envConfig.get("nachos.home.dir"));

        initializeStaticFields();

        URL nachosConfigUrl = Machine.class.getClassLoader().getResource(envConfig.get("nachos.conf.name"));
        if ( nachosConfigUrl == null ) {
            throw new RuntimeException(envConfig.get("nachos.conf.name") + " config not found in main resources");
        }
        configFilePath = nachosConfigUrl.getPath();
    }

    /**
     * Prevent instantiation.
     */
    private Machine() {
    }

    private static void initializeStaticFields() {
        interrupt = null;
        timer = null;
        processor = null;
        console = null;
        stubFileSystem = null;
        networkLink = null;
        nachosRuntimeRecorder = null;
        shellProgramName = null;
        securityManager = null;
        privilege = null;
        args = null;
        stats = new Stats();
        numPhysPages = -1;
        configFilePath = null;
        randomSeed = 0L;
    }

    /**
     * Nachos main entry point.
     *
     * @param args the command line arguments.
     */
    public static void main(final String[] args) throws Throwable {
        init(args);
        nachosRuntimeRecorder = new NachosRuntimeRecorder();

        try {
            new TCB().start(new Runnable() {
                public void run() {
                    Lib.constructObject(Config.getString("Kernel.kernel"), Kernel.class);
                    nachosRuntimeRecorder.init(privilege);
                    Kernel.kernel.initialize(args);

                    Kernel.kernel.selfTest();
                    Kernel.kernel.run();
                    Machine.halt();
                }
            });
        }
        catch ( NachosSystemExitException e ) {
            System.out.println("Exit status: " + e.status);
            if ( e.status != 0 ) {
                throw e.getCause();
            }
        }
    }

    /**
     * Create devices and initialize nachos machine.
     *
     * @param args command line arguments
     */
    private static void init(final String[] args) {
        System.out.print("nachos 5.0j initializing...");

        Lib.assertTrue(Machine.args == null);
        Machine.args = args;
        processArgs();
        Lib.seedRandom(randomSeed);

        Config.load(configFilePath);

        securityManager = new NachosSecurityManager(nachosHomeDirectory);
        privilege = securityManager.getPrivilege();

        Lib.givePrivilege(privilege);

        privilege.machine = new MachinePrivilege();

        TCB.givePrivilege(privilege);
        privilege.stats = stats;

        securityManager.enable();
        createDevices();
        checkUserClasses();
    }

    /**
     * Yield to non-Nachos threads. Use in non-preemptive JVM's to give
     * non-Nachos threads a chance to run.
     */
    public static void yield() {
        Thread.yield();
    }

    /**
     * Print stats, and terminate Nachos.
     */
    public static void halt() {
        System.out.print("Machine halting!\n\n");
        stats.print();
        privilege.exit();
    }

    /**
     * Return an array containing all command line arguments.
     *
     * @return the command line arguments passed to Nachos.
     */
    public static String[] getCommandLineArguments() {
        String[] result = new String[args.length];

        System.arraycopy(args, 0, result, 0, args.length);

        return result;
    }

    /**
     * Parse and process command line arguments.
     */
    private static void processArgs() {
        for ( int i = 0; i < args.length; ) {
            String arg = args[i++];
            if ( arg.length() > 0 && arg.charAt(0) == '-' ) {
                switch ( arg ) {
                    case "-d":
                        Lib.assertTrue(i < args.length, "switch without argument");
                        Lib.enableDebugFlags(args[i++]);
                        break;
                    case "-h":
                        System.out.print(help);
                        System.exit(0);
                    case "-m":
                        Lib.assertTrue(i < args.length, "switch without argument");
                        try {
                            numPhysPages = Integer.parseInt(args[i++]);
                        }
                        catch ( NumberFormatException e ) {
                            Lib.assertNotReached("bad value for -m switch");
                        }
                        break;
                    case "-s":
                        Lib.assertTrue(i < args.length, "switch without argument");
                        try {
                            randomSeed = Long.parseLong(args[i++]);
                        }
                        catch ( NumberFormatException e ) {
                            Lib.assertNotReached("bad value for -s switch");
                        }
                        break;
                    case "-x":
                        Lib.assertTrue(i < args.length, "switch without argument");
                        shellProgramName = args[i++];
                        break;
                    case "-z":
                        System.out.print(copyright);
                        System.exit(0);
                    case "-[]":
                        Lib.assertTrue(i < args.length, "switch without argument");
                        configFilePath = args[i++];
                        break;
                }
            }
        }
    }

    /**
     * Instantiate nachos simulated hardware devices.
     */
    private static void createDevices() {
        interrupt = new Interrupt(privilege);
        timer = new Timer(privilege);

        if ( Config.getBoolean("Machine.processor") ) {
            if ( numPhysPages == -1 ) {
                numPhysPages = Config.getInteger("Processor.numPhysPages");
            }
            processor = new Processor(privilege, numPhysPages);
        }

        if ( Config.getBoolean("Machine.console") ) {
            console = new StandardConsole(privilege);
        }

        if ( Config.getBoolean("Machine.stubFileSystem") ) {
            stubFileSystem = new StubFileSystem(privilege, nachosHomeDirectory);
        }

        if ( Config.getBoolean("Machine.networkLink") ) {
            networkLink = new NetworkLink(privilege);
        }
    }

    /**
     * Check classes and their methods signatures, which will be changed by students
     * to prevent errors.
     */
    private static void checkUserClasses() {
        System.out.print(" user-check");

        Class<? extends Kernel> clsKernel = Lib.loadClass("nachos.machine.Kernel", Kernel.class);
        Class<? extends FileSystem> clsFileSystem = Lib.loadClass("nachos.machine.io.FileSystem", FileSystem.class);
        Class<? extends SerialConsole> clsSerialConsole = Lib.loadClass("nachos.machine.io.SerialConsole", SerialConsole.class);
        Class<? extends OpenFile> clsOpenFile = Lib.loadClass("nachos.machine.io.OpenFile", OpenFile.class);

        // Alarm class check
        Class<? extends Alarm> clsAlarm = Lib.loadClass("nachos.threads.Alarm", Alarm.class);
        Lib.checkMethod(clsAlarm, "waitFor", new Class[]{long.class}, void.class);
        Lib.checkMethod(clsAlarm, "timerInterrupt", new Class[]{}, void.class);

        // Boat class check
        Class<? extends Boat> clsBoat = Lib.loadClass("nachos.threads.Boat", Boat.class);
        Lib.checkConstructor(clsBoat, new Class[]{});
        Lib.checkMethod(clsBoat, "begin", new Class[]{int.class, int.class}, void.class);

        // Communicator class check
        Class<? extends Communicator> clsCommunicator = Lib.loadClass("nachos.threads.Communicator", Communicator.class);
        Lib.checkConstructor(clsCommunicator, new Class[]{});
        Lib.checkMethod(clsCommunicator, "speak", new Class[]{int.class}, void.class);
        Lib.checkMethod(clsCommunicator, "listen", new Class[]{}, int.class);

        // Lock class check
        Class<? extends Lock> clsLock = Lib.loadClass("nachos.threads.Lock", Lock.class);
        Lib.checkConstructor(clsLock, new Class[]{});
        Lib.checkMethod(clsLock, "acquire", new Class[]{}, void.class);
        Lib.checkMethod(clsLock, "release", new Class[]{}, void.class);
        Lib.checkMethod(clsLock, "isHeldByCurrentThread", new Class[]{}, boolean.class);

        // Condition classes check
        Class<? extends Condition> clsCondition = Lib.loadClass("nachos.threads.Condition", Condition.class);
        Lib.checkMethod(clsCondition, "sleep", new Class[]{}, void.class);
        Lib.checkMethod(clsCondition, "wake", new Class[]{}, void.class);
        Lib.checkMethod(clsCondition, "wakeAll", new Class[]{}, void.class);
        Class<? extends SemaphoresCondition> clsSemaphoresCondition = Lib.loadClass("nachos.threads.SemaphoresCondition", SemaphoresCondition.class);
        Lib.checkDerivation(SemaphoresCondition.class, Condition.class);
        Lib.checkConstructor(clsSemaphoresCondition, new Class[]{clsLock});
        Class<? extends InterruptsCondition> clsInterruptsCondition = Lib.loadClass("nachos.threads.InterruptsCondition", InterruptsCondition.class);
        Lib.checkDerivation(InterruptsCondition.class, Condition.class);
        Lib.checkConstructor(clsInterruptsCondition, new Class[]{clsLock});

        // KThread class check
        Class<? extends KThread> clsKThread = Lib.loadClass("nachos.threads.KThread", KThread.class);
        Lib.checkField(clsKThread, "schedulingState", Object.class);
        Lib.checkConstructor(clsKThread, new Class[]{});
        Lib.checkConstructor(clsKThread, new Class[]{Runnable.class});
        Lib.checkStaticMethod(clsKThread, "currentThread", new Class[]{}, clsKThread);
        Lib.checkStaticMethod(clsKThread, "finish", new Class[]{}, void.class);
        Lib.checkStaticMethod(clsKThread, "yield", new Class[]{}, void.class);
        Lib.checkStaticMethod(clsKThread, "sleep", new Class[]{}, void.class);
        Lib.checkMethod(clsKThread, "setTarget", new Class[]{Runnable.class}, clsKThread);
        Lib.checkMethod(clsKThread, "getName", new Class[]{}, String.class);
        Lib.checkMethod(clsKThread, "setName", new Class[]{String.class}, clsKThread);
        Lib.checkMethod(clsKThread, "fork", new Class[]{}, void.class);
        Lib.checkMethod(clsKThread, "ready", new Class[]{}, void.class);
        Lib.checkMethod(clsKThread, "join", new Class[]{}, void.class);

        // ThreadQueue class check
        Class<? extends ThreadQueue> clsThreadQueue = Lib.loadClass("nachos.threads.ThreadQueue", ThreadQueue.class);
        Lib.checkMethod(clsThreadQueue, "waitForAccess", new Class[]{clsKThread}, void.class);
        Lib.checkMethod(clsThreadQueue, "nextThread", new Class[]{}, clsKThread);
        Lib.checkMethod(clsThreadQueue, "acquire", new Class[]{clsKThread}, void.class);
        Lib.checkMethod(clsThreadQueue, "print", new Class[]{}, void.class);

        // Scheduler classes check
        Class<? extends Scheduler> clsScheduler = Lib.loadClass("nachos.threads.Scheduler", Scheduler.class);
        Lib.checkMethod(clsScheduler, "newThreadQueue", new Class[]{boolean.class}, clsThreadQueue);
        Lib.checkMethod(clsScheduler, "getPriority", new Class[]{clsKThread}, long.class);
        Lib.checkMethod(clsScheduler, "getPriority", new Class[]{}, long.class);
        Lib.checkMethod(clsScheduler, "setPriority", new Class[]{long.class}, void.class);
        Lib.checkMethod(clsScheduler, "setPriority", new Class[]{clsKThread, long.class}, void.class);
        Lib.checkMethod(clsScheduler, "getEffectivePriority", new Class[]{clsKThread}, long.class);
        Lib.checkMethod(clsScheduler, "getEffectivePriority", new Class[]{}, long.class);
        Lib.checkMethod(clsScheduler, "increasePriority", new Class[]{}, boolean.class);
        Lib.checkMethod(clsScheduler, "decreasePriority", new Class[]{}, boolean.class);
        Lib.checkMethod(clsScheduler, "getDefaultPriority", new Class[]{}, long.class);
        Lib.checkMethod(clsScheduler, "getMinimumPriority", new Class[]{}, long.class);
        Lib.checkMethod(clsScheduler, "getMaximumPriority", new Class[]{}, long.class);
        Class<? extends RoundRobinScheduler> clsRoundRobinScheduler = Lib.loadClass("nachos.threads.RoundRobinScheduler", RoundRobinScheduler.class);
        Lib.checkDerivation(clsRoundRobinScheduler, clsScheduler);
        Lib.checkConstructor(clsRoundRobinScheduler, new Class[]{});
        Class<? extends PriorityScheduler> clsPriorityScheduler = Lib.loadClass("nachos.threads.PriorityScheduler", PriorityScheduler.class);
        Lib.checkDerivation(clsPriorityScheduler, clsScheduler);
        Lib.checkConstructor(clsPriorityScheduler, new Class[]{});
        Class<? extends LotteryScheduler> clsLotteryScheduler = Lib.loadClass("nachos.threads.LotteryScheduler", LotteryScheduler.class);
        Lib.checkDerivation(clsLotteryScheduler, clsPriorityScheduler);
        Lib.checkConstructor(clsLotteryScheduler, new Class[]{});

        // Semaphore class check
        Class<? extends Semaphore> clsSemaphore = Lib.loadClass("nachos.threads.Semaphore", Semaphore.class);
        Lib.checkConstructor(clsSemaphore, new Class[]{int.class});
        Lib.checkMethod(clsSemaphore, "P", new Class[]{}, void.class);
        Lib.checkMethod(clsSemaphore, "V", new Class[]{}, void.class);

        // ThreadedKernel class check
        Class<? extends ThreadedKernel> clsThreadedKernel = Lib.loadClass("nachos.threads.ThreadedKernel", ThreadedKernel.class);
        Lib.checkDerivation(clsThreadedKernel, clsKernel);
        Lib.checkStaticField(clsThreadedKernel, "scheduler", clsScheduler);
        Lib.checkStaticField(clsThreadedKernel, "alarm", clsAlarm);
        Lib.checkStaticField(clsThreadedKernel, "fileSystem", clsFileSystem);

        // SynchConsole class check
        Class<? extends SynchConsole> clsSynchConsole = Lib.loadClass("nachos.userprog.SynchConsole", SynchConsole.class);
        Lib.checkConstructor(clsSynchConsole, new Class[]{clsSerialConsole});
        Lib.checkMethod(clsSynchConsole, "readByte", new Class[]{boolean.class}, int.class);
        Lib.checkMethod(clsSynchConsole, "writeByte", new Class[]{int.class}, void.class);
        Lib.checkMethod(clsSynchConsole, "openForReading", new Class[]{}, clsOpenFile);
        Lib.checkMethod(clsSynchConsole, "openForWriting", new Class[]{}, clsOpenFile);

        // UserProcess class check
        Class<? extends UserProcess> clsUserProcess = Lib.loadClass("nachos.userprog.UserProcess", UserProcess.class);
        Lib.checkConstructor(clsUserProcess, new Class[]{});
        Lib.checkStaticMethod(clsUserProcess, "newUserProcess", new Class[]{}, clsUserProcess);
        Lib.checkMethod(clsUserProcess, "asyncExecute", new Class[]{String.class, String[].class}, boolean.class);
        Lib.checkMethod(clsUserProcess, "syncExecute", new Class[]{String.class, String[].class}, boolean.class);
        Lib.checkMethod(clsUserProcess, "saveState", new Class[]{}, void.class);
        Lib.checkMethod(clsUserProcess, "restoreState", new Class[]{}, void.class);
        Lib.checkMethod(clsUserProcess, "initRegisters", new Class[]{}, void.class);
        Lib.checkMethod(clsUserProcess, "handleSyscall", new Class[]{int.class, int.class, int.class, int.class, int.class}, int.class);
        Lib.checkMethod(clsUserProcess, "handleException", new Class[]{int.class}, void.class);

        // UserKernel class check
        Class<? extends UserKernel> clsUserKernel = Lib.loadClass("nachos.userprog.UserKernel", UserKernel.class);
        Lib.checkDerivation(clsUserKernel, clsThreadedKernel);
        Lib.checkStaticField(clsUserKernel, "console", clsSynchConsole);
        Lib.checkConstructor(clsUserKernel, new Class[]{});
        Lib.checkStaticMethod(clsUserKernel, "currentProcess", new Class[]{}, clsUserProcess);
        Lib.checkMethod(clsUserKernel, "exceptionHandler", new Class[]{}, void.class);

        // UThread class check
        Class<? extends UThread> clsUThread = Lib.loadClass("nachos.userprog.UThread", UThread.class);
        Lib.checkDerivation(clsUThread, clsKThread);
        Lib.checkConstructor(clsUThread, new Class[]{clsUserProcess});
        Lib.checkField(clsUThread, "process", clsUserProcess);

        // VMKernel class check
        Class<? extends VMKernel> clsVMKernel = Lib.loadClass("nachos.vm.VMKernel", VMKernel.class);
        Lib.checkDerivation(clsVMKernel, clsUserKernel);
        Lib.checkConstructor(clsVMKernel, new Class[]{});

        // VMProcess class check
        Class<? extends VMProcess> clsVMProcess = Lib.loadClass("nachos.vm.VMProcess", VMProcess.class);
        Lib.checkDerivation(clsVMProcess, clsUserProcess);
        Lib.checkConstructor(clsVMProcess, new Class[]{});
    }

    /**
     * Return the hardware interrupt manager.
     *
     * @return the hardware interrupt manager.
     */
    public static Interrupt interrupt() {
        return interrupt;
    }

    /**
     * Return the hardware timer.
     *
     * @return the hardware timer.
     */
    public static Timer timer() {
        return timer;
    }

    /**
     * Return the MIPS processor.
     *
     * @return the MIPS processor, or <tt>null</tt> if it is not present.
     */
    public static Processor processor() {
        return processor;
    }

    /**
     * Return the hardware console.
     *
     * @return the hardware console, or <tt>null</tt> if it is not present.
     */
    public static SerialConsole console() {
        return console;
    }

    /**
     * Return the stub filesystem.
     *
     * @return the stub file system, or <tt>null</tt> if it is not present.
     */
    public static FileSystem stubFileSystem() {
        return stubFileSystem;
    }

    /**
     * Return the network link.
     *
     * @return the network link,  or <tt>null</tt> if it is not present.
     */
    public static NetworkLink networkLink() {
        return networkLink;
    }

    /**
     * Return the nachos runtime recorder.
     *
     * @return the nachos runtime recorder.
     */
    public static NachosRuntimeRecorder nachosRuntimeRecorder() {
        return nachosRuntimeRecorder;
    }

    /**
     * Return the name of the shell program that a user-programming kernel
     * must run. Make sure <tt>UserKernel.run()</tt> <i>always</i> uses this
     * method to decide which program to run.
     *
     * @return the name of the shell program to run.
     */
    public static String getShellProgramName() {
        if ( shellProgramName == null ) {
            shellProgramName = Config.getString("Kernel.shellProgram");
        }

        return shellProgramName;
    }

    public static class MachinePrivilege implements Privilege.MachinePrivilege {
        public void setConsole(SerialConsole console) {
            Machine.console = console;
        }
    }
}
