package tools.data.input;

import java.io.IOException;

public class ByteArrayByteStream implements SeekableInputStreamBytestream {
    private int pos = 0;
    private long bytesRead = 0;
    private byte[] arr;

    public ByteArrayByteStream(byte[] arr) {
        this.arr = arr;
    }

    @Override
    public long getPosition() {
        return pos;
    }

    @Override
    public void seek(long offset) throws IOException {
        pos = (int) offset;
    }

    @Override
    public long getBytesRead() {
        return bytesRead;
    }

    @Override
    public int readByte() {
        bytesRead++;
        return ((int) arr[pos++]) & 0xFF;
    }

    @Override
    public long available() {
        return arr.length - pos;
    }
}