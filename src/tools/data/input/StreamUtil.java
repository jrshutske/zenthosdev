package tools.data.input;

import java.awt.Point;
import tools.data.output.LittleEndianWriter;

public class StreamUtil {
    public static Point readShortPoint(LittleEndianAccessor lea) {
        int x = lea.readShort();
        int y = lea.readShort();
        return new Point(x, y);
    }

    public static void writeShortPoint(LittleEndianWriter lew, Point p) {
        lew.writeShort(p.x);
        lew.writeShort(p.y);
    }
}