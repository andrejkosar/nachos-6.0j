#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test if last call to exit terminates nachos machine.
 *
 * Parent process executes the child process, without waiting for it
 * to finish (not joining it). After parent process marks child process
 * execution start, it calls exit, which should not cause nachos machine
 * termination. Because of RoundRobinScheduler used with this test, we
 * are guaranteed that child process will be scheduled only after parent
 * process finishes it's execution. Child process calls exit and should
 * cause nachos machine termination, as it is last process running.
 *
 * argc     - equals 2
 * argv[0]  - indicates which part should run
 * argv[1]  - exit status of the both processes
 *
 * returns  - does not return on success
 */
int main(int argc, char **argv) {
    char *args[2];
    int child_pid = -1;

    // Make sure we have been called with correct number of arguments.
    assert(2 == argc);

    if (0 == strcmp("first", argv[0])) {
        // Start child process execution.
        args[0] = "second";
        args[1] = argv[1];
        child_pid = exec("test_exit_1.elf", 2, args);
        assert(-1 != child_pid);

        // Call exit. Should not cause nachos machine termination.
        exit(atoi(argv[1]));
        assertNotReached();
    } else if (0 == strcmp("second", argv[0])) {
        // Call exit. Should cause nachos machine termination.
        exit(atoi(argv[1]));
        assertNotReached();
    } else {
        // Wrong usage fail program.
        assertNotReached();
    }
}