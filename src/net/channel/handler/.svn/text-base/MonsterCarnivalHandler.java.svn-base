package net.channel.handler;

import java.awt.Point;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.MaplePacket;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

public class MonsterCarnivalHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int tab = slea.readByte();
        int num = slea.readByte();
        c.getPlayer().getMap().broadcastMessage(playerSummoned(c.getPlayer().getName(), tab, num));
        if (tab == 0) {
            MapleMonster mob = MapleLifeFactory.getMonster(getMonsterIdByNum(num));
            //c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, randomizePosition(c.getPlayer().getMapId()));
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, randomizePosition(c.getPlayer().getMapId(), 1));
        }
    }

    public Point randomizePosition(int mapid, int team) {
        int posx = 0;
        int posy = 0;
        if (mapid == 980000301) { // Room 3 iirc
            posy = 162;
            if (team == 0) { // Maple red goes left
                posx = rand(-1554, -151);
            } else { // Maple blue goes right
                posx = rand(148, 1571);
            }
        }
        return new Point(posx, posy);
    }

    public int getMonsterIdByNum(int num) {
        int mid = 0;
        num++;

        switch (num) {
            case 1:
                mid = 3000005;
                break;
            case 2:
                mid = 3230302;
                break;
            case 3:
                mid = 3110102;
                break;
            case 4:
                mid = 3230306;
                break;
            case 5:
                mid = 3230305;
                break;
            case 6:
                mid = 4230113;
                break;
            case 7:
                mid = 4230111;
                break;
            case 8:
                mid = 3230103;
                break;
            case 9:
                mid = 4230115;
                break;
            case 10:
                mid = 4130103;
                break;
            default:
                mid = 210100;
                break;
        }
        return mid;
    }

    private static int rand(int lbound, int ubound) {
        return (int) ((Math.random() * (ubound - lbound + 1)) + lbound);
    }

    private static MaplePacket playerSummoned(String name, int tab, int number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
        mplew.write(tab);
        mplew.write(number);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }
}