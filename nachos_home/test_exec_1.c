#include "stdlib.h"

/*
 * Program simply returns value provided by parameter.
 *
 * argc     - equals 1
 * argv[0]  - value returned by this program
 *
 * returns  - value provided by the argument on success
 */
int main(int argc, char **argv) {
    assert(argc == 1);

    return atoi(argv[0]);
}