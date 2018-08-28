package net.channel.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import client.MapleClient;
import database.DatabaseConnection;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class TrockAddMapHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        Connection con = DatabaseConnection.getConnection();
        byte addrem;
        addrem = slea.readByte();
        byte rocktype = slea.readByte();
        if (addrem == 0x00) {
            int mapId = slea.readInt();
            try {
                PreparedStatement ps = con.prepareStatement("DELETE FROM trocklocations WHERE characterid = ? AND mapid = ?");
                ps.setInt(1, c.getPlayer().getId());
                ps.setInt(2, mapId);
                ps.executeUpdate();
                ps.close();
            } catch (Exception e) {
            }
        } else if (addrem == 0x01) {
//            if (FieldLimit.CANNOTVIPROCK.check(c.getPlayer().getMap().getFieldLimit())) {
//                try {
//                    PreparedStatement ps = con.prepareStatement("INSERT into trocklocations (characterid, mapid) VALUES (?, ?)");
//                    ps.setInt(1, c.getPlayer().getId());
//                    ps.setInt(2, c.getPlayer().getMapId());
//                    ps.executeUpdate();
//                    ps.close();
//                } catch (Exception e) {
//                }
//            } else {
                c.getPlayer().dropMessage("You may not save this map.");
//            }
        }
        c.getSession().write(MaplePacketCreator.trockRefreshMapList(c.getPlayer().getId(), rocktype));
    }
}
