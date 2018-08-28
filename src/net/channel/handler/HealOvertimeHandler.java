package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import server.AntiCheats;
import tools.data.input.SeekableLittleEndianAccessor;

public class HealOvertimeHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(8);
        int healHP = slea.readShort();
        if (healHP != 0) {
            if (AntiCheats.getInstance().checkHPRegen()) {
                c.getPlayer().addHP(healHP);
            }
        }
        int healMP = slea.readShort();
        if (healMP != 0) {
            if (AntiCheats.getInstance().checkMPRegen()) {
                c.getPlayer().addMP(healMP);
            }
        }
    }
}