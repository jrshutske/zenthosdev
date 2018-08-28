package net.channel;

import client.MapleCharacter;
import constants.ServerProperties;
import database.DatabaseConnection;
import java.awt.Point;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import net.MaplePacket;
import net.MapleServerHandler;
import net.PacketProcessor;
import net.channel.remote.ChannelWorldInterface;
import net.mina.MapleCodecFactory;
import net.world.MapleParty;
import net.world.guild.MapleGuild;
import net.world.guild.MapleGuildCharacter;
import net.world.guild.MapleGuildSummary;
import net.world.remote.WorldChannelInterface;
import net.world.remote.WorldRegistry;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import provider.MapleWZProvider;
import scripting.event.EventScriptManager;
import server.ClanHolder;
import server.MapleSquad;
import server.MapleSquadType;
import server.MapleTrade;
import server.PlayerInteraction.HiredMerchant;
import server.ShutdownServer;
import server.TimerManager;
import server.maps.FakeCharacter;
import server.maps.MapleMapFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;

public class ChannelServer implements Runnable, ChannelServerMBean {
    private boolean finishedShutdown = false;
    private boolean shutdown = false;
    private Boolean worldReady = true;
    private ChannelWorldInterface cwi;
    private ClanHolder clans = new ClanHolder();
    private Collection<FakeCharacter> clones = new LinkedList<FakeCharacter>();;
    private EventScriptManager eventSM;
    private final MapleMapFactory mapFactory;
    private int channel;
    private int port = 7575;
    private IoAcceptor acceptor;
    private Map<Integer, MapleGuildSummary> gsStore = new HashMap<Integer, MapleGuildSummary>();
    private Map<MapleSquadType, MapleSquad> mapleSquads = new HashMap<MapleSquadType, MapleSquad>();
    private PlayerStorage players = new PlayerStorage();
    private static int uniqueID = 1;
    private static Map<Integer, ChannelServer> instances = new HashMap<Integer, ChannelServer>();
    private static Map<String, ChannelServer> pendingInstances = new HashMap<String, ChannelServer>();
    private static WorldRegistry worldRegistry;
    private String arrayString = "";
    private String ip;
    private String key;
    private WorldChannelInterface wci = null;

    private ChannelServer(String key) {
        mapFactory = new MapleMapFactory(MapleWZProvider.mapWZ, MapleWZProvider.stringWZ);
        this.key = key;
    }

    public static WorldRegistry getWorldRegistry() {
        return worldRegistry;
    }

    public ChannelServer getThis() {
        return this;
    }

