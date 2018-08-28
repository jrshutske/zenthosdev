package client.messages;

public class Command {
    private final static String x = ",";
    private final static String getAdminsCommands =
        "speakall," +
        "systemsay," +
        "dcall," +
        "killnear," +
        "drop," +
        "closemerchants," +
        "shutdown," +
        "shutdownworld," +
        "shutdownnow," +
        "setrebirths," +
        "mesoperson," +
        "gmperson," +
        "kill," +
        "jobperson," +
        "threads," +
        "showtrace," +
        "shopitem," +
        "pnpc," +
        "tdrops," +
        "givemonsbuff," +
        "givemonstatus," +
        "sreactor," +
        "hreactor," +
        "lreactor," +
        "dreactor," +
        "saveall," +
        "notice," +
        "strip," +
        "speak," +
        "changechannel," +
        "clearguilds," +
        "clearPortalScripts," +
        "clearReactorDrops," +
        "monsterdebug," +
        "itemperson," +
        "setaccgm," +
        "servercheck," +
        "itemvac," +
        "playernpc," +
        "removeplayernpcs," +
        "pmob," +
        "pnpc"
    ;

    private final static String getGMCommands =
        "lowhp," +
        "sp," +
        "ap," +
        "job," +
        "whereami," +
        "shop," +
        "opennpc," +
        "levelup," +
        "setmaxmp," +
        "setmaxhp," +
        "healmap," +
        "item," +
        "noname," +
        "dropmesos," +
        "level," +
        "online," +
        "banreason," +
        "joinguild," +
        "unbuffmap," +
        "mesos," +
        "setname," +
        "clearslot," +
        "ariantpq," +
        "array," +
        "slap," +
        "rreactor," +
        "coke," +
        "papu," +
        "zakum," +
        "ergoth," +
        "ludimini," +
        "cornian," +
        "balrog," +
        "mushmom," +
        "wyvern," +
        "pirate," +
        "clone," +
        "anego," +
        "theboss," +
        "snackbar," +
        "papapixie," +
        "nxslimes," +
        "horseman," +
        "blackcrow," +
        "leafreboss," +
        "shark," +
        "franken," +
        "bird," +
        "pianus," +
        "centipede," +
        "horntail," +
        "killall," +
        "say," +
        "gender," +
        "spy," +
        "levelperson," +
        "skill," +
        "setall," +
        "giftnx," +
        "maxskills," +
        "fame," +
        "unhide," +
        "heal," +
        "unbuff," +
        "sendhint," +
        "smega," +
        "mutesmega," +
        "mute," +
        "givedisease," +
        "dc," +
        "charinfo," +
        "connected," +
        "clock," +
        "warp," +
        "warphere," +
        "jail," +
        "map," +
        "warpallhere," +
        "warpwholeworld," +
        "mesosrate," +
        "droprate," +
        "bossdroprate," +
        "exprate," +
        "servermessage," +
        "whosthere," +
        "getrings," +
        "ring," +
        "removering," +
        "nearestPortal," +
        "unban," +
        "spawn," +
        "ban," +
        "checktokens," +
        "addtokens," +
        "tempban," +
        "search," +
        "msearch," +
        "npc," +
        "removenpcs," +
        "mynpcpos," +
        "cleardrops," +
        "clearshops," +
        "clearevents," +
        "permban," +
        "emote," +
        "proitem," +
        "addclones," +
        "removeclones," +
        "removeallclones," +
        "follow," +
        "pause," +
        "stance," +
        "killmonster," +
        "removeoid," +
        "gmtext," +
        "currentdate," +
        "maxmesos," +
        "fullcharge," +
        "youlose"
    ;

    private final static String getDonatorCommands =
        "buffme," +
        "goto," +
        "sexchange," +
        "storage"
    ;

    private final static String getExtrasCommands =
        "cody," +
        "storage," +
        "news," +
        "kin," +
        "nimakin," +
        "reward," +
        "reward1," +
        "fredrick," +
        "spinel," +
        "clan," +
        "banme," +
        "goafk," +
        "slime," +
        "go," +
        "buynx"
    ;

    private final static String getPlayersCommands =
        "command," +
        "commands," +
        "help," +
        "checkstats," +
        "save," +
        "expfix," +
        "dispose," +
        "emo," +
        "rebirth," +
        "reborn," +
        "togglesmega," +
        "str," +
        "dex," +
        "int," +
        "luk," +
        "hp," +
        "mp," +
        "gm," +
        "revive," +
        "onlinetime"
    ;

    public static boolean isCommand(String command, char charZero, boolean isGM) {
        String[] commandList;
        if (charZero == '@') {
            commandList = (getPlayersCommands + x + getExtrasCommands).split(x);
        } else {
            commandList = isGM ? (getDonatorCommands + x + getGMCommands + x + getAdminsCommands).split(x) : getDonatorCommands.split(x);
        }
        for (int i = 0; i < commandList.length; i++) {
            if (commandList[i].equals(command)) {
                return true;
            }
        }
        return false;
    }

    public static boolean findCommand(int type, String command, int cGML, int reqLevel) {
        if (cGML >= reqLevel && cGML > -1 && cGML < 4) {
            String[] commandList;
            if (type == 0) {
                commandList = getPlayersCommands.split(x);
            } else if (type == 1) {
                commandList = getExtrasCommands.split(x);
            } else if (type == 2) {
                commandList = getDonatorCommands.split(x);
            } else if (type == 3) {
                commandList = getGMCommands.split(x);
            } else if (type == 4) {
                commandList = getAdminsCommands.split(x);
            } else {
                return false;
            }
            for (int i = 0; i < commandList.length; i++) {
                if (commandList[i].equals(command)) {
                    return true;
                }
            }
        }
        return false;
    }
}