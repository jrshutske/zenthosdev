package net.channel.handler;

import client.Equip;
import client.IItem;
import client.Item;
import client.MapleBuffStat;
import client.MapleClient;
import java.rmi.RemoteException;
import constants.ServerProperties;
import net.AbstractMaplePacketHandler;
import net.channel.ChannelServer;
import net.world.remote.WorldChannelInterface;
import server.MTSItemInfo;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import server.maps.FakeCharacter;
import server.maps.SavedLocationType;

public class EnterMTSHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (ServerProperties.MTtoFM) {
            if (!(c.getPlayer().isAlive())) {
                c.getPlayer().dropMessage("You can't enter the FM when you are dead.");
            } else {
                if (c.getPlayer().getMapId() != 910000000) {
                    c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET);
                    c.getPlayer().changeMap(c.getChannelServer().getMapFactory().getMap(910000000), c.getChannelServer().getMapFactory().getMap(910000000).getPortal("out00"));
                }
            }
            c.getSession().write(MaplePacketCreator.enableActions());
        } else {
            if (c.getPlayer().getNoPets() > 0) {
                c.getPlayer().unequipAllPets();
            }
            for (FakeCharacter fc : c.getPlayer().getFakeChars()) {
                c.getPlayer().getMap().removePlayer(fc.getFakeChar());
            }
            if (c.getPlayer().getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
                c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            }
            try {
                WorldChannelInterface wci = ChannelServer.getInstance(c.getChannel()).getWorldInterface();
                wci.addBuffsToStorage(c.getPlayer().getId(), c.getPlayer().getAllBuffs());
                wci.addCooldownsToStorage(c.getPlayer().getId(), c.getPlayer().getAllCooldowns());
            } catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }
            c.getPlayer().getMap().removePlayer(c.getPlayer());
            c.getSession().write(MaplePacketCreator.warpMTS(c));
            c.getPlayer().setInMTS(true);
            c.getSession().write(MaplePacketCreator.enableMTS());
            c.getSession().write(MaplePacketCreator.MTSWantedListingOver(0, 0));
            c.getSession().write(MaplePacketCreator.showMTSCash(c.getPlayer()));
            List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
            int pages = 0;
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM mts_items WHERE tab = 1 AND transfer = 0 ORDER BY id DESC LIMIT ?, 16");
                ps.setInt(1, 0);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getInt("type") != 1) {
                        Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                        i.setOwner(rs.getString("owner"));
                        items.add(new MTSItemInfo(i, rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                    } else {
                        Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                        equip.setQuantity((short) rs.getInt("quantity"));
                        equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                        equip.setLevel((byte) rs.getInt("level"));
                        equip.setStr((short) rs.getInt("str"));
                        equip.setDex((short) rs.getInt("dex"));
                        equip.setInt((short) rs.getInt("int"));
                        equip.setLuk((short) rs.getInt("luk"));
                        equip.setHp((short) rs.getInt("hp"));
                        equip.setMp((short) rs.getInt("mp"));
                        equip.setWatk((short) rs.getInt("watk"));
                        equip.setMatk((short) rs.getInt("matk"));
                        equip.setWdef((short) rs.getInt("wdef"));
                        equip.setMdef((short) rs.getInt("mdef"));
                        equip.setAcc((short) rs.getInt("acc"));
                        equip.setAvoid((short) rs.getInt("avoid"));
                        equip.setHands((short) rs.getInt("hands"));
                        equip.setSpeed((short) rs.getInt("speed"));
                        equip.setJump((short) rs.getInt("jump"));
                        equip.setVicious((byte) rs.getInt("vicious"));
                        equip.setFlag((byte) rs.getInt("flag"));
                        equip.setItemExp((byte) rs.getInt("itemexp"));
                        equip.setOwner(rs.getString("owner"));
                        items.add(new MTSItemInfo((IItem) equip, rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                    }
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT COUNT(*) FROM mts_items");
                rs = ps.executeQuery();
                if (rs.next()) {
                    pages = (int) Math.ceil(rs.getInt(1) / 16);
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                System.out.println("Err1: " + e);
            }
            c.getSession().write(MaplePacketCreator.sendMTS(items, 1, 0, 0, pages));
            c.getSession().write(MaplePacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
            c.getSession().write(MaplePacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
            c.getPlayer().saveToDB(true, true);
        }
    }

    public List<MTSItemInfo> getNotYetSold(int cid) {
        List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        ResultSet rs;
        try {
            ps = con.prepareStatement("SELECT * FROM mts_items WHERE seller = ? AND transfer = 0 ORDER BY id DESC");
            ps.setInt(1, cid);
            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("type") != 1) {
                    Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                    i.setOwner(rs.getString("owner"));
                    items.add(new MTSItemInfo((IItem) i, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                } else {
                    Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                    equip.setQuantity((short) rs.getInt("quantity"));
                    equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                    equip.setLevel((byte) rs.getInt("level"));
                    equip.setStr((short) rs.getInt("str"));
                    equip.setDex((short) rs.getInt("dex"));
                    equip.setInt((short) rs.getInt("int"));
                    equip.setLuk((short) rs.getInt("luk"));
                    equip.setHp((short) rs.getInt("hp"));
                    equip.setMp((short) rs.getInt("mp"));
                    equip.setWatk((short) rs.getInt("watk"));
                    equip.setMatk((short) rs.getInt("matk"));
                    equip.setWdef((short) rs.getInt("wdef"));
                    equip.setMdef((short) rs.getInt("mdef"));
                    equip.setAcc((short) rs.getInt("acc"));
                    equip.setAvoid((short) rs.getInt("avoid"));
                    equip.setHands((short) rs.getInt("hands"));
                    equip.setSpeed((short) rs.getInt("speed"));
                    equip.setJump((short) rs.getInt("jump"));
                    equip.setVicious((byte) rs.getInt("vicious"));
                    equip.setFlag((byte) rs.getInt("flag"));
                    equip.setItemExp((byte) rs.getInt("itemexp"));
                    equip.setOwner(rs.getString("owner"));
                    items.add(new MTSItemInfo((IItem) equip, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.out.println("Err8: " + e);
        }
        return items;
    }

    public List<MTSItemInfo> getTransfer(int cid) {
        List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        ResultSet rs;
        try {
            ps = con.prepareStatement("SELECT * FROM mts_items WHERE transfer = 1 AND seller = ? ORDER BY id DESC");
            ps.setInt(1, cid);
            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("type") != 1) {
                    Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                    i.setOwner(rs.getString("owner"));
                    items.add(new MTSItemInfo((IItem) i, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                } else {
                    Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                    equip.setQuantity((short) rs.getInt("quantity"));
                    equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                    equip.setLevel((byte) rs.getInt("level"));
                    equip.setStr((short) rs.getInt("str"));
                    equip.setDex((short) rs.getInt("dex"));
                    equip.setInt((short) rs.getInt("int"));
                    equip.setLuk((short) rs.getInt("luk"));
                    equip.setHp((short) rs.getInt("hp"));
                    equip.setMp((short) rs.getInt("mp"));
                    equip.setWatk((short) rs.getInt("watk"));
                    equip.setMatk((short) rs.getInt("matk"));
                    equip.setWdef((short) rs.getInt("wdef"));
                    equip.setMdef((short) rs.getInt("mdef"));
                    equip.setAcc((short) rs.getInt("acc"));
                    equip.setAvoid((short) rs.getInt("avoid"));
                    equip.setHands((short) rs.getInt("hands"));
                    equip.setSpeed((short) rs.getInt("speed"));
                    equip.setJump((short) rs.getInt("jump"));
                    equip.setVicious((byte) rs.getInt("vicious"));
                    equip.setFlag((byte) rs.getInt("flag"));
                    equip.setItemExp((byte) rs.getInt("itemexp"));
                    equip.setOwner(rs.getString("owner"));
                    items.add(new MTSItemInfo((IItem) equip, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.out.println("Err7: " + e);
        }
        return items;
    }
}