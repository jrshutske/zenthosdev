package tools.data.output;

import java.io.ByteArrayOutputStream;

public class BAOSByteOutputStream implements ByteOutputStream {
    private ByteArrayOutputStream baos;

    public BAOSByteOutputStream(ByteArrayOutputStream baos) {
        super();
        this.baos = baos;
    }

    @Override
    public void writeByte(byte b) {
        baos.write(b);
    }
}