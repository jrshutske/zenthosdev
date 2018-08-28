package net.login.handler;

import client.MapleClient;
import java.net.InetAddress;
import net.AbstractMaplePacketHandler;
import net.login.LoginServer;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class CharSelectedHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String pic = slea.readMapleAsciiString();
        int charId = slea.readInt();
        String macs = slea.readMapleAsciiString();
        c.updateMacs(macs);
        if (c.hasBannedMac()) {
            c.getSession().close();
            return;
        }
        if (c.checkPic(pic)) {
            try {
                if (c.getIdleTask() != null) {
                    c.getIdleTask().cancel(true);
                }
                c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                String channelServerIP = MapleClient.getChannelServerIPFromSubnet(c.getSession().getRemoteAddress().toString().replace("/", "").split(":")[0], c.getChannel());
                if (channelServerIP.equals("0.0.0.0")) {
                    String[] socket = LoginServer.getInstance().getIP(c.getChannel()).split(":");
                    c.getSession().write(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
                } else {
                    String[] socket = LoginServer.getInstance().getIP(c.getChannel()).split(":");
                    c.getSession().write(MaplePacketCreator.getServerIP(InetAddress.getByName(channelServerIP), Integer.parseInt(socket[1]), charId));
                }
            } catch (Exception e) {
                System.out.println("Host not found: " + e);
            }
        } else {
            c.getSession().write(MaplePacketCreator.WrongPic());
        }
    }
}