// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine.elf;

import nachos.machine.io.OpenFile;
import nachos.machine.lib.Lib;

import java.nio.ByteOrder;

public class Elf {
    static final char dbgElf = 'e';

    ElfHeader header;
    OpenFile file;
    private ElfSectionHeader strTabSectionHeader;
    private ElfSectionHeader[] sectionHeaders;
    private ElfProgramHeader[] programHeaders;

    public Elf(OpenFile file) throws ElfLoadingException {
        this.file = file;

        parseAndValidateHeader();
        parseAndValidateSectionHeaders();
        parseAndValidateProgramHeaders();
    }

    @Override
    public String toString() {
        return "ElfObject{\n" +
                "\tHeader=" + header +
                "\n\n\tSectionHeaders=" + arrayToFormattedString(sectionHeaders) +
                "\n\n\tProgramHeaders=" + arrayToFormattedString(programHeaders) +
                "\n}";
    }

    private String arrayToFormattedString(Object[] objects) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int i;
        for ( i = 0; i < objects.length - 1; i++ ) {
            sb.append("\n\t\t").append(i).append(": ").append(objects[i].toString()).append(',');
        }
        sb.append("\n\t\t").append(i).append(": ").append(objects[i].toString()).append("\n\t]");
        return sb.toString();
    }

    private void parseAndValidateHeader() throws ElfLoadingException {
        byte[] headerBytes = new byte[ElfHeader.SIZE];
        Lib.strictReadFile(file, 0, headerBytes, 0, ElfHeader.SIZE);
        header = new ElfHeader(this);

        int offset = 0;
        offset = header.load(offset, headerBytes);
        Lib.assertTrue(offset == ElfHeader.SIZE, new ElfLoadingException(
                "Cannot parse elf object header. Size mismatch: expected=" + ElfHeader.SIZE + ", actual=" + offset));

        // Elf class validation.
        Lib.assertTrue(header.ident.fileClass == ElfHeader.Ident.FileClass.Class32,
                new ElfLoadingException("Unsupported file class " + header.ident.fileClass));

        // Endianness validation.
        Lib.assertTrue(header.ident.dataEncoding == ByteOrder.LITTLE_ENDIAN,
                new ElfLoadingException("Unsupported data encoding " + header.ident.dataEncoding));
    }

    private void parseAndValidateSectionHeaders() throws ElfLoadingException {
        // Assume that null section does not exist.
        sectionHeaders = new ElfSectionHeader[header.shnum - 1];
        int position = (int) header.shoff + header.shentsize;

        // Load each section header.
        byte[] sectionHeaderAndDataBytes = new byte[header.shentsize];
        for ( int i = 0; i < sectionHeaders.length; i++ ) {
            Lib.strictReadFile(file, position, sectionHeaderAndDataBytes, 0, header.shentsize);
            sectionHeaders[i] = new ElfSectionHeader(this);

            int offset = 0;
            offset = sectionHeaders[i].load(offset, sectionHeaderAndDataBytes);
            Lib.assertTrue(offset == ElfSectionHeader.SIZE, new ElfLoadingException(
                    "Cannot parse an elf section header. Size mismatch: "
                            + "expected=" + ElfSectionHeader.SIZE + " actual=" + offset));

            if ( sectionHeaders[i].type == ElfSectionHeader.Type.Strtab ) {
                strTabSectionHeader = sectionHeaders[i];
            }

            position += header.shentsize;
        }

        // Infer section header names from strTabSection.
        for ( ElfSectionHeader sectionHeader : sectionHeaders ) {
            if ( strTabSectionHeader != null ) {
                // What is the max size of the ELF section header? Will use 64 bytes for now
                byte[] sectionNameBytes = new byte[64];
                int pos = (int) (strTabSectionHeader.offset + sectionHeader.nameIndex);
                file.read(pos, sectionNameBytes, 0, sectionNameBytes.length);

                String name = new String(sectionNameBytes);
                int endIndex = name.indexOf('\0');
                if ( endIndex != -1 ) {
                    sectionHeader.name = name.substring(0, endIndex);
                }
                else {
                    sectionHeader.name = name;
                }
            }
            else {
                sectionHeader.name = "(strTab not loaded)";
            }
        }
    }

    private void parseAndValidateProgramHeaders() throws ElfLoadingException {
        programHeaders = new ElfProgramHeader[header.phnum];
        int position = (int) header.phoff;

        byte[] programHeaderAndDataBytes = new byte[header.phentsize];
        for ( int i = 0; i < programHeaders.length; i++ ) {
            Lib.strictReadFile(file, position, programHeaderAndDataBytes, 0, header.phentsize);
            programHeaders[i] = new ElfProgramHeader(this);

            int offset = 0;
            offset = programHeaders[i].load(offset, programHeaderAndDataBytes);
            Lib.assertTrue(offset == ElfProgramHeader.SIZE, new ElfLoadingException(
                    "Cannot parse an elf program header. Size mismatch: " +
                            "expected=" + ElfProgramHeader.SIZE + ", actual=" + offset));

            position += header.phentsize;
        }
    }

    public int getNumSections() {
        return sectionHeaders.length;
    }

    public ElfSectionHeader getSection(int index) {
        return sectionHeaders[index];
    }

    public void close() {
        file.close();
        file = null;
        header = null;
        sectionHeaders = null;
        programHeaders = null;
    }

    public int getEntryPoint() {
        return (int) header.entry;
    }
}
