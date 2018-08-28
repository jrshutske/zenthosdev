package client.messages.commands;

import constants.ServerProperties;
import java.util.HashMap;
import client.MapleCharacter;
import client.MapleClient;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.maps.SavedLocationType;

public class Extras {
    public static void execute(MapleClient c, String[] splitted, String command) throws Exception {
        MapleCharacter player = c.getPlayer();
        if (ServerProperties.extraCommands) {
            if (command.equals("cody")) {
                NPCScriptManager.getInstance().start(c, 9200000);
            } else if (command.equals("storage")) {
                player.getStorage().sendStorage(c, 2080005);
            } else if (command.equals("news")) {
                NPCScriptManager.getInstance().start(c, 9040011);
            } else if (command.equals("kin")) {
                NPCScriptManager.getInstance().start(c, 9900000);
            } else if (command.equals("nimakin")) {
                NPCScriptManager.getInstance().start(c, 9900001);
            } else if (command.equals("reward")) {
                NPCScriptManager.getInstance().start(c, 2050019);
            } else if (command.equals("reward1")) {
                NPCScriptManager.getInstance().start(c, 2020004);
            } else if (command.equals("fredrick")) {
                NPCScriptManager.getInstance().start(c, 9030000);
            } else if (command.equals("spinel")) {
                NPCScriptManager.getInstance().start(c, 9000020);
            } else if (command.equals("clan")) {
                NPCScriptManager.getInstance().start(c, 9201061, "ClanNPC", null);
            } else if (command.equals("banme")) {
                player.ban("ZDev| " + player.getName() + " banned him/her self.", false);
            } else if (command.equals("goafk")) {
                player.setChalkboard("I'm AFK! Drop me a message!");
            } else if (command.equals("slime")) {
                if (player.getMeso() >= 50000000) {
                    player.gainMeso(-50000000);
                    MapleInventoryManipulator.addById(c, 4001013, (byte) 1);
                } else {
                    player.dropMessage("You don't have enough mesos.");
                }
            } else if (command.equals("go")) {
                if (player.getMapId() == 980000404) {
                    player.dropMessage("You can't use this command while in Jail.");
                    return;
                }
                HashMap<String, Integer> maps = new HashMap<String, Integer>();
                maps.put("aqua", 230000000);
                maps.put("balrog", 105090900);
                maps.put("ellinia", 101000000);
                maps.put("elnath", 211000000);
                maps.put("excavation", 990000000);
                maps.put("florina", 110000000);
                maps.put("fm", 910000000);
                maps.put("griffey", 240020101);
                maps.put("guild", 200000301);
                maps.put("happy", 209000000);
                maps.put("henesys", 100000000);
                maps.put("herb", 251000000);
                maps.put("horseman", 682000001);
                maps.put("kerning", 103000000);
                maps.put("korean", 222000000);
                maps.put("leafre", 240000000);
                maps.put("lith", 104000000);
                maps.put("ludi", 220000000);
                maps.put("mall", 910000022);
                maps.put("manon", 240020401);
                maps.put("mulung", 250000000);
                maps.put("mushmom", 100000005);
                maps.put("nlc", 600000000);
                maps.put("omega", 221000000);
                maps.put("orbis", 200000000);
                maps.put("perion", 102000000);
                maps.put("showa", 801000000);
                maps.put("shrine", 800000000);
                maps.put("skelegon", 104040001);
                maps.put("sleepywood", 105040300);
                if (splitted.length != 2) {
                    StringBuilder builder = new StringBuilder("Syntax: @go <mapname>");
                    int i = 0;
                    for (String mapss : maps.keySet()) {
                        if (1 % 10 == 0) {
                            player.msg(builder.toString());
                        } else {
                            builder.append(mapss + ", ");
                        }
                    }
                    player.msg(builder.toString());
                } else if (maps.containsKey(splitted[1])) {
                    int map = maps.get(splitted[1]);
                    if (map == 910000000) {
                        player.saveLocation(SavedLocationType.FREE_MARKET);
                    }
                    player.changeMap(map);
                    player.msg("Please feel free to suggest any more locations");
                } else {
                    player.msg("I could not find the map that you requested, go get an eye test.");
                }
                maps.clear();
            } else if (command.equals("buynx")) {
                if (splitted.length != 2) {
                    player.msg("Syntax: @buynx <number>");
                    return;
                }
                int nxamount;
                try {
                    nxamount = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException asd) {
                    return;
                }
                int nxcost = 5000;
                int cost = nxamount * nxcost;
                if (nxamount > 0 && nxamount < 420000) {
                    if (player.getMeso() >= cost) {
                        player.gainMeso(-cost, true, true, true);
                        player.modifyCSPoints(1, nxamount);
                        player.msg("You spent " + cost + " mesos. You have gained " + nxamount + " nx.");
                    } else {
                        player.msg("You don't have enough mesos. 1 NX is " + nxcost + " mesos.");
                    }
                }
            } else {
                System.out.println("Player Extra Command: @" + command + " does not exist.");
            }
        } else {
            player.msg("Player Extra Commands are disable.");
        }
    }
}