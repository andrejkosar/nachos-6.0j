// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.io;

/**
 * An <tt>OpenFile</tt> that maintains a current file position.
 */
public abstract class OpenFileWithPosition extends OpenFile {
    /**
     * The current value of the file pointer.
     */
    protected int position = 0;

    /**
     * Allocate a new <tt>OpenFileWithPosition</tt> with the specified name on
     * the specified file system.
     *
     * @param fileSystem the file system to which this file belongs.
     * @param name       the name of the file, on that file system.
     */
    public OpenFileWithPosition(FileSystem fileSystem, String name) {
        super(fileSystem, name);
    }

    /**
     * Allocate a new unnamed <tt>OpenFileWithPosition</tt> that is not
     * associated with any file system.
     */
    public OpenFileWithPosition() {
        super();
    }

    @Override
    public void seek(int position) {
        this.position = position;
    }

    @Override
    public int tell() {
        return position;
    }

    @Override
    public int read(byte[] buf, int offset, int length) {
        int amount = read(position, buf, offset, length);
        if ( amount == -1 ) {
            return -1;
        }

        position += amount;
        return amount;
    }

    @Override
    public int write(byte[] buf, int offset, int length) {
        int amount = write(position, buf, offset, length);
        if ( amount == -1 ) {
            return -1;
        }

        position += amount;
        return amount;
    }
}
