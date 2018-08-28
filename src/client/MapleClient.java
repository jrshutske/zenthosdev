package client;

import constants.ServerProperties;
import database.DatabaseConnection;
import database.DatabaseException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import javax.script.ScriptEngine;
import net.channel.ChannelServer;
import net.login.LoginServer;
import net.world.MapleMessengerCharacter;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.PlayerCoolDownValueHolder;
import net.world.guild.MapleGuildCharacter;
import net.world.remote.WorldChannelInterface;
import org.apache.mina.common.IoSession;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestActionManager;
import scripting.quest.QuestScriptManager;
import server.MapleInventoryManipulator;
import server.MapleTrade;
import server.PlayerInteraction.HiredMerchant;
import server.PlayerInteraction.IPlayerInteractionManager;
import server.PlayerInteraction.MaplePlayerShopItem;
import tools.IPAddressTool;
import tools.MapleAESOFB;
import tools.MaplePacketCreator;

public class MapleClient {
    private Calendar birthday = null;
    private Calendar tempban = null;
    private IoSession session;
    private Map<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();
    private MapleAESOFB receive;
    private MapleAESOFB send;
    private int timesTalked;
    private MapleCharacter player;
    private ScheduledFuture<?> idleTask = null;
    private Set<String> macs = new HashSet<String>();
    private String accountName;
    private String pic = null;
    private boolean gm = false;
    private boolean loggedIn = false;
    private boolean serverTransition = false;
    private byte greason = 1;
    private int accId = 1;
    private int attemptedLogins = 0;
    private int attemptedPic = 0;
    private int channel = 1;
    private int world;
    private int timeTalked =0;
    public static final String CLIENT_KEY = "CLIENT";
    public static final int LOGIN_LOGGEDIN = 2;
    public static final int LOGIN_NOTLOGGEDIN = 0;
    public static final int LOGIN_SERVER_TRANSITION = 1;
    public static final int LOGIN_WAITING = 3;

    public MapleClient(MapleAESOFB send, MapleAESOFB receive, IoSession session) {
        this.send = send;
        this.receive = receive;
        this.session = session;
    }

    public MapleAESOFB getReceiveCrypto() {
        return receive;
    }

    public MapleAESOFB getSendCrypto() {
        return send;
    }

    public synchronized IoSession getSession() {
        return session;
    }

    public MapleCharacter getPlayer() {
        return player;
    }
    

    public void setPlayer(MapleCharacter player) {
        this.player = player;
    }

    public void sendCharList(int server) {
        this.session.write(MaplePacketCreator.getCharList(this, server));
    }

    public List<MapleCharacter> loadCharacters(int serverId) {
        List<MapleCharacter> chars = new LinkedList<MapleCharacter>();
        for (CharNameAndId cni : loadCharactersInternal(serverId)) {
            try {
                chars.add(MapleCharacter.loadCharFromDB(cni.id, this, false));
            } catch (SQLException e) {
                System.out.println("Loading characters failed: " + e);
            }
        }
        return chars;
    }

    public List<String> loadCharacterNames(int serverId) {
        List<String> chars = new LinkedList<String>();
        for (CharNameAndId cni : loadCharactersInternal(serverId)) {
            chars.add(cni.name);
        }
        return chars;
    }

