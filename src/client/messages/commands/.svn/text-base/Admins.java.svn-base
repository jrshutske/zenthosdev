package client.messages.commands;

import client.MapleJob;
import java.awt.Point;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import client.IItem;
import client.Item;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MaplePet;
import client.MapleStat;
import database.DatabaseConnection;
import net.MaplePacket;
import server.MapleInventoryManipulator;
import net.channel.ChannelServer;
import net.channel.handler.ChangeChannelHandler;
import scripting.portal.PortalScriptManager;
import scripting.reactor.ReactorScriptManager;
import server.MapleItemInformationProvider;
import server.MapleShopFactory;
import server.PlayerInteraction.HiredMerchant;
import server.ShutdownServer;
import server.life.MapleLifeFactory;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleReactor;
import server.maps.MapleMapItem;
import server.maps.MapleReactorFactory;
import server.maps.MapleReactorStats;
import server.maps.PlayerNPCs;
import tools.MaplePacketCreator;
import tools.StringUtil;
import static client.messages.CommandSupport.getOptionalIntArg;

public class Admins {
    public static void execute(MapleClient c, String[] splitted, String command) throws Exception {
        MapleCharacter player = c.getPlayer();
        ChannelServer cserv = c.getChannelServer();
        if (command.equals("speakall")) {
            String text = StringUtil.joinStringFrom(splitted, 1);
            for (MapleCharacter mch : player.getMap().getCharacters()) {
                mch.getMap().broadcastMessage(MaplePacketCreator.getChatText(mch.getId(), text, false, 0));
            }
        } else if (command.equals("systemsay")) {
            System.out.println(StringUtil.joinStringFrom(splitted, 1));
        } else if (command.equals("dcall")) {
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                for (MapleCharacter cplayer : channel.getPlayerStorage().getAllCharacters()) {
                    if (cplayer != player) {
                        cplayer.getClient().disconnect();
                        cplayer.getClient().getSession().close();
                    }
                }
            }
        } else if (command.equals("killnear")) {
            MapleMap map = player.getMap();
            List<MapleMapObject> players = map.getMapObjectsInRange(player.getPosition(), (double) 50000, Arrays.asList(MapleMapObjectType.PLAYER));
            for (MapleMapObject closeplayers : players) {
                MapleCharacter playernear = (MapleCharacter) closeplayers;
                if (playernear.isAlive() && playernear != player && !playernear.isGM()) {
                    playernear.setHp(0);
                    playernear.updateSingleStat(MapleStat.HP, 0);
                    playernear.dropMessage(5, "You were too close to a GM.");
                }
            }
        } else if (command.equals("drop")) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            int itemId = Integer.parseInt(splitted[1]);
            short quantity = (short) getOptionalIntArg(splitted, 2, 1);
            IItem toDrop;
            if (ii.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                toDrop = ii.getEquipById(itemId);
            } else {
                toDrop = new Item(itemId, (byte) 0, quantity);
            }
            player.getMap().spawnItemDrop(player, player, toDrop, player.getPosition(), true, true);
        } else if (command.equals("closemerchants")) {
            player.msg("Closing and saving merchants, please wait...");
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                for (int i = 910000001; i <= 910000022; i++) {
                    for (MapleMapObject obj : channel.getMapFactory().getMap(i).getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.HIRED_MERCHANT))) {
                        HiredMerchant hm = (HiredMerchant) obj;
                        hm.closeShop(true);
                    }
                }
            }
            player.msg("All merchants have been closed and saved.");
        } else if (command.equals("shutdown")) {
            int time = 60000;
            if (splitted.length > 1) {
                time = Integer.parseInt(splitted[1]) * 60000;
            }
            c.getChannelServer().shutdown(time);
        } else if (command.equals("shutdownworld")) {
            int time = 60000;
            if (splitted.length > 1) {
                time = Integer.parseInt(splitted[1]) * 60000;
            }
            c.getChannelServer().shutdownWorld(time);
        } else if (command.equals("shutdownnow")) {
            new ShutdownServer(c.getChannel()).run();
        } else if (command.equals("setrebirths")) {
            int rebirths;
            try {
                rebirths = Integer.parseInt(splitted[2]);
            } catch (NumberFormatException asd) {
                return;
            }
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.setReborns(rebirths);
            } else {
                player.msg("Player was not found");
            }
        } else if (command.equals("mesoperson")) {
            int mesos;
            try {
                mesos = Integer.parseInt(splitted[2]);
            } catch (NumberFormatException blackness) {
                return;
            }
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.gainMeso(mesos, true, true, true);
            } else {
                player.msg("Player was not found");
            }
        } else if (command.equals("gmperson")) {
            if (splitted.length == 3) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    int level;
                    try {
                        level = Integer.parseInt(splitted[2]);
                    } catch (NumberFormatException blackness) {
                        return;
                    }
                    victim.setGM(level);
                    if (victim.isGM()) {
                        victim.dropMessage(5, "You now have level " + level + " GM powers.");
                    }
                } else {
                    player.msg("The player " + splitted[1] + " is either offline or not in this channel");
                }
            }
        } else if (command.equals("kill")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.setHp(0);
                victim.setMp(0);
                victim.updateSingleStat(MapleStat.HP, 0);
                victim.updateSingleStat(MapleStat.MP, 0);
            } else {
                player.msg("Player not found");
            }
        } else if (command.equals("jobperson")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int job;
            try {
                job = Integer.parseInt(splitted[2]);
            } catch (NumberFormatException blackness) {
                return;
            }
            if (victim != null) {
                if (MapleJob.getById(job) != null) {
                    victim.changeJob(MapleJob.getById(job));
                }
            } else {
                player.msg("Player not found");
            }
        } else if (command.equals("threads")) {
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            String filter = "";
            if (splitted.length > 1) {
                filter = splitted[1];
            }
            for (int i = 0; i < threads.length; i++) {
                String tstring = threads[i].toString();
                if (tstring.toLowerCase().indexOf(filter.toLowerCase()) > -1) {
                    player.msg(i + ": " + tstring);
                }
            }
        } else if (command.equals("showtrace")) {
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            Thread t = threads[Integer.parseInt(splitted[1])];
            player.msg(t.toString() + ":");
            for (StackTraceElement elem : t.getStackTrace()) {
                player.msg(elem.toString());
            }

        } else if (command.equals("shopitem")) {
            if (splitted.length < 5) {
                player.msg("!shopitem <shopid> <itemid> <price> <position>");
            } else {
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position) VALUES (" + Integer.parseInt(splitted[1]) + ", " + Integer.parseInt(splitted[2]) + ", " + Integer.parseInt(splitted[3]) + ", " + Integer.parseInt(splitted[4]) + ");");
                    ps.executeUpdate();
                    ps.close();
                    MapleShopFactory.getInstance().clear();
                    player.msg("Done adding shop item.");
                } catch (SQLException e) {
                    player.msg("Something wrong happened.");
                }
            }

        } else if (command.equals("pnpc")) {
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            int xpos = player.getPosition().x;
            int ypos = player.getPosition().y;
            int fh = player.getMap().getFootholds().findBelow(player.getPosition()).getId();
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(player.getPosition());
                npc.setCy(ypos);
                npc.setRx0(xpos + 50);
                npc.setRx1(xpos - 50);
                npc.setFh(fh);
                npc.setCustom(true);
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                    ps.setInt(1, npcId);
                    ps.setInt(2, 0);
                    ps.setInt(3, fh);
                    ps.setInt(4, ypos);
                    ps.setInt(4, ypos);
                    ps.setInt(5, xpos + 50);
                    ps.setInt(6, xpos - 50);
                    ps.setString(7, "n");
                    ps.setInt(8, xpos);
                    ps.setInt(9, ypos);
                    ps.setInt(10, player.getMapId());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    player.msg("Failed to save NPC to the database");
                }
                player.getMap().addMapObject(npc);
                player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
            } else {
                player.msg("You have entered an invalid Npc-Id");
            }
        } else if (command.equals("tdrops")) {
            player.getMap().toggleDrops();
        } else if (command.equals("givemonsbuff")) {
            int mask = 0;
            mask |= Integer.decode(splitted[1]);
            MobSkill skill = MobSkillFactory.getMobSkill(128, 1);
            c.getSession().write(MaplePacketCreator.applyMonsterStatusTest(Integer.valueOf(splitted[2]), mask, 0, skill, Integer.valueOf(splitted[3])));
        } else if (command.equals("givemonstatus")) {
            int mask = 0;
            mask |= Integer.decode(splitted[1]);
            c.getSession().write(MaplePacketCreator.applyMonsterStatusTest2(Integer.valueOf(splitted[2]), mask, 1000, Integer.valueOf(splitted[3])));
        } else if (command.equals("sreactor")) {
            MapleReactorStats reactorSt = MapleReactorFactory.getReactor(Integer.parseInt(splitted[1]));
            MapleReactor reactor = new MapleReactor(reactorSt, Integer.parseInt(splitted[1]));
            reactor.setDelay(-1);
            reactor.setPosition(player.getPosition());
            player.getMap().spawnReactor(reactor);
        } else if (command.equals("hreactor")) {
            player.getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
        } else if (command.equals("lreactor")) {
            MapleMap map = player.getMap();
            List<MapleMapObject> reactors = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.REACTOR));
            for (MapleMapObject reactorL : reactors) {
                MapleReactor reactor2l = (MapleReactor) reactorL;
                player.msg("Reactor: oID: " + reactor2l.getObjectId() + " reactorID: " + reactor2l.getId() + " Position: " + reactor2l.getPosition().toString() + " State: " + reactor2l.getState());
            }
        } else if (command.equals("dreactor")) {
            MapleMap map = player.getMap();
            List<MapleMapObject> reactors = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.REACTOR));
            if (splitted[1].equalsIgnoreCase("all")) {
                for (MapleMapObject reactorL : reactors) {
                    MapleReactor reactor2l = (MapleReactor) reactorL;
                    player.getMap().destroyReactor(reactor2l.getObjectId());
                }
            } else {
                player.getMap().destroyReactor(Integer.parseInt(splitted[1]));
            }
        } else if (command.equals("saveall")) {
            for (ChannelServer chan : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : chan.getPlayerStorage().getAllCharacters()) {
                    chr.saveToDB(true, false);
                }
            }
            player.msg("save complete");
        } else if (command.equals("notice")) {
            int joinmod = 1;
            int range = -1;
            if (splitted[1].equalsIgnoreCase("m")) {
                range = 0;
            } else if (splitted[1].equalsIgnoreCase("c")) {
                range = 1;
            } else if (splitted[1].equalsIgnoreCase("w")) {
                range = 2;
            }
            int tfrom = 2;
            int type;
            if (range == -1) {
                range = 2;
                tfrom = 1;
            }
            if (splitted[tfrom].equalsIgnoreCase("n")) {
                type = 0;
            } else if (splitted[tfrom].equalsIgnoreCase("p")) {
                type = 1;
            } else if (splitted[tfrom].equalsIgnoreCase("l")) {
                type = 2;
            } else if (splitted[tfrom].equalsIgnoreCase("nv")) {
                type = 5;
            } else if (splitted[tfrom].equalsIgnoreCase("v")) {
                type = 5;
            } else if (splitted[tfrom].equalsIgnoreCase("b")) {
                type = 6;
            } else {
                type = 0;
                joinmod = 0;
            }
            String prefix = "";
            if (splitted[tfrom].equalsIgnoreCase("nv")) {
                prefix = "[Notice] ";
            }
            joinmod += tfrom;
            String outputMessage = StringUtil.joinStringFrom(splitted, joinmod);
            if (outputMessage.equalsIgnoreCase("!array")) {
                outputMessage = c.getChannelServer().getArrayString();
            }
            MaplePacket packet = MaplePacketCreator.serverNotice(type, prefix + outputMessage);
            if (range == 0) {
                player.getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                try {
                    ChannelServer.getInstance(c.getChannel()).getWorldInterface().broadcastMessage(player.getName(), packet.getBytes());
                } catch (RemoteException e) {
                    c.getChannelServer().reconnectWorld();
                }
            }
        } else if (command.equals("strip")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.unequipEverything();
                victim.dropMessage("You've been stripped by " + player.getName() + ".");
            } else {
                player.dropMessage(6, "Player is not on.");
            }
        } else if (command.equals("speak")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                String text = StringUtil.joinStringFrom(splitted, 2);
                victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), text, false, 0));
            } else {
                player.msg("Player not found");
            }
        } else if (command.equals("changechannel")) {
            int channel;

            if (splitted.length == 3) {
                try {
                    channel = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException blackness) {
                    return;
                }
                if (channel <= ChannelServer.getAllInstances().size() || channel < 0) {
                    String name = splitted[1];
                    try {
                        int vchannel = c.getChannelServer().getWorldInterface().find(name);
                        if (vchannel > -1) {
                            ChannelServer pserv = ChannelServer.getInstance(vchannel);
                            MapleCharacter victim = pserv.getPlayerStorage().getCharacterByName(name);
                            ChangeChannelHandler.changeChannel(channel, victim.getClient());
                        } else {
                            player.msg("Player not found");
                        }
                    } catch (RemoteException rawr) {
                        c.getChannelServer().reconnectWorld();
                    }
                } else {
                    player.msg("Channel not found.");
                }
            } else {
                try {
                    channel = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException blackness) {
                    return;
                }
                if (channel <= ChannelServer.getAllInstances().size() || channel < 0) {
                    ChangeChannelHandler.changeChannel(channel, c);
                }
            }

        } else if (command.equals("clearguilds")) {
            try {
                player.msg("Attempting to reload all guilds... this may take a while...");
                cserv.getWorldInterface().clearGuilds();
                player.msg("Completed.");
            } catch (RemoteException re) {
                player.msg("RemoteException occurred while attempting to reload guilds.");
            }
        } else if (command.equals("clearPortalScripts")) {
            PortalScriptManager.getInstance().clearScripts();
        } else if (command.equals("clearReactorDrops")) {
            ReactorScriptManager.getInstance().clearDrops();
        } else if (command.equals("monsterdebug")) {
            MapleMap map = player.getMap();
            double range = Double.POSITIVE_INFINITY;
            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            List<MapleMapObject> monsters = map.getMapObjectsInRange(player.getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER));
            for (MapleMapObject monstermo : monsters) {
                MapleMonster monster = (MapleMonster) monstermo;
                player.msg("Monster " + monster.toString());
            }
        } else if (command.equals("itemperson")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int item;
            try {
                item = Integer.parseInt(splitted[2]);
            } catch (NumberFormatException blackness) {
                return;
            }
            short quantity = (short) getOptionalIntArg(splitted, 3, 1);
            if (victim != null) {
                MapleInventoryManipulator.addById(victim.getClient(), item, quantity);
            } else {
                player.msg("Player not found");
            }
        } else if (command.equals("setaccgm")) {
            int accountid;
            Connection con = DatabaseConnection.getConnection();
            try {
                PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
                ps.setString(1, splitted[1]);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    accountid = rs.getInt("accountid");
                    ps.close();
                    ps = con.prepareStatement("UPDATE accounts SET gm = ? WHERE id = ?");
                    ps.setInt(1, 1);
                    ps.setInt(2, accountid);
                    ps.executeUpdate();
                } else {
                    player.msg("Player was not found in the database.");
                }
                ps.close();
                rs.close();
            } catch (SQLException se) {
            }
        } else if (command.equals("servercheck")) {
            try {
                cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(1, "Server check will commence soon. Please @save, and log off safely.").getBytes());
            } catch (RemoteException asd) {
                cserv.reconnectWorld();
            }
        } else if (command.equals("itemvac")) {
            List<MapleMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
            for (MapleMapObject item : items) {
                MapleMapItem mapItem = (MapleMapItem) item;
                if (mapItem.getMeso() > 0) {
                    player.gainMeso(mapItem.getMeso(), true);
                } else if (mapItem.getItem().getItemId() >= 5000000 && mapItem.getItem().getItemId() <= 5000100) {
                    int petId = MaplePet.createPet(mapItem.getItem().getItemId());
                    if (petId == -1) {
                        return;
                    }
                    MapleInventoryManipulator.addById(c, mapItem.getItem().getItemId(), mapItem.getItem().getQuantity(), null, petId);
                } else {
                    MapleInventoryManipulator.addFromDrop(c, mapItem.getItem(), true);
                }
                mapItem.setPickedUp(true);
                player.getMap().removeMapObject(item);
                player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapItem.getObjectId(), 2, player.getId()), mapItem.getPosition());
            }
        } else if (command.equals("playernpc")) {
            int scriptId = Integer.parseInt(splitted[2]);
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int npcId;
            if (splitted.length != 3) {
                player.msg("Pleaase use the correct syntax. !playernpc <char name> <script name>");
            } else if (scriptId < 9901000 || scriptId > 9901319) {
                player.msg("Please enter a script name between 9901000 and 9901319");
            } else if (victim == null) {
                player.msg("The character is not in this channel");
            } else {
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("SELECT * FROM playernpcs WHERE ScriptId = ?");
                    ps.setInt(1, scriptId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        player.msg("The script id is already in use !");
                        rs.close();
                    } else {
                        rs.close();
                        ps = con.prepareStatement("INSERT INTO playernpcs (name, hair, face, skin, x, cy, map, ScriptId, Foothold, rx0, rx1, gender, dir) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                        ps.setString(1, victim.getName());
                        ps.setInt(2, victim.getHair());
                        ps.setInt(3, victim.getFace());
                        ps.setInt(4, victim.getSkinColor().getId());
                        ps.setInt(5, player.getPosition().x);
                        ps.setInt(6, player.getPosition().y);
                        ps.setInt(7, player.getMapId());
                        ps.setInt(8, scriptId);
                        ps.setInt(9, player.getMap().getFootholds().findBelow(player.getPosition()).getId());
                        ps.setInt(10, player.getPosition().x + 50);
                        ps.setInt(11, player.getPosition().x - 50);
                        ps.setInt(12, victim.getGender());
                        ps.setInt(13, player.isFacingLeft() ? 0 : 1);
                        ps.executeUpdate();
                        rs = ps.getGeneratedKeys();
                        rs.next();
                        npcId = rs.getInt(1);
                        ps.close();
                        ps = con.prepareStatement("INSERT INTO playernpcs_equip (NpcId, equipid, equippos) VALUES (?, ?, ?)");
                        ps.setInt(1, npcId);
                        for (IItem equip : victim.getInventory(MapleInventoryType.EQUIPPED)) {
                            ps.setInt(2, equip.getItemId());
                            ps.setInt(3, equip.getPosition());
                            ps.executeUpdate();
                        }
                        ps.close();
                        rs.close();

                        ps = con.prepareStatement("SELECT * FROM playernpcs WHERE ScriptId = ?");
                        ps.setInt(1, scriptId);
                        rs = ps.executeQuery();
                        rs.next();
                        PlayerNPCs pn = new PlayerNPCs(rs);
                        for (ChannelServer channel : ChannelServer.getAllInstances()) {
                            MapleMap map = channel.getMapFactory().getMap(player.getMapId());
                            map.broadcastMessage(MaplePacketCreator.spawnPlayerNPC(pn));
                            map.broadcastMessage(MaplePacketCreator.getPlayerNPC(pn));
                            map.addMapObject(pn);
                        }
                    }
                    ps.close();
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else if (command.equals("removeplayernpcs")) {
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                for (MapleMapObject object : channel.getMapFactory().getMap(player.getMapId()).getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.PLAYER_NPC))) {
                    channel.getMapFactory().getMap(player.getMapId()).removeMapObject(object);
                }
            }
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM playernpcs WHERE map = ?");
            ps.setInt(1, player.getMapId());
            ps.executeUpdate();
            ps.close();
        } else if (command.equals("pmob")) {
            int npcId = Integer.parseInt(splitted[1]);
            int mobTime = Integer.parseInt(splitted[2]);
            int xpos = player.getPosition().x;
            int ypos = player.getPosition().y;
            int fh = player.getMap().getFootholds().findBelow(player.getPosition()).getId();
            if (splitted[2] == null) {
                mobTime = 0;
            }
            MapleMonster mob = MapleLifeFactory.getMonster(npcId);
            if (mob != null && !mob.getName().equals("MISSINGNO")) {
                mob.setPosition(player.getPosition());
                mob.setCy(ypos);
                mob.setRx0(xpos + 50);
                mob.setRx1(xpos - 50);
                mob.setFh(fh);
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid, mobtime ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                    ps.setInt(1, npcId);
                    ps.setInt(2, 0);
                    ps.setInt(3, fh);
                    ps.setInt(4, ypos);
                    ps.setInt(5, xpos + 50);
                    ps.setInt(6, xpos - 50);
                    ps.setString(7, "m");
                    ps.setInt(8, xpos);
                    ps.setInt(9, ypos);
                    ps.setInt(10, player.getMapId());
                    ps.setInt(11, mobTime);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    player.msg("Failed to save MOB to the database");
                }
                player.getMap().addMonsterSpawn(mob, mobTime);
            } else {
                player.msg("You have entered an invalid Npc-Id");
            }
        } else if (command.equals("pnpc")) {
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            int xpos = player.getPosition().x;
            int ypos = player.getPosition().y;
            int fh = player.getMap().getFootholds().findBelow(player.getPosition()).getId();
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(player.getPosition());
                npc.setCy(ypos);
                npc.setRx0(xpos + 50);
                npc.setRx1(xpos - 50);
                npc.setFh(fh);
                npc.setCustom(true);
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                    ps.setInt(1, npcId);
                    ps.setInt(2, 0);
                    ps.setInt(3, fh);
                    ps.setInt(4, ypos);
                    ps.setInt(5, xpos + 50);
                    ps.setInt(6, xpos - 50);
                    ps.setString(7, "n");
                    ps.setInt(8, xpos);
                    ps.setInt(9, ypos);
                    ps.setInt(10, player.getMapId());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    player.msg("Failed to save NPC to the database");
                }
                player.getMap().addMapObject(npc);
                player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
            } else {
                player.msg("You have entered an invalid Npc-Id");
            }
        } else {
            System.out.println("Administrator Command: !" + command + " does not exist.");
        }
    }
}