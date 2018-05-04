// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.security;

import java.security.Permission;

public class NachosSecurityException extends SecurityException {
    NachosSecurityException() {
    }

    NachosSecurityException(Permission perm) {
        super("Lacked permission: " + perm);
    }
}
