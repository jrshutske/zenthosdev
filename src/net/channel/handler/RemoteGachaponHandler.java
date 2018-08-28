package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import scripting.npc.NPCScriptManager;
import server.MapleItemInformationProvider;
import tools.data.input.SeekableLittleEndianAccessor;

public final class RemoteGachaponHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int type = slea.readInt();
        if (c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(type)).countById(type) < 1) {
            return;
        }
        int mode = slea.readInt();
        if (type == 5451000) {
            int npcId = 9100100;
            if (mode != 8 && mode != 9) {
                npcId += mode;
            } else {
                npcId = mode == 8 ? 9100109 : 9100117;
            }
            NPCScriptManager.getInstance().start(c, npcId);
        }
    }
}