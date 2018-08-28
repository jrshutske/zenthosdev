package net.channel.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import client.MapleCharacter;
import client.MapleClient;
import database.DatabaseConnection;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public class VIPAddMapHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        Connection con = DatabaseConnection.getConnection();
        int operation = slea.readByte();
        int type = slea.readByte();
        MapleCharacter player = c.getPlayer();
        switch (operation) {
            case 0:
                int mapid = slea.readInt();
                try {
                    PreparedStatement ps = con.prepareStatement("DELETE FROM VIPRockMaps WHERE cid = ? AND mapid = ? AND type = ?");
                    ps.setInt(1, player.getId());
                    ps.setInt(2, mapid);
                    ps.setInt(3, type);
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException sqle) {
                }
                break;
            case 1:
                try {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO VIPRockMaps (`cid`, `mapid`, `type`) VALUES (?, ?, ?)");
                    ps.setInt(1, player.getId());
                    ps.setInt(2, player.getMapId());
                    ps.setInt(3, type);
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException sqle) {
                }
                break;
            default:
                System.out.println("Unhandled VIP Rock operation: " + slea.toString());
                break;
        }
//        c.getSession().write(MaplePacketCreator.refreshVIPRockMapList(player.getVIPRockMaps(type), type));
    }
}