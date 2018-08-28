package net.login.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class DeleteCharHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String pic = slea.readMapleAsciiString();
        int cid = slea.readInt();
        if (!c.getPic().equals(pic)) {
            c.getSession().write(MaplePacketCreator.WrongPic());
        } else {
            int state = c.deleteCharacter(cid) ? 0 : 1;
            c.getSession().write(MaplePacketCreator.deleteCharResponse(cid, state));
        }
    }
}