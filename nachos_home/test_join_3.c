#include "syscall.h"
#include "stdlib.h"

/*
 * Purpose is to test if second call to join returns -1 when
 * called on the same child for the second time.
 *
 * Creates child process and calls join on it. Child process
 * just returns integer passed via argument. Second call to
 * join on the same child process, that should be finished by
 * the time of the call, should fail.
 *
 * argc     - equals 2
 * argv[0]  - indicates which part should run
 * argv[1]  - integer which will be passed to child as
 *            it's return status
 *
 * returns  - returns passed integer on success
 */
int main(int argc, char **argv) {
    char *args[2];
    int child_pid = -1;
    int child_status = -1;

    // Make sure we have been called with correct number of arguments.
    assert(2 == argc);

    if (0 == strcmp("first", argv[0])) {
        // Create child process.
        args[0] = "second";
        args[1] = argv[1];
        child_pid = exec("test_join_3.elf", 2, args);
        assert(-1 != child_pid);

        // Wait for child to finish by calling join on it.
        assert(1 == join(child_pid, &child_status));
        assert(atoi(argv[1]) == child_status);

        // Call join on the same child for the second time.
        // Should fail.
        assert(-1 == join(child_pid, &child_status));

        return child_status;
    } else if (0 == strcmp("second", argv[0])) {
        // Simply return integer passed via argument.
        return atoi(argv[1]);
    } else {
        // Wrong usage, fail program.
        assertNotReached();
    }

    return 0;
}