package net.channel.handler;

import java.util.List;
import client.MapleCharacter;
import client.MapleClient;
import net.MaplePacket;
import server.maps.FakeCharacter;
import server.TimerManager;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class MovePlayerHandler extends AbstractMovementPacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(9);
        final List<LifeMovementFragment> res = parseMovement(slea);
        if (res != null) {
            if (slea.available() != 18) {
                return;
            }
            MapleCharacter player = c.getPlayer();
            MaplePacket packet = MaplePacketCreator.movePlayer(player.getId(), res);
            if (!player.isHidden()) {
                c.getPlayer().getMap().broadcastMessage(player, packet, false);
            } else {
                c.getPlayer().getMap().broadcastGMMessage(player, packet, false);
            }
            updatePosition(res, c.getPlayer(), 0);
            c.getPlayer().getMap().movePlayer(c.getPlayer(), c.getPlayer().getPosition());
            if (c.getPlayer().hasFakeChar()) {
                int i = 1;
                for (final FakeCharacter ch : c.getPlayer().getFakeChars()) {
                    if (ch.follow() && ch.getFakeChar().getMap() == player.getMap()) {
                        TimerManager.getInstance().schedule(new Runnable() {

                            @Override
                            public void run() {
                                ch.getFakeChar().getMap().broadcastMessage(ch.getFakeChar(), MaplePacketCreator.movePlayer(ch.getFakeChar().getId(), res), false);
                                updatePosition(res, ch.getFakeChar(), 0);
                                ch.getFakeChar().getMap().movePlayer(ch.getFakeChar(), ch.getFakeChar().getPosition());
                            }
                        }, i * 300);
                        i++;
                    }
                }
            }
        }
    }
}