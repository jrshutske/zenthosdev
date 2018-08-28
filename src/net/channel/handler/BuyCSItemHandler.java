package net.channel.handler;

import client.MapleCharacter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import client.MapleClient;
import client.MaplePet;
import database.DatabaseConnection;
import java.util.Calendar;
import net.AbstractMaplePacketHandler;
import server.AntiCheats;
import server.CashItemFactory;
import server.CashItemInfo;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class BuyCSItemHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().inCS()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final int action = slea.readByte();
        if (action == 3) {
            slea.skip(1);
            int useNX = slea.readInt();
            int snCS = slea.readInt();
            CashItemInfo item = CashItemFactory.getItem(snCS);
            int itemID = item.getId();
            if (c.getPlayer().getCSPoints(useNX) >= item.getPrice()) {
                c.getPlayer().modifyCSPoints(useNX, -item.getPrice());
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                AntiCheats.autoBan(c, "Trying to purchase from the CS when they have no NX");
                return;
            }
            if(itemID >= 5390000 && itemID <= 5390002 || itemID == 1812006){
                c.getSession().write(MaplePacketCreator.enableActions());
                c.getPlayer().dropMessage(1, "You may not purchase this item.");
                return;
            }
            if (itemID >= 5000000 && itemID <= 5000100) {
                int petId = MaplePet.createPet(itemID);
                if (petId == -1) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                MapleInventoryManipulator.addById(c, itemID, (short) 1, null, petId);
            } else {
                MapleInventoryManipulator.addById(c, itemID, (short) item.getCount());
            }
            updateInformation(c, snCS);
        } else if (action == 4) {
            if (checkBirthday(c, slea.readInt())) {
                final CashItemInfo item = CashItemFactory.getItem(slea.readInt());
                String recipient = slea.readMapleAsciiString();
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                String message = slea.readMapleAsciiString();
                if (victim != null) {
                    MapleInventoryManipulator.addById(victim.getClient(), item.getId(), (short) 1);
                    c.getPlayer().modifyCSPoints(4, -item.getPrice());
                    try {
                        victim.sendNote(victim.getId(), message);
                    } catch (SQLException s) {
                    }
                } else {
                    c.getPlayer().dropMessage("Make sure the user you are gifting to is\r\n on the same channel.");
                }
            } else {
                c.getPlayer().dropMessage("The birthday you entered was incorrect.");
            }
        } else if (action == 5) {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("DELETE FROM wishlist WHERE charid = ?");
                ps.setInt(1, c.getPlayer().getId());
                ps.executeUpdate();
                ps.close();
                int i = 10;
                while (i > 0) {
                    int sn = slea.readInt();
                    if (sn != 0) {
                        ps = con.prepareStatement("INSERT INTO wishlist(charid, sn) VALUES(?, ?) ");
                        ps.setInt(1, c.getPlayer().getId());
                        ps.setInt(2, sn);
                        ps.executeUpdate();
                        ps.close();
                    }
                    i--;
                }
            } catch (SQLException se) {
            }
            c.getSession().write(MaplePacketCreator.sendWishList(c.getPlayer(), true));
        } else if (action == 7) {
            slea.skip(1);
            byte toCharge = slea.readByte();
            int toIncrease = slea.readInt();
            if (c.getPlayer().getCSPoints(toCharge) >= 4000 && c.getPlayer().getStorage().getSlots() < 48) {
                c.getPlayer().modifyCSPoints(toCharge, -4000);
                if (toIncrease == 0) {
                    c.getPlayer().getStorage().gainSlots(4);
                }
                updateInformation(c);
            }
        } else if (action == 28) {
            slea.skip(1);
            int useNX = slea.readInt();
            int snCS = slea.readInt();
            CashItemInfo item = CashItemFactory.getItem(snCS);
            if (c.getPlayer().getCSPoints(useNX) >= item.getPrice()) {
                c.getPlayer().modifyCSPoints(useNX, -item.getPrice());
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                AntiCheats.autoBan(c, "Trying to purchase from the CS when they have no NX");
                return;
            }
            for (int i : CashItemFactory.getPackageItems(item.getId())) {
                if (i >= 5000000 && i <= 5000100) {
                    int petId = MaplePet.createPet(i);
                    if (petId == -1) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    MapleInventoryManipulator.addById(c, i, (short) 1, null, petId);
                } else {
                    MapleInventoryManipulator.addById(c, i, (short) item.getCount());
                }
            }
            updateInformation(c, snCS);
        } else if (action == 30) {
            int snCS = slea.readInt();
            CashItemInfo item = CashItemFactory.getItem(snCS);
            if (c.getPlayer().getMeso() >= item.getPrice()) {
                c.getPlayer().gainMeso(-item.getPrice(), false);
                MapleInventoryManipulator.addById(c, item.getId(), (short) item.getCount());
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                AntiCheats.autoBan(c, "Trying to purchase from the CS with an insufficient amount");
                return;
            }
        }
    }

    private void updateInformation(MapleClient c, int item) {
        CashItemInfo Item = CashItemFactory.getItem(item);
        c.getSession().write(MaplePacketCreator.showBoughtCSItem(c, Item));
        updateInformation(c);
    }

    private void updateInformation(MapleClient c) {
        c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
        c.getSession().write(MaplePacketCreator.enableCSUse0());
        c.getSession().write(MaplePacketCreator.enableCSUse1());
        c.getSession().write(MaplePacketCreator.enableCSUse2());
        c.getSession().write(MaplePacketCreator.enableCSUse3());
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    private boolean checkBirthday(MapleClient c, int idate) {
        int year = idate / 10000;
        int month = (idate - year * 10000) / 100;
        int day = idate - year * 10000 - month * 100;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, day);
        return c.checkBirthDate(cal);
    }
}