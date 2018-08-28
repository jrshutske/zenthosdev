package tools.data.output;

import java.nio.charset.Charset;

public class GenericLittleEndianWriter implements LittleEndianWriter {
    private static Charset ASCII = Charset.forName("US-ASCII");
    private ByteOutputStream bos;

    protected GenericLittleEndianWriter() {
    }

    protected void setByteOutputStream(ByteOutputStream bos) {
        this.bos = bos;
    }

    public GenericLittleEndianWriter(ByteOutputStream bos) {
        this.bos = bos;
    }

    @Override
    public void write(byte[] b) {
        for (int x = 0; x < b.length; x++) {
            bos.writeByte(b[x]);
        }
    }

    @Override
    public void write(byte b) {
        bos.writeByte(b);
    }

    @Override
    public void write(int b) {
        bos.writeByte((byte) b);
    }

    public void write0(int times) {
        for(int i = 0; i < times; i++)
        bos.writeByte((byte) 0);
    }

    @Override
    public void writeShort(int i) {
        bos.writeByte((byte) (i & 0xFF));
        bos.writeByte((byte) ((i >>> 8) & 0xFF));
    }

    @Override
    public void writeInt(int i) {
        bos.writeByte((byte) (i & 0xFF));
        bos.writeByte((byte) ((i >>> 8) & 0xFF));
        bos.writeByte((byte) ((i >>> 16) & 0xFF));
        bos.writeByte((byte) ((i >>> 24) & 0xFF));
    }

    @Override
    public void writeAsciiString(String s) {
        write(s.getBytes(ASCII));
    }

    @Override
    public void writeMapleAsciiString(String s) {
        writeShort((short) s.length());
        writeAsciiString(s);
    }

    @Override
    public void writeNullTerminatedAsciiString(String s) {
        writeAsciiString(s);
        write(0);
    }

    @Override
    public void writeLong(long l) {
        bos.writeByte((byte) (l & 0xFF));
        bos.writeByte((byte) ((l >>> 8) & 0xFF));
        bos.writeByte((byte) ((l >>> 16) & 0xFF));
        bos.writeByte((byte) ((l >>> 24) & 0xFF));
        bos.writeByte((byte) ((l >>> 32) & 0xFF));
        bos.writeByte((byte) ((l >>> 40) & 0xFF));
        bos.writeByte((byte) ((l >>> 48) & 0xFF));
        bos.writeByte((byte) ((l >>> 56) & 0xFF));
    }
}