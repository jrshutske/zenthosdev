package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class FamilyAddHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        System.out.println(slea.toString());
        String toAdd = slea.readMapleAsciiString();
        MapleCharacter addChr = c.getChannelServer().getPlayerStorage().getCharacterByName(toAdd);
        if (addChr != null) {
            addChr.getClient().getSession().write(MaplePacketCreator.sendFamilyInvite(c.getPlayer().getId(), toAdd));
            c.getPlayer().dropMessage("The invite has been sent.");
        } else {
            c.getPlayer().dropMessage("The player cannot be found!");
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}

