#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test if calling halt does not stop nachos
 * machine execution, if called by other than root process.
 *
 * Creates child process joining to it. Child process simply
 * calls halt, which should child process execution to return
 * immediately, but not to halt nachos machine. After child
 * process returns, parent process calls halt, which should cause
 * nachos machine to stop it's execution.
 *
 * argc     - equals 1
 * argv[0]  - indicates which part should run
 *
 * returns  - does not return on success
 */
int main(int argc, char **argv) {
    char *args[1];
    int child_pid = -1;
    int child_status = -1;

    // Make sure we have been called with correct number of arguments.
    assert(1 == argc);

    if (0 == strcmp("first", argv[0])) {
        // Execute child process joining it.
        args[0] = "second";
        child_pid = exec("test_halt_2.elf", 1, args);
        assert(-1 != child_pid);
        assert(1 == join(child_pid, &child_status));

        // Call halt, which should cause whole machine to stop it's execution.
        halt();
        assertNotReached();
    } else if (0 == strcmp("second", argv[0])) {
        // Call halt, which should cause child process execution to stop, but
        // parent process execution should continue.
        halt();
        assertNotReached();
    } else {
        assertNotReached();
    }

    return 0;
}