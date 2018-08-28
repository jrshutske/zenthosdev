package provider.wz;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import tools.data.input.GenericLittleEndianAccessor;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.InputStreamByteStream;
import tools.data.input.LittleEndianAccessor;
import tools.data.input.RandomAccessByteStream;
import tools.data.input.SeekableLittleEndianAccessor;

public class WZFile implements MapleDataProvider {
    private File wzfile;
    private LittleEndianAccessor lea;
    private SeekableLittleEndianAccessor slea;
    private int headerSize;
    private WZDirectoryEntry root;
    private boolean provideImages;
    private int cOffset;

    static {
        ListWZFile.init();
    }

    public WZFile(File wzfile, boolean provideImages) throws IOException {
        this.wzfile = wzfile;
        lea = new GenericLittleEndianAccessor(new InputStreamByteStream(new BufferedInputStream(new FileInputStream(wzfile))));
        RandomAccessFile raf = new RandomAccessFile(wzfile, "r");
        slea = new GenericSeekableLittleEndianAccessor(new RandomAccessByteStream(raf));
        root = new WZDirectoryEntry(wzfile.getName(), 0, 0, null);
        this.provideImages = provideImages;
        load();
    }

    @SuppressWarnings("unused")
    private void load() throws IOException {
        String sPKG = lea.readAsciiString(4);
        int size1 = lea.readInt();
        int size2 = lea.readInt();
        headerSize = lea.readInt();
        String copyright = lea.readNullTerminatedAsciiString();
        short version = lea.readShort();
        parseDirectory(root);
        cOffset = (int) lea.getBytesRead();
        getOffsets(root);
    }

    private void getOffsets(MapleDataDirectoryEntry dir) {
        for (MapleDataFileEntry file : dir.getFiles()) {
            file.setOffset(cOffset);
            cOffset += file.getSize();
        }
        for (MapleDataDirectoryEntry sdir : dir.getSubdirectories()) {
            getOffsets(sdir);
        }
    }

    private void parseDirectory(WZDirectoryEntry dir) {
        int entries = WZTool.readValue(lea);
        for (int i = 0; i < entries; i++) {
            byte marker = lea.readByte();
            String name = null;
            @SuppressWarnings("unused")
            int dummyInt;
            int size, checksum;
            switch (marker) {
                case 0x02:
                    name = WZTool.readDecodedStringAtOffset(slea, lea.readInt() + this.headerSize + 1,true);
                    size = WZTool.readValue(lea);
                    checksum = WZTool.readValue(lea);
                    dummyInt = lea.readInt();
                    dir.addFile(new WZFileEntry(name, size, checksum, dir));
                    break;
                case 0x03:
                case 0x04:
                    name = WZTool.readDecodedString(lea);
                    size = WZTool.readValue(lea);
                    checksum = WZTool.readValue(lea);
                    dummyInt = lea.readInt();
                    if (marker == 3) {
                        dir.addDirectory(new WZDirectoryEntry(name, size, checksum, dir));
                    } else {
                        dir.addFile(new WZFileEntry(name, size, checksum, dir));
                    }
                    break;
                default:
                    System.out.println("Default case in marker (" + marker + ")");
            }
        }
        for (MapleDataDirectoryEntry idir : dir.getSubdirectories()) {
                parseDirectory((WZDirectoryEntry) idir);
        }
    }

    public WZIMGFile getImgFile(String path) throws IOException {
        String segments[] = path.split("/");
        WZDirectoryEntry dir = root;
        for (int x = 0; x < segments.length - 1; x++) {
            dir = (WZDirectoryEntry) dir.getEntry(segments[x]);
            if (dir == null) {
                return null;
            }
        }
        WZFileEntry entry = (WZFileEntry) dir.getEntry(segments[segments.length - 1]);
        if (entry == null) {
            return null;
        }
        String fullPath = wzfile.getName().substring(0, wzfile.getName().length() - 3).toLowerCase() + "/" + path;
        return new WZIMGFile(this.wzfile, entry, provideImages, ListWZFile.isModernImgFile(fullPath));
    }

    public synchronized MapleData getData(String path) {
        try {
            WZIMGFile imgFile = getImgFile(path);
            if (imgFile == null) {
                return null;
            }
            MapleData ret = imgFile.getRoot();
            return ret;
        } catch (IOException e) {
            System.out.println("THROW " + e);
        }
        return null;
    }

    public MapleDataDirectoryEntry getRoot() {
            return root;
    }
}