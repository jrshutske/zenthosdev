package net.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import client.ISkill;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.ServerProperties;
import constants.skills.Aran;
import constants.skills.Assassin;
import constants.skills.Bandit;
import constants.skills.Bishop;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.ChiefBandit;
import constants.skills.Cleric;
import constants.skills.Corsair;
import constants.skills.Crusader;
import constants.skills.DragonKnight;
import constants.skills.FPArchMage;
import constants.skills.Gunslinger;
import constants.skills.GM;
import constants.skills.ILArchMage;
import constants.skills.Marauder;
import constants.skills.Marksman;
import constants.skills.NightWalker;
import constants.skills.Paladin;
import constants.skills.Rogue;
import constants.skills.Shadower;
import constants.skills.ThunderBreaker;
import constants.skills.WindArcher;
import net.AbstractMaplePacketHandler;
import server.AntiCheats;
import server.MapleStatEffect;
import server.TimerManager;
import server.life.Element;
import server.life.ElementalEffectiveness;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.pvp.PvPLibrary;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.input.LittleEndianAccessor;

public abstract class AbstractDealDamageHandler extends AbstractMaplePacketHandler {
    public class AttackInfo {
        public int numAttacked, numDamage, numAttackedAndDamage;
        public int skill, stance, direction, charge;
        public int v80thing, display;
        public int xCoord, yCoord;
        public List<Pair<Integer, List<Integer>>> allDamage;
        public boolean isHH = false;
        public int speed = 4;

        private MapleStatEffect getAttackEffect(MapleCharacter chr, ISkill theSkill) {
            ISkill mySkill = theSkill;
            if (mySkill == null) {
                mySkill = SkillFactory.getSkill(skill);
            }
            int skillLevel = chr.getSkillLevel(mySkill);
            if (skillLevel == 0) {
                return null;
            }
            return mySkill.getEffect(skillLevel);
        }

        public MapleStatEffect getAttackEffect(MapleCharacter chr) {
            return getAttackEffect(chr, null);
        }
    }

