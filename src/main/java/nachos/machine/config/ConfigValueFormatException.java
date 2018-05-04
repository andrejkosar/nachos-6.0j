// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.config;

public class ConfigValueFormatException extends RuntimeException {
    ConfigValueFormatException(String key, String value, String expectedFormat) {
        super("Config key \"" + key + "\" has wrong value \"" + value + "\". Expected value format: " + expectedFormat);
    }

    ConfigValueFormatException(String key, String value, String expectedFormat, Throwable cause) {
        super("Config key \"" + key + "\" has wrong value \"" + value + "\". Expected value format: " + expectedFormat, cause);
    }
}
