package net;

import client.MapleClient;
import net.channel.ChannelServer;
import net.login.LoginWorker;
import tools.MapleAESOFB;
import tools.MaplePacketCreator;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

public class MapleServerHandler extends IoHandlerAdapter {
    private final static byte MAPLE_VERSION = 83;
    private PacketProcessor processor;
    private int channel = -1;

    public MapleServerHandler(PacketProcessor processor) {
        this.processor = processor;
    }

    public MapleServerHandler(PacketProcessor processor, int channel) {
        this.processor = processor;
        this.channel = channel;
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        Runnable r = ((MaplePacket) message).getOnSend();
        if (r != null) {
            r.run();
        }
        super.messageSent(session, message);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        System.out.println("Exception Caught: " + cause.getMessage());
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        if (channel > -1) {
            if (ChannelServer.getInstance(channel).isShutdown()) {
                session.close();
                return;
            }
        }
        byte key[] = {0x13, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, (byte) 0xB4, 0x00, 0x00, 0x00, 0x1B, 0x00, 0x00, 0x00, 0x0F, 0x00, 0x00, 0x00, 0x33, 0x00, 0x00, 0x00, 0x52, 0x00, 0x00, 0x00};
        byte ivRecv[] = {70, 114, 122, 82};
        byte ivSend[] = {82, 48, 120, 115};
        ivRecv[3] = (byte) (Math.random() * 255);
        ivSend[3] = (byte) (Math.random() * 255);
        MapleAESOFB sendCypher = new MapleAESOFB(key, ivSend, (short) (0xFFFF - MAPLE_VERSION));
        MapleAESOFB recvCypher = new MapleAESOFB(key, ivRecv, MAPLE_VERSION);
        MapleClient client = new MapleClient(sendCypher, recvCypher, session);
        client.setChannel(channel);
        session.write(MaplePacketCreator.getHello(MAPLE_VERSION, ivSend, ivRecv));
        session.setAttribute(MapleClient.CLIENT_KEY, client);
        session.setIdleTime(IdleStatus.READER_IDLE, 30);
        session.setIdleTime(IdleStatus.WRITER_IDLE, 30);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        synchronized (session) {
            MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
            if (client != null) {
                client.disconnect();
                LoginWorker.getInstance().deregisterClient(client);
                session.removeAttribute(MapleClient.CLIENT_KEY);
            }
        }
        super.sessionClosed(session);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        byte[] content = (byte[]) message;
        SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream(content));
        short packetId = slea.readShort();
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        MaplePacketHandler packetHandler = processor.getHandler(packetId);
        if (packetHandler != null && packetHandler.validateState(client)) {
            try {
                packetHandler.handlePacket(slea, client);
            } catch (Throwable t) {
                if (!packetHandler.getClass().getName().equals("net.channel.handler.QuestActionHandler")) {
                    System.out.println("Exception during processing packet: " + packetHandler.getClass().getName() + ": " + t.getMessage());
                }
            }
        }
    }

    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (client == null) {
            System.out.println("Auto DC : " + session.getRemoteAddress().toString() + " : Client is inactive.");
            session.close();
        }
        super.sessionIdle(session, status);
    }
}