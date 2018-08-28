package net.channel.handler;

import client.IItem;
import client.Item;
import client.MapleClient;
import client.MapleInventory;
import client.MapleInventoryType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class ItemIdSortHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        byte mode = slea.readByte();
        if (mode < 0 || mode > 5) {
            return;
        }
        MapleInventory Inv = c.getPlayer().getInventory(MapleInventoryType.getByType(mode));
        ArrayList<Item> itemarray = new ArrayList<Item>();
        for (Iterator<IItem> it = Inv.iterator(); it.hasNext();) {
            Item item = (Item) it.next();
            itemarray.add((Item) (item.copy()));
        }
        Collections.sort(itemarray);
        for (IItem item : itemarray) {
            MapleInventoryManipulator.removeById(c, MapleInventoryType.getByType(mode), item.getItemId(), item.getQuantity(), false, false);
        }
        for (Item i : itemarray) {
            MapleInventoryManipulator.addFromDrop(c, i, false);
        }
        c.getSession().write(MaplePacketCreator.finishedSort2(mode));
    }
}