// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.elf;

import nachos.machine.lib.Lib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ElfProgramHeader {
    final static int SIZE = 32;

    final Elf elf;

    Type type;
    long offset;
    long vaddr;
    long paddr;
    long filesz;
    long memsz;
    Set<Flag> flags;
    long align;

    public ElfProgramHeader(Elf elf) {
        this.elf = elf;
    }

    int load(int offset, byte[] bytes) throws ElfLoadingException {
        Lib.assertTrue(offset + SIZE <= bytes.length, new ElfLoadingException(
                "There is not enough bytes to parse an elf program header"));
        int initialOffset = offset;

        type = Type.of(Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding));
        offset += 4;
        this.offset = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        vaddr = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        paddr = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        filesz = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        memsz = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        flags = Flag.of(Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding));
        offset += 4;
        align = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;

        int bytesRead = offset - initialOffset;
        Lib.assertTrue(bytesRead == SIZE, new ElfLoadingException(
                "Cannot parse an elf program header. Size mismatch: expected=" + SIZE + ", actual=" + bytesRead));

        return offset;
    }

    @Override
    public String toString() {
        return "ElfProgramHeader{" +
                "\n\t\t\ttype=" + type +
                "\n\t\t\toffset=" + offset +
                "\n\t\t\tvaddr=" + vaddr +
                "\n\t\t\tpaddr=" + paddr +
                "\n\t\t\tfilesz=" + filesz +
                "\n\t\t\tmemsz=" + memsz +
                "\n\t\t\tflags=" + flags +
                "\n\t\t\talign=" + align +
                "\n\t\t}";
    }

    enum Type {
        Null(0), Load(1), Dynamic(2), Interp(3), Note(4), Shlib(5), Phdr(6);

        private static final long reservedLowerBound = 7;
        private static final long reservedUpperBound = 0x6fffffff;
        private static final long processorSpecificLowerBound = 0x70000000;
        private static final long processorSpecificUpperBound = 0x7fffffff;
        private static final Map<Long, Type> reverseMap;

        static {
            reverseMap = new HashMap<>();
            for ( Type type : Type.values() ) {
                reverseMap.put(type.value, type);
            }
        }

        final long value;

        Type(long value) {
            this.value = value;
        }

        static Type of(long value) throws ElfLoadingException {
            if ( value >= reservedLowerBound && value <= reservedUpperBound ) {
                Lib.debug(Elf.dbgElf, "Elf program header field p_type value is from reserved range");
                return null;
            }
            else if ( value >= processorSpecificLowerBound && value <= processorSpecificUpperBound ) {
                Lib.debug(Elf.dbgElf, "Elf program header field p_type value is processor specific");
                return null;
            }
            else {
                Type type = reverseMap.get(value);
                Lib.assertTrue(type != null, new ElfLoadingException("Error parsing elf program header p_type field"));
                return type;
            }
        }
    }

    enum Flag {
        X(1), W(2), R(4), Maskproc(0xf0000000);

        final long value;

        Flag(long value) {
            this.value = value;
        }

        static Set<Flag> of(long value) {
            Set<Flag> flags = new HashSet<>();
            for ( Flag flag : Flag.values() ) {
                if ( (value & flag.value) == flag.value ) {
                    flags.add(flag);
                }
            }
            return flags;
        }
    }
}
