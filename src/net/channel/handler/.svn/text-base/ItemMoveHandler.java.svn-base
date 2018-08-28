package net.channel.handler;

import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.AntiCheats;
import server.MapleInventoryManipulator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class ItemMoveHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
        byte src = (byte) slea.readShort();
        byte dst = (byte) slea.readShort();
        long checkq = slea.readShort();
        short quantity = (short)(int)checkq;
        if (src < 0 && dst > 0) {
            MapleInventoryManipulator.unequip(c, src, dst);
        } else if (dst < 0) {
            MapleInventoryManipulator.equip(c, src, dst);
        } else if (dst == 0) {
            if (c.getPlayer().getInventory(type).getItem(src) == null) return;
            if (checkq > 4000 || checkq < 1) {
                AntiCheats.autoBan(c, "Drop-dupe attemp item ("+c.getPlayer().getInventory(type).getItem(src).getItemId()+")");
                return;
            }
            MapleInventoryManipulator.drop(c, type, src, quantity);
        } else {
            MapleInventoryManipulator.move(c, type, src, dst);
        }
    }
}