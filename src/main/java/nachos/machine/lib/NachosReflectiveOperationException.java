// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.lib;

public class NachosReflectiveOperationException extends RuntimeException {
    NachosReflectiveOperationException() {
    }

    NachosReflectiveOperationException(String message) {
        super(message);
    }

    NachosReflectiveOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    NachosReflectiveOperationException(Throwable cause) {
        super(cause);
    }
}
