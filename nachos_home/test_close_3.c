#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test whether close does not delete file marked for
 * deletion while this file is still opened by some other process.
 *
 * Creates file and executes child process joining it. Child process
 * will open same file again, call unlink on it, which should mark the
 * file for deletion, as it is still opened by parent process and at last
 * call close on the file descriptor opened by child process. Call to close
 * by child process should not delete file, as it is still opened by parent
 * process, even if it is already marked for deletion. After child process
 * returns, parent process call to close should cause file to be deleted.
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
        child_pid = exec("test_close_3.elf", 2, args);
        assert(-1 != child_pid);
        assert(1 == join(child_pid, &child_status));
        assert(0 == child_status);

        // Close file which should cause file deletion.
        assert(0 == close(fd));
    } else if (0 == strcmp("second", argv[0])) {
        // Open file, mark it for deletion and close it.
        // Closing file should not cause file deletion, because it is
        // still opened by parent process.
        fd = open(argv[1]);
        assert(-1 != fd);
        assert(0 == unlink(argv[1]));
        assert(0 == close(fd));
    } else {
        // Wrong usage fail program.
        assertNotReached();
    }

    return 0;
}