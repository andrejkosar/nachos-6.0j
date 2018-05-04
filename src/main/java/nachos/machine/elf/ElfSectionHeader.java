// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.elf;

import nachos.machine.Machine;
import nachos.machine.lib.Lib;
import nachos.machine.processor.Processor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ElfSectionHeader {
    final static int SIZE = 40;

    final Elf elf;

    long nameIndex;
    String name;
    Type type;
    Set<Flag> flags;
    long addr;
    long offset;
    long size;
    long link;
    long info;
    long addralign;
    long entsize;

    private int numPages;
    private int firstVPN;

    public ElfSectionHeader(Elf elf) {
        this.elf = elf;
    }

    int load(int offset, byte[] bytes) throws ElfLoadingException {
        Lib.assertTrue(offset + SIZE <= bytes.length,
                new ElfLoadingException("There is not enough bytes to parse an elf section header"));

        nameIndex = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        type = Type.of(Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding));
        offset += 4;
        flags = Flag.of(Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding));
        offset += 4;
        addr = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        this.offset = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        size = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        link = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        info = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        addralign = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;
        entsize = Lib.bytesToUnsignedInt(bytes, offset, elf.header.ident.dataEncoding);
        offset += 4;

        if ( flags.contains(Flag.Alloc) ) {
            numPages = Lib.divRoundUp((int) size, Processor.pageSize);
            firstVPN = (int) addr / Processor.pageSize;
        }
        else {
            numPages = 0;
            firstVPN = 0;
        }

        return offset;
    }

    public int getNumPages() {
        return numPages;
    }

    public boolean isLoadable() {
        return flags.contains(Flag.Alloc);
    }

    public int getFirstVPN() {
        return firstVPN;
    }

    @Override
    public String toString() {
        return "ElfSectionHeader{" +
                "\n\t\t\tname = " + name +
                "\n\t\t\ttype = " + type +
                "\n\t\t\tflags = " + flags +
                "\n\t\t\taddr = " + addr +
                "\n\t\t\toffset = " + offset +
                "\n\t\t\tsize = " + size +
                "\n\t\t\tlink = " + link +
                "\n\t\t\tinfo = " + info +
                "\n\t\t\taddralign = " + addralign +
                "\n\t\t\tentsize = " + entsize +
                "\n\t\t}";
    }

    public String getName() {
        return name;
    }

    public boolean isReadOnly() {
        return !flags.contains(Flag.Write);
    }

    public void loadPage(int spn, int ppn) {
        Lib.assertTrue(elf.file != null);

        Lib.assertTrue(spn >= 0 && spn < numPages);
        Lib.assertTrue(ppn >= 0 && ppn < Machine.processor().getNumPhysPages());

        int pageSize = Processor.pageSize;
        byte[] memory = Machine.processor().getMemory();
        int paddr = ppn * pageSize;
        int faddr = (int) offset + spn * pageSize;
        int initlen;

        if ( type == Type.Nobits ) {
            initlen = 0;
        }
        else if ( spn == numPages - 1 ) {
            /*
             * initlen = size % pageSize;
             * Bug identified by Steven Schlansker 3/20/08
             * Bug fix by Michael Rauser
             */
            initlen = (int) ((size == pageSize) ? pageSize : (size % pageSize));
        }
        else {
            initlen = pageSize;
        }

        if ( initlen > 0 ) {
            Lib.strictReadFile(elf.file, faddr, memory, paddr, initlen);
        }

        Arrays.fill(memory, paddr + initlen, paddr + pageSize, (byte) 0);
    }

    enum Flag {
        Write(1), Alloc(2), Execinstr(4), Maskproc(0xf0000000);

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

    enum Type {
        Null(0), Progbits(1), Symtab(2), Strtab(3), Rela(4), Hash(5),
        Dynamic(6), Note(7), Nobits(8), Rel(9), Shlib(10), Dynsyn(11);

        private static final long reservedLowerBound = 12;
        private static final long reservedUpperBound = 0x6fffffff;
        private static final long processorSpecificLowerBound = 0x70000000;
        private static final long processorSpecificUpperBound = 0x7fffffff;
        private static final long applicationReservedLowerBound = 0x80000000;
        private static final long applicationReservedUpperBound = 0xffffffff;
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
                Lib.debug(Elf.dbgElf, "Elf section header field sh_type value is from reserved range");
                return null;
            }
            else if ( value >= processorSpecificLowerBound && value <= processorSpecificUpperBound ) {
                Lib.debug(Elf.dbgElf, "Elf section header field sh_type value is processor specific");
                return null;
            }
            else if ( value >= applicationReservedLowerBound && value <= applicationReservedUpperBound ) {
                Lib.debug(Elf.dbgElf, "Elf section header field sh_type value is reserved for application programs");
                return null;
            }
            else {
                Type type = reverseMap.get(value);
                Lib.assertTrue(type != null, new ElfLoadingException("Error parsing elf section header sh_type field"));
                return type;
            }
        }
    }
}
