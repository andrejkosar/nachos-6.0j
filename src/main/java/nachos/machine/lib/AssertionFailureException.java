// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.lib;

public class AssertionFailureException extends RuntimeException {
    AssertionFailureException() {
    }

    AssertionFailureException(String message) {
        super(message);
    }
}
