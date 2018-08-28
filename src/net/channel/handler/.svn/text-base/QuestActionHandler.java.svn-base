package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import scripting.quest.QuestScriptManager;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class QuestActionHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte action = slea.readByte();
        short quest = slea.readShort();
        MapleCharacter player = c.getPlayer();
        int npc = 0;
        if (action != 3) {
            npc = slea.readInt();
            slea.readInt();
        }
        if (action == 1) {
            MapleQuest.getInstance(quest).start(player, npc);
        } else if (action == 2) {
            if (slea.available() >= 4) {
                int selection = slea.readInt();
                MapleQuest.getInstance(quest).complete(player, npc, selection);
            } else {
                MapleQuest.getInstance(quest).complete(player, npc);
            }
            c.getSession().write(MaplePacketCreator.showOwnBuffEffect(0, 9));
            player.getMap().broadcastMessage(player, MaplePacketCreator.showBuffeffect(player.getId(), 0, 9, (byte) 0), false);
        } else if (action == 3) {
            MapleQuest.getInstance(quest).forfeit(player);
        } else if (action == 4) {
            QuestScriptManager.getInstance().start(c, npc, quest);
        } else if (action == 5) {
            QuestScriptManager.getInstance().end(c, npc, quest);
        }
    }
}