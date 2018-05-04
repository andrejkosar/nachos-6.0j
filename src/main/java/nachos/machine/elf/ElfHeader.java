// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.elf;

import nachos.machine.lib.Lib;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

class ElfHeader {
    final static int SIZE = 52;

    final Elf elf;

    Ident ident;
    FileType type;
    Machine machine;
    Version version;
    long entry;
    long phoff;
    long shoff;
    long flags;
    int ehsize;
    int phentsize;
    int phnum;
    int shentsize;
    int shnum;
    int shstrndx;

    public ElfHeader(Elf elf) {
        this.elf = elf;
    }

    int load(int offset, byte[] bytes) throws ElfLoadingException {
        Lib.assertTrue(offset + SIZE <= bytes.length,
                new ElfLoadingException("There is not enough bytes to parse elf header"));

        ident = new Ident();
        offset = ident.load(offset, bytes);
        type = FileType.of(Lib.bytesToUnsignedShort(bytes, offset, ident.dataEncoding));
        offset += 2;
        machine = Machine.of(Lib.bytesToUnsignedShort(bytes, offset, ident.dataEncoding));
        offset += 2;
        version = Version.of(Lib.bytesToUnsignedInt(bytes, offset, ident.dataEncoding));
        offset += 4;
        entry = Lib.bytesToUnsignedInt(bytes, offset, ident.dataEncoding);
        offset += 4;
        phoff = Lib.bytesToUnsignedInt(bytes, offset, ident.dataEncoding);
        offset += 4;
        shoff = Lib.bytesToUnsignedInt(bytes, offset, ident.dataEncoding);
        offset += 4;
        flags = Lib.bytesToUnsignedInt(bytes, offset, ident.dataEncoding);
        offset += 4;
        ehsize = Lib.bytesToUnsignedShort(bytes, offset, ident.dataEncoding);
        offset += 2;
        phentsize = Lib.bytesToUnsignedShort(bytes, offset, ident.dataEncoding);
        offset += 2;
        phnum = Lib.bytesToUnsignedShort(bytes, offset, ident.dataEncoding);
        offset += 2;
        shentsize = Lib.bytesToUnsignedShort(bytes, offset, ident.dataEncoding);
        offset += 2;
        shnum = Lib.bytesToUnsignedShort(bytes, offset, ident.dataEncoding);
        offset += 2;
        shstrndx = Lib.bytesToUnsignedShort(bytes, offset, ident.dataEncoding);
        offset += 2;

        return offset;
    }

    @Override
    public String toString() {
        return "\n\tElfHeader{" +
                "\n\t\tident = " + ident +
                "\n\t\ttype = " + type +
                "\n\t\tmachine = " + machine +
                "\n\t\tversion = " + version +
                "\n\t\tentry = " + entry +
                "\n\t\tphoff = " + phoff +
                "\n\t\tshoff = " + shoff +
                "\n\t\tflags = " + flags +
                "\n\t\tehsize = " + ehsize +
                "\n\t\tphentsize = " + phentsize +
                "\n\t\tphnum = " + phnum +
                "\n\t\tshentsize = " + shentsize +
                "\n\t\tshnum = " + shnum +
                "\n\t\tshstrndx = " + shstrndx +
                "\n\t}";
    }

    enum Version {
        Invalid(0), Current(1);

        private static final Map<Long, Version> reverseMap;

        static {
            reverseMap = new HashMap<>();
            for ( Version version : Version.values() ) {
                reverseMap.put(version.value, version);
            }
        }

        final long value;

        Version(long value) {
            this.value = value;
        }

        static Version of(long value) throws ElfLoadingException {
            Version version = reverseMap.get(value);
            Lib.assertTrue(version != null, new ElfLoadingException("Error parsing elf header e_version field"));
            return version;
        }
    }

    enum Machine {
        None(0), M32(1), Sparc(2), Intel386(3), Motorola68K(4),
        Motorola88K(5), Intel860(7), MipsRS3000(8), MipsRS4000(10);

        private static final int reservedLowerBound = 11;
        private static final int reservedUpperBound = 16;
        private static final Map<Integer, Machine> reverseMap;

        static {
            reverseMap = new HashMap<>();
            for ( Machine machine : Machine.values() ) {
                reverseMap.put(machine.value, machine);
            }
        }

        final int value;

        Machine(int value) {
            this.value = value;
        }

