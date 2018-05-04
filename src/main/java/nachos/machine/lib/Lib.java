// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.lib;

import nachos.machine.Machine;
import nachos.machine.io.ArrayFile;
import nachos.machine.io.OpenFile;
import nachos.machine.security.NachosSecurityManager;
import nachos.machine.security.Privilege;
import nachos.machine.tcb.NachosSystemExitException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Random;

/**
 * Provides miscellaneous library routines.
 */
public final class Lib {
    /**
     * Random instance used in methods of this class when generating random numbers.
     */
    private static Random random;

    /**
     * Debug flags specified on the command line.
     */
    private static boolean debugFlags[];

    /**
     * NachosSecurityManager privilege to allow {@link #constructObject(String, Class)} instantiations.
     */
    private static Privilege privilege;

    static {
        initializeStaticFields();
    }

    /**
     * Prevent instantiation.
     */
    private Lib() {
    }

    private static void initializeStaticFields() {
        random = null;
        debugFlags = null;
        privilege = null;
    }

    /**
     * Seed the random number generater. May only be called once.
     *
     * @param randomSeed the seed for the random number generator.
     */
    public static void seedRandom(long randomSeed) {
        assertTrue(random == null);
        random = new Random(randomSeed);
    }

    /**
     * Give privilege to Lib to be able to execute {@link #constructObject(String, Class)}.
     *
     * @param privilege privilege object
     */
    public static void givePrivilege(Privilege privilege) {
        Lib.privilege = privilege;
    }

    /**
     * Return a random integer between 0 and <i>bound - 1</i>. Must not be
     * called before {@link #seedRandom(long)} seeds the random number generator.
     *
     * @param bound a positive value specifying the number of possible return values.
     * @return a random integer in the specified range.
     */
    public static int random(int bound) {
        assertTrue(bound > 0);
        return random.nextInt(bound);
    }

    /**
     * Return a random double between 0.0 (inclusive) and 1.0 (exclusive). Must not be
     * called before {@link #seedRandom(long)} seeds the random number generator.
     *
     * @return a random double between 0.0 and 1.0.
     */
    public static double random() {
        return random.nextDouble();
    }

    /**
     * Return a random long between 0 and <i>bound - 1</i>. Must not be
     * called before {@link #seedRandom(long)} seeds the random number generator.
     *
     * @param bound a positive value specifying the number of possible return values.
     * @return a random long in the specified range.
     */
    public static long random(long bound) {
        assertTrue(bound > 0);
        return random(0, bound);
    }

    /**
     * Return a random long between lowerBound and upperBound - 1. Must not be
     * called before {@link #seedRandom(long)} seeds the random number generator.
     *
     * @param lowerBound a value specifying lower bound inclusive.
     * @param upperBound a value specifying upper bound exclusive.
     * @return a random long in the specified range.
     */
    public static long random(long lowerBound, long upperBound) {
        assertTrue(lowerBound < upperBound);
        return lowerBound + ((long) (random.nextDouble() * (upperBound - lowerBound)));
    }

    /**
     * Asserts that <i>expression</i> is <tt>true</tt>. If not, then Nachos
     * exits with an error message.
     *
     * @param expression the expression to assert.
     */
    public static void assertTrue(boolean expression) {
        if ( !expression ) {
            throw new AssertionFailureException();
        }
    }

    /**
     * Asserts that <i>expression</i> is <tt>true</tt>. If not, then Nachos
     * exits with the specified error message.
     *
     * @param expression the expression to assert.
     * @param message    the error message.
     */
    public static void assertTrue(boolean expression, String message) {
        if ( !expression ) {
            throw new AssertionFailureException(message);
        }
    }

    /**
     * Asserts that <i>expression</i> is <tt>true</tt>. If not, then Nachos
     * exits with the specified exception.
     *
     * @param expression the expression to assert.
     * @param e          the exception to be thrown.
     * @param <T>        exception type to throw on assertion failure.
     * @throws T exception instance to throw on assertion failure.
     */
    public static <T extends Exception> void assertTrue(boolean expression, T e) throws T {
        if ( !expression ) {
            throw e;
        }
    }

