package net.channel.handler;

import java.rmi.RemoteException;
import client.MapleCharacter;
import client.MapleClient;
import client.messages.CommandProcessor;
import net.AbstractMaplePacketHandler;
import net.channel.ChannelServer;
import server.AntiCheats;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class WhisperHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte();
        String recipient = slea.readMapleAsciiString();
        int channel;
        try {
            channel = c.getChannelServer().getWorldInterface().find(recipient);
        } catch (RemoteException re) {
            c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
            c.getChannelServer().reconnectWorld();
            return;
        }
        if (channel == -1) {
            c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
        } else {
            ChannelServer pserv = ChannelServer.getInstance(channel);
            MapleCharacter victim = pserv.getPlayerStorage().getCharacterByName(recipient);
            if (mode == 6) {
                String text = slea.readMapleAsciiString();
                if (!c.getPlayer().isGM() && text.length() > 100) {
                    AntiCheats.autoBan(c, c.getPlayer().getName() + " had infinite text with a text length of " + text.length());
                    return;
                } else if (CommandProcessor.getInstance().isCommand(c, text, text.charAt(0), c.getPlayer().isGM())) {
                    return;
                } else if (c.getPlayer().getCanTalk() && !c.getPlayer().getAntiCheats().Spam(250, 9)) {
                    victim.getClient().getSession().write(MaplePacketCreator.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                    c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                } else {
                    c.getPlayer().dropMessage(1, "Please try again later.");
                }
            } else if (mode == 5) {
                if (!victim.isGM() || (c.getPlayer().isGM() && victim.isGM())) {
                    if (victim.inCS()) {
                        c.getSession().write(MaplePacketCreator.getFindReply(victim.getName(), -1, 2));
                    } else if (victim.inMTS()) {
                        c.getSession().write(MaplePacketCreator.getFindReply(victim.getName(), -1, 0));
                    } else if (c.getChannel() == victim.getClient().getChannel()) {
                        c.getSession().write(MaplePacketCreator.getFindReply(victim.getName(), victim.getMapId(), 1));
                    } else {
                        c.getSession().write(MaplePacketCreator.getFindReply(victim.getName(), 0, 3));
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                }
            }
        }
    }
}