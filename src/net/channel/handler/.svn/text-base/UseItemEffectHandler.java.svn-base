package net.channel.handler;

import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.maps.FakeCharacter;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class UseItemEffectHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int itemId = slea.readInt();
        if (itemId >= 5000000 && itemId <= 5000053) {
            System.out.println(slea.toString());
        }
        if (itemId != 0) {
            IItem toUse = c.getPlayer().getInventory(MapleInventoryType.CASH).findById(itemId);
            if (toUse == null) {
                return;
            }
        }
        c.getPlayer().setItemEffect(itemId);
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.itemEffect(c.getPlayer().getId(), itemId), false);
        if (c.getPlayer().hasFakeChar()) {
            for (FakeCharacter ch : c.getPlayer().getFakeChars()) {
                ch.getFakeChar().setItemEffect(itemId);
                c.getPlayer().getMap().broadcastMessage(ch.getFakeChar(), MaplePacketCreator.itemEffect(c.getPlayer().getId(), itemId), false);
            }
        }
    }
}