package tools.data.output;

public interface LittleEndianWriter {
    public void write(byte b[]);
    public void write(byte b);
    public void write(int b);
    public void writeInt(int i);
    public void writeShort(int s);
    public void writeLong(long l);
    void writeAsciiString(String s);
    void writeNullTerminatedAsciiString(String s);
    void writeMapleAsciiString(String s);
}