    protected synchronized void applyAttack(AttackInfo attack, MapleCharacter player, int maxDamagePerMonster, int attackCount) {
        ISkill theSkill = null;
        MapleStatEffect attackEffect = null;
        if (!player.isAlive()) {
            player.getAntiCheats().addCheatToken(0);
            return;
        }
        if (attackCount != attack.numDamage && attack.skill != ChiefBandit.MESO_EXPLOSION) {
            return;
        }
        if (attack.skill != 0) {
            theSkill = SkillFactory.getSkill(attack.skill);
            attackEffect = attack.getAttackEffect(player, theSkill);
            if (attackEffect == null) {
                player.getClient().getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (attack.skill != Cleric.HEAL) {
                if (player.isAlive()) {
                    attackEffect.applyTo(player);
                } else {
                    player.getClient().getSession().write(MaplePacketCreator.enableActions());
                }
            } else if (SkillFactory.getSkill(attack.skill).isGMSkill() && !player.isGM() || player.getSkillLevel(theSkill) < 1) {
                player.getClient().fullDisconnect();
                return;
            }
        }
        int totDamage = 0;
        if (isPvPAllowed(attack.skill)) {
            int PvPis = ServerProperties.pvpIs;
            int MapChannel = (PvPis >= 100000000 ? player.getMapId() : player.getClient().getChannel());
            if (MapChannel == PvPis) {
                PvPLibrary.doPvP(player, attack);
            }
        }
        final MapleMap map = player.getMap();
        if (attack.skill == ChiefBandit.MESO_EXPLOSION) {
            int delay = 0;
            for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
                MapleMapObject mapobject = map.getMapObject(oned.getLeft().intValue());
                if (mapobject != null && mapobject.getType() == MapleMapObjectType.ITEM) {
                    final MapleMapItem mapitem = (MapleMapItem) mapobject;
                    if (mapitem.getMeso() > 9) {
                        synchronized (mapitem) {
                            if (mapitem.isPickedUp()) {
                                return;
                            }
                            TimerManager.getInstance().schedule(new Runnable() {

                                public void run() {
                                    map.removeMapObject(mapitem);
                                    map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 4, 0), mapitem.getPosition());
                                    mapitem.setPickedUp(true);
                                }
                            }, delay);
                            delay += 100;
                        }
                    } else if (mapitem.getMeso() == 0) {
                        player.getAntiCheats().addCheatToken(5);
                        return;
                    }
                } else if (mapobject != null && mapobject.getType() != MapleMapObjectType.MONSTER) {
                    player.getAntiCheats().addCheatToken(5);
                    return;
                }
            }
        }
        for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
            MapleMonster monster = map.getMonsterByOid(oned.getLeft().intValue());
            if (monster != null) {
                int totDamageToOneMonster = 0;
                for (Integer eachd : oned.getRight()) {
                    totDamageToOneMonster += eachd.intValue();
                }
                totDamage += totDamageToOneMonster;
                player.checkMonsterAggro(monster);
                if (totDamageToOneMonster > attack.numDamage + 1) {
                    int dmgCheck = player.getAntiCheats().checkSameDamage(totDamageToOneMonster);
                    if (dmgCheck > 5 && totDamageToOneMonster < 99999 && monster.getId() < 9500317 && monster.getId() > 9500319) {
                        player.getAntiCheats().addCheatToken(1);
                        return;
                    }
                }
                if (totDamageToOneMonster >= 100000000) {
                    AntiCheats.autoBan(player.getClient(), player.getName() + " dealt " + totDamageToOneMonster + " to monster " + monster.getId());
                    return;
                }
                if (attack.skill == Cleric.HEAL && !monster.getUndead()) {
                    player.getAntiCheats().addCheatToken(3);
                    return;
                }
                if (player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null) {
                    switch (attack.skill) {
                        case 0:
                        case Rogue.DOUBLE_STAB:
                        case Bandit.SAVAGE_BLOW:
                        case ChiefBandit.ASSAULTER:
                        case ChiefBandit.BAND_OF_THIEVES:
                        case Shadower.ASSASSINATE:
                        case Shadower.TAUNT:
                        case Shadower.BOOMERANG_STEP:
                            int delay = 0;
                            int maxmeso = player.getBuffedValue(MapleBuffStat.PICKPOCKET).intValue();
                            int reqdamage = 20000;
                            Point monsterPosition = monster.getPosition();
                            for (Integer eachd : oned.getRight()) {
                                if (SkillFactory.getSkill(ChiefBandit.PICKPOCKET).getEffect(player.getSkillLevel(SkillFactory.getSkill(ChiefBandit.PICKPOCKET))).makeChanceResult()) {
                                    double perc = (double) eachd / (double) reqdamage;
                                    final int todrop = Math.min((int) Math.max(perc * (double) maxmeso, (double) 1), maxmeso);
                                    final MapleMap tdmap = player.getMap();
                                    final Point tdpos = new Point((int) (monsterPosition.getX() + (Math.random() * 100) - 50), (int) (monsterPosition.getY()));
                                    final MapleMonster tdmob = monster;
                                    final MapleCharacter tdchar = player;
                                    TimerManager.getInstance().schedule(new Runnable() {
                                        public void run() {
                                            tdmap.spawnMesoDrop(todrop, todrop, tdpos, tdmob, tdchar, false);
                                        }
                                    }, delay);
                                    delay += 100;
                                }
                            }
                            break;
                    }
                }
                if (attack.skill == Paladin.HEAVENS_HAMMER) {
                    if (attack.isHH) {
                        int HHDmg = (player.calculateMaxBaseDamage(player.getTotalWatk()) * (theSkill.getEffect(player.getSkillLevel(theSkill)).getDamage() / 100));
                        HHDmg = (int) (Math.floor(Math.random() * (HHDmg - HHDmg * .80) + HHDmg * .80));
                        map.damageMonster(player, monster, HHDmg);
                    }
                } else if (attack.skill == Shadower.BOOMERANG_STEP) {
                    totDamageToOneMonster = 95000 + (int) Math.random() * 4999;
                } else if (attack.skill == Marauder.ENERGY_DRAIN || attack.skill == ThunderBreaker.ENERGY_DRAIN || attack.skill == NightWalker.VAMPIRE || attack.skill == Assassin.DRAIN) {
                    int gainhp = (int) ((double) totDamageToOneMonster * (double) SkillFactory.getSkill(attack.skill).getEffect(player.getSkillLevel(SkillFactory.getSkill(attack.skill))).getX() / 100.0);
                    gainhp = Math.min(monster.getMaxHp(), Math.min(gainhp, player.getMaxHp() / 2));
                    player.addHP(gainhp);
                } else {
                    if (totDamageToOneMonster > 0 && monster.isAlive()) {
                        if (player.getBuffedValue(MapleBuffStat.BLIND) != null) {
                            if (SkillFactory.getSkill(3221006).getEffect(player.getSkillLevel(SkillFactory.getSkill(3221006))).makeChanceResult()) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.ACC, SkillFactory.getSkill(3221006).getEffect(player.getSkillLevel(SkillFactory.getSkill(3221006))).getX()), SkillFactory.getSkill(3221006), false);
                                monster.applyStatus(player, monsterStatusEffect, false, SkillFactory.getSkill(3221006).getEffect(player.getSkillLevel(SkillFactory.getSkill(3221006))).getY() * 1000);

                            }
                        }
                        if (player.getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null) {
                            final ISkill skill = SkillFactory.getSkill(Aran.BODY_PRESSURE);
                            if (skill.getEffect(player.getSkillLevel(skill)).makeChanceResult()) {
                                monster.applyStatus(player, new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.NEUTRALIZE, 1), skill, false), false, skill.getEffect(player.getSkillLevel(skill)).getX() * 1000, false);
                            }
                        }
                        if (player.getBuffedValue(MapleBuffStat.HAMSTRING) != null) {
                            if (SkillFactory.getSkill(3121007).getEffect(player.getSkillLevel(SkillFactory.getSkill(3121007))).makeChanceResult()) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, SkillFactory.getSkill(3121007).getEffect(player.getSkillLevel(SkillFactory.getSkill(3121007))).getX()), SkillFactory.getSkill(3121007), false);
                                monster.applyStatus(player, monsterStatusEffect, false, SkillFactory.getSkill(3121007).getEffect(player.getSkillLevel(SkillFactory.getSkill(3121007))).getY() * 1000);
                            }
                        }
                        if (player.getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
                            player.addHP(Math.min(monster.getMaxHp(), Math.min((int) ((double) totDamage * (double) SkillFactory.getSkill(Aran.COMBO_DRAIN).getEffect(player.getSkillLevel(SkillFactory.getSkill(Aran.COMBO_DRAIN))).getX() / 100.0), player.getMaxHp() / 2)));
                        }
                        final int id = player.getJob().getId();
                        if (id == 121 || id == 122) {
                            for (int charge = 1211005; charge < 1211007; charge++) {
                                ISkill chargeSkill = SkillFactory.getSkill(charge);
                                if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, chargeSkill)) {
                                    final ElementalEffectiveness iceEffectiveness = monster.getEffectiveness(Element.ICE);
                                    if (totDamageToOneMonster > 0 && iceEffectiveness == ElementalEffectiveness.NORMAL || iceEffectiveness == ElementalEffectiveness.WEAK) {
                                        monster.applyStatus(player, new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.FREEZE, 1), chargeSkill, false), false, chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 2000);
                                    }
                                }
                            }
                        } else if (id == 412 || id == 422 || id == 1411) {
                            ISkill type = SkillFactory.getSkill(player.getJob().getId() == 412 ? 4120005 : (player.getJob().getId() == 1411 ? 14110004 : 4220005));
                            if (player.getSkillLevel(type) > 0) {
                                MapleStatEffect venomEffect = type.getEffect(player.getSkillLevel(type));
                                for (int i = 0; i < attackCount; i++) {
                                    if (venomEffect.makeChanceResult()) {
                                        if (monster.getVenomMulti() < 3) {
                                            monster.setVenomMulti((monster.getVenomMulti() + 1));
                                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), type, false);
                                            monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (totDamageToOneMonster > 0 && attackEffect != null && attackEffect.getMonsterStati().size() > 0) {
                    if (attackEffect.makeChanceResult()) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, false);
                        monster.applyStatus(player, monsterStatusEffect, attackEffect.isPoison(), attackEffect.getDuration());
                    }
                }
                if (!attack.isHH) {
                    map.damageMonster(player, monster, totDamageToOneMonster);
                }
            }
        }
        if (totDamage > 1) {
            player.getAntiCheats().checkLastHit(false);
        }
    }

    private boolean isPvPAllowed(int skillId) {
        int[] notAllowed = {Crusader.SHOUT, DragonKnight.DRAGON_ROAR, GM.GM_ROAR1, GM.GM_ROAR2};
        for (int i = 0; i < notAllowed.length; i++) {
            if (notAllowed[i] == skillId) {
                return false;
            }
        }
        return true;
    }

    public AttackInfo parseDamage(LittleEndianAccessor lea, boolean ranged) {
        AttackInfo ret = new AttackInfo();
        lea.readByte();
        ret.numAttackedAndDamage = lea.readByte();
        ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
        ret.numDamage = ret.numAttackedAndDamage & 0xF;
        ret.allDamage = new ArrayList<Pair<Integer, List<Integer>>>();
        ret.skill = lea.readInt();
        switch (ret.skill) {
            case FPArchMage.BIG_BANG:
            case ILArchMage.BIG_BANG:
            case Bishop.BIG_BANG:
            case Gunslinger.GRENADE:
            case Brawler.CORKSCREW_BLOW:
            case ThunderBreaker.CORKSCREW_BLOW:
            case NightWalker.POISON_BOMB:
                ret.charge = lea.readInt();
                break;
            default:
                ret.charge = 0;
                break;
        }
        lea.skip(8);
        ret.display = lea.readByte();
        ret.v80thing = lea.readByte();
        ret.stance = lea.readByte();
        if (ret.skill == ChiefBandit.MESO_EXPLOSION) {
            if (ret.numAttackedAndDamage == 0) {
                lea.skip(10);
                for (int j = 0; j < lea.readByte(); j++) {
                    ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(lea.readInt()), null));
                    lea.readByte();
                }
                return ret;
            } else {
                lea.skip(6);
            }
            for (int i = 0; i < ret.numAttacked + 1; i++) {
                int oid = lea.readInt();
                if (i < ret.numAttacked) {
                    lea.skip(12);
                    int bullets = lea.readByte();
                    List<Integer> allDamageNumbers = new ArrayList<Integer>();
                    for (int j = 0; j < bullets; j++) {
                        int damage = lea.readInt();
                        allDamageNumbers.add(Integer.valueOf(damage));
                    }
                    ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(oid), allDamageNumbers));
                    lea.skip(4);
                } else {
                    int bullets = lea.readByte();
                    for (int j = 0; j < bullets; j++) {
                        int mesoid = lea.readInt();
                        lea.skip(1);
                        ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(mesoid), null));
                    }
                }
            }
            return ret;
        }
        if (ret.skill == Paladin.HEAVENS_HAMMER) {
            ret.isHH = true;
        }
        lea.readByte();
        ret.speed = lea.readByte();
        if (ranged) {
            lea.readByte();
            ret.direction = lea.readByte();
            lea.skip(2);
            lea.skip(5);
            switch (ret.skill) {
                case Bowmaster.HURRICANE:
                case Marksman.PIERCING_ARROW:
                case Corsair.RAPID_FIRE:
                case WindArcher.HURRICANE:
                    lea.skip(4);
                    break;
                default:
                    break;
            }
        } else {
            lea.skip(4);
        }
        for (int i = 0; i < ret.numAttacked; i++) {
            int oid = lea.readInt();
            lea.skip(14);
            List<Integer> allDamageNumbers = new ArrayList<Integer>();
            for (int j = 0; j < ret.numDamage; j++) {
                int damage = lea.readInt();
                if (ret.skill == Marksman.SNIPE) {
                    damage += 0x80000000;
                }
                allDamageNumbers.add(Integer.valueOf(damage));
            }
            if (ret.skill != Corsair.RAPID_FIRE) {
                lea.skip(4);
            }
            ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(oid), allDamageNumbers));
        }
        if (ret.skill == NightWalker.POISON_BOMB) {
            lea.skip(4);
            ret.xCoord = lea.readShort();
            ret.yCoord = lea.readShort();
        }
        return ret;
    }
}