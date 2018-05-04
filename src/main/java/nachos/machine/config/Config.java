// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.config;

import nachos.machine.lib.Lib;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides routines to access the Nachos configuration.
 */
public final class Config {
    /**
     * Flag to indicate if configuration is loaded already.
     */
    private static boolean loaded;

    /**
     * Map containing configuration values.
     */
    private static Map<String, String> config;

    static {
        initializeStaticFields();
    }

    private static void initializeStaticFields() {
        loaded = false;
        config = null;
    }

    /**
     * Load configuration information from the specified file. Must be called
     * before the Nachos security manager is installed.
     * <p>
     * Can be called only once.
     *
     * @param filename the name of the file containing the configuration to use.
     */
    public static void load(String filename) {
        System.out.print(" config");

        Lib.assertTrue(!loaded);
        loaded = true;

        config = parseConfigFile(filename);
    }

    /**
     * Load configuration from the specified file.
     *
     * @param filename the name of the file containing the configuration to use.
     * @return the map representing configuration.
     */
    public static Map<String, String> parseConfigFile(String filename) {
        try ( Reader reader = new FileReader(filename) ) {
            Map<String, String> config = new HashMap<>();

            StreamTokenizer s = new StreamTokenizer(reader);

            s.resetSyntax();
            s.whitespaceChars(0x00, 0x20);
            s.wordChars(0x21, 0xFF);
            s.eolIsSignificant(true);
            s.commentChar('#');
            s.quoteChar('"');

            int line = 1;

            s.nextToken();

            while ( true ) {
                if ( s.ttype == StreamTokenizer.TT_EOF ) {
                    break;
                }

                if ( s.ttype == StreamTokenizer.TT_EOL ) {
                    line++;
                    s.nextToken();
                    continue;
                }

                if ( s.ttype != StreamTokenizer.TT_WORD ) {
                    throw new ConfigLineException(filename, line);
                }

                String key = s.sval;

                if ( s.nextToken() != StreamTokenizer.TT_WORD || !s.sval.equals("=") ) {
                    throw new ConfigLineException(filename, line);
                }

                if ( s.nextToken() != StreamTokenizer.TT_WORD && s.ttype != '"' ) {
                    throw new ConfigLineException(filename, line);
                }

                String value = s.sval;

                // ignore everything after first string
                // noinspection StatementWithEmptyBody
                while ( s.nextToken() != StreamTokenizer.TT_EOL && s.ttype != StreamTokenizer.TT_EOF ) {
                }

                if ( config.get(key) != null ) {
                    throw new ConfigLineException(filename, line);
                }

                config.put(key, value);
                line++;
            }

            return config;
        }
        catch ( IOException e ) {
            throw new ConfigFileException(filename, e);
        }
    }

    /**
     * Get the value of a key loaded from configuration file.
     *
     * @param key the key to look up.
     * @return the value of the specified key, or <tt>null</tt> if it is not
     * present.
     */
    public static String getString(String key) {
        String value = config.get(key);
        if ( value == null ) {
            throw new ConfigKeyNotFoundException(key);
        }
        return config.get(key);
    }

    /**
     * Get the value of a key loaded from configuration file, returning the specified
     * default if the key does not exist.
     *
     * @param key          the key to look up.
     * @param defaultValue the value to return if the key does not exist.
     * @return the value of the specified key, or <tt>defaultValue</tt> if it
     * is not present.
     */
    public static String getString(String key, String defaultValue) {
        try {
            return getString(key);
        }
        catch ( ConfigKeyNotFoundException e ) {
            return defaultValue;
        }
    }

    /**
     * Get the value of an integer key loaded from configuration file.
     *
     * @param key the key to look up.
     * @return the value of the specified key.
     */
    public static int getInteger(String key) {
        String value = getString(key);

        try {
            return new Integer(value);
        }
        catch ( NumberFormatException e ) {
            throw new ConfigValueFormatException(key, value, "integer", e);
        }
    }

    /**
     * Get the value of an integer key loaded from configuration file, returning the
     * specified default if the key does not exist.
     *
     * @param key          the key to look up.
     * @param defaultValue the value to return if the key does not exist.
     * @return the value of the specified key, or <tt>defaultValue</tt> if the
     * key does not exist.
     */
    public static int getInteger(String key, int defaultValue) {
        try {
            return getInteger(key);
        }
        catch ( ConfigKeyNotFoundException e ) {
            return defaultValue;
        }
    }

    /**
     * Get the value of a double key loaded from configuration file.
     *
     * @param key the key to look up.
     * @return the value of the specified key.
     */
    public static double getDouble(String key) {
        String value = getString(key);
        try {
            return new Double(value);
        }
        catch ( NumberFormatException e ) {
            throw new ConfigValueFormatException(key, value, "double", e);
        }
    }

    /**
     * Get the value of a double key loaded from configuration file, returning the
     * specified default if the key does not exist.
     *
     * @param key          the key to look up.
     * @param defaultValue the value to return if the key does not exist.
     * @return the value of the specified key, or <tt>defaultValue</tt> if the
     * key does not exist.
     */
    public static double getDouble(String key, double defaultValue) {
        try {
            return getDouble(key);
        }
        catch ( ConfigKeyNotFoundException e ) {
            return defaultValue;
        }
    }

    /**
     * Get the value of a boolean key loaded from configuration file.
     *
     * @param key the key to look up.
     * @return the value of the specified key.
     */
    public static boolean getBoolean(String key) {
        String value = getString(key);

        if ( value.equals("1") || value.toLowerCase().equals("true") ) {
            return true;
        }
        else if ( value.equals("0") || value.toLowerCase().equals("false") ) {
            return false;
        }
        else {
            throw new ConfigValueFormatException(key, value, "boolean");
        }
    }

    /**
     * Get the value of a boolean key loaded from configuration file, returning the
     * specified default if the key does not exist.
     *
     * @param key          the key to look up.
     * @param defaultValue the value to return if the key does not exist.
     * @return the value of the specified key, or <tt>defaultValue</tt> if the
     * key does not exist.
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        try {
            return getBoolean(key);
        }
        catch ( ConfigKeyNotFoundException e ) {
            return defaultValue;
        }
    }
}
