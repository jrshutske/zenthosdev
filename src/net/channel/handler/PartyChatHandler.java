package net.channel.handler;

import java.rmi.RemoteException;
import client.MapleCharacter;
import client.MapleClient;
import client.messages.CommandProcessor;
import net.AbstractMaplePacketHandler;
import server.AntiCheats;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class PartyChatHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int type = slea.readByte();
        int numRecipients = slea.readByte();
        int recipients[] = new int[numRecipients];
        for (int i = 0; i < numRecipients; i++) {
            recipients[i] = slea.readInt();
        }
        String text = slea.readMapleAsciiString();
        MapleCharacter player = c.getPlayer();
        if (!player.isGM() && text.length() > 100) {
            AntiCheats.autoBan(c, player.getName() + " had infinite text with a text length of " + text.length());
            return;
        } else if (CommandProcessor.getInstance().isCommand(c, text, text.charAt(0), player.isGM())) {
            return;
        } else if (player.getCanTalk() && !player.getAntiCheats().Spam(250, 9)) {
            try {
                if (type == 0) {
                    c.getChannelServer().getWorldInterface().buddyChat(recipients, player.getId(), player.getName(), text);
                } else if (type == 1 && player.getParty() != null) {
                    c.getChannelServer().getWorldInterface().partyChat(player.getParty().getId(), text, player.getName());
                } else if (type == 2 && player.getGuildId() > 0) {
                    c.getChannelServer().getWorldInterface().guildChat(player.getGuildId(), player.getName(), player.getId(), text);
                } else if (type == 3 && player.getGuild() != null) {
                    int allianceId = player.getGuild().getAllianceId();
                    if (allianceId > 0) {
                        c.getChannelServer().getWorldInterface().allianceMessage(allianceId, MaplePacketCreator.multiChat(player.getName(), text, 3), player.getId(), -1);
                    }
                }
            } catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }
        } else {
            player.dropMessage(1, "Please try again later.");
        }
    }
}