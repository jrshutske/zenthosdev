package net.channel.handler;

import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class UseRemoteHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int type = slea.readInt();
        int mode = slea.readInt();
        if (type == 5451000) {
            MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, type, 1, true, true);
            int npcId = 9100100;
            if (mode != 8 && npcId != 9) {
                npcId += mode;
            } else {
                switch (mode) {
                    case 8:
                        npcId = 9100109;
                        break;
                    case 9:
                        npcId = 9100117;
                        break;
                }
            }
            NPCScriptManager.getInstance().start(c, npcId);
        }
    }
}