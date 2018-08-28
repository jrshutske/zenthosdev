package tools.data.output;

import org.apache.mina.common.ByteBuffer;

public class ByteBufferLittleEndianWriter extends GenericLittleEndianWriter {
    private ByteBuffer bb;

    public ByteBufferLittleEndianWriter() {
        this(50, true);
    }

    public ByteBufferLittleEndianWriter(int size) {
        this(size, false);
    }

    public ByteBufferLittleEndianWriter(int initialSize, boolean autoExpand) {
        bb = ByteBuffer.allocate(initialSize);
        bb.setAutoExpand(autoExpand);
        setByteOutputStream(new ByteBufferOutputstream(bb));
    }

    public ByteBuffer getFlippedBB() {
        return bb.flip();
    }

    public ByteBuffer getByteBuffer() {
        return bb;
    }
}