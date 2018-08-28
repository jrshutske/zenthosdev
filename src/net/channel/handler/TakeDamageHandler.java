package net.channel.handler;

import java.util.Collections;
import client.ISkill;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleStat;
import client.MapleClient;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import client.MapleInventoryType;
import constants.ServerProperties;
import net.AbstractMaplePacketHandler;
import server.AntiCheats;
import server.life.MapleMonster;
import server.life.MobAttackInfo;
import server.life.MobAttackInfoFactory;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMist;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class TakeDamageHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        slea.readInt();
        int damagefrom = slea.readByte();
        slea.readByte();
        int damage = slea.readInt();
        int oid = 0;
        int monsteridfrom = 0;
        int pgmr = 0;
        int direction = 0;
        int pos_x = 0;
        int pos_y = 0;
        int fake = 0;
        boolean is_pgmr = false;
        boolean is_pg = true;
        int mpattack = 0;
        MapleMonster attacker = null;
        if (damagefrom == -2) {
            int debuffLevel = slea.readByte();
            int debuffId = slea.readByte();
            if (debuffId == 125) {
                debuffLevel = debuffLevel - 1;
            }
            MobSkill skill = MobSkillFactory.getMobSkill(debuffId, debuffLevel);
            if (skill != null) {
                skill.applyEffect(player, attacker, false);
            }
        } else {
            monsteridfrom = slea.readInt();
            oid = slea.readInt();
            if (monsteridfrom != 0 && damage != -1) {
                attacker = (MapleMonster) player.getMap().getMapObject(monsteridfrom);
            } else {
                attacker = (MapleMonster) player.getMap().getMapObject(oid);
            }
            direction = slea.readByte();
        }
        if (damagefrom != -1 && damagefrom != -2 && attacker != null) {
            MobAttackInfo attackInfo = MobAttackInfoFactory.getMobAttackInfo(attacker, damagefrom);
            if (damage != -1) {
                if (attackInfo.isDeadlyAttack()) {
                    mpattack = player.getMp() - 1;
                } else {
                    mpattack += attackInfo.getMpBurn();
                }
                if (mpattack - player.getMp() < 0) {
                    mpattack = player.getMp();
                }
            }
            MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
            if (skill != null && damage > 0) {
                skill.applyEffect(player, attacker, false);
            }
            if (attacker != null) {
                attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
            }
        }
        try {
            for (MapleMapObject mmo : player.getMap().getMapObjects()) {
                if (mmo instanceof MapleMist) {
                    MapleMist mist = (MapleMist) mmo;
                    if (mist.getSourceSkill().getId() == 4221006) {
                        for (MapleMapObject mmoplayer : player.getMap().getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.PLAYER))) {
                            if (player == (MapleCharacter) mmoplayer) {
                                damage = -1;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to handle smokescreen: " + e);
        }
        if (damage == -1) {
            int job = (player.getJob().getId() / 10 - 40);
            fake = 4020002 + (job * 100000);
            if (damagefrom == -1 && damagefrom != -2 && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) != null) {
                int[] guardianSkillId = {1120005, 1220006};
                for (int guardian : guardianSkillId) {
                    ISkill guardianSkill = SkillFactory.getSkill(guardian);
                    if (player.getSkillLevel(guardianSkill) > 0 && attacker != null) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.STUN, 1), guardianSkill, false);
                        attacker.applyStatus(player, monsterStatusEffect, false, 2 * 1000);
                    }
                }
            }
        }
        if (damage < -1 || damage > 100000) {
            AntiCheats.autoBan(player.getClient(), player.getName() + " took " + damage + " of damage");
            return;
        } else if (damage > 60000) {
            System.out.println(player.getName() + " receive " + damage + " of abnormal amount of damage.");
            c.disconnect();
            return;
        }
        if (damage > 0) {
            player.getAntiCheats().checkLastHit(true);
            if (!player.isHidden() && player.isAlive()) {
                if (player.getBuffedValue(MapleBuffStat.MORPH) != null) {
                    player.cancelMorphs();
                }
                if (attacker != null && !attacker.isBoss()) {
                    if (damagefrom == -1 && player.getBuffedValue(MapleBuffStat.POWERGUARD) != null) {
                        int bouncedamage = (int) (damage * (player.getBuffedValue(MapleBuffStat.POWERGUARD).doubleValue() / 100));
                        bouncedamage = Math.min(bouncedamage, attacker.getMaxHp() / 10);
                        player.getMap().damageMonster(player, attacker, bouncedamage);
                        damage -= bouncedamage;
                        player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(oid, bouncedamage), false, true);
                        player.checkMonsterAggro(attacker);
                    }
                    if ((damagefrom == 0 || damagefrom == 1) && player.getBuffedValue(MapleBuffStat.MANA_REFLECTION) != null) {
                        int[] manaReflectSkillId = {2121002, 2221002, 2321002};
                        for (int manaReflect : manaReflectSkillId) {
                            ISkill manaReflectSkill = SkillFactory.getSkill(manaReflect);
                            if (player.isBuffFrom(MapleBuffStat.MANA_REFLECTION, manaReflectSkill) && player.getSkillLevel(manaReflectSkill) > 0 && manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill)).makeChanceResult()) {
                                int bouncedamage = (damage * (manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill)).getX() / 100));
                                if (bouncedamage > attacker.getMaxHp() * .2) {
                                    bouncedamage = (int) (attacker.getMaxHp() * .2);
                                }
                                player.getMap().damageMonster(player, attacker, bouncedamage);
                                player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(oid, bouncedamage), true);
                                player.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(manaReflect, 5));
                                player.getMap().broadcastMessage(player, MaplePacketCreator.showBuffeffect(player.getId(), manaReflect, 5, (byte) 3), false);
                                break;
                            }
                        }
                    }
                }
                if (damagefrom == -1) {
                    try {
                        int[] achillesSkillId = {1120004, 1220005, 1320005};
                        for (int achilles : achillesSkillId) {
                            ISkill achillesSkill = SkillFactory.getSkill(achilles);
                            if (player.getSkillLevel(achillesSkill) > 0) {
                                double multiplier = achillesSkill.getEffect(player.getSkillLevel(achillesSkill)).getX() / 1000.0;
                                int newdamage = (int) (multiplier * damage);
                                damage = newdamage;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to handle achilles: " + e);
                    }
                }
                if (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null && mpattack == 0) {
                    int mploss = (int) (damage * (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0));
                    int hploss = damage - mploss;
                    if (mploss > player.getMp()) {
                        hploss += mploss - player.getMp();
                        mploss = player.getMp();
                    }
                    player.addMPHP(-hploss, -mploss);
                } else if (player.getBuffedValue(MapleBuffStat.MESOGUARD) != null) {
                    damage = (damage % 2 == 0) ? damage / 2 : (damage / 2) + 1;
                    int mesoloss = (int) (damage * (player.getBuffedValue(MapleBuffStat.MESOGUARD).doubleValue() / 100.0));
                    if (player.getMeso() < mesoloss) {
                        player.gainMeso(-player.getMeso(), false);
                        player.cancelBuffStats(MapleBuffStat.MESOGUARD);
                    } else {
                        player.gainMeso(-mesoloss, false);
                    }
                    player.addMPHP(-damage, -mpattack);
                } else {
                    player.addMPHP(-damage, -mpattack);
                }
                if (c.getPlayer().getMap().getId() == 980010101) {
                    if (monsteridfrom == 9300166) {
                        player.setBombPoints(player.getBombPoints() - 1);
                        if (player.getBombPoints() < 1) {
                            player.setHp(0);
                            player.updateSingleStat(MapleStat.HP, 0);
                            player.setBombPoints(10);
                            c.getPlayer().dropMessage("[" + ServerProperties.getServerName + "] You have died in Battle at the Bomberman Arena.");
                            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(1, "[" + ServerProperties.getServerName + "] The person " + player.getName() + " has died in Bomberman PvP."));
                            return;
                        } else {
                            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(1, "[" + ServerProperties.getServerName + "] The player " + player.getName() + " now has " + player.getBombPoints() + " points left in Bomberman PvP."));
                            return;
                        }
                    }
                } else {
                    player.getMap().broadcastMessage(player, MaplePacketCreator.damagePlayer(damagefrom, monsteridfrom, player.getId(), damage, fake, direction, is_pgmr, pgmr, is_pg, oid, pos_x, pos_y), false);
                }
            }
        }
    }
}