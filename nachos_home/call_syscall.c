#include "syscall.h"
#include "stdlib.h"

/*
 * Program calls syscall specified by parameter
 * and returns it's return value.
 *
 * argc      - at least 1, but depends on the called syscall
 * argv[0]   - syscall to call
 * argv[1-N] - syscall arguments
 *
 * returns   - 0 on success, except, when calling halt or exit
 *             when method does not return
 */
int main(int argc, char **argv) {
    int child_status = -1;
    char buffer[50];

    assert(argc > 0);

    if (0 == strcmp("creat", argv[0])) {
        assert(argc == 2);
        creat(argv[1]);
    } else if (0 == strcmp("open", argv[0])) {
        assert(argc == 2);
        open(argv[1]);
    } else if (0 == strcmp("read", argv[0])) {
        assert(argc == 3);
        read(atoi(argv[1]), buffer, atoi(argv[2]));
    } else if (0 == strcmp("write", argv[0])) {
        assert(argc == 4);
        write(atoi(argv[1]), argv[2], atoi(argv[3]));
    } else if (0 == strcmp("close", argv[0])) {
        assert(argc == 2);
        close(atoi(argv[1]));
    } else if (0 == strcmp("unlink", argv[0])) {
        assert(argc == 2);
        unlink(argv[1]);
    } else if (0 == strcmp("halt", argv[0])) {
        assert(argc == 1);
        halt();
        assertNotReached();
    } else if (0 == strcmp("exit", argv[0])) {
        assert(argc == 2);
        exit(atoi(argv[1]));
        assertNotReached();
    } else if (0 == strcmp("exec", argv[0])) {
        assert(argc >= 3);
        exec(argv[1], atoi(argv[2]), &argv[3]);
    } else if (0 == strcmp("join", argv[0])) {
        assert(argc == 2);
        join(atoi(argv[1]), &child_status);
    } else {
        assertNotReached();
    }

    return 0;
}