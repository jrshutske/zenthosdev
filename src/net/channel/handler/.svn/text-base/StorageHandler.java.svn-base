package net.channel.handler;

import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import constants.InventoryConstants;
import net.AbstractMaplePacketHandler;
import server.AntiCheats;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStorage;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class StorageHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte mode = slea.readByte();
        final MapleStorage storage = c.getPlayer().getStorage();
        if (mode == 4) {
            byte type = slea.readByte();
            byte slot = slea.readByte();
            slot = storage.getSlot(MapleInventoryType.getByType(type), slot);
            IItem item = storage.takeOut(slot);
            if (item != null) {
                if (MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                    MapleInventoryManipulator.addFromDrop(c, item, false);
                } else {
                    storage.store(item);
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "Your inventory is full"));
                }
                storage.sendTakenOut(c, ii.getInventoryType(item.getItemId()));
            } else {
                AntiCheats.autoBan(c, "Trying to take out item from storage which does not exist");
                return;
            }
        } else if (mode == 5) {
            byte slot = (byte) slea.readShort();
            int itemId = slea.readInt();
            short quantity = slea.readShort();
            if (quantity < 1) {
                AntiCheats.autoBan(c, "Trying to store " + quantity + " of " + itemId);
                return;
            }
            if (storage.isFull()) {
                c.getSession().write(MaplePacketCreator.getStorageFull());
                return;
            }
            if (c.getPlayer().getMeso() < 100) {
                c.getSession().write(MaplePacketCreator.serverNotice(1, "You don't have enough mesos to store the item"));
            } else {
                MapleInventoryType type = ii.getInventoryType(itemId);
                IItem item = c.getPlayer().getInventory(type).getItem(slot).copy();
                if (item.getItemId() == itemId && (item.getQuantity() >= quantity || InventoryConstants.isRechargable(itemId))) {
                    if (InventoryConstants.isRechargable(itemId)) {
                        quantity = item.getQuantity();
                    }
                    c.getPlayer().gainMeso(-100, false, true, false);
                    MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                    item.setQuantity(quantity);
                    storage.store(item);
                } else {
                    return;
                }
            }
            storage.sendStored(c, ii.getInventoryType(itemId));
        } else if (mode == 6) {
            c.getPlayer().dropMessage(1, "Sorry, storage sorting is unavailable.");
        } else if (mode == 7) {
            int meso = slea.readInt();
            int storageMesos = storage.getMeso();
            int playerMesos = c.getPlayer().getMeso();
            if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                if (meso < 0 && (storageMesos - meso) < 0) {
                    meso = -(Integer.MAX_VALUE - storageMesos);
                    if ((-meso) > playerMesos) {
                        throw new RuntimeException("Different amount of mesos in storage.");
                    }
                } else if (meso > 0 && (playerMesos + meso) < 0) {
                    meso = (Integer.MAX_VALUE - playerMesos);
                    if ((meso) > storageMesos) {
                        throw new RuntimeException("Different amount of mesos in storage.");
                    }
                }
                storage.setMeso(storageMesos - meso);
                c.getPlayer().gainMeso(meso, false, true, false);
            } else {
                return;
            }
            storage.sendMeso(c);
        } else if (mode == 8) {
            storage.close();
        }
    }
}