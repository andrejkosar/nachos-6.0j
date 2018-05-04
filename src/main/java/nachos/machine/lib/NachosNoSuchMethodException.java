// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.lib;

public class NachosNoSuchMethodException extends NachosReflectiveOperationException {
    NachosNoSuchMethodException(Class<?> cls, String methodName) {
        super("Class " + cls + " does not have method " + methodName + " with specified parameters");
    }
}
