// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.lib;

public class NachosUnexpectedTypeException extends RuntimeException {
    NachosUnexpectedTypeException(Class<?> expected, Class<?> actual) {
        super("Expected class " + expected + " is not assignable from actual class " + actual);
    }
}