    private List<CharNameAndId> loadCharactersInternal(int serverId) {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        List<CharNameAndId> chars = new LinkedList<CharNameAndId>();
        try {
            ps = con.prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ?");
            ps.setInt(1, this.accId);
            ps.setInt(2, serverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                chars.add(new CharNameAndId(rs.getString("name"), rs.getInt("id")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.out.println("THROW: " + e);
        }
        return chars;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    private Calendar getTempBanCalendar(ResultSet rs) throws SQLException {
        Calendar lTempban = Calendar.getInstance();
        long blubb = rs.getLong("tempban");
        if (blubb == 0) {
            lTempban.setTimeInMillis(0);
            return lTempban;
        }
        Calendar today = Calendar.getInstance();
        lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
        if (today.getTimeInMillis() < lTempban.getTimeInMillis()) {
            return lTempban;
        }
        lTempban.setTimeInMillis(0);
        return lTempban;
    }
    public int getTimeTalked(){
        return timeTalked;
    }
    public void setTimeTalked(int timeTalked){
        this.timeTalked = timeTalked; 
    }

    public Calendar getTempBanCalendar() {
        return tempban;
    }

    public byte getBanReason() {
        return greason;
    }
    
    public int getTimesTalked() {
        return timesTalked;
    }

    public boolean hasBannedIP() {
        boolean ret = false;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')");
            ps.setString(1, session.getRemoteAddress().toString());
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                ret = true;
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.out.println("Error checking ip bans: " + ex);
        }
        return ret;
    }

    public boolean hasBannedMac() {
        if (macs.isEmpty()) {
            return false;
        }
        boolean ret = false;
        int i = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM macbans WHERE mac IN (");
            for (i = 0; i < macs.size(); i++) {
                sql.append("?");
                if (i != macs.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");
            PreparedStatement ps = con.prepareStatement(sql.toString());
            i = 0;
            for (String mac : macs) {
                i++;
                ps.setString(i, mac);
            }
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                ret = true;
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.out.println("Error checking mac bans: " + ex);
        }
        return ret;
    }

    private void loadMacsIfNescessary() throws SQLException {
        if (macs.isEmpty()) {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT macs FROM accounts WHERE id = ?");
            ps.setInt(1, accId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String[] macData = rs.getString("macs").split(", ");
                for (String mac : macData) {
                    if (mac.length() != 0) {
                        macs.add(mac);
                    }
                }
            } else {
                throw new RuntimeException("No valid account associated with this client.");
            }
            rs.close();
            ps.close();
        }
    }

    public void banMacs() {
        Connection con = DatabaseConnection.getConnection();
        try {
            loadMacsIfNescessary();
            List<String> filtered = new LinkedList<String>();
            PreparedStatement ps = con.prepareStatement("SELECT filter FROM macfilters");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                filtered.add(rs.getString("filter"));
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)");
            for (String mac : macs) {
                boolean matched = false;
                for (String filter : filtered) {
                    if (mac.matches(filter)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    ps.setString(1, mac);
                    try {
                        ps.executeUpdate();
                    } catch (SQLException e) {
                    }
                }
            }
            ps.close();
        } catch (SQLException e) {
            System.out.println("Error banning MACs: " + e);
        }
    }

    private void unban() {
        int i;
        try {
            Connection con = DatabaseConnection.getConnection();
            loadMacsIfNescessary();
            StringBuilder sql = new StringBuilder("DELETE FROM macbans WHERE mac IN (");
            for (i = 0; i < macs.size(); i++) {
                sql.append("?");
                if (i != macs.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");
            PreparedStatement ps = con.prepareStatement(sql.toString());
            i = 0;
            for (String mac : macs) {
                i++;
                ps.setString(i, mac);
            }
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("DELETE FROM ipbans WHERE ip LIKE CONCAT(?, '%')");
            ps.setString(1, getSession().getRemoteAddress().toString().split(":")[0]);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("UPDATE accounts SET banned = 0 WHERE id = ?");
            ps.setInt(1, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.out.println("Error while unbanning: " + e);
        }
    }

    public int finishLogin(boolean success) {
        if (success) {
            synchronized (MapleClient.class) {
                if (getLoginState() > LOGIN_NOTLOGGEDIN && getLoginState() != LOGIN_WAITING) {
                    loggedIn = false;
                    return 7;
                }
            }
            updateLoginState(LOGIN_LOGGEDIN);
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("UPDATE accounts SET LastLoginInMilliseconds = ? WHERE id = ?");
                ps.setLong(1, System.currentTimeMillis());
                ps.setInt(2, getAccID());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
            return 0;
        } else {
            return 10;
        }
    }

    public int login(String login, String pwd, boolean ipMacBanned) {
        byte loginok = 5;
        attemptedLogins++;
        if (attemptedLogins > 5) {
            session.close();
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT id, password, salt, pic, tempban, banned, gm, lastknownip, greason FROM accounts WHERE name = ?");
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int banned = rs.getInt("banned");
                accId = rs.getInt("id");
                gm = rs.getInt("gm") > 0;
                String passhash = rs.getString("password");
                String salt = rs.getString("salt");
                pic = rs.getString("pic");
                greason = rs.getByte("greason");
                tempban = getTempBanCalendar(rs);
                if (!rs.getString("lastknownip").equals(session.getRemoteAddress().toString())) {
                    PreparedStatement lkip = con.prepareStatement("UPDATE accounts SET lastknownip = ? WHERE id = ?");
                    String sockAddr = session.getRemoteAddress().toString();
                    lkip.setString(1, sockAddr.substring(1, sockAddr.lastIndexOf(':')));
                    lkip.setInt(2, accId);
                    lkip.executeUpdate();
                    lkip.close();
                }
                if (ServerProperties.isServerCheck && !gm) {
                    return 7;
                } else if (banned == 1) {
                    loginok = 3;
                } else {
                    if (banned == -1) {
                        unban();
                    }
                    if (getLoginState() > LOGIN_NOTLOGGEDIN) {
                        loggedIn = false;
                        loginok = 7;
                    } else if (passhash.equals(pwd)) {
                        loginok = 0;
                    } else {
                        boolean updatePasswordHash = false;
                        if (LoginCrypto.isLegacyPassword(passhash) && LoginCrypto.checkPassword(pwd, passhash)) {
                            loginok = 0;
                            updatePasswordHash = true;
                        } else if (salt == null && LoginCrypto.checkSha1Hash(passhash, pwd)) {
                            loginok = 0;
                            updatePasswordHash = true;
                        } else if (LoginCrypto.checkSaltedSha512Hash(passhash, pwd, salt)) {
                            loginok = 0;
                        } else {
                            loggedIn = false;
                            loginok = 4;
                        }
                        if (updatePasswordHash) {
                            PreparedStatement pss = con.prepareStatement("UPDATE `accounts` SET `password` = ?, `salt` = ? WHERE id = ?");
                            try {
                                String newSalt = LoginCrypto.makeSalt();
                                pss.setString(1, LoginCrypto.makeSaltedSha512Hash(pwd, newSalt));
                                pss.setString(2, newSalt);
                                pss.setInt(3, accId);
                                pss.executeUpdate();
                            } finally {
                                pss.close();
                            }
                        }
                    }
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.out.println("ERROR: " + e);
        }
        return loginok;
    }

    public static String getChannelServerIPFromSubnet(String clientIPAddress, int channel) {
        long ipAddress = IPAddressTool.dottedQuadToLong(clientIPAddress);
        Properties subnetInfo = LoginServer.getInstance().getSubnetInfo();
        if (subnetInfo.contains("net.login.subnetcount")) {
            int subnetCount = Integer.parseInt(subnetInfo.getProperty("net.login.subnetcount"));
            for (int i = 0; i < subnetCount; i++) {
                String[] connectionInfo = subnetInfo.getProperty("net.login.subnet." + i).split(":");
                long subnet = IPAddressTool.dottedQuadToLong(connectionInfo[0]);
                long channelIP = IPAddressTool.dottedQuadToLong(connectionInfo[1]);
                int channelNumber = Integer.parseInt(connectionInfo[2]);
                if (((ipAddress & subnet) == (channelIP & subnet)) && (channel == channelNumber)) {
                    return connectionInfo[1];
                }
            }
        }
        return "0.0.0.0";
    }

    public void updateMacs(String macData) {
        for (String mac : macData.split(", ")) {
            macs.add(mac);
        }
        StringBuilder newMacData = new StringBuilder();
        Iterator<String> iter = macs.iterator();
        while (iter.hasNext()) {
            String cur = iter.next();
            newMacData.append(cur);
            if (iter.hasNext()) {
                newMacData.append(", ");
            }
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET macs = ? WHERE id = ?");
            ps.setString(1, newMacData.toString());
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.out.println("Error saving MACs: " + e);
        }
    }

    public void setAccID(int id) {
        this.accId = id;
    }
    public void setTimesTalked(int timesTalked) {
        this.timesTalked = timesTalked;
    }

    public int getAccID() {
        return this.accId;
    }

    public void updateLoginState(int newstate) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?");
            ps.setInt(1, newstate);
            ps.setInt(2, getAccID());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.out.println("ERROR: " + e);
        }
        if (newstate == LOGIN_NOTLOGGEDIN) {
            loggedIn = false;
            serverTransition = false;
        } else if (newstate == LOGIN_WAITING) {
            loggedIn = false;
            serverTransition = false;
        } else {
            serverTransition = (newstate == LOGIN_SERVER_TRANSITION);
            loggedIn = !serverTransition;
        }
    }

    public int getLoginState() {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT loggedin, lastlogin, UNIX_TIMESTAMP(birthday) as birthday FROM accounts WHERE id = ?");
            ps.setInt(1, getAccID());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new DatabaseException("Everything sucks");
            }
            birthday = Calendar.getInstance();
            long blubb = rs.getLong("birthday");
            if (blubb > 0) {
                birthday.setTimeInMillis(blubb * 1000);
            }
            int state = rs.getInt("loggedin");
            if (state == LOGIN_SERVER_TRANSITION) {
                Timestamp ts = rs.getTimestamp("lastlogin");
                long t = ts.getTime();
                long now = System.currentTimeMillis();
                if (t + 30000 < now) {
                    state = LOGIN_NOTLOGGEDIN;
                    updateLoginState(LOGIN_NOTLOGGEDIN);
                }
            }
            rs.close();
            ps.close();
            if (state == LOGIN_LOGGEDIN) {
                loggedIn = true;
            } else {
                loggedIn = false;
            }
            return state;
        } catch (SQLException e) {
            System.out.println("ERROR: " + e);
            loggedIn = false;
            return LOGIN_NOTLOGGEDIN;
        }
    }

    public boolean checkBirthDate(Calendar date) {
        if (date.get(Calendar.YEAR) == birthday.get(Calendar.YEAR)) {
            if (date.get(Calendar.MONTH) == birthday.get(Calendar.MONTH)) {
                if (date.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void fullDisconnect() {
        this.disconnect();
        this.getSession().close();
    }

    public void disconnect() {
        MapleCharacter chr = this.getPlayer();
        if (chr != null && isLoggedIn()) {
            if (chr.getTrade() != null) {
                MapleTrade.cancelTrade(chr);
            }
            List<PlayerCoolDownValueHolder> cooldowns = chr.getAllCooldowns();
            if (cooldowns != null && cooldowns.size() > 0) {
                Connection con = DatabaseConnection.getConnection();
                for (PlayerCoolDownValueHolder cooling : cooldowns) {
                    try {
                        PreparedStatement ps = con.prepareStatement("INSERT INTO CoolDowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)");
                        ps.setInt(1, chr.getId());
                        ps.setInt(2, cooling.skillId);
                        ps.setLong(3, cooling.startTime);
                        ps.setLong(4, cooling.length);
                        ps.executeUpdate();
                        ps.close();
                    } catch (SQLException se) {
                        se.printStackTrace();
                    }
                }
            }
            chr.cancelAllBuffs();
            chr.cancelAllDebuffs();
            if (chr.getEventInstance() != null) {
                chr.getEventInstance().playerDisconnected(chr);
            }
            IPlayerInteractionManager interaction = chr.getInteraction();
            if (interaction != null) {
                if (interaction.isOwner(chr)) {
                    if (interaction.getShopType() == 1) {
                        HiredMerchant hm = (HiredMerchant) interaction;
                        hm.setOpen(true);
                    } else if (interaction.getShopType() == 2) {
                        for (MaplePlayerShopItem items : interaction.getItems()) {
                            if (items.getBundles() > 0) {
                                IItem item = items.getItem();
                                item.setQuantity(items.getBundles());
                                MapleInventoryManipulator.addFromDrop(this, item);
                            }
                        }
                        interaction.removeAllVisitors(3, 1);
                        interaction.closeShop(true);
                    } else if (interaction.getShopType() == 3 || interaction.getShopType() == 4) {
                        interaction.removeAllVisitors(3, 1);
                        interaction.closeShop(true);
                    }
                } else {
                    interaction.removeVisitor(chr);
                }
            }
            try {
                WorldChannelInterface wci = getChannelServer().getWorldInterface();
                if (chr.getMessenger() != null) {
                    MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
                    wci.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
                    chr.setMessenger(null);
                }
            } catch (RemoteException e) {
                getChannelServer().reconnectWorld();
            }
            chr.unequipAllPets();
            if (!chr.isAlive()) {
                chr.setHp(50, true);
            }
            chr.setMessenger(null);
            chr.saveToDB(true, true);
            chr.getMap().removePlayer(chr);
            try {
                WorldChannelInterface wci = getChannelServer().getWorldInterface();
                if (chr.getParty() != null) {
                    try {
                        MaplePartyCharacter chrp = new MaplePartyCharacter(chr);
                        chrp.setOnline(false);
                        wci.updateParty(chr.getParty().getId(), PartyOperation.LOG_ONOFF, chrp);
                    } catch (Exception e) {
                        System.out.println("Failed removing party character. Player already removed: " + e);
                    }
                }
                if (!this.serverTransition && isLoggedIn()) {
                    wci.loggedOff(chr.getName(), chr.getId(), channel, chr.getBuddylist().getBuddyIds());
                } else {
                    wci.loggedOn(chr.getName(), chr.getId(), channel, chr.getBuddylist().getBuddyIds());
                }
                if (chr.getGuildId() > 0) {
                    wci.setGuildMemberOnline(chr.getMGC(), false, -1);
                    int allianceId = chr.getGuild().getAllianceId();
                    if (allianceId > 0) {
                        wci.allianceMessage(allianceId, MaplePacketCreator.allianceMemberOnline(chr, false), chr.getId(), -1);
                    }
                }
            } catch (RemoteException e) {
                getChannelServer().reconnectWorld();
            } catch (Exception e) {
                System.out.println("ERROR: " + e);
            } finally {
                if (getChannelServer() != null) {
                    getChannelServer().removePlayer(chr);
                } else {
                    System.out.println("No channelserver associated to char: " + chr.getName());
                }
            }
        }
        if (!this.serverTransition && isLoggedIn()) {
            this.updateLoginState(LOGIN_NOTLOGGEDIN);
        }
        NPCScriptManager npcsm = NPCScriptManager.getInstance();
        if (npcsm != null) {
            npcsm.dispose(this);
        }
        if (QuestScriptManager.getInstance() != null) {
            QuestScriptManager.getInstance().dispose(this);
        }
    }

    public int getChannel() {
        return channel;
    }

    public ChannelServer getChannelServer() {
        return ChannelServer.getInstance(getChannel());
    }

    public boolean deleteCharacter(int cid) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT name, guildid, guildrank, allianceRank FROM characters WHERE id = ? AND accountid = ?");
            ps.setInt(1, cid);
            ps.setInt(2, accId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return false;
            }
            if (rs.getInt("guildid") > 0) {
                MapleGuildCharacter mgc = new MapleGuildCharacter(cid, 0, rs.getString("name"), -1, 0, rs.getInt("guildrank"), rs.getInt("guildid"), false, rs.getInt("allianceRank"));
                try {
                    LoginServer.getInstance().getWorldInterface().deleteGuildCharacter(mgc);
                } catch (RemoteException re) {
                    getChannelServer().reconnectWorld();
                    rs.close();
                    ps.close();
                    return false;
                }
            }
            rs.close();
            ps.close();
            String[] tables = {"eventstats", "famelog", "inventoryitems", "keymap", "queststatus", "savedlocations", "skillmacros", "skills"};
            for (byte i = 0; i < tables.length; i++) {
                ps = con.prepareStatement("DELETE FROM `" + tables[i] + "` WHERE characterid = ?");
                ps.setInt(1, cid);
                ps.executeUpdate();
                ps.close();
            }
            ps = con.prepareStatement("DELETE FROM characters WHERE id = ?");
            ps.setInt(1, cid);
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException e) {
            System.out.println("ERROR " + e);
        }
        return false;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getPic() {
        return pic;
    }

    public boolean checkPic(String pic) {
        attemptedPic++;
        if (attemptedPic > 5) {
            session.close();
        }
        return pic.equals(this.pic);
    }

    public void setPic(String pic) {
        Connection con = DatabaseConnection.getConnection();
        try {
            this.pic = pic;
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET pic = ? WHERE id = ?");
            ps.setString(1, pic);
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.out.println("SQL THROW: " + e);
        }
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getWorld() {
        return world;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void pongReceived() {
        System.currentTimeMillis();
    }

    public static int findAccIdForCharacterName(String charName) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, charName);
            ResultSet rs = ps.executeQuery();
            int ret = -1;
            if (rs.next()) {
                ret = rs.getInt("accountid");
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException e) {
            System.out.println("SQL THROW: " + e);
        }
        return -1;
    }

    public static int getAccIdFromCID(int id) {
        Connection con = DatabaseConnection.getConnection();
        int ret = -1;
        try {
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret = rs.getInt("accountid");
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException e) {
            System.out.println("SQL THROW: " + e);
        }
        return -1;
    }

    public Set<String> getMacs() {
        return Collections.unmodifiableSet(macs);
    }

    public boolean isGm() {
        return gm;
    }

    public void setScriptEngine(String name, ScriptEngine e) {
        engines.put(name, e);
    }

    public ScriptEngine getScriptEngine(String name) {
        return engines.get(name);
    }

    public void removeScriptEngine(String name) {
        engines.remove(name);
    }

    public ScheduledFuture<?> getIdleTask() {
        return idleTask;
    }

    public void setIdleTask(ScheduledFuture<?> idleTask) {
        this.idleTask = idleTask;
    }

    public NPCConversationManager getCM() {
        return NPCScriptManager.getInstance().getCM(this);
    }

    public QuestActionManager getQM() {
        return QuestScriptManager.getInstance().getQM(this);
    }

    private static class CharNameAndId {
        private String name;
        private int id;
        private CharNameAndId(String name, int id) {
            super();
            this.name = name;
            this.id = id;
        }
    }
}