    /**
     * Asserts that this call is never made.
     * Same as calling {@link #assertTrue(boolean)} with <tt>false</tt>.
     */
    public static void assertNotReached() {
        //noinspection ConstantConditions
        assertTrue(false);
    }

    /**
     * Asserts that this call is never made, with the specified error messsage.
     * Same as calling {@link #assertTrue(boolean, String)} with <tt>false</tt>.
     *
     * @param message the error message.
     */
    public static void assertNotReached(String message) {
        //noinspection ConstantConditions
        assertTrue(false, message);
    }

    /**
     * <p>
     * Print <i>message</i> if <i>flag</i> was enabled on the command line. To
     * specify which flags to enable, use the -d command line option. For
     * example, to enable flags a, c, and e, do the following:
     * </p>
     * <pre>nachos -d ace</pre>
     *
     * Nachos uses several debugging flags already, but you are encouraged to
     * add your own.
     *
     * @param flag    the debug flag that must be set to print this message.
     * @param message the debug message.
     */
    public static void debug(char flag, String message) {
        if ( test(flag) ) {
            System.out.println(message);
        }
    }

    /**
     * Tests if <i>flag</i> was enabled on the command line.
     *
     * @param flag the debug flag to test.
     * @return <tt>true</tt> if this flag was enabled on the command line.
     */
    public static boolean test(char flag) {
        if ( debugFlags == null ) {
            return false;
        }
        else if ( debugFlags[(int) '+'] ) {
            return true;
        }
        else {
            return flag < 0x80 && debugFlags[(int) flag];
        }
    }

    /**
     * Enable all the debug flags in <i>flagsString</i>.
     *
     * @param flagsString the flags to enable.
     */
    public static void enableDebugFlags(String flagsString) {
        if ( debugFlags == null ) {
            debugFlags = new boolean[0x80];
        }

        char[] newFlags = flagsString.toCharArray();
        for ( char c : newFlags ) {
            if ( c < 0x80 ) {
                debugFlags[(int) c] = true;
            }
        }
    }

    /**
     * Read a file, verifying that the requested number of bytes is read, and
     * verifying that the read operation took a non-zero amount of time.
     *
     * @param file     the file to read.
     * @param position the file offset at which to start reading.
     * @param buf      the buffer in which to store the data.
     * @param offset   the buffer offset at which storing begins.
     * @param length   the number of bytes to read.
     */
    public static void strictReadFile(OpenFile file, int position, byte[] buf, int offset, int length) {
        long startTime = Machine.timer().getTime();
        assertTrue(file.read(position, buf, offset, length) == length);
        long finishTime = Machine.timer().getTime();
        assertTrue(finishTime > startTime);
    }

    /**
     * Load an entire file into memory.
     *
     * @param file the file to load.
     * @return an array containing the contents of the entire file, or
     * <tt>null</tt> if an error occurred.
     */
    public static byte[] loadFile(OpenFile file) {
        int startOffset = file.tell();

        int length = file.length();
        if ( length < 0 ) {
            return null;
        }

        byte[] data = new byte[length];

        file.seek(0);
        int amount = file.read(data, 0, length);
        file.seek(startOffset);

        if ( amount == length ) {
            return data;
        }
        else {
            return null;
        }
    }

    /**
     * Take a read-only snapshot of a file.
     *
     * @param file the file to take a snapshot of.
     * @return a read-only snapshot of the file.
     */
    public static OpenFile cloneFile(OpenFile file) {
        OpenFile clone = new ArrayFile(loadFile(file));

        clone.seek(file.tell());

        return clone;
    }

