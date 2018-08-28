package net.channel.handler;

import java.awt.Rectangle;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import client.ExpTable;
import client.IEquip;
import client.IItem;
import client.ISkill;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MapleJob;
import client.MaplePet;
import client.MapleStat;
import client.SkillFactory;
import constants.InventoryConstants;
import constants.ServerProperties;
import net.AbstractMaplePacketHandler;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.maps.MapleMap;
import server.maps.MapleMist;
import server.maps.MapleTVEffect;
import tools.Pair;

public class UseCashItemHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        if (player.getAntiCheats().Spam(1000, 8)) {
            player.dropMessage("Please try again later.");
            return;
        }
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        int itemType = itemId / 10000;
        String medal = "";
        IItem medalItem = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -49);
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (medalItem != null) {
            medal = "<" + ii.getName(medalItem.getItemId()) + "> ";
        }
        IItem item = player.getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item.getItemId() != itemId || item == null || item.getQuantity() < 1) {
            c.fullDisconnect();
            return;
        }
        try {
            switch (itemType) {
                case 504:
                    byte rocktype = slea.readByte();
                    boolean vip = itemId == 5041000;
                    if (rocktype == 0x00) {
                        int mapId = slea.readInt();
                        if (c.getChannelServer().getMapFactory().getMap(mapId).getReturnMapId() == 999999999) {
                            player.changeMap(mapId);
                        }
                    } else {
                        String name = slea.readMapleAsciiString();
                        MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                        if (victim == null) {
                            int channel = c.getChannelServer().getWorldInterface().find(name);
                            if (channel == -1) {
                                player.dropMessage(1, "This player is not online.");
                                break;
                            }
                            ChangeChannelHandler.changeChannel(channel, player.getClient());
                            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                        }
                        if (victim == null) {
                            player.dropMessage("System error !");
                        } else {
                            MapleMap map = victim.getMap();
                            switch (map.getId()) {
                                case 180000000:
                                case 0:
                                    player.dropMessage(1, "You cannot use VIP teleport rocks on a GM.");
                                    break;
                            }
                            if ((victim.isGM() && player.isGM()) || !victim.isGM()) {
                                if ((player.getMap().getMapName().equals(victim.getMap().getMapName()) && !vip) || vip) {
                                    player.changeMap(map, map.findClosestSpawnpoint(victim.getPosition()));
                                } else {
                                    player.dropMessage(1, "You cannot warp to this player because he's not in the same continent.");
                                }
                            } else {
                                player.dropMessage(1, "You cannot use VIP teleport rocks on a GM.");
                            }
                        }
                    }
                    remove(c, itemId);
                    break;
                case 505:
                    if (itemId > 5050000) {
                        int SPTo = slea.readInt();
                        int SPFrom = slea.readInt();
                        ISkill skillSPTo = SkillFactory.getSkill(SPTo);
                        ISkill skillSPFrom = SkillFactory.getSkill(SPFrom);
                        int maxlevel = skillSPTo.getMaxLevel();
                        int curLevel = player.getSkillLevel(skillSPTo);
                        int curLevelSPFrom = player.getSkillLevel(skillSPFrom);
                        if (curLevel + 1 <= maxlevel && curLevelSPFrom > 0) {
                            player.changeSkillLevel(skillSPFrom, curLevelSPFrom - 1, player.getMasterLevel(skillSPFrom));
                            player.changeSkillLevel(skillSPTo, curLevel + 1, player.getMasterLevel(skillSPTo));
                        }
                    } else {
                        List<Pair<MapleStat, Integer>> statupdate = new ArrayList<Pair<MapleStat, Integer>>(2);
                        int APTo = slea.readInt();
                        int APFrom = slea.readInt();
                        switch (APFrom) {
                            case 64:
                                if (player.getStr() < 5) {
                                    break;
                                }
                                player.setStr(player.getStr() - 1);
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.STR, player.getStr()));
                                break;
                            case 128:
                                if (player.getDex() < 5) {
                                    break;
                                }
                                player.setDex(player.getDex() - 1);
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.DEX, player.getDex()));
                                break;
                            case 256:
                                if (player.getInt() < 5) {
                                    break;
                                }
                                player.setInt(player.getInt() - 1);
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.INT, player.getInt()));
                                break;
                            case 512:
                                if (player.getLuk() < 5) {
                                    break;
                                }
                                player.setLuk(player.getLuk() - 1);
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.LUK, player.getLuk()));
                                break;
                            case 2048:
                                if (player.getHpApUsed() < 1 || player.getHpApUsed() == 10000) {
                                    break;
                                }
                                int maxhp = player.getMaxHp();
                                if (player.getJob().isA(MapleJob.BEGINNER) || player.getJob().isA(MapleJob.NOBLESSE)) {
                                    maxhp -= 12;
                                } else if (player.getJob().isA(MapleJob.WARRIOR) || player.getJob().isA(MapleJob.DAWNWARRIOR1)) {
                                    ISkill improvingMaxHP = SkillFactory.getSkill(1000001);
                                    int improvingMaxHPLevel = player.getSkillLevel(improvingMaxHP);
                                    maxhp -= 24;
                                    if (improvingMaxHPLevel >= 1) {
                                        maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                    }
                                } else if (player.getJob().isA(MapleJob.MAGICIAN) || player.getJob().isA(MapleJob.BLAZEWIZARD1)) {
                                    maxhp -= 10;
                                } else if (player.getJob().isA(MapleJob.BOWMAN) || player.getJob().isA(MapleJob.WINDARCHER1)) {
                                    maxhp -= 20;
                                } else if (player.getJob().isA(MapleJob.THIEF) || player.getJob().isA(MapleJob.NIGHTWALKER1)) {
                                    maxhp -= 20;
                                } else if (player.getJob().isA(MapleJob.PIRATE) || player.getJob().isA(MapleJob.THUNDERBREAKER1)) {
                                    ISkill improvingMaxHP = SkillFactory.getSkill(5100000);
                                    int improvingMaxHPLevel = player.getSkillLevel(improvingMaxHP);
                                    maxhp -= 20;
                                    if (improvingMaxHPLevel >= 1) {
                                        maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                    }
                                }
                                if (maxhp < ((player.getLevel() * 2) + 148)) {
                                    break;
                                }
                                player.setHpApUsed(player.getHpApUsed() - 1);
                                player.setHp(maxhp);
                                player.setMaxHp(maxhp);
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.HP, player.getMaxHp()));
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, player.getMaxHp()));
                                break;
                            case 8192:
                                if (player.getHpApUsed() <= 1 || player.getMpApUsed() == 10000) {
                                    break;
                                }
                                int maxmp = player.getMaxMp();
                                if (player.getJob().isA(MapleJob.BEGINNER) || player.getJob().isA(MapleJob.NOBLESSE)) {
                                    maxmp -= 8;
                                } else if (player.getJob().isA(MapleJob.WARRIOR) || player.getJob().isA(MapleJob.DAWNWARRIOR1)) {
                                    maxmp -= 4;
                                } else if (player.getJob().isA(MapleJob.MAGICIAN) || player.getJob().isA(MapleJob.BLAZEWIZARD1)) {
                                    ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
                                    int improvingMaxMPLevel = player.getSkillLevel(improvingMaxMP);
                                    maxmp -= 20;
                                    if (improvingMaxMPLevel >= 1) {
                                        maxmp -= improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
                                    }
                                } else if (player.getJob().isA(MapleJob.BOWMAN) || player.getJob().isA(MapleJob.WINDARCHER1)) {
                                    maxmp -= 12;
                                } else if (player.getJob().isA(MapleJob.THIEF) || player.getJob().isA(MapleJob.NIGHTWALKER1)) {
                                    maxmp -= 12;
                                } else if (player.getJob().isA(MapleJob.PIRATE) || player.getJob().isA(MapleJob.THUNDERBREAKER1)) {
                                    maxmp -= 16;
                                }
                                if (maxmp < ((player.getLevel() * 2) + 148)) {
                                    break;
                                }
                                player.setMpApUsed(player.getMpApUsed() - 1);
                                player.setMp(maxmp);
                                player.setMaxMp(maxmp);
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MP, player.getMaxMp()));
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, player.getMaxMp()));
                                break;
                            default:
                                c.getSession().write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true));
                                break;
                        }
                        switch (APTo) {
                            case 64:
                                if (player.getStr() >= 30000) {
                                    break;
                                }
                                player.setStr(player.getStr() + 1);
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.STR, player.getStr()));
                                break;
                            case 128:
                                if (player.getDex() >= 30000) {
                                    break;
                                }
                                player.setDex(player.getDex() + 1);
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.DEX, player.getDex()));
                                break;
                            case 256:
                                if (player.getInt() >= 30000) {
                                    break;
                                }
                                player.setInt(player.getInt() + 1);
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.INT, player.getInt()));
                                break;
                            case 512:
                                if (player.getLuk() >= 30000) {
                                    break;
                                }
                                player.setLuk(player.getLuk() + 1);
                                statupdate.add(new Pair<MapleStat, Integer>(MapleStat.LUK, player.getLuk()));
                                break;
                            case 2048:
                                int maxhp = player.getMaxHp();
                                if (maxhp >= 30000) {
                                    c.getSession().write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true));
                                    break;
                                } else {
                                    if (player.getJob().isA(MapleJob.BEGINNER) || player.getJob().isA(MapleJob.NOBLESSE)) {
                                        maxhp += rand(8, 12);
                                    } else if (player.getJob().isA(MapleJob.WARRIOR) || player.getJob().isA(MapleJob.DAWNWARRIOR1)) {
                                        ISkill improvingMaxHP = SkillFactory.getSkill(1000001);
                                        int improvingMaxHPLevel = player.getSkillLevel(improvingMaxHP);
                                        maxhp += rand(20, 25);
                                        if (improvingMaxHPLevel >= 1) {
                                            maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                        }
                                    } else if (player.getJob().isA(MapleJob.MAGICIAN) || player.getJob().isA(MapleJob.BLAZEWIZARD1)) {
                                        maxhp += rand(10, 20);
                                    } else if (player.getJob().isA(MapleJob.BOWMAN) || player.getJob().isA(MapleJob.WINDARCHER1)) {
                                        maxhp += rand(16, 20);
                                    } else if (player.getJob().isA(MapleJob.THIEF) || player.getJob().isA(MapleJob.NIGHTWALKER1)) {
                                        maxhp += rand(16, 20);
                                    } else if (player.getJob().isA(MapleJob.PIRATE) || player.getJob().isA(MapleJob.THUNDERBREAKER1)) {
                                        ISkill improvingMaxHP = SkillFactory.getSkill(5100000);
                                        int improvingMaxHPLevel = player.getSkillLevel(improvingMaxHP);
                                        maxhp += 20;
                                        if (improvingMaxHPLevel >= 1) {
                                            maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                        }
                                    }
                                    maxhp = Math.min(30000, maxhp);
                                    player.setHpApUsed(player.getHpApUsed() + 1);
                                    player.setMaxHp(maxhp);
                                    statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, player.getMaxHp()));
                                    break;
                                }
                            case 8192:
                                int maxmp = player.getMaxMp();
                                if (maxmp >= 30000) {
                                    break;
                                } else {
                                    if (player.getJob().isA(MapleJob.BEGINNER) || player.getJob().isA(MapleJob.NOBLESSE)) {
                                        maxmp += rand(6, 8);
                                    } else if (player.getJob().isA(MapleJob.WARRIOR) || player.getJob().isA(MapleJob.DAWNWARRIOR1)) {
                                        maxmp += rand(2, 4);
                                    } else if (player.getJob().isA(MapleJob.MAGICIAN) || player.getJob().isA(MapleJob.BLAZEWIZARD1)) {
                                        ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
                                        int improvingMaxMPLevel = player.getSkillLevel(improvingMaxMP);
                                        maxmp += rand(18, 20);
                                        if (improvingMaxMPLevel >= 1) {
                                            maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
                                        }
                                    } else if (player.getJob().isA(MapleJob.BOWMAN) || player.getJob().isA(MapleJob.WINDARCHER1)) {
                                        maxmp += rand(10, 12);
                                    } else if (player.getJob().isA(MapleJob.THIEF) || player.getJob().isA(MapleJob.NIGHTWALKER1)) {
                                        maxmp += rand(10, 12);
                                    } else if (player.getJob().isA(MapleJob.PIRATE) || player.getJob().isA(MapleJob.THUNDERBREAKER1)) {
                                        maxmp += rand(10, 12);
                                    }
                                    maxmp = Math.min(30000, maxmp);
                                    player.setMpApUsed(player.getMpApUsed() + 1);
                                    player.setMaxMp(maxmp);
                                    statupdate.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, player.getMaxMp()));
                                    break;
                                }
                            default:
                                c.getSession().write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true));
                                break;
                        }
                        c.getSession().write(MaplePacketCreator.updatePlayerStats(statupdate, true));
                    }
                    remove(c, itemId);
                    break;
                case 506:
                    int tagType = itemId % 10;
                    IItem eq = null;
                    if (tagType == 0) {
                        int equipSlot = slea.readShort();
                        if (equipSlot == 0) {
                            break;
                        }
                        eq = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) equipSlot);
                        eq.setOwner(player.getName());
                    } else if (tagType == 1) {
                        MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                        IItem getItem = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                        if (getItem == null) {
                            return;
                        }
                        byte flag = getItem.getFlag();
                        flag |= InventoryConstants.LOCK;
                        getItem.setFlag(flag);
                        c.getSession().write(MaplePacketCreator.updateItemInSlot(getItem));
                    } else if (tagType == 2) {
                        byte inventory2 = (byte) slea.readInt();
                        byte slot2 = (byte) slea.readInt();
                        IItem item2 = c.getPlayer().getInventory(MapleInventoryType.getByType(inventory2)).getItem(slot2);
                        if (item2 == null) {
                            return;
                        }
                        if (getIncubatedItem(c, itemId)) {
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.getByType(inventory2), slot2, (short) 1, false);
                            remove(c, itemId);
                        }
                        return;
                    }
                    slea.readInt();
                    c.getSession().write(MaplePacketCreator.updateEquipSlot(eq));
                    remove(c, itemId);
                    break;
                case 507:
                    if (player.getCanSmega() && player.getSmegaEnabled() && !player.getAntiCheats().Spam(3000, 4)) {
                        boolean whisper;
                        switch (itemId / 1000 % 10) {
                            case 1:
                                if (player.getLevel() >= 10) {
                                    player.getClient().getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(2, medal + player.getName() + " : " + slea.readMapleAsciiString()));
                                } else {
                                    player.dropMessage("You may not use this until you're level 10.");
                                }
                                break;
                            case 2:
                                c.getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(3, c.getChannel(), medal + player.getName() + " : " + slea.readMapleAsciiString(), (slea.readByte() != 0)).getBytes());
                                break;
                            case 5:
                                int tvType = itemId % 10;
                                boolean megassenger = false;
                                boolean ear = false;
                                MapleCharacter victim = null;
                                if (tvType != 1) {
                                    if (tvType >= 3) {
                                        megassenger = true;
                                        if (tvType == 3) {
                                            slea.readByte();
                                        }
                                        ear = 1 == slea.readByte();
                                    } else if (tvType != 2) {
                                        slea.readByte();
                                    }
                                    if (tvType != 4) {
                                        String name = slea.readMapleAsciiString();
                                        if (name.length() > 0) {
                                            int channel = c.getChannelServer().getWorldInterface().find(name);
                                            if (channel == -1) {
                                                player.dropMessage(1, "Player could not be found.");
                                                break;
                                            }
                                            victim = net.channel.ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(name);
                                        }
                                    }
                                }
                                List<String> messages = new LinkedList<String>();
                                StringBuilder builder = new StringBuilder();
                                for (int i = 0; i < 5; i++) {
                                    String message = slea.readMapleAsciiString();
                                    if (megassenger) {
                                        builder.append(" ");
                                        builder.append(message);
                                    }
                                    messages.add(message);
                                }
                                slea.readInt();
                                if (!MapleTVEffect.active) {
                                    if (megassenger) {
                                        c.getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(3, c.getChannel(), medal + player.getName() + " : " + builder.toString(), ear).getBytes());
                                    }
                                    new MapleTVEffect(player, victim, messages, tvType);
                                    remove(c, itemId);
                                } else {
                                    player.dropMessage(1, "The Maple TV is already in use.");
                                    return;
                                }
                                break;
                            case 6:
                                String msg = medal + c.getPlayer().getName() + " : " + slea.readMapleAsciiString();
                                whisper = slea.readByte() == 1;
                                if (slea.readByte() == 1) {
                                    item = c.getPlayer().getInventory(MapleInventoryType.getByType((byte) slea.readInt())).getItem((byte) slea.readInt());
                                    if (item == null) {
                                        return;
                                    } else if (ii.isDropRestricted(item.getItemId())) {
                                        player.dropMessage(1, "You cannot trade this item.");
                                        c.getSession().write(MaplePacketCreator.enableActions());
                                        return;
                                    }
                                }
                                c.getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.itemMegaphone(msg, whisper, c.getChannel(), item).getBytes());
                                break;
                            case 7:
                                int lines = slea.readByte();
                                if (lines < 1 || lines > 3) {
                                    return;
                                }
                                String[] msg2 = new String[lines];
                                for (int i = 0; i < lines; i++) {
                                    msg2[i] = medal + c.getPlayer().getName() + " : " + slea.readMapleAsciiString();
                                }
                                whisper = slea.readByte() == 1;
                                c.getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.getMultiMegaphone(msg2, c.getChannel(), whisper).getBytes());
                                break;
                            }
                            remove(c, itemId);
                        } else {
                            player.dropMessage("You have lost your megaphone privilages.");
                        }
                    break;
                case 508:
                    slea.readMapleAsciiString();
                    c.getSession().write(MaplePacketCreator.enableActions());
                    break;
                case 509:
                    String sendTo = slea.readMapleAsciiString();
                    String msg = slea.readMapleAsciiString();
                    int recipientId = MapleCharacter.getIdByName(sendTo, c.getWorld());
                    if (recipientId > -1) {
                        try {
                            player.sendNote(recipientId, msg);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        remove(c, itemId);
                    } else {
                        player.dropMessage(5, "This player was not found in the database.");
                    }
                    break;
                case 510:
                    player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.musicChange("Jukebox/Congratulation"), true);
                    break;
                case 512:
                    player.getMap().startMapEffect(ii.getMsg(itemId).replaceFirst("%s", player.getName()).replaceFirst("%s", slea.readMapleAsciiString()), itemId);
                    remove(c, itemId);
                    break;
                case 517:
                    MaplePet pet = player.getPet(0);
                    if (pet != null) {
                        String newName = slea.readMapleAsciiString();
                        if (newName.length() > 2 && newName.length() < 14) {
                            pet.setName(newName);
                            c.getSession().write(MaplePacketCreator.updatePet(pet, true));
                            c.getSession().write(MaplePacketCreator.enableActions());
                            player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.changePetName(c.getPlayer(), newName, 1), true);
                            remove(c, itemId);
                        } else {
                            player.dropMessage("Names must be 2 - 14 characters.");
                        }
                    } else {
                        c.getSession().write(MaplePacketCreator.enableActions());
                    }
                    break;
                case 520:
                    if (ii.getMeso(itemId) + player.getMeso() < Integer.MAX_VALUE) {
                        player.gainMeso(ii.getMeso(itemId), true, false, true);
                        remove(c, itemId);
                        c.getSession().write(MaplePacketCreator.enableActions());
                    } else {
                        player.dropMessage(1, "Cannot hold anymore mesos.");
                    }
                    break;
                case 524:
                    for (int i = 0; i < 3; i++) {
                        if (player.getPet(i) == null) break;
                        pet = player.getPet(i);
                        if (player.getInventory(MapleInventoryType.CASH).getItem(slot) != null) {
                            int petID = pet.getItemId();
                            if (itemId == 5240012 && petID >= 5000028 && petID <= 5000033 ||
                                itemId == 5240021 && petID >= 5000047 && petID <= 5000053 ||
                                itemId == 5240004 && (petID == 5000007 || petID == 5000023) ||
                                itemId == 5240006 && (petID == 5000003 || petID == 5000007 || petID >= 5000009 && petID <= 5000010 || petID == 5000012 || petID == 5000044)) {
                                pet.setFullness(100);
                                int closeGain = 100 * ServerProperties.getPetExpRate;
                                if (pet.getCloseness() + closeGain > 30000) {
                                    pet.setCloseness(30000);
                                } else {
                                    pet.setCloseness(pet.getCloseness() + closeGain);
                                }
                                while (pet.getCloseness() >= ExpTable.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                                    pet.setLevel(pet.getLevel() + 1);
                                    c.getSession().write(MaplePacketCreator.showOwnPetLevelUp(player.getPetIndex(pet)));
                                    player.getMap().broadcastMessage(MaplePacketCreator.showPetLevelUp(c.getPlayer(), player.getPetIndex(pet)));
                                }
                                c.getSession().write(MaplePacketCreator.updatePet(pet, true));
                                player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.commandResponse(player.getId(), (byte) 1, 0, true, true), true);
                                remove(c, itemId);
                            } else if (pet.canConsume(itemId)) {
                                pet.setFullness(100);
                                int closeGain = 100 * ServerProperties.getPetExpRate;
                                if (pet.getCloseness() + closeGain > 30000) {
                                    pet.setCloseness(30000);
                                } else {
                                    pet.setCloseness(pet.getCloseness() + closeGain);
                                }
                                while (pet.getCloseness() >= ExpTable.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                                    pet.setLevel(pet.getLevel() + 1);
                                    c.getSession().write(MaplePacketCreator.showOwnPetLevelUp(player.getPetIndex(pet)));
                                    player.getMap().broadcastMessage(MaplePacketCreator.showPetLevelUp(c.getPlayer(), player.getPetIndex(pet)));
                                }
                                c.getSession().write(MaplePacketCreator.updatePet(pet, true));
                                player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.commandResponse(player.getId(), (byte) 1, 0, true, true), true);
                                remove(c, itemId);
                            }
                            c.getSession().write(MaplePacketCreator.enableActions());
                        } else {
                            break;
                        }
                    }
                    break;
                case 528:
                    if (itemId == 5281000) {
                        Rectangle bounds = new Rectangle((int) player.getPosition().getX(), (int) player.getPosition().getY(), 1, 1);
                        MapleStatEffect mse = new MapleStatEffect();
                        mse.setSourceId(2111003);
                        MapleMist mist = new MapleMist(bounds, c.getPlayer(), mse);
                        player.getMap().spawnMist(mist, 10000, false, true);
                        player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), "Oh no, I farted!", false, 1));
                    }
                    break;
                case 530:
                    ii.getItemEffect(itemId).applyTo(player);
                    remove(c, itemId);
                    break;
                case 533:
                    NPCScriptManager.getInstance().start(c, 9010009, null, null);
                    break;
                case 537:
                    String text = slea.readMapleAsciiString();
                    slea.readInt();
                    player.setChalkboard(text);
                    break;
                case 539:
                    if (player.getCanSmega() && player.getSmegaEnabled() && !player.getAntiCheats().Spam(3000, 4)) {
                        List<String> lines = new LinkedList<String>();
                        for (int i = 0; i < 4; i++) {
                            lines.add(slea.readMapleAsciiString());
                        }
                        c.getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.getAvatarMega(c.getPlayer(), medal, c.getChannel(), itemId, lines, (slea.readByte() != 0)).getBytes());
                        remove(c, itemId);
                    } else {
                        player.dropMessage("You have lost your megaphone privilages.");
                    }
                    break;
                case 552:
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    if (item == null || item.getQuantity() <= 0 || (item.getFlag() & InventoryConstants.KARMA) > 0/* && ii.isKarmaAble(item.getItemId())*/) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    item.setFlag((byte) InventoryConstants.KARMA);
                    /*c.getPlayer().forceUpdateItem(type, item);*/
                    remove(c, itemId);
                    c.getSession().write(MaplePacketCreator.enableActions());
                    break;
                case 557:
                    slea.readInt();
                    int itemSlot = slea.readInt();
                    slea.readInt();
                    final IEquip equip = (IEquip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) itemSlot);
                    if (equip.getVicious() == 2 || c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5570000) == null) {
                        return;
                    }
                    equip.setVicious(equip.getVicious() + 1);
                    equip.setUpgradeSlots(equip.getUpgradeSlots() + 1);
                    remove(c, itemId);
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getSession().write(MaplePacketCreator.sendHammerData(equip.getVicious()));
                    c.getSession().write(MaplePacketCreator.hammerItem(equip));
                    break;
                default:
                    System.out.println("New cash item was used: (ItemID: " + itemId + " Type: " + itemType + ").");
            }
            c.getSession().write(MaplePacketCreator.enableActions());
        } catch (RemoteException re) {
            c.getChannelServer().reconnectWorld();
            System.out.println("Remote Error: " + re);
        }
    }

    private static void remove(MapleClient c, int itemId) {
        MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, itemId, 1, true, false);
    }

    private static boolean getIncubatedItem(MapleClient c, int id) {
        final int[] ids = {1012070, 1302049, 1302063, 1322027, 2000004, 2000005, 2020013, 2020015, 2040307, 2040509, 2040519, 2040521, 2040533, 2040715, 2040717, 2040810, 2040811, 2070005, 2070006, 4020009,};
        final int[] quantitys = {1, 1, 1, 1, 240, 200, 200, 200, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3};
        int amount = 0;
        for (int i = 0; i < ids.length; i++) {
            if (i == id) {
                amount = quantitys[i];
            }
        }
        if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) (id / 1000000))).isFull()) {
            return false;
        }
        MapleInventoryManipulator.addById(c, id, (short) amount);
        return true;
    }

    private int rand(int lbound, int ubound) {
        return MapleCharacter.rand(lbound, ubound);
    }
}