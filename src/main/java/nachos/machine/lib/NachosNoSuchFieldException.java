// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.lib;

public class NachosNoSuchFieldException extends NachosReflectiveOperationException {
    NachosNoSuchFieldException(Class<?> cls, String fieldName) {
        super("Class " + cls + " does not contain field " + fieldName);
    }
}
