#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test whether close system call deletes file
 * marked for deletion using unlink.
 *
 * Creates file and executes child process joining it. Child process
 * will simply call unlink on the same file, which should mark it for
 * deletion (not delete directly as file is still opened in parent process).
 * After child process returns, parent process calls close on the file, deleting
 * it in the process if handleClose is implemented correctly.
 *
 * argc     - equals 2
 * argv[0]  - indicates which part should run
 * argv[1]  - file that does not exist in nachos_home directory
 *
 * returns - 0 on success
 */
int main(int argc, char **argv) {
    int fd = -1;
    char *args[2];
    int child_pid = -1;
    int child_status = -1;

    // Make sure we have been called with correct number of arguments.
    assert(2 == argc);

    if (0 == strcmp("first", argv[0])) {
        // Create file.
        fd = creat(argv[1]);
        assert(-1 != fd);

        // Execute child process and wait for it's return.
        args[0] = "second";
        args[1] = argv[1];
        child_pid = exec("test_close_2.elf", 2, args);
        assert(-1 != child_pid);
        assert(1 == join(child_pid, &child_status));
        assert(0 == child_status);

        // Close the file, which should delete it.
        assert(0 == close(fd));
    } else if (0 == strcmp("second", argv[0])) {
        // Call unlink on the file, which should mark it for deletion only, as
        // file is still opened in parent process.
        assert(0 == unlink(argv[1]));
    } else {
        // Wrong usage fail program.
        assertNotReached();
    }

    return 0;
}