package net.login;

import constants.ServerProperties;
import database.DatabaseConnection;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import net.MapleServerHandler;
import net.PacketProcessor;
import net.login.remote.LoginWorldInterface;
import net.mina.MapleCodecFactory;
import net.world.remote.WorldLoginInterface;
import net.world.remote.WorldRegistry;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import server.TimerManager;

public class LoginServer implements Runnable, LoginServerMBean {
    private IoAcceptor acceptor;
    private static WorldRegistry worldRegistry = null;
    private Map<Integer, String> channelServer = new HashMap<Integer, String>();
    private LoginWorldInterface lwi;
    private WorldLoginInterface wli;
    private Boolean worldReady = Boolean.TRUE;
    private Properties subnetInfo = new Properties();
    private Map<Integer, Integer> load = new HashMap<Integer, Integer>();
    private static LoginServer instance = new LoginServer();

    static {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            mBeanServer.registerMBean(instance, new ObjectName("net.login:type=LoginServer,name=LoginServer"));
        } catch (Exception e) {
            System.out.println("MBEAN ERROR" + e);
        }
    }

    public int getPossibleLogins() {
        int ret = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement limitCheck = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE loggedin > 1 AND gm = 0");
            ResultSet rs = limitCheck.executeQuery();
            if (rs.next()) {
                int usersOn = rs.getInt(1);
                if (usersOn < ServerProperties.getUserLimit) {
                    ret = ServerProperties.getUserLimit - usersOn;
                }
            }
            rs.close();
            limitCheck.close();
        } catch (Exception ex) {
            System.out.println("loginlimit error" + ex);
        }
        return ret;
    }

    public void reconnectWorld() {
        try {
            wli.isAvailable();
        } catch (RemoteException ex) {
            synchronized (worldReady) {
                worldReady = Boolean.FALSE;
            }
            synchronized (lwi) {
                synchronized (worldReady) {
                    if (worldReady) {
                        return;
                    }
                }
                System.out.println("Reconnecting to world server");
                synchronized (wli) {
                    try {
                        Registry registry = LocateRegistry.getRegistry("localhost", Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
                        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
                        lwi = new LoginWorldInterfaceImpl();
                        wli = worldRegistry.registerLoginServer("releaselogin", lwi);
                        DatabaseConnection.getConnection();
                    } catch (Exception e) {
                        System.out.println("Reconnecting failed" + e);
                    }
                    worldReady = Boolean.TRUE;
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
            Registry registry = LocateRegistry.getRegistry("localhost", Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
            worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
            lwi = new LoginWorldInterfaceImpl();
            wli = worldRegistry.registerLoginServer("releaselogin", lwi);
            DatabaseConnection.getConnection();
        } catch (Exception e) {
            throw new RuntimeException("Could not connect to world server.", e);
        }
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        acceptor = new SocketAcceptor();
        SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
        TimerManager tMan = TimerManager.getInstance();
        tMan.start();
        tMan.register(LoginWorker.getInstance(), 500);
        tMan.register(new RankingWorker(), 3600000);
        try {
            acceptor.bind(new InetSocketAddress(8484), new MapleServerHandler(PacketProcessor.getProcessor(PacketProcessor.Mode.LOGINSERVER)), cfg);
            System.out.println("Listening on IP: " + ServerProperties.getIPAddress + " Port: 8484.");
        } catch (IOException e) {
            System.out.println("Binding to port 8484 failed: " + e);
        }
    }

    public static LoginServer getInstance() {
        return instance;
    }

    public Set<Integer> getChannels() {
        return channelServer.keySet();
    }

    public void addChannel(int channel, String ip) {
        channelServer.put(channel, ip);
        load.put(channel, 0);
    }

    public void removeChannel(int channel) {
        channelServer.remove(channel);
        load.remove(channel);
    }

    public String getIP(int channel) {
        return channelServer.get(channel);
    }

    public void shutdown() {
        System.out.println("The Server is Shutting down.");
        try {
            worldRegistry.deregisterLoginServer(lwi);
        } catch (RemoteException e) {
        }
        TimerManager.getInstance().stop();
        System.exit(0);
    }

    public WorldLoginInterface getWorldInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return wli;
    }

    public static void main(String args[]) {
        try {
            LoginServer.getInstance().run();
        } catch (Exception ex) {
            System.out.println("Error initializing loginserver" + ex);
        }
    }

    public Properties getSubnetInfo() {
        return subnetInfo;
    }

    public Map<Integer, Integer> getLoad() {
        return load;
    }

    public void setLoad(Map<Integer, Integer> load) {
        this.load = load;
    }

    @Override
    public int getNumberOfSessions() {
        return acceptor.getManagedSessions(new InetSocketAddress(8484)).size();
    }
}