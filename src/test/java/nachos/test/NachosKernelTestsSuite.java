// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test;

import nachos.machine.Kernel;
import nachos.machine.Machine;
import nachos.machine.Stats;
import nachos.machine.config.Config;
import nachos.machine.lib.Lib;
import nachos.machine.recorder.NachosRuntimeRecorder;
import nachos.machine.tcb.NachosSystemExitException;
import nachos.machine.tcb.TCB;
import nachos.threads.KThread;
import nachos.userprog.UserKernel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public abstract class NachosKernelTestsSuite extends NachosTestsSuite {
    NachosRuntimeRecorder nachosRuntimeRecorder;

    protected NachosKernelTestsSuite(String confFile) {
        super(confFile);
    }

    protected NachosKernelTestsSuite(String confFile, String machineArgs) {
        super(confFile, machineArgs);
    }

    protected NachosKernelTestsSuite(String confFile, long timeout) {
        super(confFile, timeout);
    }

    protected NachosKernelTestsSuite(String confFile, String machineArgs, long timeout) {
        super(confFile, machineArgs, timeout);
    }

    @BeforeClass
    public static void initializeThreadExecutor() {
        try {
            Lib.callStaticMethod(Machine.class, void.class, "checkUserClasses");
            executor = Executors.newFixedThreadPool(4);
            securityManager = new NachosTestSecurityManager(nachosHomeDirectory);
            privilege = securityManager.getPrivilege();
            securityManager.enable();
        }
        catch ( Exception e ) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @AfterClass
    public static void shutdownThreadExecutor() {
        executor.shutdown();
        securityManager.disable();
    }

    @Before
    public void setUpNachos() throws IOException, InterruptedException {
        Lib.callStaticMethod(Config.class, void.class, "initializeStaticFields");
        Lib.callStaticMethod(Kernel.class, void.class, "initializeStaticFields");
        Lib.callStaticMethod(Lib.class, void.class, "initializeStaticFields");
        Lib.callStaticMethod(Machine.class, void.class, "initializeStaticFields");
        Lib.callStaticMethod(TCB.class, void.class, "initializeStaticFields");
        Lib.callStaticMethod(KThread.class, void.class, "initializeStaticFields");
        Lib.callStaticMethod(UserKernel.class, void.class, "initializeStaticFields");

        System.out.print("nachos 5.0j initializing...");

        Lib.assertTrue(Lib.getStaticField(Machine.class, String[].class, "args") == null);
        Lib.setStaticField(Machine.class, "args", machineArgs.split(" "));
        Lib.callStaticMethod(Machine.class, void.class, "processArgs");
        Lib.seedRandom(Lib.getStaticField(Machine.class, long.class, "randomSeed"));

        Config.load(Lib.getStaticField(Machine.class, String.class, "configFilePath"));

        securityManager.reset();
        Lib.setStaticField(Machine.class, "securityManager", securityManager);

        Lib.setStaticField(Machine.class, "privilege", privilege);
        Lib.givePrivilege(privilege);

        privilege.machine = new Machine.MachinePrivilege();

        TCB.givePrivilege(privilege);
        privilege.stats = Lib.getStaticField(Machine.class, Stats.class, "stats");

        Lib.callStaticMethod(Machine.class, void.class, "createDevices");

        nachosRuntimeRecorder = new NachosRuntimeRecorder();
        Lib.setStaticField(Machine.class, "nachosRuntimeRecorder", nachosRuntimeRecorder);
    }

    @After
    public void tearDownNachos() throws IOException, InterruptedException {
        System.setSecurityManager(null);
    }

    protected void runKernelSteps(Runnable steps) throws Throwable {
        try {
            executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    securityManager.isCurrentThreadSecured.set(true);
                    try {
                        // This will be executed in current thread, therefore we can catch exceptions.
                        new TCB().start(new Runnable() {
                            public void run() {
                                try {
                                    Lib.constructObject(Config.getString("Kernel.kernel"), Kernel.class);
                                    nachosRuntimeRecorder.init(privilege);
                                    Kernel.kernel.initialize(Machine.getCommandLineArguments());

                                    // This call is here just because calling await(0, 0) will eventually timeout, so
                                    // we add this always successful call to increment expectedResumes to at least 1.
                                    threadAssertTrue(true);

                                    steps.run();
                                    await(0, expectedResumes);
                                    Machine.halt();
                                }
                                catch ( TimeoutException e ) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                    catch ( NachosSystemExitException e ) {
                        securityManager.isCurrentThreadSecured.set(false);
                        System.out.print("Exit status: " + e.status + "\n\n");
                        if ( e.status != 0 ) {
                            throw new Exception(e.getCause());
                        }
                    }
                    return null;
                }
            }).get();
        }
        catch ( ExecutionException e ) {
            throw e.getCause().getCause();
        }
    }

    protected boolean areAllThreadsFinished(List<KThread> threads) {
        for ( KThread thread : threads ) {
            if ( privilegedGetInstanceField(thread, KThread.Status.class, "status") != KThread.Status.Finished ) {
                return false;
            }
        }
        return true;
    }

    protected boolean areAllThreadsFinished(KThread... threads) {
        for ( KThread thread : threads ) {
            if ( privilegedGetInstanceField(thread, KThread.Status.class, "status") != KThread.Status.Finished ) {
                return false;
            }
        }
        return true;
    }
}
