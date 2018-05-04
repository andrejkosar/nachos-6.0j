// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.security;

import nachos.machine.Machine;
import nachos.machine.lib.Lib;

import java.io.File;
import java.io.FilePermission;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.function.BiFunction;

/**
 * Protects the environment from malicious Nachos code.
 */
public class NachosSecurityManager extends SecurityManager {
    /**
     * Debug flag, which should be used when calling {@link Lib#debug(char, String)}
     * inside this security manager implementation.
     */
    private static final char dbgSecurity = 's';

    /**
     * Inspections which will be checked when {@link #checkPermission(Permission)} will
     * be called by JVM.
     */
    protected Map<String, BiFunction<String, Permission, Boolean>> inspections = new HashMap<>();

    /**
     * Reference to nachos home directory.
     */
    private File nachosHomeDirectory;

    /**
     * Thread witch has been recently allowed privileged access.
     */
    protected Thread privileged = null;

    /**
     * Number of times privilege was provided.
     */
    protected int privilegeCount = 0;

    /**
     * Allocate a new Nachos security manager.
     *
     * @param nachosHomeDirectory the directory usable by the stub file system.
     */
    public NachosSecurityManager(File nachosHomeDirectory) {
        this.nachosHomeDirectory = nachosHomeDirectory;
        defineInspections();
    }

    /**
     * Return a privilege object for this security manager. This security
     * manager must not be the active security manager.
     *
     * @return a privilege object for this security manager.
     */
    public Privilege getPrivilege() {
        Lib.assertTrue(this != System.getSecurityManager());
        return new PrivilegeProvider();
    }

    /**
     * Install this security manager.
     */
    public void enable() {
        Lib.assertTrue(this != System.getSecurityManager());

        doPrivileged(new Runnable() {
            @Override
            public void run() {
                System.setSecurityManager(NachosSecurityManager.this);
            }
        });
    }

    /**
     * Enable privilege for calling thread.
     */
    protected void enablePrivilege() {
        if ( privilegeCount == 0 ) {
            Lib.assertTrue(privileged == null);
            privileged = Thread.currentThread();
            privilegeCount++;
        }
        else {
            Lib.assertTrue(privileged == Thread.currentThread());
            privilegeCount++;
        }
    }

    /**
     * Disables privilege by decrementing privilegeCount and
     * erasing reference to the privileged thread if count equals 0.
     */
    protected void disablePrivilege() {
        Lib.assertTrue(privileged != null && privilegeCount > 0);
        privilegeCount--;
        if ( privilegeCount == 0 ) {
            privileged = null;
        }
    }

    /**
     * Checks whether has current thread privileged access.
     *
     * @return true if current thread has privileged access.
     */
    protected boolean isPrivileged() {
        return privileged == Thread.currentThread();
    }

    /**
     * Perform the specified action with no return value using privilege.
     *
     * @param action the action to perform.
     * @param <T>    return type of the underlying method call. Not used.
     * @see Privilege#doPrivileged(Runnable)
     */
    private <T> void doPrivileged(final Runnable action) {
        doPrivileged(new PrivilegedAction<T>() {
            @Override
            public T run() {
                action.run();
                return null;
            }
        });
    }

    /**
     * Perform the specified {@link PrivilegedAction} with privilege.
     * Unlike {@link #doPrivileged(Runnable)} can return value.
     *
     * @param action the action to perform.
     * @param <T>    return type of the performed action.
     * @return the value returned by the performed action.
     * @see Privilege#doPrivileged(PrivilegedAction)
     */
    private <T> T doPrivileged(PrivilegedAction<T> action) {
        T result;
        enablePrivilege();
        try {
            result = action.run();
        }
        finally {
            disablePrivilege();
        }
        return result;
    }