    /**
     * Convert a short into its byte string representation.
     *
     * @param array  the array in which to store the byte string.
     * @param offset the offset in the array where the string will start.
     * @param value  the value to convert.
     * @param bo     the endianness of the byte array.
     */
    public static void bytesFromShort(byte[] array, int offset, short value, ByteOrder bo) {
        byte[] data = ByteBuffer.allocate(2).order(bo).putShort(value).array();
        System.arraycopy(data, 0, array, offset, 2);
    }

    /**
     * Convert an int into its byte string representation.
     *
     * @param array  the array in which to store the byte string.
     * @param offset the offset in the array where the string will start.
     * @param value  the value to convert.
     * @param bo     the endianness of the byte array.
     */
    public static void bytesFromInt(byte[] array, int offset, int value, ByteOrder bo) {
        byte[] data = ByteBuffer.allocate(4).order(bo).putInt(value).array();
        System.arraycopy(data, 0, array, offset, 4);
    }

    /**
     * Convert an int into its byte string representation, and
     * return an array containing it.
     *
     * @param value the value to convert.
     * @param bo    the endianness of the byte array.
     * @return an array containing the byte string.
     */
    public static byte[] bytesFromInt(int value, ByteOrder bo) {
        return ByteBuffer.allocate(4).order(bo).putInt(value).array();
    }

    /**
     * Convert an int into a byte string representation of the
     * specified length.
     *
     * @param array  the array in which to store the byte string.
     * @param offset the offset in the array where the string will start.
     * @param length the number of bytes to store (must be 1, 2, or 4).
     * @param value  the value to convert.
     * @param bo     the endianness of the byte array.
     */
    public static void bytesFromInt(byte[] array, int offset, int length, int value, ByteOrder bo) {
        assertTrue(length == 1 || length == 2 || length == 4);

        switch ( length ) {
            case 1:
                array[offset] = (byte) value;
                break;
            case 2:
                bytesFromShort(array, offset, (short) value, bo);
                break;
            case 4:
                bytesFromInt(array, offset, value, bo);
                break;
        }
    }

    /**
     * Convert to a short from its byte string representation.
     *
     * @param array  the array containing the byte string.
     * @param offset the offset of the byte string in the array.
     * @param bo     the endianness of the byte array.
     * @return the corresponding short value.
     */
    public static short bytesToShort(byte[] array, int offset, ByteOrder bo) {
        byte[] data = new byte[]{0, 0, 0, 0};
        System.arraycopy(array, offset, data, (bo == ByteOrder.LITTLE_ENDIAN ? 0 : 2), 2);
        return ByteBuffer.wrap(data).order(bo).getShort();
    }

    /**
     * Convert to an unsigned short from its byte string
     * representation.
     *
     * @param array  the array containing the byte string.
     * @param offset the offset of the byte string in the array.
     * @param bo     the endianness of the byte array.
     * @return the corresponding unsigned short value.
     */
    public static int bytesToUnsignedShort(byte[] array, int offset, ByteOrder bo) {
        byte[] data = new byte[]{0, 0, 0, 0};
        System.arraycopy(array, offset, data, (bo == ByteOrder.LITTLE_ENDIAN ? 0 : 2), 2);
        return ByteBuffer.wrap(data).order(bo).getInt();
    }

    /**
     * Convert to an int from its byte string representation.
     *
     * @param array  the array containing the byte string.
     * @param offset the offset of the byte string in the array.
     * @param bo     the endianness of the byte array.
     * @return the corresponding int value.
     */
    public static int bytesToInt(byte[] array, int offset, ByteOrder bo) {
        byte[] data = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        System.arraycopy(array, offset, data, (bo == ByteOrder.LITTLE_ENDIAN ? 0 : 4), 4);
        return ByteBuffer.wrap(data).order(bo).getInt();
    }

    /**
     * Convert to an unsigned int from its byte string representation.
     *
     * @param array  the array containing the byte string.
     * @param offset the offset of the byte string in the array.
     * @param bo     the endianness of the byte array.
     * @return the corresponding unsigned int value.
     */
    public static long bytesToUnsignedInt(byte[] array, int offset, ByteOrder bo) {
        byte[] data = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        System.arraycopy(array, offset, data, (bo == ByteOrder.LITTLE_ENDIAN ? 0 : 4), 4);
        return ByteBuffer.wrap(data).order(bo).getLong();
    }

