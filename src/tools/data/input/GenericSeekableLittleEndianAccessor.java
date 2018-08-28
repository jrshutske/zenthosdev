package tools.data.input;

import java.io.IOException;

public class GenericSeekableLittleEndianAccessor extends GenericLittleEndianAccessor implements SeekableLittleEndianAccessor {
    private SeekableInputStreamBytestream bs;

    public GenericSeekableLittleEndianAccessor(SeekableInputStreamBytestream bs) {
        super(bs);
        this.bs = bs;
    }

    @Override
    public void seek(long offset) {
        try {
            bs.seek(offset);
        } catch (IOException e) {
            System.out.println("Seek failed " + e);
        }
    }

    @Override
    public long getPosition() {
        try {
            return bs.getPosition();
        } catch (IOException e) {
            System.out.println("getPosition failed " + e);
            return -1;
        }
    }

    @Override
    public void skip(int num) {
        seek(getPosition() + num);
    }
}