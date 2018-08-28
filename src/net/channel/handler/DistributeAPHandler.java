package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.MapleStat;
import client.SkillFactory;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class DistributeAPHandler extends AbstractMaplePacketHandler {
    private static final short Max = Short.MAX_VALUE;

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        int num = slea.readInt();
        if (c.getPlayer().getRemainingAp() > 0) {
            if (addStat(c, num)) {
                c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - 1);
                c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    static boolean addStat(MapleClient c, int id) {
        switch (id) {
            case 64: // Str
                if (c.getPlayer().getStr() >= Max) {
                    return false;
                }
                c.getPlayer().addAP(c, 1, 1);
                break;
            case 128: // Dex
                if (c.getPlayer().getDex() >= Max) {
                    return false;
                }
                c.getPlayer().addAP(c, 2, 1);
                break;
            case 256: // Int
                if (c.getPlayer().getInt() >= Max) {
                    return false;
                }
                c.getPlayer().addAP(c, 3, 1);
                break;
            case 512: // Luk
                if (c.getPlayer().getLuk() >= Max) {
                    return false;
                }
                c.getPlayer().addAP(c, 4, 1);
                break;
            case 2048: // HP
                addHP(c.getPlayer(), addHP(c));
                break;
            case 8192: // MP
                addMP(c.getPlayer(), addMP(c));
                break;
            default:
                c.getSession().write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true));
                return false;
        }
        return true;
    }

    static int addHP(MapleClient c) {
        MapleCharacter player = c.getPlayer();
        MapleJob job = player.getJob();
        int MaxHP = player.getMaxHp();
        if (player.getHpApUsed() > 9999 || MaxHP >= 30000) {
            return MaxHP;
        }
        if (job.hasNoJob()) {
            MaxHP += 8;
        } else if (job.isA(MapleJob.WARRIOR) || job.isA(MapleJob.DAWNWARRIOR1) || job.isA(MapleJob.ARAN1)) {
            if (player.getSkillLevel((job.getId() / 1000 == 1) ? SkillFactory.getSkill(10000000) : SkillFactory.getSkill(1000001)) > 0) {
                MaxHP += 20;
            } else {
                MaxHP += 8;
            }
        } else if (job.isA(MapleJob.MAGICIAN) || job.isA(MapleJob.BLAZEWIZARD1)) {
            MaxHP += 6;
        } else if (job.isA(MapleJob.BOWMAN) || job.isA(MapleJob.WINDARCHER1)) {
            MaxHP += 8;
        } else if (job.isA(MapleJob.THIEF) || job.isA(MapleJob.NIGHTWALKER1)) {
            MaxHP += 8;
        } else if (job.isA(MapleJob.PIRATE) || job.isA(MapleJob.THUNDERBREAKER1)) {
            if (player.getSkillLevel((job.getId() / 1000 == 1) ? SkillFactory.getSkill(15100000) : SkillFactory.getSkill(5100000)) > 0) {
                MaxHP += 18;
            } else {
                MaxHP += 8;
            }
        } else {
            MaxHP += 10;
        }
        return MaxHP;
    }

    static int addMP(MapleClient c) {
        MapleCharacter player = c.getPlayer();
        MapleJob job = player.getJob();
        int MaxMP = player.getMaxMp();
        if (player.getMpApUsed() > 9999 || player.getMaxMp() >= 30000) {
            return MaxMP;
        }
        if (job.hasNoJob()) {
            MaxMP += 6;
        } else if (job.isA(MapleJob.WARRIOR) || job.isA(MapleJob.DAWNWARRIOR1) || job.isA(MapleJob.ARAN1)) {
            MaxMP += 2;
        } else if (job.isA(MapleJob.MAGICIAN) || job.isA(MapleJob.BLAZEWIZARD1)) {
            if (player.getSkillLevel((job.getId() / 1000 == 1) ? SkillFactory.getSkill(12000000) : SkillFactory.getSkill(2000001)) > 0) {
                MaxMP += 18;
            } else {
                MaxMP += 14;
            }
        } else if (job.isA(MapleJob.BOWMAN) || job.isA(MapleJob.WINDARCHER1) || job.isA(MapleJob.THIEF) || job.isA(MapleJob.NIGHTWALKER1)) {
            MaxMP += 10;
        } else if (job.isA(MapleJob.PIRATE) || job.isA(MapleJob.THUNDERBREAKER1)) {
            MaxMP += 14;
        } else {
            MaxMP += 10;
        }
        return MaxMP;
    }

    static void addHP(MapleCharacter player, int MaxHP) {
        MaxHP = Math.min(30000, MaxHP);
        player.setHpApUsed(player.getHpApUsed() + 1);
        player.setMaxHp(MaxHP);
        player.updateSingleStat(MapleStat.MAXHP, MaxHP);
    }

    static void addMP(MapleCharacter player, int MaxMP) {
        MaxMP = Math.min(30000, MaxMP);
        player.setMpApUsed(player.getMpApUsed() + 1);
        player.setMaxMp(MaxMP);
        player.updateSingleStat(MapleStat.MAXMP, MaxMP);
    }
}