    /**
     * Convert to an int from a byte string representation of the
     * specified length.
     *
     * @param array  the array containing the byte string.
     * @param offset the offset of the byte string in the array.
     * @param length the length of the byte string.
     * @param bo     the endianness of the byte array
     * @return the corresponding value.
     */
    public static int bytesToInt(byte[] array, int offset, int length, ByteOrder bo) {
        assertTrue(length == 1 || length == 2 || length == 4);

        switch ( length ) {
            case 1:
                return array[offset];
            case 2:
                return bytesToShort(array, offset, bo);
            case 4:
                return bytesToInt(array, offset, bo);
            default:
                return -1;
        }
    }

    /**
     * Convert to a string from a possibly null-terminated array of bytes.
     *
     * @param array  the array containing the byte string.
     * @param offset the offset of the byte string in the array.
     * @param length the maximum length of the byte string.
     * @return a string containing the specified bytes, up to and not
     * including the null-terminator (if present).
     */
    public static String bytesToString(byte[] array, int offset, int length) {
        int i;
        for ( i = 0; i < length; i++ ) {
            if ( array[offset + i] == '\0' ) {
                break;
            }
        }

        return new String(array, offset, i);
    }

    /**
     * Mask out and shift a bit substring.
     *
     * @param bits   the bit string.
     * @param lowest the first bit of the substring within the string.
     * @param size   the number of bits in the substring.
     * @return the substring.
     */
    public static int extract(int bits, int lowest, int size) {
        if ( size == 32 ) {
            return (bits >> lowest);
        }
        else {
            return ((bits >> lowest) & ((1 << size) - 1));
        }
    }

    /**
     * Mask out and shift a bit substring.
     *
     * @param bits   the bit string.
     * @param lowest the first bit of the substring within the string.
     * @param size   the number of bits in the substring.
     * @return the substring.
     */
    public static long extract(long bits, int lowest, int size) {
        if ( size == 64 ) {
            return (bits >> lowest);
        }
        else {
            return ((bits >> lowest) & ((1L << size) - 1));
        }
    }

    /**
     * Mask out and shift a bit substring; then sign extend the substring.
     *
     * @param bits   the bit string.
     * @param lowest the first bit of the substring within the string.
     * @param size   the number of bits in the substring.
     * @return the substring, sign-extended.
     */
    public static int extend(int bits, int lowest, int size) {
        int extra = 32 - (lowest + size);
        return ((extract(bits, lowest, size) << extra) >> extra);
    }

    /**
     * Test if a bit is set in a bit string.
     *
     * @param flag the flag to test.
     * @param bits the bit string.
     * @return <tt>true</tt> if <tt>(bits {@literal &}; flag)</tt> is non-zero.
     */
    public static boolean test(long flag, long bits) {
        return ((bits & flag) != 0);
    }

    /**
     * Creates a padded upper-case string representation of the integer
     * argument in base 16.
     *
     * @param i an integer.
     * @return a padded upper-case string representation in base 16.
     */
    public static String toHexString(int i) {
        return toHexString(i, 8);
    }

    /**
     * Creates a padded upper-case string representation of the integer
     * argument in base 16, padding to at most the specified number of digits.
     *
     * @param i   an integer.
     * @param pad the minimum number of hex digits to pad to.
     * @return a padded upper-case string representation in base 16.
     */
    public static String toHexString(int i, int pad) {
        String result = Integer.toHexString(i).toUpperCase();
        while ( result.length() < pad ) {
            result = "0" + result;
        }
        return result;
    }

    /**
     * Divide two non-negative integers, round the quotient up to the nearest
     * integer, and return it.
     *
     * @param a the numerator.
     * @param b the denominator.
     * @return <tt>ceiling(a / b)</tt>.
     */
    public static int divRoundUp(int a, int b) {
        assertTrue(a >= 0 && b > 0);

        return ((a + (b - 1)) / b);
    }

