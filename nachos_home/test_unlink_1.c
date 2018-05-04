#include "syscall.h"
#include "stdlib.h"

/*
 * Opens file, calls unlink on it and then closes the file
 * descriptor.
 *
 * argc     - equals 1
 * argv[0]  - file that exists in nachos_home directory
 *
 * returns  - 0 on success
 */
int main(int argc, char **argv) {
    int fd = -1;

    // Make sure we have been called with correct number of arguments.
    assert(1 == argc);

    // Open file. Call unlink on the same file, which should mark the file
    // for deletion (not delete it immediately). After that call close which
    // should delete the file.
    fd = open(argv[0]);
    assert(-1 != fd);
    assert(0 == unlink(argv[0]));
    assert(0 == close(fd));

    return 0;
}