package client.messages;

import client.MapleClient;
import client.messages.commands.Admins;
import client.messages.commands.Donator;
import client.messages.commands.Extras;
import client.messages.commands.GM;
import client.messages.commands.PlayerCommands;

public class CommandProcessor {
    private static CommandProcessor instance = new CommandProcessor();
    private final char SGM = '!', Player = '@';

    public static CommandProcessor getInstance() {
        return instance;
    }

    private CommandProcessor() {
        instance = this;
    }

    public boolean isCommand(MapleClient c, String line, char charZero, boolean isGM) {
        String[] splitted = line.split(" ");
        String command = splitted[0].substring(1).toLowerCase();
        if (charZero != SGM && charZero != Player) {
            return false;
        }
        if (splitted[0].length() < 2) {
            return false;
        }
        if (splitted[0].charAt(1) == SGM) {
            return false;
        }
        if (splitted[0].charAt(1) == Player) {
            return false;
        }
        if (!Command.isCommand(command, charZero, isGM)) {
            return false;
        }
        if (c.getPlayer().getAntiCheats().Spam(1000, 7)) {
            c.getPlayer().dropMessage(1, "Please try again later.");
            return true;
        }
        return instance.processCommand(c, splitted, command, charZero);
    }

    private boolean processCommand(MapleClient c, String[] splitted, String command, char charZero) {
        int cGML = c.getPlayer().getGMLevel();
        try {
            if (charZero == SGM) {
                if (Command.findCommand(4, command, cGML, 3)) {
                    if (c.getAccID() != 1) {
                        System.out.println("Notice: " + c.getPlayer().getName() + " used a command: !" + command);
                    }
                    Admins.execute(c, splitted, command);
                    return true;
                } else if (Command.findCommand(3, command, cGML, 2)) {
                    GM.execute(c, splitted, command);
                    return true;
                } else if (Command.findCommand(2, command, cGML, 1)) {
                    Donator.execute(c, splitted, command);
                    return true;
                } else {
                    return false;
                }
            } else {
                if (Command.findCommand(1, command, cGML, 0)) {
                    Extras.execute(c, splitted, command);
                    return true;
                } else if (Command.findCommand(0, command, cGML, 0)) {
                    PlayerCommands.execute(c, splitted, command);
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            System.out.println("Command Error: " + charZero + command + ": " + e);
        }
        return false;
    }
}