    public void reconnectWorld() {
        try {
            wci.isAvailable();
        } catch (RemoteException ex) {
            synchronized (worldReady) {
                worldReady = false;
            }
            synchronized (cwi) {
                synchronized (worldReady) {
                    if (worldReady) {
                        return;
                    }
                }
                System.out.println("Reconnecting to world server.");
                synchronized (wci) {
                    try {
                        Registry registry = LocateRegistry.getRegistry("localhost", Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
                        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
                        cwi = new ChannelWorldInterfaceImpl(this);
                        wci = worldRegistry.registerChannelServer(key, cwi);
                        DatabaseConnection.getConnection();
                        wci.serverReady();
                    } catch (Exception e) {
                        System.out.println("Reconnecting failed" + e);
                    }
                    worldReady = true;
                }
            }
            synchronized (worldReady) {
                worldReady.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        try {
            cwi = new ChannelWorldInterfaceImpl(this);
            wci = worldRegistry.registerChannelServer(key, cwi);
            eventSM = new EventScriptManager(this, ServerProperties.getActiveEvents);
            DatabaseConnection.getConnection();
            Connection c = DatabaseConnection.getConnection();
            PreparedStatement ps;
            try {
                ps = c.prepareStatement("UPDATE accounts SET loggedin = 0");
                ps.executeUpdate();
                ps = c.prepareStatement("UPDATE characters SET HasMerchant = 0");
                ps.executeUpdate();
                ps.close();
            } catch (SQLException ex) {
                System.out.println("Could not reset databases" + ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        port = 7575 + channel - 1;
        ip = ServerProperties.getIPAddress + ":" + port;
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        acceptor = new SocketAcceptor();
        SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
        TimerManager.getInstance().start();
        try {
            MapleServerHandler serverHandler = new MapleServerHandler(PacketProcessor.getProcessor(PacketProcessor.Mode.CHANNELSERVER), channel);
            acceptor.bind(new InetSocketAddress(port), serverHandler, cfg);
            System.out.println("[Channel] " + getChannel() + " Port: " + port + " is online.");
            wci.serverReady();
            eventSM.init();
        } catch (IOException e) {
            System.out.println("Binding to port " + port + " failed (ch: " + getChannel() + ")" + e);
        }
    }

    public void shutdown() {
        shutdown = true;
        List<CloseFuture> futures = new LinkedList<CloseFuture>();
        Collection<MapleCharacter> allchars = players.getAllCharacters();
        MapleCharacter chrs[] = allchars.toArray(new MapleCharacter[allchars.size()]);
        for (MapleCharacter chr : chrs) {
            if (chr.getTrade() != null) {
                MapleTrade.cancelTrade(chr);
            }
            if (chr.getEventInstance() != null) {
                chr.getEventInstance().playerDisconnected(chr);
            }
            chr.saveToDB(true, true);
            removePlayer(chr);
        }
        for (MapleCharacter chr : chrs) {
            futures.add(chr.getClient().getSession().close());
        }
        for (CloseFuture future : futures) {
            future.join(500);
        }
        finishedShutdown = true;
        wci = null;
        cwi = null;
    }

    public static void main(String args[]) throws FileNotFoundException, IOException, NotBoundException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException {
        Registry registry = LocateRegistry.getRegistry("localhost", Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
        for (int i = 0; i < ServerProperties.getChannelLimit; i++) {
            newInstance("release" + (i + 1)).run();
        }
        DatabaseConnection.getConnection();
        ClanHolder.loadAllClans();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                for (ChannelServer channel : getAllInstances()) {
                    for (int i = 910000001; i <= 910000022; i++) {
                        for (MapleMapObject obj : channel.getMapFactory().getMap(i).getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.HIRED_MERCHANT))) {
                            HiredMerchant hm = (HiredMerchant) obj;
                            hm.closeShop(true);
                        }
                    }
                    for (MapleCharacter mc : channel.getPlayerStorage().getAllCharacters()) {
                        mc.saveToDB(true, true);
                    }
                }
            }
        });
    }

    public static ChannelServer newInstance(String key) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException {
        ChannelServer instance = new ChannelServer(key);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(instance, new ObjectName("net.channel:type=ChannelServer,name=ChannelServer" + uniqueID++));
        pendingInstances.put(key, instance);
        return instance;
    }

    public void unbind() {
        acceptor.unbindAll();
    }

    public boolean hasFinishedShutdown() {
        return finishedShutdown;
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public static ChannelServer getInstance(int channel) {
        return instances.get(channel);
    }

    public void addPlayer(MapleCharacter chr) {
        players.registerPlayer(chr);
        if (chr.getClan() > -1) {
            clans.playerOnline(chr);
        }
    }

    public IPlayerStorage getPlayerStorage() {
        return players;
    }

    public void removePlayer(MapleCharacter chr) {
        players.deregisterPlayer(chr);
        if (chr.getClan() > -1) {
            clans.deregisterPlayer(chr);
        }
    }

    public void addToClan(MapleCharacter chr) {
        clans.registerPlayer(chr);
    }

    public ClanHolder getClanHolder() {
        return clans;
    }

    public int getConnectedClients() {
        return players.getAllCharacters().size();
    }

    public void broadcastPacket(MaplePacket data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            chr.getClient().getSession().write(data);
        }
    }

    public String getArrayString() {
        return arrayString;
    }

    public void setArrayString(String newStr) {
        arrayString = newStr;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        if (pendingInstances.containsKey(key)) {
            pendingInstances.remove(key);
        }
        if (instances.containsKey(channel)) {
            instances.remove(channel);
        }
        instances.put(channel, this);
        this.channel = channel;
        this.mapFactory.setChannel(channel);
    }

    public static Collection<ChannelServer> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public String getIP() {
        return ip;
    }

    public String getIP(int channel) {
        try {
            return getWorldInterface().getIP(channel);
        } catch (RemoteException e) {
            System.out.println("Lost connection to world server" + e);
            throw new RuntimeException("Lost connection to world server");
        }
    }

    public WorldChannelInterface getWorldInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return wci;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public void shutdown(int time) {
        broadcastPacket(MaplePacketCreator.serverNotice(0, "The world will be shut down in " + (time / 60000) + " minutes, please log off safely."));
        TimerManager.getInstance().schedule(new ShutdownServer(getChannel()), time);
    }

    @Override
    public void shutdownWorld(int time) {
        try {
            getWorldInterface().shutdown(time);
        } catch (RemoteException e) {
            reconnectWorld();
        }
    }

    public int getLoadedMaps() {
        return mapFactory.getLoadedMaps();
    }

    public EventScriptManager getEventSM() {
        return eventSM;
    }

    public void reloadEvents() {
        eventSM.cancel();
        eventSM = new EventScriptManager(this, ServerProperties.getActiveEvents);
        eventSM.init();
    }

    public MapleGuild getGuild(MapleGuildCharacter mgc) {
        int gid = mgc.getGuildId();
        MapleGuild g = null;
        try {
            g = this.getWorldInterface().getGuild(gid, mgc);
        } catch (RemoteException re) {
            System.out.println("RemoteException while fetching MapleGuild." + re);
            return null;
        }
        if (gsStore.get(gid) == null) {
            gsStore.put(gid, new MapleGuildSummary(g));
        }
        return g;
    }

    public MapleGuildSummary getGuildSummary(int gid) {
        if (gsStore.containsKey(gid)) {
            return gsStore.get(gid);
        } else {
            try {
                MapleGuild g = this.getWorldInterface().getGuild(gid, null);
                if (g != null) {
                    gsStore.put(gid, new MapleGuildSummary(g));
                }
                return gsStore.get(gid);
            } catch (RemoteException re) {
                System.out.println("RemoteException while fetching GuildSummary." + re);
                return null;
            }
        }
    }

    public void updateGuildSummary(int gid, MapleGuildSummary mgs) {
        gsStore.put(gid, mgs);
    }

    public void reloadGuildSummary() {
        try {
            MapleGuild g;
            for (int i : gsStore.keySet()) {
                g = this.getWorldInterface().getGuild(i, null);
                if (g != null) {
                    gsStore.put(i, new MapleGuildSummary(g));
                } else {
                    gsStore.remove(i);
                }
            }
        } catch (RemoteException re) {
            System.out.println("RemoteException while reloading GuildSummary: " + re);
        }
    }

    public MapleSquad getMapleSquad(MapleSquadType type) {
        return mapleSquads.get(type);
    }

    public boolean addMapleSquad(MapleSquad squad, MapleSquadType type) {
        if (mapleSquads.get(type) == null) {
            mapleSquads.remove(type);
            mapleSquads.put(type, squad);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeMapleSquad(MapleSquad squad, MapleSquadType type) {
        if (mapleSquads.containsKey(type)) {
            if (mapleSquads.get(type) == squad) {
                mapleSquads.remove(type);
                return true;
            }
        }
        return false;
    }

    public void broadcastSMega(MaplePacket data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            if (chr.getSmegaEnabled()) {
                chr.getClient().getSession().write(data);
            }
        }
    }

    public void broadcastGMPacket(MaplePacket data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            if (chr.isGM()) {
                chr.getClient().getSession().write(data);
            }
        }
    }

    public void broadcastToClan(MaplePacket data, int clan) {
        for (MapleCharacter chr : clans.getAllOnlinePlayersFromClan(clan)) {
            chr.getClient().getSession().write(data);
        }
    }

    public int onlineClanMembers(int clan) {
        return clans.countOnlineByClan(clan);
    }

    public List<MapleCharacter> getPartyMembers(MapleParty party) {
        List<MapleCharacter> partym = new LinkedList<MapleCharacter>();
        for (net.world.MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getChannel()) {
                MapleCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;
    }

    public void addClone(FakeCharacter fc) {
        clones.add(fc);
    }

    public void removeClone(FakeCharacter fc) {
        clones.remove(fc);
    }

    public Collection<FakeCharacter> getAllClones() {
        return clones;
    }
}