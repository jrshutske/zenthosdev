package net.channel.handler;

import client.MapleClient;
import tools.Randomizer;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MobDamageMobFriendlyHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int attacker = slea.readInt();
        slea.readInt();
        int damaged = slea.readInt();
        int damage = Randomizer.getInstance().nextInt(((c.getPlayer().getMap().getMonsterByOid(damaged).getMaxHp() / 13 + c.getPlayer().getMap().getMonsterByOid(attacker).getPADamage() * 10)) * 2 + 500);
        if (c.getPlayer().getMap().getMonsterByOid(damaged) == null || c.getPlayer().getMap().getMonsterByOid(attacker) == null) {
            return;
        }
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.MobDamageMobFriendly(c.getPlayer().getMap().getMonsterByOid(damaged), damage), c.getPlayer().getMap().getMonsterByOid(damaged).getPosition());
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
