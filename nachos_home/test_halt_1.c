#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test if calling halt actually stops nachos machine
 * execution and prevents any created child process to start it's
 * execution.
 *
 * Creates child process, but does not wait for start of it's execution.
 * After creating child process calls halt, which should stop nachos
 * machine execution and prevent child process to start.
 *
 * argc     - equals 1
 * argv[0]  - indicates which part should run
 *
 * returns  - does not return on success
 */
int main(int argc, char **argv) {
    char *args[1];
    int child_pid = -1;

    // Make sure we have been called with correct number of arguments.
    assert(1 == argc);

    if (0 == strcmp("first", argv[0])) {
        // Create child process.
        args[0] = "second";
        child_pid = exec("test_halt_1.elf", 1, args);
        assert(-1 != child_pid);

        // Call halt, which should cause whole machine to stop it's execution.
        halt();
        assertNotReached();
    } else if (0 == strcmp("second", argv[0])) {
        // Child process should not get chance to start it's execution.
        // Fail if it does.
        assertNotReached();
    } else {
        // Wrong usage fail program.
        assertNotReached();
    }

    return 0;
}