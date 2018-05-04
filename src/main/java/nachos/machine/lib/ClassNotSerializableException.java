package nachos.machine.lib;

public class ClassNotSerializableException extends RuntimeException {
    ClassNotSerializableException(Class<?> cls) {
        super("Class " + cls + " is not serializable");
    }
}
