#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test if calling unlink does not delete file
 * immediately, if the file is opened by other than current process.
 *
 * Opens file and executes child process joining it. Child process
 * will simply call unlink on the same file which should not cause
 * immediate file deletion, because file is still open in the parent
 * process. After child process returns calls close on the same file,
 * which should delete the file.
 *
 * argc     - equals 2
 * argv[0]  - indicates which part should run
 * argv[1]  - file that exists in nachos_home directory
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
        // Open file.
        fd = open(argv[1]);
        assert(-1 != fd);

        // Execute child process and wait for it's return.
        args[0] = "second";
        args[1] = argv[1];
        child_pid = exec("test_unlink_2.elf", 2, args);
        assert(-1 != child_pid);
        assert(1 == join(child_pid, &child_status));
        assert(0 == child_status);

        // Close file which should cause file deletion.
        assert(0 == close(fd));
    } else if (0 == strcmp("second", argv[0])) {
        // Call unlink on the file from child process, which should not
        // cause immediate file deletion.
        assert(0 == unlink(argv[1]));
    } else {
        // Wrong usage fail program.
        assertNotReached();
    }

    return 0;
}