#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test if calling join returns -1 when provided
 * child process pid does not belong to process, which is child
 * process of the current process.
 *
 * Creates child process which tries to join on the root process.
 * Calling join inside child process should fail, because root
 * process is not child process, of the process which calls join
 * on it (caller is the child process of the root process).
 *
 * argc     - equals 1
 * argv[0]  - indicates which part should run
 *
 * returns  - 0 on success
 */
int main(int argc, char **argv) {
    char *args_1[1];
    int child_pid_1 = -1;
    int child_status_1 = -1;
    int child_status_2 = -1;

    // Make sure we have been called with correct number of arguments.
    assert(argc == 1);

    if (0 == strcmp("first", argv[0])) {
        // Create first child process.
        args_1[0] = "second";
        child_pid_1 = exec("test_join_2.elf", 1, args_1);
        assert(-1 != child_pid_1);

        // Call join on second child first. Should be OK.
        assert(1 == join(child_pid_1, &child_status_1));
        assert(0 == child_status_1);
    } else if (0 == strcmp("second", argv[0])) {
        // Try to call join on the first child process, which
        // pid was passed via argument from parent process.
        // Should fail.
        assert(-1 == join(rootPid, &child_status_2));
    } else {
        // Wrong usage, fail program.
        assertNotReached();
    }

    return 0;
}