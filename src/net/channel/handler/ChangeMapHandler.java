package net.channel.handler;

import java.net.InetAddress;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.MaplePacket;
import net.channel.ChannelServer;
import server.MaplePortal;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class ChangeMapHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (slea.available() == 0) {
            int channel = c.getChannel();
            String ip = ChannelServer.getInstance(c.getChannel()).getIP(channel);
            String[] socket = ip.split(":");
            if (c.getPlayer().inCS() || c.getPlayer().inMTS()) {
                c.getPlayer().saveToDB(true, true);
                c.getPlayer().setInCS(false);
                c.getPlayer().setInMTS(false);
            } else {
                c.getPlayer().saveToDB(true, false);
            }
            ChannelServer.getInstance(c.getChannel()).removePlayer(c.getPlayer());
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
            try {
                MaplePacket packet = MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]));
                c.getSession().write(packet);
                c.getSession().close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            slea.readByte();
            int targetid = slea.readInt();
            String startwp = slea.readMapleAsciiString();
            MaplePortal portal = c.getPlayer().getMap().getPortal(startwp);
            MapleCharacter player = c.getPlayer();
            if (player.getBuffedValue(MapleBuffStat.MORPH) != null && player.getBuffedValue(MapleBuffStat.COMBO) != null) {
                player.cancelEffectFromBuffStat(MapleBuffStat.MORPH);
                player.cancelEffectFromBuffStat(MapleBuffStat.COMBO);
            }
            if (player.getBuffedValue(MapleBuffStat.PUPPET) != null) {
                player.cancelBuffStats(MapleBuffStat.PUPPET);
            }
            if (targetid != -1 && !c.getPlayer().isAlive()) {
                boolean executeStandardPath = true;
                if (player.getEventInstance() != null) {
                    executeStandardPath = player.getEventInstance().revivePlayer(player);
                }
                if (executeStandardPath) {
                    player.cancelAllBuffs();
                    player.setHp(50);
                    MapleMap to = player.getMap().getReturnMap();
                    MaplePortal pto = to.getPortal(0);
                    player.setStance(0);
                    player.changeMap(to, pto);
                }
            } else if (targetid != -1 && player.isGM()) {
                MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
                MaplePortal pto = to.getPortal(0);
                player.changeMap(to, pto);
            } else if (targetid != -1 && !player.isGM()) {
                System.out.println("Player " + player.getName() + " attempted Map jumping without being a GM");
            } else {
                if (portal != null) {
                    portal.enterPortal(c);
                } else {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    System.out.println("Portal " + startwp + " not found on map " + player.getMap().getId());
                }
            }
        }
    }
}