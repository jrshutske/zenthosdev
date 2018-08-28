package net.channel.handler;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import client.BuddylistEntry;
import client.CharacterNameAndId;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.SkillFactory;
import constants.ServerProperties;
import constants.skills.GM;
import database.DatabaseConnection;
import net.AbstractMaplePacketHandler;
import net.channel.ChannelServer;
import net.world.CharacterIdChannelPair;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.PlayerBuffValueHolder;
import net.world.PlayerCoolDownValueHolder;
import net.world.guild.MapleAlliance;
import net.world.remote.WorldChannelInterface;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class PlayerLoggedinHandler extends AbstractMaplePacketHandler {

    @Override
    public boolean validateState(MapleClient c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int cid = slea.readInt();
        MapleCharacter player = null;
        try {
            player = MapleCharacter.loadCharFromDB(cid, c, true);
            c.setPlayer(player);
        } catch (SQLException e) {
            System.out.println("Loading the char failed: " + e);
        }
        c.setAccID(player.getAccountID());
        int state = c.getLoginState();
        boolean allowLogin = true;
        ChannelServer channelServer = c.getChannelServer();
        synchronized (this) {
            try {
                WorldChannelInterface worldInterface = channelServer.getWorldInterface();
                if (state == MapleClient.LOGIN_SERVER_TRANSITION) {
                    for (String charName : c.loadCharacterNames(c.getWorld())) {
                        int worldLoc = worldInterface.find(charName);
                        if (worldLoc > -1) {
                            ChannelServer cserv = ChannelServer.getInstance(worldLoc);
                            MapleCharacter dPlayer = cserv.getPlayerStorage().getCharacterByName(charName);
                            dPlayer.getMap().removePlayer(dPlayer);
                            cserv.removePlayer(dPlayer);
                            break;
                        }
                    }
                }
            } catch (RemoteException e) {
                channelServer.reconnectWorld();
                allowLogin = false;
            }
            if (state != MapleClient.LOGIN_SERVER_TRANSITION || !allowLogin) {
                c.setPlayer(null);
                c.getSession().close();
                return;
            }
            c.updateLoginState(MapleClient.LOGIN_LOGGEDIN);
        }
        ChannelServer cserv = ChannelServer.getInstance(c.getChannel());
        cserv.addPlayer(player);
        try {
            WorldChannelInterface wci = ChannelServer.getInstance(c.getChannel()).getWorldInterface();
            List<PlayerBuffValueHolder> buffs = wci.getBuffsFromStorage(cid);
            if (buffs != null) {
                c.getPlayer().silentGiveBuffs(buffs);
            }
            List<PlayerCoolDownValueHolder> cooldowns = wci.getCooldownsFromStorage(cid);
            if (cooldowns != null) {
                c.getPlayer().giveCoolDowns(cooldowns);
            }
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM CoolDowns WHERE charid = ?");
            ps.setInt(1, c.getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getLong("length") + rs.getLong("StartTime") - System.currentTimeMillis() <= 0) {
                    continue;
                }
                c.getPlayer().giveCoolDowns(rs.getInt("SkillID"), rs.getLong("StartTime"), rs.getLong("length"));
            }
            ps = con.prepareStatement("DELETE FROM CoolDowns WHERE charid = ?");
            ps.setInt(1, c.getPlayer().getId());
            ps.executeUpdate();
            rs.close();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
        c.getSession().write(MaplePacketCreator.getCharInfo(player));
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages WHERE RecieverId = ? and checked = 1");
            ps.setInt(1, c.getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                DueyHandler.reciveMsg(c, c.getPlayer().getId());
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
        if (player.isGM()) {
//            SkillFactory.getSkill(GM.HIDE).getEffect(1).applyTo(player);
//            SkillFactory.getSkill(GM.HASTE).getEffect(1).applyTo(player);
        }
        c.getSession().write(MaplePacketCreator.serverMessage(ServerProperties.getServerMessage));
        player.getMap().addPlayer(player);
        try {
            Collection<BuddylistEntry> buddies = player.getBuddylist().getBuddies();
            int buddyIds[] = player.getBuddylist().getBuddyIds();
            cserv.getWorldInterface().loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
            if (player.getParty() != null) {
                channelServer.getWorldInterface().updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
            }
            CharacterIdChannelPair[] onlineBuddies = cserv.getWorldInterface().multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                BuddylistEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
                ble.setChannel(onlineBuddy.getChannel());
                player.getBuddylist().put(ble);
            }
            c.getSession().write(MaplePacketCreator.updateBuddylist(buddies));
            c.getPlayer().sendMacros();
            try {
                c.getPlayer().showNote();
            } catch (SQLException e) {
            }
            if (player.getGuildId() > 0) {
                c.getChannelServer().getWorldInterface().setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.getSession().write(MaplePacketCreator.showGuildInfo(player));
                int allianceId = player.getGuild().getAllianceId();
                if (allianceId > 0) {
                    MapleAlliance newAlliance = channelServer.getWorldInterface().getAlliance(allianceId);
                    if (newAlliance == null) {
                        newAlliance = MapleAlliance.loadAlliance(allianceId);
                        channelServer.getWorldInterface().addAlliance(allianceId, newAlliance);
                    }
                    c.getSession().write(MaplePacketCreator.getAllianceInfo(newAlliance));
                    c.getSession().write(MaplePacketCreator.getGuildAlliances(newAlliance, c));
                    c.getChannelServer().getWorldInterface().allianceMessage(allianceId, MaplePacketCreator.allianceMemberOnline(player, true), player.getId(), -1);
                }
            }
        } catch (RemoteException e) {
            channelServer.reconnectWorld();
        }
        player.updatePartyMemberHP();
        player.sendKeymap();
        for (MapleQuestStatus status : player.getStartedQuests()) {
            if (status.hasMobKills()) {
                c.getSession().write(MaplePacketCreator.updateQuestMobKills(status));
            }
        }
        CharacterNameAndId pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            player.getBuddylist().put(new BuddylistEntry(pendingBuddyRequest.getName(), "ZDev Group", pendingBuddyRequest.getId(), -1, false));
            c.getSession().write(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), c.getPlayer().getId(), pendingBuddyRequest.getName()));
        }
        player.checkMessenger();
        player.checkBerserk();
    }
}