// PART OF THE IMPLEMENTATION GRADING CORE TESTS. DO NOT CHANGE.

package nachos.test;

import nachos.machine.config.Config;
import nachos.machine.lib.Lib;
import nachos.machine.security.NachosSecurityManager;
import nachos.machine.security.Privilege;
import net.jodah.concurrentunit.ConcurrentTestCase;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.Timeout;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

public abstract class NachosTestsSuite extends ConcurrentTestCase {
    protected static final long LIVELOCK_TIMEOUT = 5;

    protected static final long DEFAULT_TIMEOUT = 15;
    protected static final long SHORT_TIMEOUT = 3;
    protected static final long LONG_TIMEOUT = 30;

    static final Map<String, String> testEnvConfig;
    static final File nachosHomeDirectory;

    static ExecutorService executor;
    static NachosTestSecurityManager securityManager;
    static Privilege privilege;

    static {
        final String testEnvConfigName = "test.env.conf";
        URL testEnvConfigUrl = NachosTestsSuite.class.getClassLoader().getResource(testEnvConfigName);
        if ( testEnvConfigUrl == null ) {
            throw new RuntimeException(testEnvConfigName + " config not found in main resources");
        }
        testEnvConfig = Config.parseConfigFile(testEnvConfigUrl.getFile());
        Assert.assertNotNull(testEnvConfig);
        Assert.assertNotNull(testEnvConfig.get("nachos.home.dir"));
        Assert.assertNotNull(testEnvConfig.get("enable.command.make"));
        Assert.assertNotNull(testEnvConfig.get("command.make"));
        Assert.assertNotNull(testEnvConfig.get("enable.command.make.tidy"));
        Assert.assertNotNull(testEnvConfig.get("command.make.tidy"));
        nachosHomeDirectory = new File(testEnvConfig.get("nachos.home.dir"));
    }

    @Rule
    public Timeout timeout;
    int expectedResumes;

    String machineArgs;

    NachosTestsSuite(String confFile) {
        this.expectedResumes = 0;

        URL nachosConfigUrl = getClass().getClassLoader().getResource(confFile);
        if ( nachosConfigUrl == null ) {
            throw new RuntimeException(confFile + " config not found in main resources");
        }
        this.machineArgs = "-[] " + nachosConfigUrl.getPath();
        this.timeout = Timeout.seconds(DEFAULT_TIMEOUT);
    }

    NachosTestsSuite(String confFile, String machineArgs) {
        this(confFile);
        this.machineArgs += " " + machineArgs;
    }

    NachosTestsSuite(String confFile, long timeout) {
        this(confFile);
        this.timeout = Timeout.seconds(timeout);
    }

    NachosTestsSuite(String confFile, String machineArgs, long timeout) {
        this(confFile, machineArgs);
        this.timeout = Timeout.seconds(timeout);
    }

    @Override
    public void threadAssertEquals(Object expected, Object actual) {
        threadAssertThat(actual, is(expected));
    }

    public void threadAssertNotEquals(Object unexpected, Object actual) {
        threadAssertThat(actual, is(not(unexpected)));
    }

    @Override
    public void threadAssertTrue(boolean actual) {
        threadAssertEquals(true, actual);
    }

    @Override
    public void threadAssertFalse(boolean actual) {
        threadAssertEquals(false, actual);
    }

    @Override
    public void threadAssertNull(Object actual) {
        threadAssertEquals(null, actual);
    }

    @Override
    public void threadAssertNotNull(Object actual) {
        threadAssertNotEquals(null, actual);
    }

    @Override
    public <T> void threadAssertThat(T actual, Matcher<? super T> matcher) {
        super.threadAssertThat(actual, matcher);
        expectedResumes++;
        resume();
    }

    protected void threadExpectException(Class<? extends Throwable> type, Runnable action) {
        boolean ranWithoutException = false;
        try {
            action.run();
            ranWithoutException = true;
            threadFail("Expected " + type.getSimpleName() + " has not been thrown");
        }
        catch ( Throwable e ) {
            if ( e.getClass() != type && !ranWithoutException ) {
                threadFail(e);
            }
        }
    }

    protected void threadAssertActionTimeout(long seconds, Runnable action) {
        try {
            privilege.tcb.setShouldCheckForNonNachosThreads(false);
            if ( Boolean.TRUE.equals(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    action.run();
                    return true;
                }
            }).get(seconds, TimeUnit.SECONDS)) ) {
                throw new ActionDidNotTimeoutException(seconds);
            }
        }
        catch ( TimeoutException ignored ) {
        }
        catch ( InterruptedException | ExecutionException e ) {
            throw new RuntimeException(e);
        }
        finally {
            privilege.tcb.setShouldCheckForNonNachosThreads(true);
        }
    }

    <T> T privilegedGetInstanceField(Object object, Class<T> fieldType, String fieldName) {
        if ( privilege == null ) {
            throw new RuntimeException("Should be called within nachos kernel steps");
        }

        return privilege.doPrivileged(new PrivilegedAction<T>() {
            @Override
            public T run() {
                return Lib.getInstanceField(object, fieldType, fieldName);
            }
        });
    }

    protected <T extends Serializable> T privilegedGetInstanceFieldDeepClone(Object object, Class<T> fieldType, String fieldName) {
        if ( privilege == null ) {
            throw new RuntimeException("Should be called within nachos kernel steps");
        }

        return privilege.doPrivileged(new PrivilegedAction<T>() {
            @Override
            public T run() {
                Lib.checkIfSerializable(fieldType);
                return Lib.deepClone(Lib.getInstanceField(object, fieldType, fieldName));
            }
        });
    }

    static class NachosTestSecurityManager extends NachosSecurityManager {
        final BooleanInheritableThreadLocal isCurrentThreadSecured = new BooleanInheritableThreadLocal();

        NachosTestSecurityManager(File nachosHomeDirectory) {
            super(nachosHomeDirectory);

            inspections.put("RuntimePermission.createClassLoader", new BiFunction<String, Permission, Boolean>() {
                @Override
                public Boolean apply(String name, Permission perm) {
                    if ( perm instanceof RuntimePermission ) {
                        // allow creating class loaders with privilege for deepClone()
                        if ( name.equals("createClassLoader") ) {
                            verifyPrivilege(perm);
                        }
                    }
                    return false;
                }
            });
        }

        @Override
        public void checkPermission(Permission perm) {
            if ( isCurrentThreadSecured.get() ) {
                super.checkPermission(perm);
            }
        }

        @Override
        protected boolean isPrivileged() {
            return !isCurrentThreadSecured.get() || super.isPrivileged();
        }

        void disable() {
            System.setSecurityManager(null);
        }

        void reset() {
            privileged = null;
            privilegeCount = 0;
        }

        class BooleanInheritableThreadLocal extends InheritableThreadLocal<Boolean> {
            BooleanInheritableThreadLocal() {
            }

            @Override
            protected Boolean initialValue() {
                return false;
            }
        }
    }

    private class ActionDidNotTimeoutException extends RuntimeException {
        private ActionDidNotTimeoutException(long seconds) {
            super("Action was expected to timeout after " + seconds + " seconds, but it has finished sooner.");
        }
    }
}
