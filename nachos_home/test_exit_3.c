#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test if calling exit does not delete file marked
 * for deletion, if the file still remains opened by some other process.
 *
 * Opens file, executes child process and waits for the child process
 * to return and exits with specified exit status. Child process
 * opens the same file and calls unlink on it. Then child exits.
 * File deletion should not be caused by child calling unlink nor
 * child process exiting, because file is still opened by parent process.
 * When the child returns, parent process call to exit should cause
 * file deletion.
 *
 * argc     - equals 3
 * argv[0]  - indicates which part should run
 * argv[1]  - file that exists in nachos_home directory
 * argv[2]  - exit status of the both processes
 *
 * returns  - does not return on success
 */
int main(int argc, char **argv) {
    char *args[3];
    int child_pid = -1;
    int child_status = -1;

    assert(3 == argc);

    if (0 == strcmp("first", argv[0])) {
        assert(-1 != open(argv[1]));

        args[0] = "second";
        args[1] = argv[1];
        args[2] = argv[2];
        child_pid = exec("test_exit_3.elf", 3, args);
        assert(-1 != child_pid);
        assert(1 == join(child_pid, &child_status));

        exit(atoi(argv[2]));
        assertNotReached();
    } else if (0 == strcmp("second", argv[0])) {
        assert(-1 != open(argv[1]));
        assert(0 == unlink(argv[1]));
        exit(atoi(argv[2]));
        assertNotReached();
    } else {
        assertNotReached();
    }

    return 0;
}