    /**
     * Load and return the named class, or return <tt>null</tt> if the class
     * could not be loaded.
     *
     * @param className the name of the class to load.
     * @return the loaded class, or <tt>null</tt> if an error occurred.
     */
    public static <T> Class<? extends T> tryLoadClass(String className, Class<T> classType) {
        try {
            return Class.forName(className).asSubclass(classType);
        }
        catch ( ClassNotFoundException e ) {
            return null;
        }
    }

    /**
     * Load and return the named class, terminating Nachos on any error.
     *
     * @param className the name of the class to load.
     * @return the loaded class.
     */
    public static <T> Class<? extends T> loadClass(String className, Class<T> classType) {
        try {
            return Class.forName(className).asSubclass(classType);
        }
        catch ( ClassNotFoundException e ) {
            throw new NachosReflectiveOperationException(e);
        }
    }

    /**
     * Create and return a new instance of the named class, using the
     * constructor that takes no arguments.
     *
     * @param className the name of the class to instantiate.
     * @return a new instance of the class.
     */
    public static <T> T constructObject(String className, Class<T> classType) {
        try {
            return privilege.doPrivileged(new PrivilegedExceptionAction<T>() {
                @Override
                public T run() throws Exception {
                    return loadClass(className, classType).newInstance();
                }
            });
        }
        catch ( PrivilegedActionException e ) {
            throw new NachosReflectiveOperationException(e);
        }
    }

    /**
     * Verify that the specified class extends or implements the specified
     * superclass.
     *
     * @param cls      the descendant class.
     * @param superCls the ancestor class.
     */
    public static void checkDerivation(Class<?> cls, Class<?> superCls) {
        Lib.assertTrue(superCls.isAssignableFrom(cls));
    }

    /**
     * Verifies that the specified class is public and not abstract, and that a
     * constructor with the specified signature exists and is public.
     *
     * @param cls            the class containing the constructor.
     * @param parameterTypes the list of parameters.
     */
    public static void checkConstructor(Class<?> cls, Class[] parameterTypes) {
        try {
            Lib.assertTrue(Modifier.isPublic(cls.getModifiers()) && !Modifier.isAbstract(cls.getModifiers()));
            Constructor constructor = cls.getConstructor(parameterTypes);
            Lib.assertTrue(Modifier.isPublic(constructor.getModifiers()));
        }
        catch ( NoSuchMethodException e ) {
            throw new NachosReflectiveOperationException(e);
        }
    }

