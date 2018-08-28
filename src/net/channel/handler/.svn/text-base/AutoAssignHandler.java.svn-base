package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class AutoAssignHandler extends AbstractMaplePacketHandler {
    final short Max = Short.MAX_VALUE;

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        slea.skip(8);
        int total = 0;
        if (chr.getRemainingAp() < 1) {
            return;
        }
        int togain = 0;
        for (int i = 0; i < 2; i++) {
            int type = slea.readInt();
            int tempVal = slea.readInt();
            total += tempVal;
            if (tempVal < 0 || tempVal > c.getPlayer().getRemainingAp()) {
                return;
            }
            togain += gainStatByType(chr, MapleStat.getBy5ByteEncoding(type), tempVal);
        }
        chr.setRemainingAp(chr.getRemainingAp() - (total + togain));
        chr.updateSingleStat(MapleStat.AVAILABLEAP, chr.getRemainingAp());
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    private int gainStatByType(MapleCharacter chr, MapleStat type, int gain) {
        int newVal = 0;
        if (type.equals(MapleStat.STR)) {
            newVal = chr.getStr() + gain;
            chr.setStr(newVal);
        } else if (type.equals(MapleStat.INT)) {
            newVal = chr.getInt() + gain;
            chr.setInt(newVal);
        } else if (type.equals(MapleStat.LUK)) {
            newVal = chr.getLuk() + gain;
            chr.setLuk(newVal);
        } else if (type.equals(MapleStat.DEX)) {
            newVal = chr.getDex() + gain;
            chr.setDex(newVal);
        }
        chr.updateSingleStat(type, Math.min(newVal, Max));
        if (newVal > Max) {
            return Max;
        }
        return 0;
    }
}