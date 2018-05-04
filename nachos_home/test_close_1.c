#include "syscall.h"
#include "stdlib.h"

/*
 * Opens file and closes it immediately.
 *
 * argc     - equals 1
 * argv[0]  - file that exists in nachos_home directory
 *
 * returns  - 0 on success
 */
int main(int argc, char **argv) {
    int fd = -1;

    // Make sure we have been called with correct number of arguments.
    assert(argc == 1);

    // Open file and close it immediately.
    fd = open(argv[0]);
    assert(-1 != fd);
    assert(0 == close(fd));

    return 0;
}