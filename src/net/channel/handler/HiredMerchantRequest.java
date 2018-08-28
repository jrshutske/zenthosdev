package net.channel.handler;

import java.util.Arrays;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class HiredMerchantRequest extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 23000, Arrays.asList(MapleMapObjectType.HIRED_MERCHANT, MapleMapObjectType.SHOP)).isEmpty()) {
            if (!c.getPlayer().hasMerchant()) {
                c.getSession().write(MaplePacketCreator.hiredMerchantBox());
            } else {
                c.getPlayer().dropMessage(1, "You already have a store open, please go close that store first.");
            }
        } else {
            c.getPlayer().dropMessage(1, "You may not establish a store here.");
        }
    }
}