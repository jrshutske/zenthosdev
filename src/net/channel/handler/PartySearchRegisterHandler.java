package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public class PartySearchRegisterHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        int min = slea.readInt();
        int max = slea.readInt();
        if (chr.getLevel() < min || chr.getLevel() > max || (max - min) > 30 || min > max) {
            return;
        }
    }
}