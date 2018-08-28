package net.channel.handler;

//import client.MapleCharacter;
import client.MapleClient;
//import client.command.CommandProcessor;
import net.AbstractMaplePacketHandler;
//import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class SpouseChatHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        System.out.println(slea.toString());
//        slea.readMapleAsciiString();//recipient
//        String msg = slea.readMapleAsciiString();
//        if (!CommandProcessor.processCommand(c, msg))
//            if (c.getPlayer().isMarried()) {
//                MapleCharacter wife = c.getChannelServer().getPlayerStorage().getCharacterById(c.getPlayer().getPartnerId());
//                if (wife != null) {
//                    wife.getClient().getSession().write(MaplePacketCreator.sendSpouseChat(c.getPlayer(), msg));
//                    c.getSession().write(MaplePacketCreator.sendSpouseChat(c.getPlayer(), msg));
//                } else
//                    try {
//                        if (c.getChannelServer().getWorldInterface().isConnected(wife.getName())) {
//                            c.getChannelServer().getWorldInterface().sendSpouseChat(c.getPlayer().getName(), wife.getName(), msg);
//                            c.getSession().write(MaplePacketCreator.sendSpouseChat(c.getPlayer(), msg));
//                        } else
//                            c.getPlayer().message("You are either not married or your spouse is currently offline.");
//                    } catch (Exception e) {
//                        c.getPlayer().message("You are either not married or your spouse is currently offline.");
//                        c.getChannelServer().reconnectWorld();
//                    }
//            }
    }
}
