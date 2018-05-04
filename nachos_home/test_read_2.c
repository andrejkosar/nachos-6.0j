#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test if read syscall returns -1 when called on
 * file descriptor with file position set to undefined.
 *
 * File descriptor gets file position undefined when some other
 * call to read (or write) on the same file descriptor returned -1.
 * If the handleRead implementation is correct we should be able to
 * achieve this by calling read on file descriptor with no open file
 * present.
 *
 * argc     - equals 2
 * argv[0]  - file descriptor, with no open file present
 * argv[1]  - max number of bytes that should be read from file
 *
 * returns  - 0 on success
 */
int main(int argc, char **argv) {
    char buffer[50];
    int fd;
    int count;

    // Make sure we have been called with correct number of arguments.
    assert(argc == 2);

    fd = atoi(argv[0]);
    count = atoi(argv[1]);

    // This call should fail because there is no open file present
    // on specified file descriptor.
    assert(-1 == read(fd, buffer, count));

    // This call should fail because position is undefined on specified
    // file descriptor, caused by previous call.
    assert(-1 == read(fd, buffer, count));

    return 0;
}