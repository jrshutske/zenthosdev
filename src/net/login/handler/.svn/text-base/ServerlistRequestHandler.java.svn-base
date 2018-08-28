package net.login.handler;

import client.MapleClient;
import constants.ServerProperties;
import net.AbstractMaplePacketHandler;
import net.login.LoginServer;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class ServerlistRequestHandler extends AbstractMaplePacketHandler {
    private static final String[] names = {"Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia", "Yellonde", "Demethos", "Elnido", "Kastia", "Judis", "Arkenia", "Plana", "Galicia", "Kalluna", "Stius", "Croa", "Zenith", "Medere"};

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        for (byte i = 0; i < ServerProperties.getWorldsActive; i++) {
            c.getSession().write(MaplePacketCreator.getServerList(i, names[i], LoginServer.getInstance().getLoad()));
        }
        c.getSession().write(MaplePacketCreator.getEndOfServerList());
        c.getSession().write(MaplePacketCreator.enableRecommended(true));
        c.getSession().write(MaplePacketCreator.sendRecommended(ServerProperties.getRWAmount, ServerProperties.getWRText));
    }
}