    /**
     * Perform the specified {@link PrivilegedExceptionAction} with privilege.
     * In addition to ability to return value, it can perform action, that
     * throws checked exception.
     *
     * @param action the action to perform.
     * @param <T>    return type of the performed action.
     * @return the value returned by the performed action.
     * @throws PrivilegedActionException exception with cause set to exception
     *                                   thrown when trying to perform specified action.
     * @see Privilege#doPrivileged(PrivilegedExceptionAction)
     */
    private <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        T result;
        enablePrivilege();
        try {
            result = action.run();
        }
        catch ( Exception e ) {
            throw new PrivilegedActionException(e);
        }
        finally {
            disablePrivilege();
        }
        return result;
    }

    /**
     * Helper method to throw {@link NachosSecurityException} when this security manager
     * decides that security was violated somehow.
     */
    protected void no() {
        throw new NachosSecurityException();
    }

    /**
     * Helper method to throw {@link NachosSecurityException} when this security manager
     * decides that security was violated on given {@link Permission}.
     *
     * @param perm permission on which security was violated.
     */
    protected void no(Permission perm) {
        if ( perm.getActions().equals("") ) {
            Lib.debug(dbgSecurity, "Forbid " + perm.getClass().getSimpleName() + "." + perm.getName() + " permission");
        }
        else {
            Lib.debug(dbgSecurity, "Forbid " + perm.getClass().getSimpleName() + "." + perm.getActions() + " on " + perm.getName());
        }
        throw new NachosSecurityException(perm);
    }

    /**
     * Helper method to define all inspections checked by this security manager.
     */
    protected void defineInspections() {
        // some permissions are strictly forbidden
        inspections.put("RuntimePermission.createClassLoader", new BiFunction<String, Permission, Boolean>() {
            @Override
            public Boolean apply(String name, Permission perm) {
                if ( perm instanceof RuntimePermission ) {
                    // no creating class loaders
                    if ( name.equals("createClassLoader") ) {
                        no(perm);
                    }
                }
                return false;
            }
        });
        inspections.put("FilePermission.readlink", new BiFunction<String, Permission, Boolean>() {
            @Override
            public Boolean apply(String name, Permission perm) {
                if ( perm instanceof FilePermission ) {
                    // no reading symbolic links
                    if ( perm.getActions().equals("readlink") ) {
                        no(perm);
                    }
                }
                return false;
            }
        });

        // some are always allowed
        inspections.put("PropertyPermission.read", new BiFunction<String, Permission, Boolean>() {
            @Override
            public Boolean apply(String name, Permission perm) {
                if ( perm instanceof PropertyPermission ) {
                    // allowed to read properties
                    if ( perm.getActions().equals("read") ) {
                        Lib.debug(dbgSecurity, "Allow PropertyPermission." + perm.getActions() + " on " + name);
                        return true;
                    }
                }
                return false;
            }
        });

        // some require some more checking
        inspections.put("FilePermission.read", new BiFunction<String, Permission, Boolean>() {
            @Override
            public Boolean apply(String name, Permission perm) {
                if ( perm instanceof FilePermission ) {
                    if ( perm.getActions().equals("read") ) {
                        // the nachos home directory can only be read with privilege
                        if ( isPrivileged() ) {
                            Lib.debug(dbgSecurity, "Allow FilePermission." + perm.getActions() + " on " + name);
                            return true;
                        }

                        enablePrivilege();

                        // not allowed to read nachos home directory directly w/out privilege
                        try {
                            File f = new File(name);
                            if ( f.isFile() ) {
                                File p = f.getParentFile();
                                if ( p != null && p.equals(nachosHomeDirectory) ) {
                                    no(perm);
                                }
                            }
                        }
                        finally {
                            disablePrivilege();
                        }
                        Lib.debug(dbgSecurity, "Allow FilePermission." + perm.getActions() + " on " + name);
                        return true;
                    }
                }
                return false;
            }
        });
        inspections.put("FilePermission.write,delete", new BiFunction<String, Permission, Boolean>() {
            @Override
            public Boolean apply(String name, Permission perm) {
                if ( perm instanceof FilePermission ) {
                    if ( perm.getActions().equals("write")
                            || perm.getActions().equals("delete")
                            || perm.getActions().equals("write,delete") ) {
                        // only allowed to write nachos home directory, and only with privilege
                        verifyPrivilege(perm);

                        try {
                            File f = new File(name);
                            if ( f.isFile() ) {
                                File p = f.getParentFile();
                                if ( p != null && p.equals(nachosHomeDirectory) ) {
                                    Lib.debug(dbgSecurity, "Allow FilePermission." + perm.getActions() + " on " + name);
                                    return true;
                                }
                                else {
                                    no(perm);
                                }
                            }
                        }
                        catch ( Throwable e ) {
                            no(perm);
                        }
                    }
                }
                return false;
            }
        });
        inspections.put("FilePermission.execute", new BiFunction<String, Permission, Boolean>() {
            @Override
            public Boolean apply(String name, Permission perm) {
                if ( perm instanceof FilePermission ) {
                    if ( perm.getActions().equals("execute") ) {
                        // only allowed to execute with privilege, and if there's a net
                        verifyPrivilege(perm);

                        if ( Machine.networkLink() == null ) {
                            no(perm);
                        }
                        else {
                            Lib.debug(dbgSecurity, "Allow FilePermission." + perm.getActions() + " on " + name);
                            return true;
                        }
                    }
                }
                return false;
            }
        });
    }

    /**
     * Check the specified permission. Some operations are permissible while
     * not grading. These operations are regulated here.
     *
     * @param perm the permission to check.
     */
    @Override
    public void checkPermission(Permission perm) {
        String name = perm.getName();

        for ( BiFunction<String, Permission, Boolean> inspection : inspections.values() ) {
            if ( inspection.apply(name, perm) ) {
                return;
            }
        }

        // default to requiring privilege
        verifyPrivilege(perm);
    }

    /**
     * Called by the {@link Thread} constructor to determine a
     * thread group for a child thread of the current thread. The caller must
     * be privileged in order to successfully create the thread.
     *
     * @return a thread group for the new thread, or <tt>null</tt> to use the
     * current thread's thread group.
     */
    @Override
    public ThreadGroup getThreadGroup() {
        verifyPrivilege();
        return null;
    }

    /**
     * Verify that the caller is privileged.
     */
    protected void verifyPrivilege() {
        if ( !isPrivileged() ) {
            no();
        }
    }

    /**
     * Verify that the caller is privileged, so as to check the specified
     * permission.
     *
     * @param perm the permission being checked.
     */
    protected void verifyPrivilege(Permission perm) {
        if ( !isPrivileged() ) {
            no(perm);
        }
    }

    /**
     * Implementation of the Privilege
     *
     * @see Privilege
     */
    private class PrivilegeProvider extends Privilege {
        @Override
        public void doPrivileged(Runnable action) {
            NachosSecurityManager.this.doPrivileged(action);
        }

        @Override
        public <T> T doPrivileged(PrivilegedAction<T> action) {
            return NachosSecurityManager.this.doPrivileged(action);
        }

        @Override
        public <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
            return NachosSecurityManager.this.doPrivileged(action);
        }
    }
}
