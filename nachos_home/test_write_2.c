#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test if write syscall returns -1 when called on
 * file descriptor with file position set to undefined.
 *
 * File descriptor gets file position undefined when some other
 * call to write (or read) on the same file descriptor returned -1.
 * If the handleWrite implementation is correct we should be able to
 * achieve this by calling write on file descriptor with no open file
 * present.
 *
 * argc     - equals 3
 * argv[0]  - file descriptor, with no open file present
 * argv[1]  - string (bytes) which should not be written into the file
 * argv[2]  - max number of bytes that should be written into the file
 *
 * returns  - 0 on success
 */
int main(int argc, char **argv) {
    int fd;
    int count;

    // Make sure we have been called with correct number of arguments.
    assert(argc == 3);

    fd = atoi(argv[0]);
    count = atoi(argv[2]);

    // This call should fail because there is no open file present
    // on specified file descriptor.
    assert(-1 == write(fd, argv[1], count));

    // This call should fail because position is undefined on specified
    // file descriptor, caused by previous call.
    assert(-1 == write(fd, argv[1], count));

    return 0;
}