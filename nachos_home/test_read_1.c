#include "syscall.h"
#include "stdlib.h"

/*
 * Opens file and reads specified number of bytes from it.
 *
 * argc     - equals 2
 * argv[0]  - file that exists in nachos_home directory
 * argv[1]  - max number of bytes that should be read from file
 *
 * returns  - 0 on success
 */
int main(int argc, char **argv) {
    int fd;
    char buffer[50];

    // Make sure we have been called with correct number of arguments.
    assert(argc == 2);

    // Open file and read defined count of bytes from it.
    fd = open(argv[0]);
    assert(fd != -1);
    assert(-1 != read(fd, buffer, atoi(argv[1])));

    return 0;
}