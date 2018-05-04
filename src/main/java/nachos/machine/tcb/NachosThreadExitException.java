package nachos.machine.tcb;

import nachos.machine.security.Privilege;

/**
 * Thrown by every nachos thread that is still running, except the main thread,
 * when {@link Privilege#exit()} or {@link Privilege#exit(Throwable)} is called.
 */
class NachosThreadExitException extends RuntimeException {
    NachosThreadExitException() {
    }
}
