package net.channel.handler;

import client.MapleClient;
import client.MapleInventory;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.MapleItemInformationProvider;
import server.maps.FakeCharacter;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class FaceExpressionHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int emote = slea.readInt();
        if (emote > 7) {
            int emoteid = 5159992 + emote;
            MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(emoteid);
            MapleInventory iv = c.getPlayer().getInventory(type);
            if (iv.findById(emoteid) == null) {
                return;
            }
        }
        for (FakeCharacter ch : c.getPlayer().getFakeChars()) {
            c.getPlayer().getMap().broadcastMessage(ch.getFakeChar(), MaplePacketCreator.facialExpression(ch.getFakeChar(), emote), false);
        }
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.facialExpression(c.getPlayer(), emote), false);
    }
}