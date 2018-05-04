#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test if calling exit deletes file marked for
 * deletion, if the file is not opened by other process.
 *
 * Opens file, calls unlink on it (should not cause immediate
 * deletion, as it is still opened by current process) and
 * calls exit. Call to exit should cause file deletion.
 *
 * argc     - equals 2
 * argv[0]  - file that exists in nachos_home directory
 * argv[1]  - exit status of the process
 *
 * returns  - does not return on success
 */
int main(int argc, char **argv) {
    assert(argc == 2);

    assert(-1 != open(argv[0]));
    assert(0 == unlink(argv[0]));
    exit(atoi(argv[1]));
    assertNotReached();

    return 0;
}