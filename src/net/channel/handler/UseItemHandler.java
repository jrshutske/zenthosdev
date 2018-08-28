package net.channel.handler;

import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import scripting.npc.NPCScriptManager;
import tools.data.input.SeekableLittleEndianAccessor;

public class UseItemHandler extends AbstractMaplePacketHandler {

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (itemId == 2022118) {
            c.getPlayer().dropMessage(1, "Please keep this item for scrolling purposes. Gives you 100% scroll rate.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        } else if (itemId == 2022065) {
            NPCScriptManager.getInstance().start(c, 9010000, "JobChanger", null);
        } else if (toUse != null && toUse.getQuantity() > 0) {
            if (toUse.getItemId() != itemId) {
                return;
            }
            if (itemId >= 2030000 && itemId < 2030020) {
                if (ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer())) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                }
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }

            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer());
        }
    }
}