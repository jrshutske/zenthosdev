package net.world;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

public class WorldServer {
    private int worldId;
    private static WorldServer instance = null;
    private Properties worldProp = new Properties();

    private WorldServer() {
        if (worldProp.isEmpty()) {
            try {
                InputStreamReader is = new FileReader("server.properties");
                worldProp.load(is);
                is.close();
            } catch (Exception e) {
                System.out.println("Could not find the configuration: " + e);
            }
        }
    }

    public synchronized static WorldServer getInstance() {
        if (instance == null) {
            instance = new WorldServer();
        }
        return instance;
    }

    public int getWorldId() {
        return worldId;
    }

    public Properties getWorldProp() {
        return worldProp;
    }

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
            registry.rebind("WorldRegistry", WorldRegistryImpl.getInstance());
        } catch (RemoteException ex) {
            System.out.println("Could not initialize RMI system: " + ex);
        }
    }
}