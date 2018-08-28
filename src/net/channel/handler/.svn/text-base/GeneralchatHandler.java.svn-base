package net.channel.handler;

import client.MapleClient;
import client.messages.CommandProcessor;
import net.AbstractMaplePacketHandler;
import server.AntiCheats;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class GeneralchatHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String text = slea.readMapleAsciiString();
        int show = slea.readByte();
        if (!c.getPlayer().isGM() && text.length() > 100) {
            AntiCheats.autoBan(c, c.getPlayer().getName() + " had infinite text with a text length of " + text.length());
            return;
        } else if (CommandProcessor.getInstance().isCommand(c, text, text.charAt(0), c.getPlayer().isGM())) {
            return;
        } else if (c.getPlayer().getCanTalk() && !c.getPlayer().getAntiCheats().Spam(250, 9)) {
            if (!c.getPlayer().isHidden()) {
                final int type = c.getPlayer().getGMText();
                if (type == 0) {
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), text, false, show));
                } else if (type == 7) {
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), text, true, show));
                } else {
                    if (type > 0 && type < 5) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.multiChat(c.getPlayer().getName(), text, type - 1));
                    } else if (type > 4 && type < 7) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(type, c.getPlayer().getName() + " : " + text));
                    } else if (type == 8) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                    } else {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.sendYellowTip(c.getPlayer().getName() + " : " + text));
                    }
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), text, false, 1));
                }
            } else {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, c.getPlayer().getName() + " : " + text));
            }
        } else {
            c.getPlayer().dropMessage(1, "Please try again later.");
        }
    }
}