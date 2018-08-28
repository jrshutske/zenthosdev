package net.channel.handler;

import client.MapleClient;
import client.MaplePet;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleInventoryType;

public class PetLootHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getNoPets() == 0) {
            return;
        }
        MaplePet pet = c.getPlayer().getPet(c.getPlayer().getPetIndex(slea.readInt()));
        slea.skip(13);
        int oid = slea.readInt();
        MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
        if (ob == null || pet == null) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        if (ob instanceof MapleMapItem) {
            MapleMapItem mapitem = (MapleMapItem) ob;
            synchronized (mapitem) {
                if (mapitem.isPickedUp()) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    return;
                }
                double distance = pet.getPos().distanceSq(mapitem.getPosition());
                if (distance > 90000.0) {
                    return;
                }
                if (mapitem.getMeso() > 0) {
                    if (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).findById(1812000) != null) {
                        c.getPlayer().gainMeso(mapitem.getMeso(), true, true);
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, c.getPlayer().getPetIndex(pet)), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        mapitem.setPickedUp(false);
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                } else {
                    int itemId = mapitem.getItem().getItemId();
                    if (itemId >= 5000000 && itemId <= 5000100) {
                        if (MapleInventoryManipulator.addById(c, mapitem.getItem().getItemId(), mapitem.getItem().getQuantity(), null)) {
                            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, c.getPlayer().getPetIndex(pet)), mapitem.getPosition());
                            c.getPlayer().getMap().removeMapObject(ob);
                        } else {
                            return;
                        }
                    } else {
                        if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
                            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, c.getPlayer().getId(), true, c.getPlayer().getPetIndex(pet)), mapitem.getPosition());
                            c.getPlayer().getMap().removeMapObject(ob);
                        } else {
                            return;
                        }
                    }
                }
                mapitem.setPickedUp(true);
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}