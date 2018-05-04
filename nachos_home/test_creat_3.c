#include "syscall.h"
#include "stdlib.h"

/*
 * Calls creat syscall multiple times. There are 16 file descriptors allowed
 * to be opened per process from which first two should be allocated for
 * stdin and stdout.
 * Besides that StubFileSystem has number of files opened concurrently restricted
 * to 16 as well. But if we open 14 files in one process, execute another process
 * and try to open another file, StubFileSystem will return null.
 * Reason behind this is, that besides 14 regular files, StubFileSystem has
 * opened 2 executable files (2 processes), which makes 16 files opened.
 * This program helps to execute this and thus testing if creat fails and
 * returns -1 as it should.
 *
 * argc    - equals 2
 * argv[0] - indicates which part should run
 * argv[1] - file that does not exist in nachos_home directory
 *
 * returns - 0 on success
 */
int main(int argc, char **argv) {
    char *args[2];
    int child_pid = -1;
    int child_status = -1;
    int i;

    // Make sure we have been called with correct number of arguments.
    assert(2 == argc);

    if (0 == strcmp("first", argv[0])) {
        // Call creat 14 - times. Everything should be OK.
        for (i = 0; i < 14; i++) {
            assert(-1 != creat(argv[1]));
        }
        // Execute child process joining it.
        args[0] = "second";
        args[1] = argv[1];
        child_pid = exec("test_creat_3.elf", 2, args);
        assert(-1 != child_pid);
        assert(1 == join(child_pid, &child_status));
        assert(0 == child_status);
    } else if (0 == strcmp("second", argv[0])) {
        // StubFileSystem open file count exceeded, so creat should return -1.
        assert(-1 == creat(argv[1]));
    } else {
        // Wrong usage fail program.
        assertNotReached();
    }

    return 0;
}