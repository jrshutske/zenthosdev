package net.channel.handler;

import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.maps.FakeCharacter;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class UseChairHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int itemId = slea.readInt();
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.SETUP).findById(itemId);
        if (toUse.getItemId() != itemId || toUse == null || toUse.getQuantity() < 1) {
            return;
        }
        c.getPlayer().setChair(itemId);
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showChair(c.getPlayer().getId(), itemId), false);
        c.getSession().write(MaplePacketCreator.enableActions());
        for (FakeCharacter ch : c.getPlayer().getFakeChars()) {
            ch.getFakeChar().setChair(itemId);
            ch.getFakeChar().getMap().broadcastMessage(ch.getFakeChar(), MaplePacketCreator.showChair(ch.getFakeChar().getId(), itemId), false);
        }
    }
}