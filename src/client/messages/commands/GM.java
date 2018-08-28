package client.messages.commands;

import constants.ServerProperties;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import client.IItem;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MaplePet;
import client.MapleStat;
import client.Equip;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import net.channel.ChannelServer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import client.MapleCharacterUtil;
import client.MapleDisease;
import client.MapleJob;
import client.MapleRing;
import client.SkillFactory;
import constants.InventoryConstants;
import database.DatabaseConnection;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataTool;
import scripting.npc.NPCScriptManager;
import server.MaplePortal;
import server.MapleShopFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.life.MapleNPC;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.Pair;
import net.channel.handler.ChangeChannelHandler;
import net.world.remote.WorldChannelInterface;
import net.world.remote.WorldLocation;
import server.MapleTrade;
import server.maps.FakeCharacter;
import static client.messages.CommandSupport.*;
import provider.MapleWZProvider;
import tools.StringUtil;

public class GM {
    private static String getBannedReason(String name) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps;
            ResultSet rs;
            ps = con.prepareStatement("SELECT name, banned, banreason, macs FROM accounts WHERE name = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("banned") > 0) {
                    String user, reason, mac;
                    user = rs.getString("name");
                    reason = rs.getString("banreason");
                    mac = rs.getString("macs");
                    rs.close();
                    ps.close();
                    return "Username: " + user + " | BanReason: " + reason + " | Macs: " + mac;
                } else {
                    rs.close();
                    ps.close();
                    return "Player is not banned.";
                }
            }
            rs.close();
            ps.close();
            int accid;
            ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return "This character/account does not exist.";
            } else {
                accid = rs.getInt("accountid");
            }
            ps = con.prepareStatement("SELECT name, banned, banreason, macs FROM accounts WHERE id = ?");
            ps.setInt(1, accid);
            rs = ps.executeQuery();
            if (rs.getInt("banned") > 0) {
                String user, reason, mac;
                user = rs.getString("name");
                reason = rs.getString("banreason");
                mac = rs.getString("macs");
                rs.close();
                ps.close();
                return "Username: " + user + " | BanReason: " + reason + " | Macs: " + mac;
            } else {
                rs.close();
                ps.close();
                return "Player is not banned.";
            }
        } catch (SQLException e) {
            System.out.println("SQLE: " + e);
        }
        return "Player is not banned.";
    }

    private static void clearSlot(MapleClient c, int type) {
        MapleInventoryType invent;
        if (type == 1) {
            invent = MapleInventoryType.EQUIP;
        } else if (type == 2) {
            invent = MapleInventoryType.USE;
        } else if (type == 3) {
            invent = MapleInventoryType.ETC;
        } else if (type == 4) {
            invent = MapleInventoryType.SETUP;
        } else {
            invent = MapleInventoryType.CASH;
        }
        List<Integer> itemMap = new LinkedList<Integer>();
        for (IItem item : c.getPlayer().getInventory(invent).list()) {
            itemMap.add(item.getItemId());
        }
        for (int itemid : itemMap) {
            MapleInventoryManipulator.removeAllById(c, itemid, false);
        }
    }

    public static void execute(MapleClient c, String[] splitted, String command) throws Exception {
        ChannelServer cserv = c.getChannelServer();
        Collection<ChannelServer> cservs = ChannelServer.getAllInstances();
        MapleCharacter player = c.getPlayer();
        if (command.equals("lowhp")) {
            player.setHp(1);
            player.updateSingleStat(MapleStat.HP, 1);
        } else if (command.equals("sp")) {
            if (splitted.length != 2) {
                return;
            }
            int sp;
            try {
                sp = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            player.setRemainingSp(sp + player.getRemainingSp());
            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
        } else if (command.equals("ap")) {
            if (splitted.length != 2) {
                return;
            }
            int ap;
            try {
                ap = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            player.setRemainingAp(ap + player.getRemainingAp());
            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
        } else if (command.equals("job")) {
            if (splitted.length != 2) {
                return;
            }
            int job;
            try {
                job = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            if (MapleJob.getById(job) != null) {
                player.changeJob(MapleJob.getById(job));
            }
        } else if (command.equals("whereami")) {
            player.msg("You are on map " + player.getMap().getId());
        } else if (command.equals("shop")) {
            if (splitted.length != 2) {
                return;
            }
            int shopid;
            try {
                shopid = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            MapleShopFactory.getInstance().getShop(shopid).sendShop(c);
        } else if (command.equals("opennpc")) {
            if (splitted.length != 2) {
                return;
            }
            int npcid;
            try {
                npcid = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            MapleNPC npc = MapleLifeFactory.getNPC(npcid);
            if (npc != null && !npc.getName().equalsIgnoreCase("MISSINGNO")) {
                NPCScriptManager.getInstance().start(c, npcid);
            } else {
                player.msg("UNKNOWN NPC");
            }
        } else if (command.equals("levelup")) {
            player.levelUp();
            player.setExp(0);
            player.updateSingleStat(MapleStat.EXP, 0);
        } else if (command.equals("setmaxmp")) {
            if (splitted.length != 2) {
                return;
            }
            int amt;
            try {
                amt = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            player.setMaxMp(amt);
            player.updateSingleStat(MapleStat.MAXMP, player.getMaxMp());
        } else if (command.equals("setmaxhp")) {
            if (splitted.length != 2) {
                return;
            }
            int amt;
            try {
                amt = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            player.setMaxHp(amt);
            player.updateSingleStat(MapleStat.MAXHP, player.getMaxHp());
        } else if (command.equals("healmap")) {
            for (MapleCharacter map : player.getMap().getCharacters()) {
                if (map != null) {
                    map.setHp(map.getCurrentMaxHp());
                    map.updateSingleStat(MapleStat.HP, map.getHp());
                    map.setMp(map.getCurrentMaxMp());
                    map.updateSingleStat(MapleStat.MP, map.getMp());
                }
            }
        } else if (command.equals("item")) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (splitted.length < 2) {
                return;
            }
            int item;
            short quantity = (short) getOptionalIntArg(splitted, 2, 1);
            try {
                item = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException e) {
                player.msg("Error while making item.");
                return;
            }
            if (item >= 5000000 && item <= 5000100) {
                if (quantity > 1) {
                    quantity = 1;
                }
                int petId = MaplePet.createPet(item);
                MapleInventoryManipulator.addById(c, item, quantity, player.getName(), petId);
            } else if (ii.getInventoryType(item).equals(MapleInventoryType.EQUIP) && !InventoryConstants.isRechargable(ii.getEquipById(item).getItemId())) {
                MapleInventoryManipulator.addFromDrop(c, ii.randomizeStats(c, (Equip) ii.getEquipById(item)), true, player.getName());
            } else {
                MapleInventoryManipulator.addById(c, item, quantity);
            }
        } else if (command.equals("noname")) {
            if (splitted.length < 2) {
                return;
            }
            int quantity = getOptionalIntArg(splitted, 2, 1);
            int item;
            try {
                item = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            MapleInventoryManipulator.addById(c, item, (short) quantity);
        } else if (command.equals("dropmesos")) {
            if (splitted.length < 2) {
                return;
            }
            int amt;
            try {
                amt = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            player.getMap().spawnMesoDrop(amt, amt, player.getPosition(), player, player, false);
        } else if (command.equals("level")) {
            if (splitted.length != 2) {
                return;
            }
            int level;
            try {
                level = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            player.setLevel(level);
            player.levelUp();
            player.setExp(0);
            player.updateSingleStat(MapleStat.EXP, 0);
        } else if (command.equals("online")) {
            int i = 0;
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                if (cs.getPlayerStorage().getAllCharacters().size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    player.msg("Channel " + cs.getChannel());
                    for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters()) {
                        i++;
                        if (sb.length() > 150) {
                            player.msg(sb.toString());
                            sb = new StringBuilder();
                        }
                        sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName() + "   "));
                    }
                    player.msg(sb.toString());
                }
            }
        } else if (command.equals("banreason")) {
            if (splitted.length != 2) {
                return;
            }
            player.msg(getBannedReason(splitted[1]));
        } else if (command.equals("joinguild")) {
            if (splitted.length != 2) {
                return;
            }
            Connection con = DatabaseConnection.getConnection();
            try {
                PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
                ps.setString(1, splitted[1]);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (player.getGuildId() > 0) {
                        try {
                            cserv.getWorldInterface().leaveGuild(player.getMGC());
                        } catch (java.rmi.RemoteException re) {
                            c.getSession().write(MaplePacketCreator.serverNotice(5, "Unable to connect to the World Server. Please try again later."));
                            return;
                        }
                        c.getSession().write(MaplePacketCreator.showGuildInfo(null));

                        player.setGuildId(0);
                        player.saveGuildStatus();
                    }
                    player.setGuildId(rs.getInt("guildid"));
                    player.setGuildRank(2); // Jr.master :D
                    try {
                        cserv.getWorldInterface().addGuildMember(player.getMGC());
                    } catch (RemoteException e) {
                        cserv.reconnectWorld();
                    }
                    c.getSession().write(MaplePacketCreator.showGuildInfo(player));
                    player.getMap().broadcastMessage(player, MaplePacketCreator.removePlayerFromMap(player.getId()), false);
                    player.getMap().broadcastMessage(player, MaplePacketCreator.spawnPlayerMapobject(player), false);
                    if (player.getNoPets() > 0) {
                        for (MaplePet pet : player.getPets()) {
                            player.getMap().broadcastMessage(player, MaplePacketCreator.showPet(player, pet, false, false), false);
                        }
                    }
                    player.saveGuildStatus();
                } else {
                    player.msg("Guild name does not exist.");
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                return;
            }
        } else if (command.equals("unbuffmap")) {
            for (MapleCharacter map : player.getMap().getCharacters()) {
                if (map != null && map != player) {
                    map.cancelAllBuffs();
                }
            }
        } else if (command.equals("mesos")) {
            if (splitted.length != 2) {
                return;
            }
            int meso;
            try {
                meso = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException ex) {
                return;
            }
            player.setMeso(meso);
        } else if (command.equals("setname")) {
            if (splitted.length != 3) {
                return;
            }
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            String newname = splitted[2];
            if (splitted.length == 3) {
                if (MapleCharacter.getIdByName(newname, 0) == -1) {
                    if (victim != null) {
                        victim.getClient().disconnect();
                        victim.getClient().getSession().close();
                        victim.setName(newname, true);
                        player.msg(splitted[1] + " is now named " + newname + "");
                    } else {
                        player.msg("The player " + splitted[1] + " is either offline or not in this channel");
                    }
                } else {
                    player.msg("Character name in use.");
                }
            } else {
                player.msg("Incorrect syntax !");
            }
        } else if (command.equals("clearslot")) {
            if (splitted.length == 2) {
                if (splitted[1].equalsIgnoreCase("all")) {
                    clearSlot(c, 1);
                    clearSlot(c, 2);
                    clearSlot(c, 3);
                    clearSlot(c, 4);
                    clearSlot(c, 5);
                } else if (splitted[1].equalsIgnoreCase("equip")) {
                    clearSlot(c, 1);
                } else if (splitted[1].equalsIgnoreCase("use")) {
                    clearSlot(c, 2);
                } else if (splitted[1].equalsIgnoreCase("etc")) {
                    clearSlot(c, 3);
                } else if (splitted[1].equalsIgnoreCase("setup")) {
                    clearSlot(c, 4);
                } else if (splitted[1].equalsIgnoreCase("cash")) {
                    clearSlot(c, 5);
                } else {
                    player.msg("!clearslot " + splitted[1] + " does not exist!");
                }
            }
        } else if (command.equals("ariantpq")) {
            if (splitted.length < 2) {
                player.getMap().AriantPQStart();
            } else {
                c.getSession().write(MaplePacketCreator.updateAriantPQRanking(splitted[1], 5, false));
            }
        } else if (command.equals("array")) {
            if (splitted.length >= 2) {
                if (splitted[1].equalsIgnoreCase("*CLEAR")) {
                    cserv.setArrayString("");
                    player.msg("Array flushed.");
                } else {
                    cserv.setArrayString(cserv.getArrayString() + StringUtil.joinStringFrom(splitted, 1));
                    player.msg("Added " + StringUtil.joinStringFrom(splitted, 1) + " to the array. Use !array to check.");
                }
            } else {
                player.msg("Array: " + cserv.getArrayString());
            }
        } else if (command.equals("slap")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int damage;
            try {
                damage = Integer.parseInt(splitted[2]);
            } catch (NumberFormatException ex) {
                return;
            }
            if (victim.getHp() > damage) {
                victim.setHp(victim.getHp() - damage);
                victim.updateSingleStat(MapleStat.HP, victim.getHp());
                victim.dropMessage(5, player.getName() + " picked up a big fish and slapped you across the head. You've lost " + damage + " hp");
                player.msg(victim.getName() + " has " + victim.getHp() + " HP left");
            } else {
                victim.setHp(0);
                victim.updateSingleStat(MapleStat.HP, 0);
                victim.dropMessage(5, player.getName() + " gave you a headshot with a fish (:");
            }
        } else if (command.equals("rreactor")) {
            player.getMap().resetReactors();
        } else if (command.equals("coke")) {
            int[] coke = {9500144, 9500151, 9500152, 9500153, 9500154, 9500143, 9500145, 9500149, 9500147};
            for (int i = 0; i < coke.length; i++) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(coke[i]), player.getPosition());
            }

        } else if (command.equals("papu")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8500001), player.getPosition());
            }

        } else if (command.equals("zakum")) {
            for (int m = 8800003; m <= 8800010; m++) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(m), player.getPosition());
            }
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8800000), player.getPosition());
            player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(0, "The almighty Zakum has awakened!"));
        } else if (command.equals("ergoth")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300028), player.getPosition());
            }
        } else if (command.equals("ludimini")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8160000), player.getPosition());
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8170000), player.getPosition());
            }
        } else if (command.equals("cornian")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8150201), player.getPosition());
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8150200), player.getPosition());
            }
        } else if (command.equals("balrog")) {
            int[] balrog = {8130100, 8150000, 9400536};
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                for (int i = 0; i < balrog.length; i++) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(balrog[i]), player.getPosition());
                }
            }
        } else if (command.equals("mushmom")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                int[] mushmom = {6130101, 6300005, 9400205};
                for (int i = 0; i < mushmom.length; i++) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(mushmom[i]), player.getPosition());
                }
            }
        } else if (command.equals("wyvern")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                for (int i = 8150300; i <= 8150302; i++) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(i), player.getPosition());
                }
            }
        } else if (command.equals("pirate")) {
            int[] pirate = {9300119, 9300107, 9300105, 9300106};
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                for (int i = 0; i < pirate.length; i++) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(pirate[i]), player.getPosition());
                }
            }
        } else if (command.equals("clone")) {
            int[] clone = {9001002, 9001003, 9001000, 9001001};
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                for (int i = 0; i < clone.length; i++) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(clone[i]), player.getPosition());
                }
            }
        } else if (command.equals("anego")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400121), player.getPosition());
            }
        } else if (command.equals("theboss")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400300), player.getPosition());
            }
        } else if (command.equals("snackbar")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9500179), player.getPosition());
            }
        } else if (command.equals("papapixie")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300039), player.getPosition());
            }
        } else if (command.equals("nxslimes")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400202), player.getPosition());
            }
        } else if (command.equals("horseman")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400549), player.getPosition());
            }
        } else if (command.equals("blackcrow")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400014), player.getPosition());
            }
        } else if (command.equals("leafreboss")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400014), player.getPosition());
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8180001), player.getPosition());
            }
        } else if (command.equals("shark")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8150101), player.getPosition());
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8150100), player.getPosition());
            }
        } else if (command.equals("franken")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300139), player.getPosition());
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300140), player.getPosition());
            }
        } else if (command.equals("bird")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300090), player.getPosition());
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300089), player.getPosition());
            }
        } else if (command.equals("pianus")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8510000), player.getPosition());
            }
        } else if (command.equals("centipede")) {
            for (int amnt = getOptionalIntArg(splitted, 1, 1); amnt > 0; amnt--) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9500177), player.getPosition());
            }
        } else if (command.equals("horntail")) {
            MapleMonster ht = MapleLifeFactory.getMonster(8810026);
            player.getMap().spawnMonsterOnGroudBelow(ht, player.getPosition());
            player.getMap().killMonster(ht, player, false);
            player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(0, "As the cave shakes and rattles, here comes Horntail."));
        } else if (command.equals("killall")) {
            String mapMessage = "";
            MapleMap map = player.getMap();
            double range = Double.POSITIVE_INFINITY;
            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = cserv.getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    mapMessage = " in " + map.getStreetName() + " : " + map.getMapName();
                }
            }
            List<MapleMapObject> monsters = map.getMapObjectsInRange(player.getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER));
            for (MapleMapObject monstermo : monsters) {
                MapleMonster monster = (MapleMonster) monstermo;
                map.killMonster(monster, player, false);
            }
            player.msg("Killed " + monsters.size() + " monsters" + mapMessage + ".");
        } else if (command.equals("say")) {
            if (splitted.length > 1) {
                try {
                    cserv.getWorldInterface().broadcastMessage(player.getName(), MaplePacketCreator.serverNotice(6, "[" + player.getName() + "] " + StringUtil.joinStringFrom(splitted, 1)).getBytes());
                } catch (RemoteException e) {
                    cserv.reconnectWorld();
                }
            } else {
                player.msg("Syntax: !say <message>");
            }
        } else if (command.equals("gender")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.setGender(victim.getGender() == 1 ? 0 : 1);
                victim.getClient().getSession().write(MaplePacketCreator.getCharInfo(victim));
                victim.getMap().removePlayer(victim);
                victim.getMap().addPlayer(victim);
            } else {
                player.msg("Player is not on.");
            }
        } else if (command.equals("spy")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                player.msg("Players stats are:");
                player.msg("Level: " + victim.getLevel() + "  ||  Rebirthed: " + victim.getReborns());
                player.msg("Fame: " + victim.getFame());
                player.msg("Str: " + victim.getStr() + "  ||  Dex: " + victim.getDex() + "  ||  Int: " + victim.getInt() + "  ||  Luk: " + victim.getLuk());
                player.msg("Player has " + victim.getMeso() + " mesos.");
                player.msg("Hp: " + victim.getHp() + "/" + victim.getCurrentMaxHp() + "  ||  Mp: " + victim.getMp() + "/" + victim.getCurrentMaxMp());
                player.msg("NX Cash: " + victim.getCSPoints(0));
            } else {
                player.msg("Player not found.");
            }
        } else if (command.equals("levelperson")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            victim.setLevel(getOptionalIntArg(splitted, 2, victim.getLevel() + 1));
            victim.levelUp();
            victim.setExp(0);
            victim.updateSingleStat(MapleStat.EXP, 0);
        } else if (command.equals("skill")) {
            int skill;
            try {
                skill = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            int maxlevel = SkillFactory.getSkill(skill).getMaxLevel();
            int level = getOptionalIntArg(splitted, 2, maxlevel);
            int masterlevel = getOptionalIntArg(splitted, 3, maxlevel);
            if (splitted.length == 4) {
                player.changeSkillLevel(SkillFactory.getSkill(skill), level, masterlevel);
            } else if (splitted.length == 5) {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[4]);
                if (victim != null) {
                    victim.changeSkillLevel(SkillFactory.getSkill(skill), level, masterlevel);
                } else {
                    player.msg("Victim was not found.");
                }
            }
        } else if (command.equals("setall")) {
            int max;
            try {
                max = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asda) {
                return;
            }
            player.setStr(max);
            player.setDex(max);
            player.setInt(max);
            player.setLuk(max);
            player.updateSingleStat(MapleStat.STR, max);
            player.updateSingleStat(MapleStat.DEX, max);
            player.updateSingleStat(MapleStat.INT, max);
            player.updateSingleStat(MapleStat.LUK, max);
        } else if (command.equals("giftnx")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                int amount;
                try {
                    amount = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException ex) {
                    return;
                }
                int type = getOptionalIntArg(splitted, 3, 1);
                victim.modifyCSPoints(type, amount);
                victim.dropMessage(5, player.getName() + " has gifted you " + amount + " NX points.");
                player.msg("NX recieved.");
            } else {
                player.msg("Player not found.");
            }
        } else if (command.equals("maxskills")) {
            player.maxAllSkills();
        } else if (command.equals("fame")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.setFame(getOptionalIntArg(splitted, 2, 1));
                victim.updateSingleStat(MapleStat.FAME, victim.getFame());
            } else {
                player.msg("Player not found");
            }
        } else if (command.equals("unhide")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.dispelSkill(9101004);
            } else {
                player.msg("Player not found");
            }
        } else if (command.equals("heal")) {
            MapleCharacter heal = null;
            if (splitted.length == 2) {
                heal = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (heal == null) {
                    player.msg("Player was not found");
                }
            } else {
                heal = player;
            }
            heal.setHp(heal.getCurrentMaxHp());
            heal.setMp(heal.getCurrentMaxMp());
            heal.updateSingleStat(MapleStat.HP, heal.getCurrentMaxHp());
            heal.updateSingleStat(MapleStat.MP, heal.getCurrentMaxMp());
        } else if (command.equals("unbuff")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.cancelAllBuffs();
            } else {
                player.msg("Player not found");
            }
        } else if (command.equals("sendhint")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                String message = StringUtil.joinStringFrom(splitted, 2);
                victim.getMap().broadcastMessage(victim, MaplePacketCreator.sendHint(message, 0, 0), false);
            } else {
                player.msg("Player not found");
            }
        } else if (command.equals("smega")) {
            if (splitted.length > 3) {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    String type = splitted[2];
                    String text = StringUtil.joinStringFrom(splitted, 3);
                    int itemID = 5390002; // default.
                    if (type.equalsIgnoreCase("love")) {
                        itemID = 5390002;
                    } else if (type.equalsIgnoreCase("cloud")) {
                        itemID = 5390001;
                    } else if (type.equalsIgnoreCase("diablo")) {
                        itemID = 5390000;
                    }
                    String[] lines = {"", "", "", ""};

                    if (text.length() > 30) {
                        lines[0] = text.substring(0, 10);
                        lines[1] = text.substring(10, 20);
                        lines[2] = text.substring(20, 30);
                        lines[3] = text.substring(30);
                    } else if (text.length() > 20) {
                        lines[0] = text.substring(0, 10);
                        lines[1] = text.substring(10, 20);
                        lines[2] = text.substring(20);
                    } else if (text.length() > 10) {
                        lines[0] = text.substring(0, 10);
                        lines[1] = text.substring(10);
                    } else if (text.length() <= 10) {
                        lines[0] = text;
                    }
                    LinkedList<String> list = new LinkedList<String>();
                    list.add(lines[0]);
                    list.add(lines[1]);
                    list.add(lines[2]);
                    list.add(lines[3]);
//                    try {
//                        victim.getClient().getChannelServer().getWorldInterface().broadcastSMega(null, MaplePacketCreator.getAvatarMega(victim, c.getChannel(), itemID, list, false).getBytes());
//                    } catch (RemoteException e) {
//                        cserv.reconnectWorld();
//                    }
                } else {
                    player.msg("Player not found.");
                }
            } else {
                player.msg("Syntax: !smega <player> <love/diablo/cloud> text");
            }
        } else if (command.equals("mutesmega")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.setCanSmega(!victim.getCanSmega());
                victim.dropMessage(5, "Your smega ability is now " + (victim.getCanSmega() ? "on" : "off"));
                player.dropMessage(6, "Player's smega ability is now set to " + victim.getCanSmega());
            } else {
                player.msg("Player not found");
            }
        } else if (command.equals("mute")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.canTalk(!victim.getCanTalk());
                victim.dropMessage(5, "Your chatting ability is now " + (victim.getCanTalk() ? "on" : "off"));
                player.dropMessage(6, "Player's chatting ability is now set to " + victim.getCanTalk());
            } else {
                player.msg("Player not found");
            }
        } else if (command.equals("givedisease")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int type;
            if (splitted[2].equalsIgnoreCase("SEAL")) {
                type = 120;
            } else if (splitted[2].equalsIgnoreCase("DARKNESS")) {
                type = 121;
            } else if (splitted[2].equalsIgnoreCase("WEAKEN")) {
                type = 122;
            } else if (splitted[2].equalsIgnoreCase("STUN")) {
                type = 123;
            } else if (splitted[2].equalsIgnoreCase("POISON")) {
                type = 125;
            } else if (splitted[2].equalsIgnoreCase("SEDUCE")) {
                type = 128;
            } else {
                player.msg("ERROR.");
                return;
            }
            victim.giveDebuff(MapleDisease.getType(type), MobSkillFactory.getMobSkill(type, 1));
        } else if (command.equals("dc")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            victim.getClient().disconnect();
            victim.getClient().getSession().close();
        } else if (command.equals("charinfo")) {
            StringBuilder builder = new StringBuilder();
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                return;
            }
            player.msg(builder.toString());
            builder = new StringBuilder();
            builder.append("Positions: X: ");
            builder.append(victim.getPosition().x);
            builder.append(" Y: ");
            builder.append(victim.getPosition().y);
            builder.append(" | RX0: ");
            builder.append(victim.getPosition().x + 50);
            builder.append(" | RX1: ");
            builder.append(victim.getPosition().x - 50);
            builder.append(" | FH: ");
            builder.append(victim.getMap().getFootholds().findBelow(player.getPosition()).getId());
            player.msg(builder.toString());
            builder = new StringBuilder();
            builder.append("HP: ");
            builder.append(victim.getHp());
            builder.append("/");
            builder.append(victim.getCurrentMaxHp());
            builder.append(" | MP: ");
            builder.append(victim.getMp());
            builder.append("/");
            builder.append(victim.getCurrentMaxMp());
            builder.append(" | EXP: ");
            builder.append(victim.getExp());
            builder.append(" | In a Party: ");
            builder.append(victim.getParty() != null);
            builder.append(" | In a Trade: ");
            builder.append(victim.getTrade() != null);
            player.msg(builder.toString());
            builder = new StringBuilder();
            builder.append("Remote Address: ");
            builder.append(victim.getClient().getSession().getRemoteAddress());
            player.msg(builder.toString());
        } else if (command.equals("connected")) {
            try {
                Map<Integer, Integer> connected = cserv.getWorldInterface().getConnected();
                StringBuilder conStr = new StringBuilder();
                player.msg("Connected Clients: ");

                for (int i : connected.keySet()) {
                    if (i == 0) {
                        conStr.append("Total: "); // I HAVE NO CLUE WHY.
                        conStr.append(connected.get(i));
                    } else {
                        conStr.append("Channel ");
                        conStr.append(i);
                        conStr.append(": ");
                        conStr.append(connected.get(i));
                    }
                }
                player.msg(conStr.toString());
            } catch (RemoteException e) {
                cserv.reconnectWorld();
            }
        } else if (command.equals("clock")) {
            player.getMap().broadcastMessage(MaplePacketCreator.getClock(getOptionalIntArg(splitted, 1, 60)));
        } else if (command.equals("warp")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                if (splitted.length == 2) {
                    MapleMap target = victim.getMap();
                    player.changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
                } else {
                    MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    victim.changeMap(target, target.getPortal(0));
                }
            } else {
                try {
                    victim = player;
                    WorldLocation loc = cserv.getWorldInterface().getLocation(splitted[1]);
                    if (loc != null) {
                        player.msg("You will be cross-channel warped. This may take a few seconds.");
                        MapleMap target = cserv.getMapFactory().getMap(loc.map);
                        victim.cancelAllBuffs();
                        String ip = cserv.getIP(loc.channel);
                        victim.getMap().removePlayer(victim);
                        victim.setMap(target);
                        String[] socket = ip.split(":");
                        if (victim.getTrade() != null) {
                            MapleTrade.cancelTrade(player);
                        }
                        victim.saveToDB(true, true);
                        ChannelServer.getInstance(c.getChannel()).removePlayer(player);
                        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                        try {
                            c.getSession().write(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        MapleMap target = cserv.getMapFactory().getMap(Integer.parseInt(splitted[1]));
                        player.changeMap(target, target.getPortal(0));
                    }
                } catch (Exception e) {
                }
            }
        } else if (command.equals("warphere")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            MapleMap pmap = player.getMap();
            if (victim != null) {
                victim.changeMap(pmap, player.getPosition());
            } else {
                try {
                    String name = splitted[1];
                    WorldChannelInterface wci = cserv.getWorldInterface();
                    int channel = wci.find(name);
                    if (channel > -1) {
                        ChannelServer pserv = ChannelServer.getInstance(channel);
                        MapleCharacter world_victim = pserv.getPlayerStorage().getCharacterByName(name);
                        if (world_victim != null) {
                            ChangeChannelHandler.changeChannel(c.getChannel(), world_victim.getClient());
                            world_victim.changeMap(pmap, player.getPosition());
                        }
                    } else {
                        player.msg("Player not online.");
                    }
                } catch (RemoteException e) {
                    cserv.reconnectWorld();
                }
            }
        } else if (command.equals("jail")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.changeMap(980000404, 0);
                player.msg(victim.getName() + " was jailed!");
                victim.dropMessage("You've been jailed bitch.");
            } else {
                player.msg(splitted[1] + " not found!");
            }
        } else if (command.equals("map")) {
            int mapid;
            try {
                mapid = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException mwa) {
                return;
            }
            player.changeMap(mapid, getOptionalIntArg(splitted, 2, 0));
        } else if (command.equals("warpallhere")) {
            for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                if (mch.getMapId() != player.getMapId()) {
                    mch.changeMap(player.getMap(), player.getPosition());
                }
            }
        } else if (command.equals("warpwholeworld")) {
            for (ChannelServer channels : cservs) {
                for (MapleCharacter mch : channels.getPlayerStorage().getAllCharacters()) {
                    if (mch.getClient().getChannel() != c.getChannel()) {
                        ChangeChannelHandler.changeChannel(c.getChannel(), mch.getClient());
                    }
                    if (mch.getMapId() != player.getMapId()) {
                        mch.changeMap(player.getMap(), player.getPosition());
                    }
                }
            }
        } else if (command.equals("mesosrate")) { // All these could be so much shorter but cbf.
            int set;
            try {
                set = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            if (splitted.length > 2) {
                for (ChannelServer channel : cservs) {
                    ServerProperties.getMesoRate = set;
                    channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Meso Rate has been changed to " + set + "x"));
                }
            } else if (splitted.length == 2) {
                ServerProperties.getMesoRate = set;
                cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Meso Rate has been changed to " + set + "x"));
            } else {
                player.msg("Syntax: !mesorate <number>");
            }
        } else if (command.equals("droprate")) {
            int set;
            try {
                set = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            if (splitted.length > 2) {
                for (ChannelServer channel : cservs) {
                    ServerProperties.getDropRate = set;
                    channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Drop Rate has been changed to " + set + "x"));
                }
            } else if (splitted.length == 2) {
                ServerProperties.getDropRate = set;
                cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Drop Rate has been changed to " + set + "x"));
            } else {
                player.msg("Syntax: !droprate <number>");
            }
        } else if (command.equals("bossdroprate")) {
            int set;
            try {
                set = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            if (splitted.length > 2) {
                for (ChannelServer channel : cservs) {
                    ServerProperties.getBossDropRate = set;
                    channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Boss Drop Rate has been changed to " + set + "x"));
                }
            } else if (splitted.length == 2) {
                ServerProperties.getBossDropRate = set;
                cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Boss Drop Rate has been changed to " + set + "x"));
            } else {
                player.msg("Syntax: !bossdroprate <number>");
            }
        } else if (command.equals("exprate")) {
            int set;
            try {
                set = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            if (splitted.length > 2) {
                for (ChannelServer channel : cservs) {
                    ServerProperties.getExpRate = set;
                    channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "Exp Rate has been changed to " + set + "x"));
                }
            } else if (splitted.length == 2) {
                ServerProperties.getExpRate = set;
                cserv.broadcastPacket(MaplePacketCreator.serverNotice(0, "Exp Rate has been changed to " + set + "x"));
            } else {
                player.msg("Syntax: !exprate <number>");
            }
        } else if (command.equals("servermessage")) {
            String outputMessage = StringUtil.joinStringFrom(splitted, 1);
            if (outputMessage.equalsIgnoreCase("!array")) {
                outputMessage = cserv.getArrayString();
            }
            ServerProperties.getEventMessage = outputMessage;
        } else if (command.equals("whosthere")) {
            StringBuilder builder = new StringBuilder();
            player.msg("Players on Map: ");
            for (MapleCharacter chr : player.getMap().getCharacters()) {
                if (builder.length() > 150) { // wild guess :o
                    player.msg(builder.toString());
                    builder = new StringBuilder();
                }
                builder.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                builder.append(", ");
            }
            player.dropMessage(6, builder.toString());
        } else if (command.equals("getrings")) {
            player.msg("1112800 - clover");
            player.msg("1112001 - crush");
            player.msg("1112801 - flower");
            player.msg("1112802 - Star");
            player.msg("1112803 - moonstone");
            player.msg("1112806 - Stargem");
            player.msg("1112807 - golden");
            player.msg("1112809 - silverswan");
        } else if (command.equals("ring")) {
            Map<String, Integer> rings = new HashMap<String, Integer>();
            rings.put("clover", 1112800);
            rings.put("crush", 1112001);
            rings.put("flower", 1112801);
            rings.put("star", 1112802);
            rings.put("stargem", 1112806);
            rings.put("silverswan", 1112809);
            rings.put("golden", 1112807);
            if (rings.containsKey(splitted[3])) {
                MapleCharacter partner1 = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                MapleCharacter partner2 = cserv.getPlayerStorage().getCharacterByName(splitted[2]);
                int ret = MapleRing.createRing(rings.get(splitted[3]), partner1, partner2);
                switch (ret) {
                    case -2:
                        player.msg("Partner number 1 was not found.");
                        break;

                    case -1:
                        player.msg("Partner number 2 was not found.");
                        break;

                    case 0:
                        player.msg("Error. One of the players already posesses a ring");
                        break;

                    default:
                        player.msg("Sucess !");
                }
            } else {
                player.msg("Ring name was not found.");
            }
            rings.clear();
        } else if (command.equals("removering")) {
            MapleCharacter victim = player;
            if (splitted.length == 2) {
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            }
            if (victim != null) {
                if (MapleRing.checkRingDB(victim)) {
                    MapleRing.removeRingFromDb(victim);
                } else {
                    victim.dropMessage("You have no ring..");
                }
            }
        } else if (command.equals("nearestPortal")) {
            final MaplePortal portal = player.getMap().findClosestSpawnpoint(player.getPosition());
            player.msg(portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName());
        } else if (command.equals("unban")) {
            if (MapleCharacter.unban(splitted[1])) {
                player.msg("Sucess!");
            } else {
                player.msg("Error while unbanning.");
            }
        } else if (command.equals("spawn")) {
            int mid;
            int num = getOptionalIntArg(splitted, 2, 1);
            try {
                mid = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException asd) {
                return;
            }
            if (num > 20) {
                player.msg("Remember that we know what you're doing ;] please dont over summon");
            }
            Integer hp = getNamedIntArg(splitted, 1, "hp");
            Integer exp = getNamedIntArg(splitted, 1, "exp");
            Double php = getNamedDoubleArg(splitted, 1, "php");
            Double pexp = getNamedDoubleArg(splitted, 1, "pexp");
            MapleMonster onemob = MapleLifeFactory.getMonster(mid);
            int newhp = 0;
            int newexp = 0;
            if (hp != null) {
                newhp = hp.intValue();
            } else if (php != null) {
                newhp = (int) (onemob.getMaxHp() * (php.doubleValue() / 100));
            } else {
                newhp = onemob.getMaxHp();
            }
            if (exp != null) {
                newexp = exp.intValue();
            } else if (pexp != null) {
                newexp = (int) (onemob.getExp() * (pexp.doubleValue() / 100));
            } else {
                newexp = onemob.getExp();
            }
            if (newhp < 1) {
                newhp = 1;
            }
            MapleMonsterStats overrideStats = new MapleMonsterStats();
            overrideStats.setHp(newhp);
            overrideStats.setExp(newexp);
            overrideStats.setMp(onemob.getMaxMp());
            if (num > 20) {
                num = 20;
            }
            for (int i = 0; i < num; i++) {
                MapleMonster mob = MapleLifeFactory.getMonster(mid);
                mob.setHp(newhp);
                mob.setOverrideStats(overrideStats);
                player.getMap().spawnMonsterOnGroudBelow(mob, player.getPosition());
            }
        } else if (command.equals("ban")) {
            String originalReason = StringUtil.joinStringFrom(splitted, 2);
            String reason = player.getName() + " banned " + splitted[1] + ": " + originalReason;
            MapleCharacter target = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (target != null) {
                if (!target.isGM() || player.getGMLevel() > 3) {
                    String readableTargetName = MapleCharacterUtil.makeMapleReadable(target.getName());
                    String ip = target.getClient().getSession().getRemoteAddress().toString().split(":")[0];
                    reason += "  IP: " + ip;
                    target.ban(reason, false);
                    try {
                        cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(6, readableTargetName + " has been banned for " + originalReason).getBytes());
                    } catch (RemoteException e) {
                        cserv.reconnectWorld();
                    }
                } else {
                    player.msg("Please dont ban " + ServerProperties.getServerName + " GMs");
                }
            } else {
                if (MapleCharacter.ban(splitted[1], reason, false)) {
                    String readableTargetName = MapleCharacterUtil.makeMapleReadable(target.getName());
                    String ip = target.getClient().getSession().getRemoteAddress().toString().split(":")[0];
                    reason += " (IP: " + ip + ")";
                    try {
                        cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(6, readableTargetName + " has been banned for " + originalReason).getBytes());
                    } catch (RemoteException e) {
                        cserv.reconnectWorld();
                    }
                } else {
                    player.msg("Failed to ban " + splitted[1]);
                }
            }
        } else if (command.equals("checktokens")) {
            if (splitted.length == 3) {
                MapleCharacter target = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                int type = Integer.parseInt(splitted[2]);
                if (type > 7 || type < 0) {
                    player.msg("Token Types are from 0-7.");
                }
                byte amount = target.getAntiCheats().getTokenCounters(type);
                player.msg(target.getName() + " has " + amount + " counters on " + target.getAntiCheats().getTokenType(type) + ".");
            } else {
                player.msg("Please use !CheckTokens (player name) (Token Type 0-7).");
            }
        } else if (command.equals("addtokens")) {
            if (splitted.length == 3) {
                int type = Integer.parseInt(splitted[2]);
                if (type > 7 || type < 0) {
                    player.msg("Token Types are from 0-7.");
                }
                MapleCharacter target = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                player.getAntiCheats().addCheatToken(type);
                byte amount = target.getAntiCheats().getTokenCounters(type);
                player.msg(target.getName() + " has " + amount + " counters on " + target.getAntiCheats().getTokenType(type) + ".");
            } else {
                player.msg("Please use !AddTokens (player name) (Token Type 0-7).");
            }
        } else if (command.equals("tempban")) {
            Calendar tempB = Calendar.getInstance();
            String originalReason = joinAfterString(splitted, ":");
            if (splitted.length < 4 || originalReason == null) {
                player.msg("Syntax helper: !tempban <name> [i / m / w / d / h] <amount> [r [reason id] : Text Reason");
                return;
            }

            int yChange = getNamedIntArg(splitted, 1, "y", 0);
            int mChange = getNamedIntArg(splitted, 1, "m", 0);
            int wChange = getNamedIntArg(splitted, 1, "w", 0);
            int dChange = getNamedIntArg(splitted, 1, "d", 0);
            int hChange = getNamedIntArg(splitted, 1, "h", 0);
            int iChange = getNamedIntArg(splitted, 1, "i", 0);
            int gReason = getNamedIntArg(splitted, 1, "r", 7);

            String reason = player.getName() + " tempbanned " + splitted[1] + ": " + originalReason;

            if (gReason > 14) {
                player.msg("You have entered an incorrect ban reason ID, please try again.");
                return;
            }

            DateFormat df = DateFormat.getInstance();
            tempB.set(tempB.get(Calendar.YEAR) + yChange, tempB.get(Calendar.MONTH) + mChange, tempB.get(Calendar.DATE) +
                    (wChange * 7) + dChange, tempB.get(Calendar.HOUR_OF_DAY) + hChange, tempB.get(Calendar.MINUTE) +
                    iChange);

            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);

            if (victim == null) {
                int accId = MapleClient.findAccIdForCharacterName(splitted[1]);
                if (accId >= 0 && MapleCharacter.tempban(reason, tempB, gReason, accId)) {
                    String readableTargetName = MapleCharacterUtil.makeMapleReadable(victim.getName());
                    cserv.broadcastPacket(MaplePacketCreator.serverNotice(6, readableTargetName + " has been banned for " + originalReason));

                } else {
                    player.msg("There was a problem offline banning character " + splitted[1] + ".");
                }
            } else {
                victim.tempban(reason, tempB, gReason);
                player.msg("The character " + splitted[1] + " has been successfully tempbanned till " + df.format(tempB.getTime()));
            }
        } else if (command.equals("search")) {
            if (splitted.length > 2) {
                String type = splitted[1];
                String search = StringUtil.joinStringFrom(splitted, 2);
                MapleData data = null;
                final MapleDataProvider dataProvider = MapleWZProvider.stringWZ;
                player.msg("<<Type: " + type + " | Search: " + search + ">>");
                if (type.equalsIgnoreCase("NPC") || type.equalsIgnoreCase("NPCS")) {
                    List<String> retNpcs = new ArrayList<String>();
                    data = dataProvider.getData("Npc.img");
                    List<Pair<Integer, String>> npcPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData npcIdData : data.getChildren()) {
                        int npcIdFromData = Integer.parseInt(npcIdData.getName());
                        String npcNameFromData = MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME");
                        npcPairList.add(new Pair<Integer, String>(npcIdFromData, npcNameFromData));
                    }
                    for (Pair<Integer, String> npcPair : npcPairList) {
                        if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
                        }
                    }
                    if (retNpcs != null && retNpcs.size() > 0) {
                        for (String singleRetNpc : retNpcs) {
                            player.msg(singleRetNpc);
                        }
                    } else {
                        player.msg("No NPC's Found");
                    }
                } else if (type.equalsIgnoreCase("MAP") || type.equalsIgnoreCase("MAPS")) {
                    List<String> retMaps = new ArrayList<String>();
                    data = dataProvider.getData("Map.img");
                    List<Pair<Integer, String>> mapPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData mapAreaData : data.getChildren()) {
                        for (MapleData mapIdData : mapAreaData.getChildren()) {
                            int mapIdFromData = Integer.parseInt(mapIdData.getName());
                            String mapNameFromData = MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME");
                            mapPairList.add(new Pair<Integer, String>(mapIdFromData, mapNameFromData));
                        }
                    }
                    for (Pair<Integer, String> mapPair : mapPairList) {
                        if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
                        }
                    }
                    if (retMaps != null && retMaps.size() > 0) {
                        for (String singleRetMap : retMaps) {
                            player.msg(singleRetMap);
                        }
                    } else {
                        player.msg("No Maps Found");
                    }
                } else if (type.equalsIgnoreCase("MOB") || type.equalsIgnoreCase("MOBS") || type.equalsIgnoreCase("MONSTER") || type.equalsIgnoreCase("MONSTERS")) {
                    List<String> retMobs = new ArrayList<String>();
                    data = dataProvider.getData("Mob.img");
                    List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData mobIdData : data.getChildren()) {
                        int mobIdFromData = Integer.parseInt(mobIdData.getName());
                        String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                        mobPairList.add(new Pair<Integer, String>(mobIdFromData, mobNameFromData));
                    }
                    for (Pair<Integer, String> mobPair : mobPairList) {
                        if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
                        }
                    }
                    if (retMobs != null && retMobs.size() > 0) {
                        for (String singleRetMob : retMobs) {
                            player.msg(singleRetMob);
                        }
                    } else {
                        player.msg("No Mob's Found");
                    }
                } else if (type.equalsIgnoreCase("REACTOR") || type.equalsIgnoreCase("REACTORS")) {
                    player.msg("NOT ADDED YET");

                } else if (type.equalsIgnoreCase("ITEM") || type.equalsIgnoreCase("ITEMS")) {
                    List<String> retItems = new ArrayList<String>();
                    for (Pair<Integer, String> itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                        if (itemPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retItems.add(itemPair.getLeft() + " - " + itemPair.getRight());
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            player.msg(singleRetItem);
                        }
                    } else {
                        player.msg("No Item's Found");
                    }
                } else if (type.equalsIgnoreCase("SKILL") || type.equalsIgnoreCase("SKILLS")) {
                    List<String> retSkills = new ArrayList<String>();
                    data = dataProvider.getData("Skill.img");
                    List<Pair<Integer, String>> skillPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData skillIdData : data.getChildren()) {
                        int skillIdFromData = Integer.parseInt(skillIdData.getName());
                        String skillNameFromData = MapleDataTool.getString(skillIdData.getChildByPath("name"), "NO-NAME");
                        skillPairList.add(new Pair<Integer, String>(skillIdFromData, skillNameFromData));
                    }
                    for (Pair<Integer, String> skillPair : skillPairList) {
                        if (skillPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retSkills.add(skillPair.getLeft() + " - " + skillPair.getRight());
                        }
                    }
                    if (retSkills != null && retSkills.size() > 0) {
                        for (String singleRetSkill : retSkills) {
                            player.msg(singleRetSkill);
                        }
                    } else {
                        player.msg("No Skills Found");
                    }
                } else {
                    player.msg("Sorry, that search call is unavailable");
                }
            } else {
                player.msg("Invalid search.  Proper usage: '!search <type> <search for>', where <type> is MAP, USE, ETC, CASH, EQUIP, MOB (or MONSTER), or SKILL.");
            }
        } else if (command.equals("msearch")) {
            try {
                URL url;
                URLConnection urlConn;

                BufferedReader dis;

                String replaced;
                if (splitted.length > 1) {
                    replaced = StringUtil.joinStringFrom(splitted, 1).replace(' ', '%');
                } else {
                    player.msg("Syntax: !search item name/map name/monster name");
                    return;
                }

                url = new URL("http://www.mapletip.com/search_java.php?search_value=" + replaced + "&check=true");
                urlConn = url.openConnection();
                urlConn.setDoInput(true);
                urlConn.setUseCaches(false);
                dis = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                String s;

                while ((s = dis.readLine()) != null) {
                    player.msg(s);
                }
                player.msg("Search for " + '"' + replaced.replace('%', ' ') + '"' + " was completed.");
                dis.close();
            } catch (MalformedURLException mue) {
                player.msg("Malformed URL Exception: " + mue.toString());
            } catch (IOException ioe) {
                player.msg("IO Exception: " + ioe.toString());
            } catch (Exception e) {
                player.msg("General Exception: " + e.toString());
            }
        } else if (command.equals("npc")) {
            int npcId;
            try {
                npcId = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                return;
            }
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc != null && !npc.getName().equalsIgnoreCase("MISSINGNO")) {
                npc.setPosition(player.getPosition());
                npc.setCy(player.getPosition().y);
                npc.setRx0(player.getPosition().x + 50);
                npc.setRx1(player.getPosition().x - 50);
                npc.setFh(player.getMap().getFootholds().findBelow(player.getPosition()).getId());
                npc.setCustom(true);
                player.getMap().addMapObject(npc);
                player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
            } else {
                player.msg("You have entered an invalid Npc-Id");
            }

        } else if (command.equals("removenpcs")) {
            List<MapleMapObject> npcs = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.NPC));
            for (MapleMapObject npcmo : npcs) {
                MapleNPC npc = (MapleNPC) npcmo;
                if (npc.isCustom()) {
                    player.getMap().removeMapObject(npc.getObjectId());
                }
            }
        } else if (command.equals("mynpcpos")) {
            Point pos = player.getPosition();
            player.msg("X: " + pos.x + " | Y: " + pos.y + "  | RX0: " + (pos.x + 50) + " | RX1: " + (pos.x - 50) + " | FH: " + player.getMap().getFootholds().findBelow(pos).getId());
        } else if (command.equals("cleardrops")) {
            MapleMap map = player.getMap();
            double range = Double.POSITIVE_INFINITY;
            java.util.List<MapleMapObject> items = map.getMapObjectsInRange(player.getPosition(), range, Arrays.asList(MapleMapObjectType.ITEM));
            for (MapleMapObject itemmo : items) {
                map.removeMapObject(itemmo);
                map.broadcastMessage(MaplePacketCreator.removeItemFromMap(itemmo.getObjectId(), 0, player.getId()));
            }
            player.msg("You have destroyed " + items.size() + " items on the ground.");
        } else if (command.equals("clearshops")) {
            MapleShopFactory.getInstance().clear();
        } else if (command.equals("clearevents")) {
            for (ChannelServer instance : ChannelServer.getAllInstances()) {
                instance.reloadEvents();
            }
        } else if (command.equals("permban")) {
            String name = splitted[1];
            String reason = StringUtil.joinStringFrom(splitted, 2);
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(name);
            if (victim != null) {
                if (!victim.isGM()) {
                    victim.ban(reason, true);
                    player.msg("Character permanently banned. !");
                } else {
                    player.msg("You can't ban a GM. Sorry");
                }
            } else {
                if (MapleCharacter.ban(name, reason, false)) {
                    player.msg("Permanently banned sucessfully");
                } else {
                    player.msg("Error while banning.");
                }

            }
        } else if (command.equals("emote")) {
            String name = splitted[1];
            int emote;
            try {
                emote = Integer.parseInt(splitted[2]);
            } catch (NumberFormatException nfe) {
                return;
            }
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(name);
            if (victim != null) {
                victim.getMap().broadcastMessage(victim, MaplePacketCreator.facialExpression(victim, emote), victim.getPosition());
            } else {
                player.msg("Player was not found");
            }
        } else if (command.equals("proitem")) {
            if (splitted.length == 3) {
                int itemid;
                short multiply;
                try {
                    itemid = Integer.parseInt(splitted[1]);
                    multiply = Short.parseShort(splitted[2]);
                } catch (NumberFormatException asd) {
                    return;
                }
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                IItem item = ii.getEquipById(itemid);
                MapleInventoryType type = ii.getInventoryType(itemid);
                if (type.equals(MapleInventoryType.EQUIP)) {
                    MapleInventoryManipulator.addFromDrop(c, ii.hardcoreItem((Equip) item, multiply));
                } else {
                    player.msg("Make sure it's an equippable item.");
                }
            } else {
                player.msg("Invalid syntax.");
            }
        } else if (command.equals("addclones")) {
            if (splitted.length < 2) {
                return;
            }
            int clones;
            try {
                clones = getOptionalIntArg(splitted, 1, 1);
            } catch (NumberFormatException asdasd) {
                return;
            }
            if (player.getFakeChars().size() >= 5) {
                player.msg("You are not allowed to clone yourself over 5 times.");
            } else {
                for (int i = 0; i < clones && i + player.getFakeChars().size() <= 6; i++) {
                    FakeCharacter fc = new FakeCharacter(player, player.getId() + player.getFakeChars().size() + clones + i);
                    player.getFakeChars().add(fc);
                    c.getChannelServer().addClone(fc);
                }
                player.msg("You have cloned yourself " + player.getFakeChars().size() + " times so far.");
            }
        } else if (command.equals("removeclones")) {
            for (FakeCharacter fc : player.getFakeChars()) {
                if (fc.getFakeChar().getMap() == player.getMap()) {
                    c.getChannelServer().getAllClones().remove(fc);
                    player.getMap().removePlayer(fc.getFakeChar());
                }
            }
            player.getFakeChars().clear();
            player.msg("All your clones in the map removed.");
        } else if (command.equals("removeallclones")) {
            for (FakeCharacter fc : c.getChannelServer().getAllClones()) {
                if (fc.getOwner() != null) {
                    fc.getOwner().getFakeChars().remove(fc);
                }
                fc.getFakeChar().getMap().removePlayer(fc.getFakeChar());
            }
            c.getChannelServer().getAllClones().clear();
            player.msg("ALL clones have been removed.");
        } else if (command.equals("follow")) {
            int slot = Integer.parseInt(splitted[1]);
            FakeCharacter fc = player.getFakeChars().get(slot);
            if (fc == null) {
                player.msg("Clone does not exist.");
            } else {
                fc.setFollow(true);
            }
        } else if (command.equals("pause")) {
            int slot = Integer.parseInt(splitted[1]);
            FakeCharacter fc = player.getFakeChars().get(slot);
            if (fc == null) {
                player.msg("Clone does not exist.");
            } else {
                fc.setFollow(false);
            }
        } else if (command.equals("stance")) {
            if (splitted.length == 3) {
                int slot = Integer.parseInt(splitted[1]);
                int stance = Integer.parseInt(splitted[2]);
                player.getFakeChars().get(slot).getFakeChar().setStance(stance);
            }
        } else if (command.equals("killmonster")) {
            if (splitted.length == 2) {
                MapleMap map = c.getPlayer().getMap();
                int targetId = Integer.parseInt(splitted[1]);
                List<MapleMapObject> monsters = map.getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
                for (MapleMapObject monsterm : monsters) {
                    MapleMonster monster = (MapleMonster) monsterm;
                    if (monster.getId() == targetId) {
                        map.killMonster(monster, player, false);
                        break;
                    }
                }
            }
        } else if (command.equals("removeoid")) {
            if (splitted.length == 2) {
                MapleMap map = c.getPlayer().getMap();
                int oid = Integer.parseInt(splitted[1]);
                MapleMapObject obj = map.getMapObject(oid);
                if (obj == null) {
                    player.msg("This oid does not exist.");
                } else {
                    map.removeMapObject(obj);
                }
            }
        } else if (command.equals("gmtext")) {
            int text;
            String type = splitted[1].toLowerCase();
            if (type.equals("normal")) {
                text = 0;
            } else if (type.equals("orange")) {
                text = 1;
            } else if (type.equals("pink")) {
                text = 2;
            } else if (type.equals("purple")) {
                text = 3;
            } else if (type.equals("green")) {
                text = 4;
            } else if (type.equals("red")) {
                text = 5;
            } else if (type.equals("blue")) {
                text = 6;
            } else if (type.equals("whitebg")) {
                text = 7;
            } else if (type.equals("lightinggreen")) {
                text = 8;
            } else if (type.equals("yellow")){
                text = 9;
            } else {
                player.msg("Wrong syntax: use !gmtext normal/orange/pink/purple/green/blue/red/whitebg/lightinggreen/yellow");
                return;
            }
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement("UPDATE characters SET gmtext = ? WHERE name = ?");
            ps.setString(2, player.getName());
            ps.setInt(1, text);
            ps.executeUpdate();
            ps.close();
            player.setGMText(text);
        } else if (command.equals("currentdate")) {
            Calendar cal = Calendar.getInstance();
            int day = cal.get(Calendar.DATE);
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);
            player.msg(day + "/" + month + "/" + year);
        } else if (command.equals("maxmesos")) {
            player.gainMeso(Integer.MAX_VALUE - player.getMeso());
        } else if (command.equals("fullcharge")) {
            player.setEnergyBar(10000);
            c.getSession().write(MaplePacketCreator.giveEnergyCharge(10000));
        } else if (command.equals("youlose")) {
            for (MapleCharacter victim : player.getMap().getCharacters()) {
                if (victim != null) {
                    if (victim.getHp() <= 0) {
                        victim.dropMessage("You have lost the event.");
                        victim.changeMap(100000000);
                    } else {
                        victim.setHp(victim.getCurrentMaxHp());
                        victim.updateSingleStat(MapleStat.HP, victim.getHp());
                        victim.setMp(victim.getCurrentMaxMp());
                        victim.updateSingleStat(MapleStat.MP, victim.getMp());
                    }
                }
            }
        } else {
            System.out.println("GM Command: !" + command + " does not exist.");
        }
    }
}