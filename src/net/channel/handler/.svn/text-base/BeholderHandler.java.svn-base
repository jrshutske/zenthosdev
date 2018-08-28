package net.channel.handler;

import client.MapleClient;
import constants.skills.DarkKnight;
import java.util.Collection;
import net.AbstractMaplePacketHandler;
import server.maps.MapleSummon;
import tools.data.input.SeekableLittleEndianAccessor;

public final class BeholderHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        System.out.println(slea.toString());
        Collection<MapleSummon> summons = c.getPlayer().getSummons().values();
        int oid = slea.readInt();
        MapleSummon summon = null;
        for (MapleSummon sum : summons) {
            if (sum.getObjectId() == oid) {
                summon = sum;
            }
        }
        if (summon != null) {
            int skillId = slea.readInt();
            if (skillId == DarkKnight.AURA_OF_BEHOLDER) {
                slea.readShort();
            } else if (skillId == DarkKnight.HEX_OF_BEHOLDER) {
                slea.readByte();
            }
        } else {
            c.getPlayer().getSummons().clear();
        }
    }
}