package net.channel.handler;

import client.MapleClient;
import client.MapleDisease;
import java.util.ArrayList;
import java.util.List;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class CancelDebuffHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        List<MapleDisease> diseases = c.getPlayer().getDiseases();
        List<MapleDisease> diseases_ = new ArrayList<MapleDisease>();
        for (MapleDisease disease : diseases) {
            List<MapleDisease> disease_ = new ArrayList<MapleDisease>();
            disease_.add(disease);
            diseases_.add(disease);
            c.getSession().write(MaplePacketCreator.cancelDebuff(disease_));
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.cancelForeignDebuff(c.getPlayer().getId(), disease_), false);
        }
        for (MapleDisease disease : diseases_) {
            c.getPlayer().dispelDebuff(disease);
        }
    }
}