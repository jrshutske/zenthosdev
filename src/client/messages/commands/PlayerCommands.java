package client.messages.commands;

import constants.ServerProperties;
import java.rmi.RemoteException;
import client.MapleCharacter;
import client.MapleClient;
import net.world.remote.WorldChannelInterface;
import client.MapleStat;
import net.channel.ChannelServer;
import scripting.npc.NPCScriptManager;
import tools.MaplePacketCreator;
import tools.StringUtil;

public class PlayerCommands {
    public static void execute(MapleClient c, String[] splitted, String command) throws Exception {
        MapleCharacter player = c.getPlayer();
        if (command.equals("command") || command.equals("commands") || command.equals("help")) {
            player.msg("================================================================");
            player.msg("                  " + ServerProperties.getServerName + " Commands");
            player.msg("================================================================");
            player.msg("@checkstat - | - Displays your stats.");
            player.msg("@save - | - Saves your progress.");
            player.msg("@expfix - | - Fixes your negative experience.");
            player.msg("@dispose - | - Unstucks you.");
            player.msg("@emo - | - Sets your HP zero.");
            player.msg("@rebirth - | - Resets your HP/MP and sets your level to 1 to be stronger.");
            player.msg("@togglesmega - | - Turn smegas OFF/ON.");
            player.msg("@str/@dex/@int/@luk <number> - | - Automatically add AP to your stats.");
            player.msg("@gm <message> - | - Sends a message to the GM's online.");
            player.msg("@revive - | - Revives anyone on the channel besides yourself.");
            player.msg("@onlinetime - | - Shows how long a person has been online.");
            if (ServerProperties.extraCommands) {
                player.msg("@cody/@storage/@news/@kin/@nimakin/@reward/@reward1/@fredrick/@spinel/@clan");
                player.msg("@banme - | - This command will ban you, SGM's will not unban you from this.");
                player.msg("@goafk - | - Uses a CB to say that you are AFK.");
                player.msg("@slime - | - For a small cost, it summons smiles for you.");
                player.msg("@go - | - Takes you to many towns and fighting areas.");
                player.msg("@buynx - | - You can purchase NX with this command.");
            }
        } else if (command.equals("checkstats")) {
            player.msg("Your stats are:");
            player.msg("Str: " + player.getStr());
            player.msg("Dex: " + player.getDex());
            player.msg("Int: " + player.getInt());
            player.msg("Luk: " + player.getLuk());
            player.msg("Available AP: " + player.getRemainingAp());
            player.msg("Rebirths: " + player.getReborns());
        } else if (command.equals("save")) {
            if (!player.getAntiCheats().Spam(900000, 1)) {
                player.saveToDB(true, true);
                player.msg("Saved.");
            } else {
                player.msg("You cannot save more than once every 15 minutes.");
            }
        } else if (command.equals("expfix")) {
            player.setExp(0);
            player.updateSingleStat(MapleStat.EXP, player.getExp());
        } else if (command.equals("dispose")) {
            NPCScriptManager.getInstance().dispose(c);
            player.msg("You have been disposed.");
        } else if (command.equals("emo")) {
            player.setHp(0);
            player.updateSingleStat(MapleStat.HP, 0);
        } else if (command.equals("rebirth") || command.equals("reborn")) {
            if (splitted.length == 2) {
                String job = splitted[1].toLowerCase();
                if (job.equals("adventures") || job.equals("cygnus") || job.equals("legend")) {
                    if (player.getLevel() >= 200) {
                        player.doReborn(job);
                        return;
                    } else {
                        player.msg("You must be at least level 200.");
                        return;
                    }
                }
            }
            player.msg("Please use either: @Reborn Adventures |OR| @Reborn Cygnus |OR| @Reborn Legend.");
        } else if (command.equals("togglesmega")) {
            if (player.getMeso() >= 10000000) {
                player.setSmegaEnabled(!player.getSmegaEnabled());
                String text = (!player.getSmegaEnabled() ? "[Disable] Smegas are now disable." : "[Enable] Smegas are now enable.");
                player.msg(text);
                player.gainMeso(-10000000, true);
            } else {
                player.msg("You need 10,000,000 mesos to toggle smegas.");
            }
        } else if (command.equals("str") || command.equals("dex") || command.equals("int") || command.equals("luk") || command.equals("hp") || command.equals("mp")) {
            if (splitted.length != 2) {
                player.msg("Syntax: @<Stat> <amount>");
                player.msg("Stat: <STR> <DEX> <INT> <LUK> <HP> <MP>");
                return;
            }
            int x = Integer.parseInt(splitted[1]), max = 30000;
            if (x > 0 && x <= player.getRemainingAp() && x < Short.MAX_VALUE) {
                if (command.equals("str") && x + player.getStr() < max) {
                    player.addAP(c, 1, x);
                } else if (command.equals("dex") && x + player.getDex() < max) {
                    player.addAP(c, 2, x);
                } else if (command.equals("int") && x + player.getInt() < max) {
                    player.addAP(c, 3, x);
                } else if (command.equals("luk") && x + player.getLuk() < max) {
                    player.addAP(c, 4, x);
                } else if (command.equals("hp") && x + player.getMaxHp() < max) {
                    player.addAP(c, 5, x);
                } else if (command.equals("mp") && x + player.getMaxMp() < max) {
                    player.addAP(c, 6, x);
                } else {
                    player.msg("Make sure the stat you are trying to raise will not be over " + Short.MAX_VALUE + ".");
                }
            } else {
                player.msg("Please make sure your AP is valid.");
            }
        } else if (command.equals("gm")) {
            if (splitted.length < 2) {
                return;
            }
            if (!player.getAntiCheats().Spam(300000, 2)) {
                try {
                    c.getChannelServer().getWorldInterface().broadcastGMMessage(null, MaplePacketCreator.serverNotice(6, "Channel: " + c.getChannel() + "  " + player.getName() + ": " + StringUtil.joinStringFrom(splitted, 1)).getBytes());
                } catch (RemoteException ex) {
                    c.getChannelServer().reconnectWorld();
                }
                player.msg("Message sent.");
            } else {
                player.dropMessage(1, "Please don't flood GMs with your messages.");
            }
        } else if (command.equals("revive")) {
            if (splitted.length == 2) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (player != victim) {
                    if (player.getMeso() >= 50000000) {
                        if (victim != null) {
                            if (!victim.isAlive()) {
                                victim.setHp((victim.getMaxHp() / 2));
                                player.gainMeso(-50000000);
                                victim.updateSingleStat(MapleStat.HP, (victim.getMaxHp() / 2));
                                player.msg("You have revived " + victim.getName() + ".");
                            } else {
                                player.msg(victim.getName() + " is not dead.");
                            }
                        } else {
                            player.msg("The player is not online.");
                        }
                    } else {
                        player.msg("You need 50 million mesos to do this.");
                    }
                } else {
                    player.msg("You can't revive yourself.");
                }
            } else {
                player.msg("Syntax: @revive <player name>");
            }
        } else if (command.equals("onlinetime")) {
            if (splitted.length >= 2) {
                String name = splitted[1];
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                if (victim == null) {
                    try {
                        WorldChannelInterface wci = c.getChannelServer().getWorldInterface();
                        int channel = wci.find(name);
                        if (channel == -1 || victim.isGM()) {
                            player.msg("This player is not online.");
                            return;
                        }
                        victim = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(name);
                    } catch (RemoteException re) {
                        c.getChannelServer().reconnectWorld();
                    }
                }
                long blahblah = System.currentTimeMillis() - victim.getLastLogin();
                StringBuilder sb = new StringBuilder();
                sb.append(victim.getName());
                sb.append(" has been online for");
                compareTime(sb, blahblah);
                player.msg(sb.toString());
            } else {
                player.msg("Incorrect Syntax.");
            }
        } else {
            System.out.println("Player Command: @" + command + " does not exist.");
        }
    }

    private static void compareTime(StringBuilder sb, long timeDiff) {
        double secondsAway = timeDiff / 1000;
        double minutesAway = 0;
        double hoursAway = 0;
        while (secondsAway > 60) {
            minutesAway++;
            secondsAway -= 60;
        }
        while (minutesAway > 60) {
            hoursAway++;
            minutesAway -= 60;
        }
        boolean hours = false;
        boolean minutes = false;
        if (hoursAway > 0) {
            sb.append(" ");
            sb.append((int) hoursAway);
            sb.append(" hours");
            hours = true;
        }
        if (minutesAway > 0) {
            if (hours) {
                sb.append(" -");
            }
            sb.append(" ");
            sb.append((int) minutesAway);
            sb.append(" minutes");
            minutes = true;
        }
        if (secondsAway > 0) {
            if (minutes) {
                sb.append(" and");
            }
            sb.append(" ");
            sb.append((int) secondsAway);
            sb.append(" seconds !");
        }
    }
}