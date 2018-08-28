package net.login.handler;

import client.IItem;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleInventoryType;
import client.MapleJob;
import client.MapleSkinColor;
import net.AbstractMaplePacketHandler;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class CreateCharHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String name = slea.readMapleAsciiString();
        MapleCharacter newchar = MapleCharacter.getDefault(c);
        newchar.setWorld(c.getWorld());
        int job = slea.readInt();
        int face = slea.readInt();
        newchar.setFace(face);
        newchar.setHair(slea.readInt() + slea.readInt());
        newchar.setSkinColor(MapleSkinColor.getById(slea.readInt()));
        int top = slea.readInt();
        int bottom = slea.readInt();
        int shoes = slea.readInt();
        int weapon = slea.readInt();
        newchar.setGender(slea.readByte());
        newchar.setName(name, false);
        newchar.setStr(4);
        newchar.setDex(4);
        newchar.setInt(4);
        newchar.setLuk(4);
        newchar.setRemainingAp(9);
        boolean charok = true;
        if (job == 0) {
            newchar.setJob(MapleJob.NOBLESSE);
        } else if (job == 1) {
            newchar.setJob(MapleJob.BEGINNER);
        } else if (job == 2) {
            newchar.setJob(MapleJob.LEGEND);
        } else {
            charok = false;
            System.out.println("New job class: " + job);
        }
        if (weapon != 1302000 && weapon != 1322005 && weapon != 1312004 && weapon != 1442079) {
            charok = false;
        }
        if (!MapleCharacterUtil.isNameLegal(name)) {
            charok = false;
        }
        if (MapleCharacterUtil.hasSymbols(name)) {
            charok = false;
        }
        if (name.length() < 4 || name.length() > 12) {
            charok = false;
        }
        if (charok && MapleCharacterUtil.canCreateChar(name, c.getWorld())) {
            int[] clothes = {top, bottom, shoes, weapon, 1702132};
            byte[] pos = {-5, -6, -7, -11, -111};
            for (int i = 0; i < clothes.length; i++) {
                IItem item = MapleItemInformationProvider.getInstance().getEquipById(clothes[i]);
                item.setPosition(pos[i]);
                newchar.getInventory(MapleInventoryType.EQUIPPED).addFromDB(item);
            }
            newchar.saveToDB(false, true);
            c.getSession().write(MaplePacketCreator.addNewCharEntry(newchar));
        } else {
            System.out.println("Account: " + c.getAccID() + " is trying to create a character named: " + name + ".");
            c.fullDisconnect();
        }
    }
}