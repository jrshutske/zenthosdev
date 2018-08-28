package server;

import java.rmi.RemoteException;
import java.sql.SQLException;
import database.DatabaseConnection;
import net.channel.ChannelServer;
public class ShutdownServer implements Runnable {
    private int myChannel;

    public ShutdownServer(int channel) {
        myChannel = channel;
    }

    @Override
    public void run() {
        try {
            ChannelServer.getInstance(myChannel).shutdown();
        } catch (Throwable t) {
            System.out.println("SHUTDOWN ERROR " + t);
        }
        int c = 200;
        while (ChannelServer.getInstance(myChannel).getConnectedClients() > 0 && c > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("ERROR " + e);
            }
            c--;
        }
        try {
            ChannelServer.getWorldRegistry().deregisterChannelServer(myChannel);
        } catch (RemoteException e) {
        }
        try {
            ChannelServer.getInstance(myChannel).unbind();
        } catch (Throwable t) {
            System.out.println("SHUTDOWN ERROR " + t);
        }

        boolean allShutdownFinished = true;
        for (ChannelServer cserv : ChannelServer.getAllInstances()){
            if (!cserv.hasFinishedShutdown()) {
                allShutdownFinished = false;
            }
        }
        if (allShutdownFinished) {
            TimerManager.getInstance().stop();
            try {
                DatabaseConnection.closeAll();
            } catch (SQLException e) {
                System.out.println("THROW " + e);
            }
            System.exit(0);
        }
    }
}