package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import scripting.npc.NPCScriptManager;
import server.life.MapleNPC;
import server.maps.MapleMapObject;
import server.maps.PlayerNPCs;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class NPCTalkHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        int oid = slea.readInt();
        MapleMapObject obj = c.getPlayer().getMap().getMapObject(oid);
        if (obj instanceof MapleNPC) {
            MapleNPC npc = (MapleNPC) obj;
            if (NPCScriptManager.getInstance() != null) {
                NPCScriptManager.getInstance().dispose(c);
            }
            if (!c.getPlayer().getAntiCheats().Spam(1000, 5)) {
                if (npc.getId() == 9010009) {
                    c.getSession().write(MaplePacketCreator.sendDuey((byte) 8, DueyHandler.loadItems(c.getPlayer())));
                } else if (npc.hasShop() && c.getPlayer().getShop() == null) {
                    npc.sendShop(c);
                } else {
                    if (c.getCM() != null || c.getQM() != null) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    NPCScriptManager.getInstance().start(c, npc.getId());
                }
            }
        } else if (obj instanceof PlayerNPCs) {
            NPCScriptManager.getInstance().start(c, ((PlayerNPCs) obj).getId(), null, null);
        }
    }
}