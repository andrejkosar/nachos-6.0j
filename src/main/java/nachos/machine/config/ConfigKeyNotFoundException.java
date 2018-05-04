// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.config;

public class ConfigKeyNotFoundException extends RuntimeException {
    ConfigKeyNotFoundException(String key) {
        super("Wrong config key: \"" + key + "\"");
    }
}
