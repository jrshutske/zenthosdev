package net.channel.handler;

import java.awt.Point;
import java.util.concurrent.ScheduledFuture;
import client.ISkill;
import client.MapleCharacter.CancelCooldownAction;
import client.MapleClient;
import client.SkillFactory;
import net.AbstractMaplePacketHandler;
import server.MapleStatEffect;
import server.TimerManager;
import server.life.MapleMonster;
import server.maps.FakeCharacter;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class SpecialMoveHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readShort();
        slea.readShort();
        int skillid = slea.readInt();
        Point pos = null;
        int __skillLevel = slea.readByte();
        ISkill skill = SkillFactory.getSkill(skillid);
        int skillLevel = c.getPlayer().getSkillLevel(skill);
        MapleStatEffect effect = skill.getEffect(skillLevel);
        if (skill.isGMSkill() && !c.getPlayer().isGM()) {
            c.fullDisconnect();
            return;
        }
        if (effect.getCooldown() > 0) {
            if (c.getPlayer().skillisCooling(skillid)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            } else {
                c.getSession().write(MaplePacketCreator.skillCooldown(skillid, effect.getCooldown()));
                ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(c.getPlayer(), skillid), effect.getCooldown() * 1000);
                c.getPlayer().addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown() * 1000, timer);
            }
        }
        try {
            switch (skillid) {
                case 1121001:
                case 1221001:
                case 1321001:
                    int num = slea.readInt();
                    int mobId;
                    byte success;
                    for (int i = 0; i < num; i++) {
                        mobId = slea.readInt();
                        success = slea.readByte();
                        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showMagnet(mobId, success), false);
                        MapleMonster monster = c.getPlayer().getMap().getMonsterByOid(mobId);
                        if (monster != null) {
                            monster.switchController(c.getPlayer(), monster.isControllerHasAggro());
                        }
                    }
                    byte direction = slea.readByte();
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showBuffeffect(c.getPlayer().getId(), skillid, 1, direction), false);
                    for (FakeCharacter ch : c.getPlayer().getFakeChars()) {
                        ch.getFakeChar().getMap().broadcastMessage(ch.getFakeChar(), MaplePacketCreator.showBuffeffect(ch.getFakeChar().getId(), skillid, 1, direction), false);
                    }
                    c.getSession().write(MaplePacketCreator.enableActions());
                    break;
            }
        } catch (Exception e) {
            System.out.println("Failed to handle monster magnet: " + e);
        }
        if (slea.available() == 5) {
            pos = new Point(slea.readShort(), slea.readShort());
        }
        if (skillLevel == 0 || skillLevel != __skillLevel) {
            System.out.println(c.getPlayer().getName() + " is using a move skill he doesn't have. ID: " + skill.getId());
            c.disconnect();
            return;
        } else {
            if (c.getPlayer().isAlive()) {
                if (skill.getId() != 2311002 || c.getPlayer().canDoor()) {
                    skill.getEffect(skillLevel).applyTo(c.getPlayer(), pos);
                } else {
                    c.getPlayer().dropMessage("Please wait 5 seconds before casting Mystic Door again.");
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        }
    }
}