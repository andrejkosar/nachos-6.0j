// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.config;

public class ConfigFileException extends RuntimeException {
    ConfigFileException(String configFile, Throwable cause) {
        super("Error loading config from " + configFile, cause);
    }
}