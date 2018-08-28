package net.channel.handler;

import client.ISkill;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import client.SkillFactory;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public final class DistributeSPHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        int skillid = slea.readInt();
        MapleCharacter player = c.getPlayer();
        int remainingSp = player.getRemainingSp();
        boolean isBeginnerSkill = false;
        if (skillid % 10000000 > 999 && skillid % 10000000 < 1003) {
            int total = 0;
            for (int i = 0; i < 3; i++) {
                total += player.getSkillLevel(SkillFactory.getSkill((player.getJob().getId() / 1000) * 10000000 + 1000 + i));
            }
            remainingSp = Math.min((player.getLevel() - 1), 6) - total;
            isBeginnerSkill = true;
        }
        ISkill skill = SkillFactory.getSkill(skillid);
        int curLevel = player.getSkillLevel(skill);
        if ((remainingSp > 0 && curLevel + 1 <= (skill.isFourthJob() ? player.getMasterLevel(skill) : skill.getMaxLevel()))) {
            if (!isBeginnerSkill) {
                player.setRemainingSp(player.getRemainingSp() - 1);
            }
            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
            player.changeSkillLevel(skill, curLevel + 1, player.getMasterLevel(skill));
        }
    }
}