    /**
     * Verifies that the specified class is public, and that a non-static
     * method with the specified name and signature exists, is public, and
     * returns the specified type.
     *
     * @param cls            the class containing the non-static method.
     * @param methodName     the name of the non-static method.
     * @param parameterTypes the list of parameters.
     * @param returnType     the required return type.
     */
    public static void checkMethod(Class<?> cls, String methodName, Class[] parameterTypes, Class<?> returnType) {
        try {
            Lib.assertTrue(Modifier.isPublic(cls.getModifiers()));
            Method method = cls.getMethod(methodName, parameterTypes);
            Lib.assertTrue(Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()));
            Lib.assertTrue(method.getReturnType() == returnType);
        }
        catch ( NoSuchMethodException e ) {
            throw new NachosNoSuchMethodException(cls, methodName);
        }
    }

    /**
     * Verifies that the specified class is public, and that a static method
     * with the specified name and signature exists, is public, and returns the
     * specified type.
     *
     * @param cls            the class containing the static method.
     * @param methodName     the name of the static method.
     * @param parameterTypes the list of parameters.
     * @param returnType     the required return type.
     */
    public static void checkStaticMethod(Class<?> cls, String methodName, Class[] parameterTypes, Class returnType) {
        try {
            Lib.assertTrue(Modifier.isPublic(cls.getModifiers()));
            Method method = cls.getMethod(methodName, parameterTypes);
            Lib.assertTrue(Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()));
            Lib.assertTrue(method.getReturnType() == returnType);
        }
        catch ( NoSuchMethodException e ) {
            throw new NachosNoSuchMethodException(cls, methodName);
        }
    }

    /**
     * Verifies that the specified class is public, and that a non-static field
     * with the specified name and type exists, is public, and is not final.
     *
     * @param cls       the class containing the field.
     * @param fieldName the name of the field.
     * @param fieldType the required type.
     */
    public static void checkField(Class<?> cls, String fieldName, Class fieldType) {
        try {
            Lib.assertTrue(Modifier.isPublic(cls.getModifiers()));
            Field field = cls.getField(fieldName);
            Lib.assertTrue(field.getType() == fieldType);
            Lib.assertTrue(Modifier.isPublic(field.getModifiers()) &&
                    !Modifier.isStatic(field.getModifiers()) &&
                    !Modifier.isFinal(field.getModifiers()));
        }
        catch ( NoSuchFieldException e ) {
            throw new NachosNoSuchFieldException(cls, fieldName);
        }
    }

    /**
     * Verifies that the specified class is public, and that a static field
     * with the specified name and type exists and is public.
     *
     * @param cls       the class containing the static field.
     * @param fieldName the name of the static field.
     * @param fieldType the required type.
     */
    public static void checkStaticField(Class<?> cls, String fieldName, Class fieldType) {
        try {
            Lib.assertTrue(Modifier.isPublic(cls.getModifiers()));
            Field field = cls.getField(fieldName);
            Lib.assertTrue(field.getType() == fieldType);
            Lib.assertTrue(Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()));
        }
        catch ( NoSuchFieldException e ) {
            throw new NachosNoSuchFieldException(cls, fieldName);
        }
    }

    /**
     * Gets static field from the specified class using reflection. Call within
     * thread where {@link NachosSecurityManager} is enabled requires elevated privileges.
     * Capable to access all fields regardless of their visibility.
     *
     * @param cls       the class containing static field.
     * @param fieldType the class object of the static field.
     * @param fieldName the name of the static field.
     * @param <T>       the type of the static field.
     * @return value of the static field from the specified class.
     */
    public static <T> T getStaticField(Class<?> cls, Class<T> fieldType, String fieldName) {
        Class<?> clazz = cls;
        while ( clazz != null ) {
            try {
                Field field = clazz.getDeclaredField(fieldName);

                if ( !fieldType.isAssignableFrom(field.getType()) ) {
                    throw new NachosUnexpectedTypeException(fieldType, field.getType());
                }

                field.setAccessible(true);
                //noinspection unchecked
                T result = (T) field.get(null);
                field.setAccessible(false);
                return result;
            }
            catch ( NoSuchFieldException e ) {
                clazz = clazz.getSuperclass();
            }
            catch ( IllegalAccessException e ) {
                Lib.assertNotReached();
            }
        }
        throw new NachosNoSuchFieldException(cls, fieldName);
    }

    /**
     * Sets static field value in the specified class using reflection. Call within
     * thread where {@link NachosSecurityManager} is enabled requires elevated privileges.
     * Capable to access all fields regardless of their visibility.
     *
     * @param cls        the class containing static field.
     * @param fieldName  the name of the static field.
     * @param fieldValue the value to store in the static field.
     */
    public static void setStaticField(Class<?> cls, String fieldName, Object fieldValue) {
        Class<?> clazz = cls;
        while ( clazz != null ) {
            try {
                Field field = clazz.getDeclaredField(fieldName);

                if ( !field.getType().isAssignableFrom(fieldValue.getClass()) ) {
                    throw new NachosUnexpectedTypeException(field.getType(), fieldValue.getClass());
                }

                field.setAccessible(true);
                field.set(null, fieldValue);
                field.setAccessible(false);
                return;
            }
            catch ( NoSuchFieldException e ) {
                clazz = clazz.getSuperclass();
            }
            catch ( IllegalAccessException e ) {
                Lib.assertNotReached();
            }
        }
        throw new NachosNoSuchFieldException(cls, fieldName);
    }

    /**
     * Gets instance field from the specified object instance using reflection. Call within
     * thread where {@link NachosSecurityManager} is enabled requires elevated privileges.
     * Capable to access all fields regardless of their visibility.
     *
     * @param object    the source object containing instance field.
     * @param fieldType the class object of the instance field.
     * @param fieldName the name of the instance field.
     * @param <T>       the type of the instance field.
     * @return value of the instance field from the specified object.
     */
    public static <T> T getInstanceField(Object object, Class<T> fieldType, String fieldName) {
        Class<?> cls = object.getClass();
        while ( cls != null ) {
            try {
                Field field = cls.getDeclaredField(fieldName);

                if ( !fieldType.isAssignableFrom(field.getType()) ) {
                    throw new NachosUnexpectedTypeException(fieldType, field.getType());
                }

                field.setAccessible(true);
                //noinspection unchecked
                T result = (T) field.get(object);
                field.setAccessible(false);
                return result;
            }
            catch ( NoSuchFieldException e ) {
                cls = cls.getSuperclass();
            }
            catch ( IllegalAccessException e ) {
                Lib.assertNotReached();
            }
        }
        throw new NachosNoSuchFieldException(object.getClass(), fieldName);
    }

    /**
     * Sets instance field value in the specified object using reflection. Call within
     * thread where {@link NachosSecurityManager} is enabled requires elevated privileges.
     * Capable to access all fields regardless of their visibility.
     *
     * @param object     the source object containing instance field.
     * @param fieldName  the name of the instance field.
     * @param fieldValue the value to store in the instance field.
     */
    public static void setInstanceField(Object object, String fieldName, Object fieldValue) {
        Class<?> cls = object.getClass();
        while ( cls != null ) {
            try {
                Field field = cls.getDeclaredField(fieldName);

                if ( !field.getType().isAssignableFrom(fieldValue.getClass()) ) {
                    throw new NachosUnexpectedTypeException(field.getType(), fieldValue.getClass());
                }

                field.setAccessible(true);
                field.set(object, fieldValue);
                field.setAccessible(false);
                return;
            }
            catch ( NoSuchFieldException e ) {
                cls = cls.getSuperclass();
            }
            catch ( IllegalAccessException e ) {
                Lib.assertNotReached();
            }
        }
        throw new NachosNoSuchFieldException(object.getClass(), fieldName);
    }

    /**
     * Calls parameterless static method of the specified class using reflection. Call within
     * thread where {@link NachosSecurityManager} is enabled requires elevated privileges.
     * Capable to access all methods regardless of their visibility.
     *
     * @param cls        the class containing static method.
     * @param returnType the class object of the return type of the method.
     * @param methodName the method name.
     * @param <T>        the type of the return type of the method.
     * @return value returned from method call if any.
     */
    public static <T> T callStaticMethod(Class<?> cls, Class<T> returnType, String methodName) {
        return callStaticMethod(cls, returnType, methodName, new Class[]{}, new Object[]{});
    }

    /**
     * Calls parameterized static method of the specified class using reflection. Call within
     * thread where {@link NachosSecurityManager} is enabled requires elevated privileges.
     * Capable to access all methods regardless of their visibility.
     *
     * @param cls            the class containing static method.
     * @param returnType     the class object of the return type of the method.
     * @param methodName     the method name.
     * @param parameterTypes an array of the class objects of parameter types in correct order.
     * @param args           an array of the values to pass to method in correct order.
     * @param <T>            the type of the return type of the method.
     * @return value returned from method call if any.
     */
    public static <T> T callStaticMethod(Class<?> cls, Class<T> returnType, String methodName, Class[] parameterTypes, Object[] args) {
        Class<?> clazz = cls;
        while ( clazz != null ) {
            try {
                Method method = clazz.getDeclaredMethod(methodName, parameterTypes);

                if ( !returnType.isAssignableFrom(method.getReturnType()) ) {
                    throw new NachosUnexpectedTypeException(returnType, method.getReturnType());
                }

                method.setAccessible(true);
                //noinspection unchecked
                T result = (T) method.invoke(null, args);
                method.setAccessible(false);
                return result;
            }
            catch ( NoSuchMethodException e ) {
                clazz = clazz.getSuperclass();
            }
            catch ( InvocationTargetException e ) {
                if ( e.getTargetException() instanceof NachosSystemExitException ) {
                    throw (NachosSystemExitException) e.getTargetException();
                }
                throw new RuntimeException(e);
            }
            catch ( IllegalAccessException e ) {
                throw new RuntimeException(e);
            }
        }
        throw new NachosNoSuchMethodException(cls, methodName);
    }

    /**
     * Calls parameterless instance method of the specified object instance using reflection. Call
     * within thread where {@link NachosSecurityManager} is enabled requires elevated privileges.
     * Capable to access all methods regardless of their visibility.
     *
     * @param object     the instance of the object containing method.
     * @param returnType the class object of the return type of the method.
     * @param methodName the method name.
     * @param <T>        the type of the return type of the method.
     * @return value returned from method call if any.
     */
    public static <T> T callInstanceMethod(Object object, Class<T> returnType, String methodName) {
        return callInstanceMethod(object, returnType, methodName, new Class[]{}, new Object[]{});
    }

    /**
     * Calls parameterized instance method of the specified object instance using reflection. Call
     * within thread where {@link NachosSecurityManager} is enabled requires elevated privileges.
     * Capable to access all methods regardless of their visibility.
     *
     * @param object         the instance of the object containing method.
     * @param returnType     the class object of the return type of the method.
     * @param methodName     the method name.
     * @param parameterTypes an array of the class objects of parameter types in correct order.
     * @param args           an array of the values to pass to method in correct order.
     * @param <T>            the type of the return type of the method.
     * @return value returned from method call if any.
     */
    public static <T> T callInstanceMethod(Object object, Class<T> returnType, String methodName, Class[] parameterTypes, Object[] args) {
        Class<?> cls = object.getClass();
        while ( cls != null ) {
            try {
                Method method = cls.getDeclaredMethod(methodName, parameterTypes);

                if ( !returnType.isAssignableFrom(method.getReturnType()) ) {
                    throw new NachosUnexpectedTypeException(returnType, method.getReturnType());
                }

                method.setAccessible(true);
                //noinspection unchecked
                T result = (T) method.invoke(object, args);
                method.setAccessible(false);
                return result;
            }
            catch ( NoSuchMethodException e ) {
                cls = cls.getSuperclass();
            }
            catch ( InvocationTargetException e ) {
                if ( e.getTargetException() instanceof NachosSystemExitException ) {
                    throw (NachosSystemExitException) e.getTargetException();
                }
                throw new RuntimeException(e);
            }
            catch ( IllegalAccessException e ) {
                throw new RuntimeException(e);
            }
        }
        throw new NachosNoSuchMethodException(object.getClass(), methodName);
    }

    public static void checkIfSerializable(Class<? extends Serializable> cls) {
        if ( !Modifier.isStatic(cls.getModifiers()) ) {
            Class<?> clazz = cls;
            while ( (clazz = clazz.getEnclosingClass()) != null ) {
                if ( !Serializable.class.isAssignableFrom(clazz) ) {
                    throw new ClassNotSerializableException(clazz);
                }
            }
        }
    }

    public static <T extends Serializable> T deepClone(T src) {
        checkIfSerializable(src.getClass());
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(src);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);

            //noinspection unchecked
            T result = (T) ois.readObject();

            ois.close();
            oos.close();

            return result;
        }
        catch ( IOException | ClassNotFoundException e ) {
            throw new RuntimeException(e);
        }
    }
}
