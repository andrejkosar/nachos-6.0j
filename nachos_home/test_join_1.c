#include "syscall.h"
#include "stdlib.h"
#include "stdio.h"

/*
 * Purpose is to test if calling join suspends current process
 * until child process finishes it's execution.
 *
 * Executes child process, joining it. Parent process passes to
 * child process argument with incremented integer variable, which
 * child process simply returns. If join implementation is correct
 * child process return status should be equal to incremented integer
 * variable.
 *
 * argc     - equals 2
 * argv[0]  - indicates which part should run
 * argv[1]  - integer variable, which will be incremented and passed
 *            to child process
 *
 * returns  - incremented integer variable on success
 */
int main(int argc, char **argv) {
    int i = -1;
    char i_string[11];
    char *args[2];
    int child_pid = -1;
    int child_status = -1;

    // Make sure we have been called with correct number of arguments.
    assert(argc == 2);

    if (0 == strcmp("first", argv[0])) {
        // Parse integer variable from program argument.
        i = atoi(argv[1]);

        // Increment and store integer variable into string argument
        // which will be passed to child process.
        sprintf(i_string, "%d\0", ++i);

        // Execute child process.
        args[0] = "second";
        args[1] = i_string;
        child_pid = exec("test_join_1.elf", 2, args);
        assert(-1 != child_pid);

        // Wait while child process returns. Child status should be
        // set to incremented integer variable.
        assert(1 == join(child_pid, &child_status));
        assert(i == child_status);

        return child_status;
    } else if (0 == strcmp("second", argv[0])) {
        // Parse and return integer variable passed through argument.
        return atoi(argv[1]);
    } else {
        // Wrong usage, fail program.
        assertNotReached();
    }

    return 0;
}