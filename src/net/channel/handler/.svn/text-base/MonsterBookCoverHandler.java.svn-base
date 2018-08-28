package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.MaplePacketCreator;

public final class MonsterBookCoverHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int id = slea.readInt();
        if (id == 0 || id / 10000 == 238) {
            c.getPlayer().setMonsterBookCover(id);
            c.getSession().write(MaplePacketCreator.changeCover(id));
        }
    }
}