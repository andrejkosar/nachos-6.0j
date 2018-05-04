// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.processor;

import nachos.machine.lib.Lib;

import static nachos.machine.processor.Processor.exceptionNames;

public class MipsException extends Exception {
    public boolean hasBadVAddr = false;
    public int cause;
    public int badVAddr;

    MipsException(int cause) {
        Lib.assertTrue(cause >= 0 && cause < exceptionNames.length);
        this.cause = cause;
    }

    MipsException(int cause, int badVAddr) {
        this(cause);
        hasBadVAddr = true;
        this.badVAddr = badVAddr;
    }
}
