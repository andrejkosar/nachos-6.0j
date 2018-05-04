// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.coff;

import nachos.machine.io.OpenFile;
import nachos.machine.lib.Lib;

import java.nio.ByteOrder;

/**
 * A COFF (common object file format) loader.
 */
public class Coff {
    private static final int headerLength = 20;
    private static final int aoutHeaderLength = 28;
    private static final char dbgCoff = 'c';
    /**
     * The virtual address of the first instruction of the program.
     */
    protected int entryPoint;
    /**
     * The sections in this COFF executable.
     */
    protected CoffSection sections[];
    private OpenFile file;

    /**
     * Load the COFF executable in the specified file.
     * <p>
     * Notes:
     * </p>
     * <ol>
     * <li>If the constructor returns successfully, the file becomes the
     * property of this loader, and should not be accessed any further.
     * <li>The nachos runtime recorder expects this loader class to be used. Do not load
     * sections through any other mechanism.
     * <li>This loader will verify that the file is backed by a file system,
     * by asserting that read() operations take non-zero simulated time to
     * complete. Do not supply a file backed by a simulated cache (the primary
     * purpose of this restriction is to prevent sections from being loaded
     * instantaneously while handling page faults).
     * </ol>
     *
     * @param file the file containing the executable.
     * @throws CoffLoadingException if the executable is corrupt.
     */
    public Coff(OpenFile file) throws CoffLoadingException {
        this.file = file;

        byte[] headers = new byte[headerLength + aoutHeaderLength];

        if ( file.length() < headers.length ) {
            Lib.debug(dbgCoff, "\tfile is not executable");
            throw new CoffLoadingException();
        }

        Lib.strictReadFile(file, 0, headers, 0, headers.length);

        int magic = Lib.bytesToUnsignedShort(headers, 0, ByteOrder.LITTLE_ENDIAN);
        int numSections = Lib.bytesToUnsignedShort(headers, 2, ByteOrder.LITTLE_ENDIAN);
        int optionalHeaderLength = Lib.bytesToUnsignedShort(headers, 16, ByteOrder.LITTLE_ENDIAN);
        int flags = Lib.bytesToUnsignedShort(headers, 18, ByteOrder.LITTLE_ENDIAN);
        entryPoint = Lib.bytesToInt(headers, headerLength + 16, ByteOrder.LITTLE_ENDIAN);

        if ( magic != 0x0162 ) {
            Lib.debug(dbgCoff, "\tincorrect magic number");
            throw new CoffLoadingException();
        }
        if ( numSections < 2 || numSections > 10 ) {
            Lib.debug(dbgCoff, "\tbad section count");
            throw new CoffLoadingException();
        }
        if ( (flags & 0x0003) != 0x0003 ) {
            Lib.debug(dbgCoff, "\tbad header flags");
            throw new CoffLoadingException();
        }

        int offset = headerLength + optionalHeaderLength;

        sections = new CoffSection[numSections];
        for ( int s = 0; s < numSections; s++ ) {
            int sectionEntryOffset = offset + s * CoffSection.headerLength;
            try {
                sections[s] = new CoffSection(file, this, sectionEntryOffset);
            }
            catch ( CoffLoadingException e ) {
                Lib.debug(dbgCoff, "\terror loading section " + s);
                throw e;
            }
        }
    }

    /**
     * Return the number of sections in the executable.
     *
     * @return the number of sections in the executable.
     */
    public int getNumSections() {
        return sections.length;
    }

    /**
     * Return an object that can be used to access the specified section. Valid
     * section numbers include <tt>0</tt> through <tt>getNumSections() -
     * 1</tt>.
     *
     * @param sectionNumber the section to select.
     * @return an object that can be used to access the specified section.
     */
    public CoffSection getSection(int sectionNumber) {
        Lib.assertTrue(sectionNumber >= 0 && sectionNumber < sections.length);

        return sections[sectionNumber];
    }

    /**
     * Return the program entry point. This is the value that to which the PC
     * register should be initialized to before running the program.
     *
     * @return the program entry point.
     */
    public int getEntryPoint() {
        Lib.assertTrue(file != null);

        return entryPoint;
    }

    /**
     * Close the executable file and release any resources allocated by this
     * loader.
     */
    public void close() {
        file.close();

        sections = null;
    }
}
