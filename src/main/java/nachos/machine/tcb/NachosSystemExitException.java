// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.tcb;

import nachos.machine.security.Privilege;

/**
 * Thrown by main nachos thread when {@link Privilege#exit()} or
 * {@link Privilege#exit(Throwable)} is called.
 */
public class NachosSystemExitException extends RuntimeException {
    public final int status;
    public final Throwable cause;

    NachosSystemExitException(int status, Throwable cause) {
        super(cause);
        this.status = status;
        this.cause = cause;
    }
}