        static Machine of(int value) throws ElfLoadingException {
            if ( value >= reservedLowerBound && value <= reservedUpperBound ) {
                Lib.debug(Elf.dbgElf, "Elf header field e_machine value is from reserved range");
                return null;
            }
            else {
                Machine machine = reverseMap.get(value);
                Lib.assertTrue(machine != null, new ElfLoadingException("Error parsing elf header e_machine field"));
                return machine;
            }
        }
    }

    enum FileType {
        None(0), Rel(1), Exec(2), Dyn(3), Core(4);

        private static final int reservedLowerBound = 5;
        private static final int reservedUpperBound = 0xfeff;
        private static final int processorSpecificLowerBound = 0xff00;
        private static final int processorSpecificUpperBound = 0xffff;
        private static final Map<Integer, FileType> reverseMap;

        static {
            reverseMap = new HashMap<>();
            for ( FileType fileType : FileType.values() ) {
                reverseMap.put(fileType.value, fileType);
            }
        }

        final int value;

        FileType(int value) {
            this.value = value;
        }

        static FileType of(int value) throws ElfLoadingException {
            if ( value >= reservedLowerBound && value <= reservedUpperBound ) {
                Lib.debug(Elf.dbgElf, "Elf header field e_type value is from reserved range");
                return null;
            }
            else if ( value >= processorSpecificLowerBound && value <= processorSpecificUpperBound ) {
                Lib.debug(Elf.dbgElf, "Elf header field e_type value is processor specific");
                return null;
            }
            else {
                FileType fileType = reverseMap.get(value);
                Lib.assertTrue(fileType != null, new ElfLoadingException("Error parsing elf header e_type field"));
                return fileType;
            }
        }
    }

    static class Ident {
        final static int SIZE = 16;
        byte mag0;
        byte mag1;
        byte mag2;
        byte mag3;
        FileClass fileClass;
        ByteOrder dataEncoding;
        Version version;

        int load(int offset, byte[] bytes) throws ElfLoadingException {
            Lib.assertTrue(offset + SIZE <= bytes.length,
                    new ElfLoadingException("There is not enough bytes to parse e_ident[] field"));
            int initialOffset = offset;

            mag0 = bytes[offset++];
            mag1 = bytes[offset++];
            mag2 = bytes[offset++];
            mag3 = bytes[offset++];

            // Magic number validation.
            Lib.assertTrue(mag0 == 0x7F && mag1 == 'E' && mag2 == 'L' && mag3 == 'F',
                    new ElfLoadingException("Specified file does not start with expected magic number bytes: [0x7F, 'E', 'L', 'F']"));

            fileClass = FileClass.of(bytes[offset++]);
            dataEncoding = dataEncodingOf(bytes[offset++]);
            version = Version.of(bytes[offset++]);

            int bytesRead = offset - initialOffset;
            Lib.assertTrue(bytesRead <= SIZE,
                    new ElfLoadingException("Error parsing elf header e_ident[] field. " +
                            "Size mismatch: expected=" + SIZE + ", actual=" + bytesRead));
            return SIZE;
        }

        @Override
        public String toString() {
            return "Ident{" +
                    "mag0=" + mag0 +
                    ", mag1=" + mag1 +
                    ", mag2=" + mag2 +
                    ", mag3=" + mag3 +
                    ", fileClass=" + fileClass +
                    ", dataEncoding=" + dataEncoding +
                    ", version=" + version +
                    '}';
        }

        private ByteOrder dataEncodingOf(byte value) throws ElfLoadingException {
            switch ( value ) {
                case 1:
                    return ByteOrder.LITTLE_ENDIAN;
                case 2:
                    return ByteOrder.BIG_ENDIAN;
                default:
                    throw new ElfLoadingException("Cannot parse elf header EI_DATA field");
            }
        }

        enum FileClass {
            Invalid(0), Class32(1), Class64(2);

            private static final Map<Integer, FileClass> reverseMap;

            static {
                reverseMap = new HashMap<>();
                for ( FileClass fileClass : FileClass.values() ) {
                    reverseMap.put(fileClass.value, fileClass);
                }
            }

            final int value;

            FileClass(int value) {
                this.value = value;
            }

            static FileClass of(int value) throws ElfLoadingException {
                FileClass fileClass = reverseMap.get(value);
                Lib.assertTrue(fileClass != null, new ElfLoadingException("Cannot parse elf header EI_CLASS field"));
                return fileClass;
            }
        }
    }
}
