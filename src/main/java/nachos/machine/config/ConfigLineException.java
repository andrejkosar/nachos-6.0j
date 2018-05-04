// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.config;

public class ConfigLineException extends RuntimeException {
    ConfigLineException(String configFile, int line) {
        super("Error in " + configFile + " on line " + line);
    }
}
