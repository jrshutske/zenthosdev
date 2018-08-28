package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class UseDeathItemHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int itemId = slea.readInt();
        c.getPlayer().setItemEffect(itemId);
        c.getSession().write(MaplePacketCreator.itemEffect(c.getPlayer().getId(), itemId));
    }
}