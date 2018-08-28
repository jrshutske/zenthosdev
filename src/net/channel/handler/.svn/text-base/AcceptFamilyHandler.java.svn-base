package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.channel.ChannelServer;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class AcceptFamilyHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        System.out.println(slea.toString());
        int inviterId = slea.readInt();
//        String inviterName = slea.readMapleAsciiString();
//        MapleCharacter inviter = ChannelServer.getCharacterFromAllServers(inviterId);
//        if (inviter != null) {
//            inviter.getClient().getSession().write(MaplePacketCreator.sendFamilyJoinResponse(true, c.getPlayer().getName()));
//        }
//        c.getSession().write(MaplePacketCreator.sendFamilyMessage());
    }
}
