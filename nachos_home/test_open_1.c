#include "syscall.h"
#include "stdlib.h"

/*
 * Calls open syscall multiple times. There are 16 file descriptors allowed
 * to be opened per process from which first two should be allocated for
 * stdin and stdout. So assuming, that this process is only one currently
 * in execution, we should be able to allocate 14. Following calls should fail.
 *
 * argc    - equals 1
 * argv[0] - file that exists in nachos_home directory
 *
 * returns  - 0 on success
 */
int main(int argc, char **argv) {
    int i;

    // Make sure we have been called with correct number of arguments.
    assert(1 == argc);

    // Call open 14 - times. Everything should be OK.
    for (i = 0; i < 14; i++) {
        assert(-1 != open(argv[0]));
    }

    // Following call should fail.
    assert(-1 == open(argv[0]));

    return 0;
}