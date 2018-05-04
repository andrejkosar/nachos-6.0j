// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.tcb;

/**
 * Thrown by every nachos thread when it finishes it's execution
 * inside {@link TCB#yield()}.
 */
class NachosThreadFinishedException extends RuntimeException {
    NachosThreadFinishedException() {
    }
}
