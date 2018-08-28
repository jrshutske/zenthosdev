package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import server.maps.MapleReactor;
import tools.data.input.SeekableLittleEndianAccessor;

public class ReactorHitHandler extends AbstractMaplePacketHandler {

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        int charPos = slea.readInt();
        short stance = slea.readShort();
        MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (reactor != null && reactor.isAlive()) {
            reactor.hitReactor(charPos, stance, c);
        } else {
            return;
        }
    }
}