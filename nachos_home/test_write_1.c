#include "syscall.h"
#include "stdlib.h"

/*
 * Opens file and writes specified string into it.
 *
 * argc     - equals 3
 * argv[0]  - file that does not exist in nachos_home directory
 * argv[1]  - string (bytes) which will be written into the file
 * argv[2]  - max number of bytes that should be written into the file
 *
 * returns  - 0 on success
 */
int main(int argc, char **argv) {
    int fd = -1;

    // Make sure we have been called with correct number of arguments.
    assert(argc == 3);

    // Create file and write provided bytes into it.
    fd = creat(argv[0]);
    assert(fd != -1);
    assert(-1 != write(fd, argv[1], atoi(argv[2])));

    return 0;
}