package net.channel;

public interface ChannelServerMBean {
    void shutdown(int time);
    void shutdownWorld(int time);
    int getChannel();
    int getConnectedClients();
    int getLoadedMaps();
}