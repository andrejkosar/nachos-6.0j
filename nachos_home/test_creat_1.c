#include "syscall.h"
#include "stdlib.h"

/*
 * Calls creat syscall multiple times. There are 16 file descriptors allowed
 * to be opened per process from which first two should be allocated for
 * stdin and stdout. So assuming, that this process is only one currently
 * in execution, we should be able to allocate 14. Following calls should fail.
 *
 * argc    - equals 2
 * argv[0] - file that exists in nachos_home directory
 * argv[1] - file that does not exist in nachos_home directory
 *
 * returns - 0 on success
 */
int main(int argc, char **argv) {
    int i;

    // Make sure we have been called with correct number of arguments.
    assert(2 == argc);

    // Call creat 14 - times. Everything should be OK.
    for (i = 0; i < 14; i++) {
        assert(-1 != creat(argv[0]));
    }

    // First call creat on existing file. Then try ti call creat on
    // file that does not exist. Both calls should fail and thus in the
    // second case no file should be created.
    assert(-1 == creat(argv[0]));
    assert(-1 == creat(argv[1]));

    return 0;
}