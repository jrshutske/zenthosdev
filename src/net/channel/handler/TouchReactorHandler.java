package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import scripting.reactor.ReactorScriptManager;
import server.maps.MapleReactor;
import tools.data.input.SeekableLittleEndianAccessor;

public final class TouchReactorHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (reactor != null) {
            if (slea.readByte() != 0) {
//                ReactorScriptManager.getInstance().touch(c, reactor);
            } else {
//                ReactorScriptManager.getInstance().untouch(c, reactor);
            }
        }
    }
}
