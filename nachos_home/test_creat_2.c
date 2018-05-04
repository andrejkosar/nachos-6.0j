#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to call creat on file that has been marked for deletion.
 *
 * We will achieve this by calling creat on file and then creating child
 * process (which parent process joins). Child process will call unlink on
 * the same file and immediately after that calling creat again which should fail,
 * as file was already marked for deletion (it was not deleted immediately, because
 * parent process still has file opened) by child process in the previous step.
 *
 * argc     - equals 2
 * argv[0]  - name of the system call to call
 * argv[1]  - file that does not exist in nachos_home directory
 *
 * returns  - 0 on success
 */
int main(int argc, char **argv) {
    char *args[2];
    int child_pid = -1;
    int child_status = -1;

    // Make sure we have been called with correct number of arguments.
    assert(2 == argc);

    if (0 == strcmp("first", argv[0])) {
        // Create file and exec child process, joining it.
        // Note that file still remains open in parent process.
        assert(-1 != creat(argv[1]));
        args[0] = "second";
        args[1] = argv[1];
        child_pid = exec("test_creat_2.elf", 2, args);
        assert(-1 != child_pid);
        assert(1 == join(child_pid, &child_status));
        assert(0 == child_status);
    } else if (0 == strcmp("second", argv[0])) {
        // Call unlink on the file, which is still opened by the
        // parent process. Call to creat on the same file again should fail.
        assert(0 == unlink(argv[1]));
        assert(-1 == creat(argv[1]));
    } else {
        // Wrong usage fail program.
        assertNotReached();
    }

    return 0;
}