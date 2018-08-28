package constants;

import java.util.Properties;
import net.world.WorldServer;

public class ServerProperties {
    private static final Properties server;
    public static boolean isServerCheck;
    public static final boolean allowMoreThanOne;
    public static final boolean allowUndroppablesDrop;
    public static final boolean AutoBan;
    public static final boolean AutoRegister;
    public static final boolean CanGMItem;
    public static final boolean CStoFM;
    public static final boolean extraCommands;
    public static final boolean getMultiLevel;
    public static final boolean isGodlyItems;
    public static final boolean MTtoFM;
    public static final byte autoRegLimit;
    public static final byte getChannelLimit;
    public static final byte getFlag;
    public static final byte getMaxCharacters;
    public static final byte getRWAmount;
    public static final byte getWorldsActive;
    public static final int pvpIs;
    public static final short getGodlyItemRate;
    public static final short getItemStatMultiplier;
    public static final short getLevelCap;
    public static final short getUserLimit;
    public static final String getDBpassword;
    public static final String getDBusername;
    public static final String getIPAddress;
    public static final String getServerName;
    public static final String getWRText;
    public static final String[] getActiveEvents;
    public static int getBossDropRate;
    public static int getDropRate;
    public static int getExpRate;
    public static int getMesoRate;
    public static int getPetExpRate;
    public static String getEventMessage;
    public static String getServerMessage;

    static {
        server = WorldServer.getInstance().getWorldProp();
        allowMoreThanOne = Boolean.parseBoolean(server.getProperty("MoreThanOne", "false"));
        allowUndroppablesDrop = Boolean.parseBoolean(server.getProperty("AllDrop", "false"));
        AutoBan = Boolean.parseBoolean(server.getProperty("AutoBan", "false"));
        AutoRegister = Boolean.parseBoolean(server.getProperty("AutoRegister", "false"));
        autoRegLimit = Byte.parseByte(server.getProperty("AutoRegisterLimit", "3"));
        CanGMItem = Boolean.parseBoolean(server.getProperty("GMItems", "false"));
        CStoFM = Boolean.parseBoolean(server.getProperty("CashShop", "false"));
        extraCommands = Boolean.parseBoolean(server.getProperty("ExtraCommands", "false"));
        getActiveEvents = server.getProperty("Events", "AutoMsg").split(" | ");
        getBossDropRate = Integer.parseInt(server.getProperty("BossDrop", "1"));
        getChannelLimit = Byte.parseByte(server.getProperty("ChannelCount", "2"));
        getDBpassword = server.getProperty("dbPassword", "");
        getDBusername = server.getProperty("dbUserName", "root");
        getDropRate = Integer.parseInt(server.getProperty("Drop", "1"));
        getEventMessage = server.getProperty("EventMessage", "ZenthosDev");
        getExpRate = Integer.parseInt(server.getProperty("Exp", "100"));
        getFlag = Byte.parseByte(server.getProperty("Flag", "0"));
        getGodlyItemRate = Short.parseShort(server.getProperty("GodlyItemRate", "5"));
        getIPAddress = server.getProperty("Interface", "127.0.0.1");
        getItemStatMultiplier = Short.parseShort(server.getProperty("ItemStatMultiplier", "5"));
        getLevelCap = Short.parseShort(server.getProperty("LevelCap", "200"));
        getMaxCharacters = Byte.parseByte(server.getProperty("MaxCharacters", "6"));
        getMesoRate = Integer.parseInt(server.getProperty("Meso", "100"));
        getMultiLevel = Boolean.parseBoolean(server.getProperty("MultiLevel", "false"));
        getPetExpRate = Integer.parseInt(server.getProperty("PetExp", "1"));
        getRWAmount = Byte.parseByte(server.getProperty("RecommendedWorldNumber", "1"));
        getServerMessage = server.getProperty("ServerMessage", "..::~~Welcome to ZenthosDev~~::..");
        getServerName = server.getProperty("ServerName", "ZenthosDev");
        getUserLimit = Short.parseShort(server.getProperty("Userlimit", "150"));
        getWorldsActive = Byte.parseByte(server.getProperty("WorldsAmount", "1"));
        getWRText = server.getProperty("RecommendedWorld", "ZDev: Scania!");
        isGodlyItems = Boolean.parseBoolean(server.getProperty("GodlyItems", "false"));
        isServerCheck = Boolean.parseBoolean(server.getProperty("ServerCheck", "false"));
        MTtoFM = Boolean.parseBoolean(server.getProperty("MTS", "false"));
        pvpIs = Integer.parseInt(server.getProperty("PvPis", "4"));
    }

    public static String getProperty(String name) {
        if (server.containsKey(name)) {
            return server.getProperty(name);
        } else {
            System.out.println("Error finding the properties for: " + name + ".");
            return null;
        }
    }

    public static Properties worldServerProperties() {
        return server;
    }
}