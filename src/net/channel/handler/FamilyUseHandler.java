package net.channel.handler;

import client.MapleClient;
import client.MapleCharacter;
import net.AbstractMaplePacketHandler;
import net.MaplePacket;
import net.SendPacketOpcode;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

public final class FamilyUseHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int[] repCost = {3, 5, 7, 8, 10, 12, 15, 20, 25, 40, 50};
        final int type = slea.readInt();
        MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
        if (type == 0 || type == 1) {
            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
            if (victim != null) {
                if (type == 0) {
                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().getPortal(0));
                } else {
                    victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().getPortal(0));
                }
            } else {
                return;
            }
        } else {
            int erate = type == 3 ? 150 : (type == 4 || type == 6 || type == 8 || type == 10 ? 200 : 100);
            int drate = type == 2 ? 150 : (type == 4 || type == 5 || type == 7 || type == 9 ? 200 : 100);
            if (type > 8) {
            } else {
                c.getSession().write(useRep(drate == 100 ? 2 : (erate == 100 ? 3 : 4), type, erate, drate, ((type > 5 || type == 4) ? 2 : 1) * 15 * 60 * 1000));
            }
        }
//        c.getPlayer().getFamily().gainReputation(repCost[type]);
    }

    private static MaplePacket useRep(int mode, int type, int erate, int drate, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0x60);
        mplew.write(mode);
        mplew.writeInt(type);
        if (mode < 4) {
            mplew.writeInt(erate);
            mplew.writeInt(drate);
        }
        mplew.write(0);
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    private static MaplePacket giveBuff() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        mplew.writeInt(0);
        mplew.writeLong(0);
        return null;
    }
}
