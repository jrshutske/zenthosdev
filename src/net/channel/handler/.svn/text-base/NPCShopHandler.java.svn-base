package net.channel.handler;

import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.MapleItemInformationProvider;
import tools.data.input.SeekableLittleEndianAccessor;

public class NPCShopHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte bmode = slea.readByte();
        if (bmode == 0) {
            slea.readShort();
            int itemId = slea.readInt();
            short quantity = slea.readShort();
            c.getPlayer().getShop().buy(c, itemId, quantity, c.getPlayer().getShop().getId() == 9000069 ? true : false);
        } else if (bmode == 1) {
            byte slot = (byte) slea.readShort();
            int itemId = slea.readInt();
            MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemId);
            short quantity = slea.readShort();
            c.getPlayer().getShop().sell(c, type, slot, quantity);
        } else if (bmode == 2) {
            byte slot = (byte) slea.readShort();
            c.getPlayer().getShop().recharge(c, slot);
        } else if (bmode == 3) {
            c.getPlayer().setShop(null);
        }
    }
}