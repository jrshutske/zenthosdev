package net.channel.handler;

import client.MapleCharacter;
import net.channel.ChannelServer;
import net.world.MaplePartyCharacter;
import client.MapleClient;
import client.MaplePet;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class ItemPickupHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        @SuppressWarnings("unused")
        byte mode = slea.readByte();
        slea.readInt();
        slea.readInt();
        int oid = slea.readInt();
        MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
        if (ob == null) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            c.getSession().write(MaplePacketCreator.getShowInventoryFull());
            return;
        }
        if (ob instanceof MapleMapItem) {
            MapleMapItem mapitem = (MapleMapItem) ob;
            synchronized (mapitem) {
                if (mapitem.isPickedUp()) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                double distance = c.getPlayer().getPosition().distanceSq(mapitem.getPosition());
                if (mapitem.getMeso() > 0) {
                    if (c.getPlayer().getParty() != null) {
                        ChannelServer cserv = c.getChannelServer();
                        int mesosamm = mapitem.getMeso();
                        int partynum = 0;
                        for (MaplePartyCharacter partymem : c.getPlayer().getParty().getMembers()) {
                            if (partymem.isOnline() && partymem.getMapid() == c.getPlayer().getMap().getId() && partymem.getChannel() == c.getChannel()) {
                                partynum++;
                            }
                        }
                        int mesosgain = mesosamm / partynum;
                        for (MaplePartyCharacter partymem : c.getPlayer().getParty().getMembers()) {
                            if (partymem.isOnline() && partymem.getMapid() == c.getPlayer().getMap().getId()) {
                                MapleCharacter somecharacter = cserv.getPlayerStorage().getCharacterById(partymem.getId());
                                if (somecharacter != null) {
                                    somecharacter.gainMeso(mesosgain, true, true);
                                }
                            }
                        }
                    } else {
                        c.getPlayer().gainMeso(mapitem.getMeso(), true, true);
                    }
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                    c.getPlayer().getMap().removeMapObject(ob);
                } else {
                    if (mapitem.getItem().getItemId() >= 5000000 && mapitem.getItem().getItemId() <= 5000100) {
                        int petId = MaplePet.createPet(mapitem.getItem().getItemId());
                        if (petId == -1) {
                            return;
                        }
                        MapleInventoryManipulator.addById(c, mapitem.getItem().getItemId(), mapitem.getItem().getQuantity(), null, petId);
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
                            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
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