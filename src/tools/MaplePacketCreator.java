package tools;

import client.BuddylistEntry;
import client.IEquip.ScrollResult;
import client.IEquip;
import client.IItem;
import client.ISkill;
import client.Item;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleInventory;
import client.MapleInventoryType;
import client.MapleKeyBinding;
import client.MapleMount;
import client.MaplePet;
import client.MapleQuestStatus;
import client.MapleStat;
import client.SkillMacro;
import client.status.MonsterStatus;
import constants.InventoryConstants;
import constants.ServerProperties;
import database.DatabaseConnection;
import java.awt.Point;
import java.awt.Rectangle;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import net.LongValueHolder;
import net.MaplePacket;
import net.SendPacketOpcode;
import net.channel.handler.SummonDamageHandler.SummonAttackEntry;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.PlayerCoolDownValueHolder;
import net.world.guild.MapleAlliance;
import net.world.guild.MapleGuild;
import net.world.guild.MapleGuildCharacter;
import net.world.guild.MapleGuildSummary;
import server.CashItemInfo;
import server.DueyPackages;
import server.MTSItemInfo;
import server.MapleItemInformationProvider;
import server.MapleShopItem;
import server.MapleTrade;
import server.PlayerInteraction.HiredMerchant;
import server.PlayerInteraction.IPlayerInteractionManager;
import server.PlayerInteraction.MapleMiniGame;
import server.PlayerInteraction.MaplePlayerShop;
import server.PlayerInteraction.MaplePlayerShopItem;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.life.MobSkill;
import server.maps.MapleMap;
import server.maps.MapleReactor;
import server.maps.MapleSummon;
import server.maps.PlayerNPCs;;
import server.movement.LifeMovementFragment;
import tools.data.output.LittleEndianWriter;
import tools.data.output.MaplePacketLittleEndianWriter;

public class MaplePacketCreator {
    private final static byte[] CHAR_INFO_MAGIC = new byte[]{(byte) 0xff, (byte) 0xc9, (byte) 0x9a, 0x3b};
    public static final List<Pair<MapleStat, Integer>> EMPTY_STATUPDATE = Collections.emptyList();
    private final static byte[] ITEM_MAGIC = {0x00, (byte) 0x80, 0x05, (byte) 0xBB, 0x46, (byte) 0xE6, 0x17, 0x02};

    private static int getQuestTimestamp(long realTimestamp) {
        return (int) (((int) (realTimestamp / 1000 / 60)) * 0.1396987) + 27111908;
    }

    private static long getKoreanTimestamp(long realTimestamp) {
        return realTimestamp * 10000 + 116444592000000000L;
    }

    private static long getTime(long realTimestamp) {
        return realTimestamp * 10000 + 116444592000000000L;
    }

    private static void addCharStats(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getId());
        mplew.writeAsciiString(chr.getName());
        mplew.write0(13 - chr.getName().length());
        mplew.write(chr.getGender());
        mplew.write(chr.getSkinColor().getId());
        mplew.writeInt(chr.getFace());
        mplew.writeInt(chr.getHair());
        for (int i = 0; i < chr.getPets().length; i++) {
            if (chr.getPet(i) != null) {
                mplew.writeInt(chr.getPetIndex(i));
            } else {
                mplew.writeInt(0);
            }
            mplew.writeInt(0);
        }
        mplew.write(chr.getLevel());
        mplew.writeShort(chr.getJob().getId());
        mplew.writeShort(chr.getStr());
        mplew.writeShort(chr.getDex());
        mplew.writeShort(chr.getInt());
        mplew.writeShort(chr.getLuk());
        mplew.writeShort(chr.getHp());
        mplew.writeShort(chr.getMaxHp());
        mplew.writeShort(chr.getMp());
        mplew.writeShort(chr.getMaxMp());
        mplew.writeShort(chr.getRemainingAp());
        mplew.writeShort(chr.getRemainingSp());
        mplew.writeInt(chr.getExp());
        mplew.writeShort(chr.getFame());
        mplew.writeInt(0);
        mplew.writeInt(chr.getMapId());
        mplew.write(chr.getInitialSpawnpoint());
        mplew.writeInt(0);
    }

    private static void addCharLook(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean mega) {
        mplew.write(chr.getGender());
        mplew.write(chr.getSkinColor().getId());
        mplew.writeInt(chr.getFace());
        mplew.write(mega ? 0 : 1);
        mplew.writeInt(chr.getHair());
        addCharEquips(mplew, chr.getInventory(MapleInventoryType.EQUIPPED));
        for (int i = 0; i < 3; i++) {
            if (chr.getPet(i) != null) {
                mplew.writeInt(chr.getPet(i).getItemId());
            } else {
                mplew.write0(4);
            }
        }
    }

    private static void addCharEquips(MaplePacketLittleEndianWriter mplew, MapleInventory equip) {
        Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
        for (IItem item : equip.list()) {
            byte pos = (byte) -item.getPosition();
            if (item.getItemId() == 1812006) {
                mplew.write(0x1C);
                mplew.writeInt(1812006);
            } else if (item.getItemId() == 1812007) {
                mplew.write(0x2E);
                mplew.writeInt(1812007);
            } else if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getItemId());
            } else if (pos > 100 && pos != 111) {
                pos -= 100;
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getItemId());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getItemId());
            }
        }
        for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);
        for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);
        IItem cWeapon = equip.getItem((byte) -111);
        mplew.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
    }

    private static void addCharEntry(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        addCharStats(mplew, chr);
        addCharLook(mplew, chr, false);
        if (chr.isGM()) {
            mplew.writeShort(0);
            return;
        }
        mplew.write(0);
        mplew.write(1);
        mplew.writeInt(chr.getRank());
        mplew.writeInt(chr.getRankMove());
        mplew.writeInt(chr.getJobRank());
        mplew.writeInt(chr.getJobRankMove());
    }

    private static void addQuestInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        List<MapleQuestStatus> started = chr.getStartedQuests();
        mplew.writeShort(started.size());
        for (MapleQuestStatus q : started) {
            mplew.writeShort(q.getQuest().getId());
            mplew.writeMapleAsciiString(q.getQuestData());
        }
        List<MapleQuestStatus> completed = chr.getCompletedQuests();
        mplew.writeShort(completed.size());
        for (MapleQuestStatus q : completed) {
            mplew.writeShort(q.getQuest().getId());
            mplew.writeLong(q.getCompletionTime());
        }
    }

    private static void addPetInfo(MaplePacketLittleEndianWriter mplew, MaplePet pet) {
        mplew.write(3);
        mplew.writeInt(pet.getItemId());
        mplew.write(1);
        mplew.writeInt(pet.getUniqueId());
        mplew.writeInt(0);
        mplew.write(ITEM_MAGIC);
        String petname = pet.getName();
        if (petname.length() > 13) {
            petname = petname.substring(0, 13);
        }
        mplew.writeAsciiString(petname);
        for (int i = petname.length(); i < 13; ++i) {
            mplew.write(0);
        }
        mplew.write(pet.getLevel());
        mplew.writeShort(pet.getCloseness());
        mplew.write(pet.getFullness());
        mplew.writeLong(getKoreanTimestamp((long) (System.currentTimeMillis() * 1.2)));
        mplew.write0(10);
    }

    private static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item) {
        addItemInfo(mplew, item, false, false, false);
    }

    private static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, boolean zeroPosition, boolean leaveOut) {
        addItemInfo(mplew, item, zeroPosition, leaveOut, false);
    }

    private static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, boolean zeroPosition, boolean leaveOut, boolean trade) {
        boolean ring = false;
        IEquip equip = null;
        if (item.getType() == IItem.EQUIP) {
            equip = (IEquip) item;
            if (equip.getRingId() > -1) {
                ring = true;
            }
        }
        byte pos = item.getPosition();
        boolean masking = false;
        if (zeroPosition) {
            if (!leaveOut) {
                mplew.write(0);
            }
        } else if (item.getItemId() == 1812007) {
            mplew.writeShort(0x2E);
        } else if (item.getItemId() == 1812006) {
            mplew.writeShort(0x1C);
        } else {
            if (pos < 0) {
                pos *= -1;
                if (pos > 100 || ring) {
                    masking = true;
                    mplew.write(pos - 100);
                } else {
                    mplew.write(pos);
                }
            } else {
                mplew.write(pos);
            }
            if (item.getItemId() / 1000000 == 1 && !trade) {
                mplew.write(0);
            }
        }
        if (item.getPetId() > -1) {
            addPetInfo(mplew, MaplePet.loadFromDb(item.getItemId(), item.getPosition(), item.getPetId()));
            return;
        }
        mplew.write(item.getType());
        mplew.writeInt(item.getItemId());
        if (masking || ring) {
            mplew.write(1);
            mplew.writeInt(ring ? equip.getRingId() : 0xAA52A);
            mplew.writeInt(0);
        } else {
            mplew.write(0);
        }
        mplew.write(ITEM_MAGIC);
        if (item.getType() == IItem.EQUIP) {
            mplew.write(equip.getUpgradeSlots());
            mplew.write(equip.getLevel());
            mplew.writeShort(equip.getStr());
            mplew.writeShort(equip.getDex());
            mplew.writeShort(equip.getInt());
            mplew.writeShort(equip.getLuk());
            mplew.writeShort(equip.getHp());
            mplew.writeShort(equip.getMp());
            mplew.writeShort(equip.getWatk());
            mplew.writeShort(equip.getMatk());
            mplew.writeShort(equip.getWdef());
            mplew.writeShort(equip.getMdef());
            mplew.writeShort(equip.getAcc());
            mplew.writeShort(equip.getAvoid());
            mplew.writeShort(equip.getHands());
            mplew.writeShort(equip.getSpeed());
            mplew.writeShort(equip.getJump());
            mplew.writeMapleAsciiString(equip.getOwner());
            mplew.writeShort(equip.getFlag());
            if (!masking || ring) {
                mplew.write(0);
                mplew.write(equip.getItemLevel());
                mplew.writeShort(0);
                mplew.writeShort(equip.getItemExp());
                mplew.write(equip.getVicious());
                mplew.write0(3);
                if (!ring) {
                    mplew.writeLong(0);
                    mplew.write(HexTool.getByteArrayFromHexString("00 40 E0 FD 3B 37 4F 01"));
                } else {
                    mplew.write(HexTool.getByteArrayFromHexString("10 B0 EB A4 14 97 CA 01"));
                }
            } else {
                mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00 00 26 DD E3 0A 00 40 E0 FD 3B 37 4F 01"));
            }
            mplew.writeInt(-1);
        } else {
            mplew.writeShort(item.getQuantity());
            mplew.writeMapleAsciiString(item.getOwner());
            mplew.writeShort(item.getFlag());
            if (InventoryConstants.isRechargable(item.getItemId())) {
                mplew.write(HexTool.getByteArrayFromHexString("02 00 00 00 54 00 00 34"));
            }
        }
    }

    private static void addInventoryInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getMeso());
        for (byte i = 1; i <= 5; i++) {
            mplew.write(chr.getInventory(MapleInventoryType.getByType(i)).getSlotLimit());
        }
        mplew.write(HexTool.getByteArrayFromHexString("00 40 E0 FD 3B 37 4F 01"));
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        Collection<IItem> equippedC = iv.list();
        List<Item> equipped = new ArrayList<Item>(equippedC.size());
        List<Item> equippedCash = new ArrayList<Item>(equippedC.size());
        for (IItem item : equippedC) {
            if (item.getPosition() <= -100) {
                equippedCash.add((Item) item);
            } else {
                equipped.add((Item) item);
            }
        }
        Collections.sort(equipped);
        for (Item item : equipped) {
            addItemInfo(mplew, item);
        }
        mplew.writeShort(0);
        for (Item item : equippedCash) {
            addItemInfo(mplew, item);
        }
        mplew.writeShort(0);
        for (IItem item : chr.getInventory(MapleInventoryType.EQUIP).list()) {
            addItemInfo(mplew, item);
        }
        mplew.writeInt(0);
        for (IItem item : chr.getInventory(MapleInventoryType.USE).list()) {
            addItemInfo(mplew, item);
        }
        mplew.write(0);
        for (IItem item : chr.getInventory(MapleInventoryType.SETUP).list()) {
            addItemInfo(mplew, item);
        }
        mplew.write(0);
        for (IItem item : chr.getInventory(MapleInventoryType.ETC).list()) {
            addItemInfo(mplew, item);
        }
        mplew.write(0);
        for (IItem item : chr.getInventory(MapleInventoryType.CASH).list()) {
            addItemInfo(mplew, item);
        }
    }

    private static void addSkillInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(0);
        Map<ISkill, MapleCharacter.SkillEntry> skills = chr.getSkills();
        mplew.writeShort(skills.size());
        for (Entry<ISkill, MapleCharacter.SkillEntry> skill : skills.entrySet()) {
            mplew.writeInt(skill.getKey().getId());
            mplew.writeInt(skill.getValue().skillevel);
            mplew.write(ITEM_MAGIC);
            if (skill.getKey().isFourthJob()) {
                mplew.writeInt(skill.getValue().masterlevel);
            }
        }
        mplew.writeShort(chr.getAllCooldowns().size());
        for (PlayerCoolDownValueHolder cooling : chr.getAllCooldowns()) {
            mplew.writeInt(cooling.skillId);
            int timeLeft = (int) (cooling.length + cooling.startTime - System.currentTimeMillis());
            mplew.writeShort(timeLeft / 1000);
        }
    }

    private static void addMonsterBookInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getMonsterBookCover());
        mplew.write(0);
        Map<Integer, Integer> cards = chr.getMonsterBook().getCards();
        mplew.writeShort(cards.size());
        for (Entry<Integer, Integer> all : cards.entrySet()) {
            mplew.writeShort(all.getKey() % 10000);
            mplew.write(all.getValue());
        }
    }

    public static MaplePacket getHello(short mapleVersion, byte[] sendIv, byte[] recvIv) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(0x0E);
        mplew.writeShort(mapleVersion);
        mplew.writeShort(1);
        mplew.write(49);
        mplew.write(recvIv);
        mplew.write(sendIv);
        mplew.write(8);
        return mplew.getPacket();
    }

    public static MaplePacket getPing() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
        mplew.writeShort(SendPacketOpcode.PING.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket getLoginFailed(int reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(8);
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeInt(reason);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket getAuthSuccessRequestPin(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write0(6);
        mplew.writeInt(c.getAccID());
        mplew.write(0);
        mplew.write(c.isGm() ? 1 : 0);
        mplew.write(1);
        mplew.write(1);
        mplew.writeMapleAsciiString(c.getAccountName());
        mplew.write(1);
        mplew.write(0);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.write(1);
        mplew.write(c.getPic() == null ? 1 : 0);
        return mplew.getPacket();
    }

    private static MaplePacket pinOperation(byte mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PIN_OPERATION.getValue());
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static MaplePacket pinRegistered() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PIN_ASSIGNED.getValue());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket requestPin() {
        return pinOperation((byte) 4);
    }

    public static MaplePacket requestPinAfterFailure() {
        return pinOperation((byte) 2);
    }

    public static MaplePacket registerPin() {
        return pinOperation((byte) 1);
    }

    public static MaplePacket pinAccepted() {
        return pinOperation((byte) 0);
    }

    public static MaplePacket WrongPic() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.WRONG_PIC.getValue());
        mplew.write(20);
        return mplew.getPacket();
    }

    public static MaplePacket getServerList(int serverId, String serverName, Map<Integer, Integer> channelLoad) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.write(serverId);
        mplew.writeMapleAsciiString(serverName);
        mplew.write(ServerProperties.getFlag);
        mplew.writeMapleAsciiString(ServerProperties.getEventMessage);
        mplew.write(0x64);
        mplew.write(0x0);
        mplew.write(0x64);
        mplew.write(0x0);
        mplew.write(0x0);
        int lastChannel = 1;
        Set<Integer> channels = channelLoad.keySet();
        for (int i = 30; i > 0; i--) {
            if (channels.contains(i)) {
                lastChannel = i;
                break;
            }
        }
        mplew.write(lastChannel);
        int load;
        for (int i = 1; i <= lastChannel; i++) {
            if (channels.contains(i)) {
                load = channelLoad.get(i);
            } else {
                load = 1200;
            }
            mplew.writeMapleAsciiString(serverName + "-" + i);
            mplew.writeInt(load);
            mplew.write(1);
            mplew.writeShort(i - 1);
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket getEndOfServerList() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.write(0xFF);
        return mplew.getPacket();
    }

    public static MaplePacket getServerStatus(int status) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendPacketOpcode.SERVERSTATUS.getValue());
        mplew.writeShort(status);
        return mplew.getPacket();
    }

    public static MaplePacket getServerIP(InetAddress inetAddr, int port, int clientId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVER_IP.getValue());
        mplew.writeShort(0);
        mplew.write(inetAddr.getAddress());
        mplew.writeShort(port);
        mplew.writeInt(clientId);
        mplew.write0(5);
        return mplew.getPacket();
    }

    public static MaplePacket getChannelChange(InetAddress inetAddr, int port) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHANGE_CHANNEL.getValue());
        mplew.write(1);
        mplew.write(inetAddr.getAddress());
        mplew.writeShort(port);
        return mplew.getPacket();
    }

    public static MaplePacket getCharList(MapleClient c, int serverId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHARLIST.getValue());
        mplew.write(0);
        List<MapleCharacter> chars = c.loadCharacters(serverId);
        mplew.write((byte) chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr);
        }
        mplew.write(c.getPic() == null ? 0 : 1);
        mplew.writeInt(ServerProperties.getMaxCharacters);
        return mplew.getPacket();
    }

    public static MaplePacket enableTV() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.ENABLE_TV.getValue());
        mplew.writeInt(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket removeTV() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
        mplew.writeShort(SendPacketOpcode.REMOVE_TV.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket sendTV(MapleCharacter chr, List<String> messages, int type, MapleCharacter partner) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SEND_TV.getValue());
        mplew.write(partner != null ? 3 : 1);
        mplew.write(type);
        addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        if (partner != null) {
            mplew.writeMapleAsciiString(partner.getName());
        } else {
            mplew.writeShort(0);
        }
        for (int i = 0; i < messages.size(); i++) {
            if (i == 4 && messages.get(4).length() > 15) {
                mplew.writeMapleAsciiString(messages.get(4).substring(0, 15));
            } else {
                mplew.writeMapleAsciiString(messages.get(i));
            }
        }
        mplew.writeInt(1337);
        if (partner != null) {
            addCharLook(mplew, partner, false);
        }
        return mplew.getPacket();
    }

 public static MaplePacket getCharInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WARP_TO_MAP.getValue());
        mplew.writeInt(chr.getClient().getChannel() - 1);
        mplew.write(1);
        mplew.write(1);
        mplew.writeShort(0);
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(Randomizer.getInstance().nextInt());
        }
        mplew.writeLong(-1);
        mplew.write(0);
        addCharStats(mplew, chr);
        mplew.write(chr.getBuddylist().getCapacity());
        mplew.write(0);
        addInventoryInfo(mplew, chr);
        addSkillInfo(mplew, chr);
        addQuestInfo(mplew, chr);
        mplew.writeShort(0);
        addRingInfo(mplew, chr);
        for (int x = 0; x < 15; x++) {
            mplew.write(CHAR_INFO_MAGIC);
        }
        addMonsterBookInfo(mplew, chr);
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.writeLong(getTime(System.currentTimeMillis()));
        return mplew.getPacket();
    }

    public static MaplePacket enableActions() {
        return updatePlayerStats(EMPTY_STATUPDATE, true);
    }

    public static MaplePacket updatePlayerStats(List<Pair<MapleStat, Integer>> stats) {
        return updatePlayerStats(stats, false);
    }

    public static MaplePacket updatePlayerStats(List<Pair<MapleStat, Integer>> stats, boolean itemReaction) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_STATS.getValue());
        mplew.write(itemReaction ? 1 : 0);
        int updateMask = 0;
        for (Pair<MapleStat, Integer> statupdate : stats) {
            updateMask |= statupdate.getLeft().getValue();
        }
        List<Pair<MapleStat, Integer>> mystats = stats;
        if (mystats.size() > 1) {
            Collections.sort(mystats, new Comparator<Pair<MapleStat, Integer>>() {

                @Override
                public int compare(Pair<MapleStat, Integer> o1, Pair<MapleStat, Integer> o2) {
                    int val1 = o1.getLeft().getValue();
                    int val2 = o2.getLeft().getValue();
                    return (val1 < val2 ? -1 : (val1 == val2 ? 0 : 1));
                }
            });
        }
        mplew.writeInt(updateMask);
        for (Pair<MapleStat, Integer> statupdate : mystats) {
            if (statupdate.getLeft().getValue() >= 1) {
                if (statupdate.getLeft().getValue() == 0x1) {
                    mplew.writeShort(statupdate.getRight().shortValue());
                } else if (statupdate.getLeft().getValue() <= 0x4) {
                    mplew.writeInt(statupdate.getRight());
                } else if (statupdate.getLeft().getValue() < 0x20) {
                    mplew.write(statupdate.getRight().shortValue());
                } else if (statupdate.getLeft().getValue() < 0xFFFF) {
                    mplew.writeShort(statupdate.getRight().shortValue());
                } else {
                    mplew.writeInt(statupdate.getRight().intValue());
                }
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket getWarpToMap(MapleMap to, int spawnPoint, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WARP_TO_MAP.getValue());
        mplew.writeInt(chr.getClient().getChannel() - 1);
        mplew.writeShort(0x2);
        mplew.write(0);
        mplew.writeShort(0);
        mplew.writeInt(to.getId());
        mplew.write(spawnPoint);
        mplew.writeShort(chr.getHp());
        mplew.write(0);
        mplew.writeLong(getTime(System.currentTimeMillis()));
        return mplew.getPacket();
    }

    public static MaplePacket spawnPortal(int townId, int targetId, Point pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(14);
        mplew.writeShort(SendPacketOpcode.SPAWN_PORTAL.getValue());
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        if (pos != null) {
            mplew.writeShort(pos.x);
            mplew.writeShort(pos.y);
        }
        return mplew.getPacket();
    }

    public static MaplePacket spawnDoor(int oid, Point pos, boolean town) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendPacketOpcode.SPAWN_DOOR.getValue());
        mplew.write(town ? 1 : 0);
        mplew.writeInt(oid);
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        return mplew.getPacket();
    }

    public static MaplePacket removeDoor(int oid, boolean town) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(10);
        if (town) {
            mplew.writeShort(SendPacketOpcode.SPAWN_PORTAL.getValue());
            mplew.writeInt(999999999);
            mplew.writeInt(999999999);
        } else {
            mplew.writeShort(SendPacketOpcode.REMOVE_DOOR.getValue());
            mplew.write(0);
            mplew.writeInt(oid);
        }
        return mplew.getPacket();
    }

    public static MaplePacket spawnSpecialMapObject(MapleSummon summon, int skillLevel, boolean animated) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(25);
        mplew.writeShort(SendPacketOpcode.SPAWN_SPECIAL_MAPOBJECT.getValue());
        mplew.writeInt(summon.getOwner().getId());
        mplew.writeInt(summon.getObjectId());
        mplew.writeInt(summon.getSkill());
        mplew.write(0x97);
        mplew.write(skillLevel);
        mplew.writeShort(summon.getPosition().x);
        mplew.writeShort(summon.getPosition().y);
        mplew.write(3);
        mplew.write(0);
        mplew.write(0);
        mplew.write(summon.getMovementType().getValue());
        mplew.write(1);
        mplew.write(animated ? 0 : 1);
        return mplew.getPacket();
    }

    public static MaplePacket removeSpecialMapObject(MapleSummon summon, boolean animated) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendPacketOpcode.REMOVE_SPECIAL_MAPOBJECT.getValue());
        mplew.writeInt(summon.getOwner().getId());
        mplew.writeInt(summon.getObjectId());
        mplew.write(animated ? 4 : 1);
        return mplew.getPacket();
    }

    public static MaplePacket getRelogResponse() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.RELOG_RESPONSE.getValue());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket serverMessage(String message) {
        return serverMessage(4, 0, message, true, false);
    }

    public static MaplePacket serverNotice(int type, String message) {
        return serverMessage(type, 0, message, false, false);
    }

    public static MaplePacket serverNotice(int type, int channel, String message) {
        return serverMessage(type, channel, message, false, false);
    }

    public static MaplePacket serverNotice(int type, int channel, String message, boolean smegaEar) {
        return serverMessage(type, channel, message, false, smegaEar);
    }

    private static MaplePacket serverMessage(int type, int channel, String message, boolean servermessage, boolean megaEar) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(type);
        if (servermessage) {
            mplew.write(1);
        }
        mplew.writeMapleAsciiString(message);
        if (type == 3) {
            mplew.write(channel - 1);
            mplew.write(megaEar ? 1 : 0);
        } else if (type == 6) {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket getAvatarMega(MapleCharacter chr, String medal, int channel, int itemId, List<String> message, boolean ear) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.AVATAR_MEGA.getValue());
        mplew.writeInt(itemId);
        mplew.writeMapleAsciiString(medal + chr.getName());
        for (String s : message) {
            mplew.writeMapleAsciiString(s);
        }
        mplew.writeInt(channel - 1);
        mplew.write(ear ? 1 : 0);
        addCharLook(mplew, chr, true);
        return mplew.getPacket();
    }

    public static MaplePacket gachaponMessage(IItem item, String town, MapleCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(0x0B);
        mplew.writeMapleAsciiString(player.getName() + " : got a(n)");
        mplew.writeInt(0);
        mplew.writeMapleAsciiString(town);
        addItemInfo(mplew, item, true, true, false);
        return mplew.getPacket();
    }

    public static MaplePacket spawnNPC(MapleNPC life) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(24);
        mplew.writeShort(SendPacketOpcode.SPAWN_NPC.getValue());
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        if (life.getF() == 1) {
            mplew.write(0);
        } else {
            mplew.write(1);
        }
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket spawnNPCRequestController(MapleNPC life, boolean MiniMap) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(23);
        mplew.writeShort(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        mplew.write(1);
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        if (life.getF() == 1) {
            mplew.write(0);
        } else {
            mplew.write(1);
        }
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.write(MiniMap ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket spawnMonster(MapleMonster life, boolean newSpawn) {
        return spawnMonsterInternal(life, false, newSpawn, false, 0, false);
    }

    public static MaplePacket spawnMonster(MapleMonster life, boolean newSpawn, int effect) {
        return spawnMonsterInternal(life, false, newSpawn, false, effect, false);
    }

    public static MaplePacket controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
        return spawnMonsterInternal(life, true, newSpawn, aggro, 0, false);
    }

    public static MaplePacket makeMonsterInvisible(MapleMonster life) {
        return spawnMonsterInternal(life, true, false, false, 0, true);
    }

    private static void spawnAction(LittleEndianWriter lew, MapleMonster life, int effect, boolean spawn, boolean newSpawn, boolean fake, boolean fr) {
        lew.writeInt(life.getObjectId());
        lew.write(fr ? 5 : 1);
        lew.writeInt(life.getId());
        lew.write(new byte[15]);
        lew.write(0x88);
        lew.write(new byte[6]);
        lew.writeShort(life.getPosition().x);
        lew.writeShort(life.getPosition().y);
        lew.write(life.getStance());
        lew.writeShort(life.getStartFh());
        lew.writeShort(life.getFh());
        if (effect > 0) {
            lew.write(effect);
            lew.write(0);
            lew.writeShort(0);
            if (effect == 15) {
                lew.write(0);
            }
        }
        if (spawn) {
            if (newSpawn) {
                lew.write(-2);
            } else {
                lew.write(-1);
            }
        }
        if (fr) {
            lew.writeShort(fake ? -2 : -1);
        } else {
            lew.write(-1);
        }
        lew.writeInt(0);
    }

    private static MaplePacket spawnMonsterInternal(MapleMonster life, boolean requestController, boolean newSpawn, boolean aggro, int effect, boolean makeInvis) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (makeInvis) {
            mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
            mplew.write(0);
            mplew.writeInt(life.getObjectId());
            return mplew.getPacket();
        }
        if (requestController) {
            mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
            if (aggro) {
                mplew.write(2);
            } else {
                mplew.write(1);
            }
        } else {
            mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
        }
        spawnAction(mplew, life, effect, true, newSpawn, false, false);
        return mplew.getPacket();
    }

    public static MaplePacket spawnFakeMonster(MapleMonster life, int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(1);
        spawnAction(mplew, life, effect, false, false, true, true);
        return mplew.getPacket();
    }

    public static MaplePacket makeMonsterReal(MapleMonster life) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(30);
        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
        spawnAction(mplew, life, 0, false, false, false, true);
        return mplew.getPacket();
    }

    public static MaplePacket stopControllingMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(0);
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    public static MaplePacket moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills) {
        return moveMonsterResponse(objectid, moveid, currentMp, useSkills, 0, 0);
    }

    public static MaplePacket moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(13);
        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
        mplew.writeInt(objectid);
        mplew.writeShort(moveid);
        mplew.write(useSkills ? 1 : 0);
        mplew.writeShort(currentMp);
        mplew.write(skillId);
        mplew.write(skillLevel);
        return mplew.getPacket();
    }

    public static MaplePacket getChatText(int cidfrom, String text, boolean whiteBG, int show) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHATTEXT.getValue());
        mplew.writeInt(cidfrom);
        mplew.write(whiteBG ? 1 : 0);
        mplew.writeMapleAsciiString(text);
        mplew.write(show);
        return mplew.getPacket();
    }

    public static MaplePacket getShowExpGain(int gain, boolean inChat, boolean white) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(3);
        mplew.write(white ? 1 : 0);
        mplew.writeInt(gain);
        mplew.writeInt(inChat ? 1 : 0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write0(18);
        if (inChat) {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket getShowFameGain(int gain) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(4);
        mplew.writeInt(gain);
        return mplew.getPacket();
    }

    public static MaplePacket getShowMesoGain(int gain) {
        return getShowMesoGain(gain, false);
    }

    public static MaplePacket getShowMesoGain(int gain, boolean inChat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        if (!inChat) {
            mplew.write(0);
            mplew.write(1);
            mplew.write(0);
        } else {
            mplew.write(5);
        }
        mplew.writeInt(gain);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket getShowItemGain(int itemId, short quantity) {
        return getShowItemGain(itemId, quantity, false);
    }

    public static MaplePacket getShowItemGain(int itemId, short quantity, boolean inChat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (inChat) {
            mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
            mplew.write(3);
            mplew.write(1);
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.writeShort(0);
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket killMonster(int oid, boolean animation) {
        return killMonster(oid, animation ? 1 : 0);
    }

    public static MaplePacket killMonster(int oid, int animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(animation);
        mplew.write(animation);
        return mplew.getPacket();
    }

    public static MaplePacket dropMesoFromMapObject(int amount, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod) {
        return dropItemFromMapObjectInternal(amount, itemoid, dropperoid, ownerid, dropfrom, dropto, mod, true);
    }

    public static MaplePacket dropItemFromMapObject(int itemid, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod) {
        return dropItemFromMapObjectInternal(itemid, itemoid, dropperoid, ownerid, dropfrom, dropto, mod, false);
    }

    public static MaplePacket dropItemFromMapObjectInternal(int itemid, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod, boolean mesos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        mplew.write(mod);
        mplew.writeInt(itemoid);
        mplew.write(mesos ? 1 : 0);
        mplew.writeInt(itemid);
        mplew.writeInt(ownerid);
        mplew.write(0);
        mplew.writeShort(dropto.x);
        mplew.writeShort(dropto.y);
        mplew.writeInt(ownerid);
        if (mod != 2) {
            mplew.writeShort(dropfrom.x);
            mplew.writeShort(dropfrom.y);
            mplew.writeShort(0);
        }
        if (!mesos) {
            mplew.write(ITEM_MAGIC);
        }
        mplew.write(1);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket spawnPlayerMapobject(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_PLAYER.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel());
        mplew.writeMapleAsciiString(chr.getName());
        if (chr.getGuildId() < 1) {
            mplew.writeMapleAsciiString("");
            mplew.write(new byte[6]);
        } else {
            MapleGuildSummary gs = chr.getClient().getChannelServer().getGuildSummary(chr.getGuildId());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
                mplew.writeShort(gs.getLogoBG());
                mplew.write(gs.getLogoBGColor());
                mplew.writeShort(gs.getLogo());
                mplew.write(gs.getLogoColor());
            } else {
                mplew.writeMapleAsciiString("");
                mplew.write(new byte[6]);
            }
        }
        mplew.write0(6);
        mplew.write(0xFC);
        mplew.write(1);
        long buffmask = 0;
        Integer buffvalue = null;
        if (chr.getBuffedValue(MapleBuffStat.DARKSIGHT) != null && !chr.isHidden()) {
            buffmask |= MapleBuffStat.DARKSIGHT.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.COMBO) != null) {
            buffmask |= MapleBuffStat.COMBO.getValue();
            buffvalue = Integer.valueOf(chr.getBuffedValue(MapleBuffStat.COMBO).intValue());
        }
        if (chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null) {
            buffmask |= MapleBuffStat.SHADOWPARTNER.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.SOULARROW) != null) {
            buffmask |= MapleBuffStat.SOULARROW.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) {
            buffvalue = Integer.valueOf(chr.getBuffedValue(MapleBuffStat.MORPH).intValue());
            mplew.writeInt(2);
        } else {
            if (buffmask == 0) {
                mplew.write0(4);
            } else {
                mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 80"));
            }
        }
        mplew.writeInt((int) ((buffmask >> 40) & 0xffffffffL));
        if (buffvalue != null) {
            if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) {
                mplew.writeShort(buffvalue);
            } else {
                mplew.write(buffvalue.byteValue());
            }
        }
        mplew.writeInt((int) (buffmask & 0xffffffffL));
        int CHAR_MAGIC_SPAWN = Randomizer.getInstance().nextInt();
        mplew.write0(6);
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        mplew.write0(11);
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        mplew.write0(11);
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        mplew.write0(3);
        IItem mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
        if (chr.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null && mount != null) {
            mplew.writeInt(mount.getItemId());
            mplew.writeInt(1004);
        } else {
            mplew.writeLong(0);
        }
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        mplew.write0(9);
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write0(10);
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        mplew.write0(13);
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        mplew.write0(3);
        mplew.writeShort(chr.getJob().getId());
        addCharLook(mplew, chr, false);
        mplew.writeInt(0);
        mplew.writeInt(chr.getItemEffect());
        mplew.writeInt(chr.getChair());
        mplew.writeShort(chr.getPosition().x);
        mplew.writeShort(chr.getPosition().y);
        mplew.write(chr.getStance());
        mplew.writeInt(0);
        if (chr.getMount() == null) {
            mplew.writeInt(1);
            mplew.write0(8);
        } else {
            mplew.writeInt(chr.getMount().getLevel());
            mplew.writeInt(chr.getMount().getExp());
            mplew.writeInt(chr.getMount().getTiredness());
        }
        mplew.writeShort(0);
        addRingLook(mplew, chr, true);
        addRingLook(mplew, chr, false);
        addMarriageRingLook(mplew, chr);
        mplew.write0(3);
        return mplew.getPacket();
    }

    private static void addRingLook(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean crush) {
        mplew.write(0);
//        List<MapleRing> rings;
//        if (crush) {
//            rings = chr.getCrushRings();
//        } else {
//            rings = chr.getFriendshipRings();
//        }
//        mplew.write(rings.size() > 0 ? 1 : 0);
//        for (MapleRing ring : rings) {
//            mplew.writeInt(ring.getRingId());
//            mplew.writeInt(0);
//            mplew.writeInt(ring.getPartnerRingId());
//            mplew.writeInt(0);
//            mplew.writeInt(ring.getItemId());
//        }
    }

    private static void addMarriageRingLook(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(0);
//        mplew.write(chr.getMarriageRings().size() > 0 ? 1 : 0);
//        for (MapleRing ring : chr.getMarriageRings()) {
//            mplew.writeInt(ring.getPartnerChrId());
//            mplew.writeInt(chr.getId());
//            mplew.writeInt(ring.getItemId());
//        }
    }

    private static void addAnnounceBox(MaplePacketLittleEndianWriter mplew, IPlayerInteractionManager interaction) {//check
        mplew.write(4);
        if (interaction.getShopType() == 2) {
            mplew.writeInt(((MaplePlayerShop) interaction).getObjectId());
        } else {
            mplew.writeInt(((MapleMiniGame) interaction).getObjectId());
        }
        mplew.writeMapleAsciiString(interaction.getDescription());
        mplew.write(0);
        mplew.write(interaction.getItemType());
        mplew.write(1);
        mplew.write(interaction.getFreeSlot() > -1 ? 4 : 1);
        if (interaction.getShopType() == 2) {
            mplew.write(0);
        } else {
            mplew.write(((MapleMiniGame) interaction).getStarted() ? 1 : 0);
        }
    }

    public static MaplePacket facialExpression(MapleCharacter from, int expression) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FACIAL_EXPRESSION.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(expression);
        return mplew.getPacket();
    }

    private static void serializeMovementList(LittleEndianWriter lew, List<LifeMovementFragment> moves) {
        lew.write(moves.size());
        for (LifeMovementFragment move : moves) {
            move.serialize(lew);
        }
    }

    public static MaplePacket movePlayer(int cid, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MOVE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(0);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    public static MaplePacket moveSummon(int cid, int oid, Point startPos, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MOVE_SUMMON.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(oid);
        mplew.writeShort(startPos.x);
        mplew.writeShort(startPos.y);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    public static MaplePacket moveMonster(int useskill, int skill, int skill_1, int skill_2, int skill_3, int oid, Point startPos, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.write(useskill);
        mplew.write(skill);
        mplew.write(skill_1);
        mplew.write(skill_2);
        mplew.write(skill_3);
        mplew.write(0);
        mplew.writeShort(startPos.x);
        mplew.writeShort(startPos.y);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    public static MaplePacket summonAttack(int cid, int summonSkillId, int newStance, List<SummonAttackEntry> allDamage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SUMMON_ATTACK.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(newStance);
        mplew.write(allDamage.size());
        for (SummonAttackEntry attackEntry : allDamage) {
            mplew.writeInt(attackEntry.getMonsterOid());
            mplew.write(6);
            mplew.writeInt(attackEntry.getDamage());
        }
        return mplew.getPacket();
    }

    public static MaplePacket closeRangeAttack(int cid, int skill, int stance, int numAttackedAndDamage, List<Pair<Integer, List<Integer>>> damage, int speed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CLOSE_RANGE_ATTACK.getValue());
        if (skill == 4211006) {
            addMesoExplosion(mplew, cid, skill, stance, numAttackedAndDamage, 0, damage, speed);
        } else {
            addAttackBody(mplew, cid, skill, stance, numAttackedAndDamage, 0, damage, speed);
        }
        return mplew.getPacket();
    }

    public static MaplePacket rangedAttack(int cid, int skill, int stance, int numAttackedAndDamage, int projectile, List<Pair<Integer, List<Integer>>> damage, int speed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.RANGED_ATTACK.getValue());
        addAttackBody(mplew, cid, skill, stance, numAttackedAndDamage, projectile, damage, speed);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket magicAttack(int cid, int skill, int stance, int numAttackedAndDamage, List<Pair<Integer, List<Integer>>> damage, int charge, int speed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAGIC_ATTACK.getValue());
        addAttackBody(mplew, cid, skill, stance, numAttackedAndDamage, 0, damage, speed);
        if (charge != -1) {
            mplew.writeInt(charge);
        }
        return mplew.getPacket();
    }

    private static void addAttackBody(LittleEndianWriter lew, int cid, int skill, int stance, int numAttackedAndDamage, int projectile, List<Pair<Integer, List<Integer>>> damage, int speed) {
        lew.writeInt(cid);
        lew.write(numAttackedAndDamage);
        lew.write(0x5B);
        if (skill > 0) {
            lew.write(0xFF);
            lew.writeInt(skill);
        } else {
            lew.write(0);
        }
        lew.write(0);
        lew.write(stance);
        lew.write(speed);
        lew.write(0x0A);
        lew.writeInt(projectile);
        for (Pair<Integer, List<Integer>> oned : damage) {
            if (oned.getRight() != null) {
                lew.writeInt(oned.getLeft().intValue());
                lew.write(0xFF);
                for (Integer eachd : oned.getRight()) {
                    lew.writeInt(eachd.intValue());
                }
            }
        }
    }

    private static void addMesoExplosion(LittleEndianWriter lew, int cid, int skill, int stance, int numAttackedAndDamage, int projectile, List<Pair<Integer, List<Integer>>> damage, int speed) {
        lew.writeInt(cid);
        lew.write(numAttackedAndDamage);
        lew.write(0x1E);
        lew.writeInt(skill);
        lew.write(0);
        lew.write(stance);
        lew.write(speed);
        lew.write(0x0A);
        lew.writeInt(projectile);
        for (Pair<Integer, List<Integer>> oned : damage) {
            if (oned.getRight() != null) {
                lew.writeInt(oned.getLeft().intValue());
                lew.write(0xFF);
                lew.write(oned.getRight().size());
                for (Integer eachd : oned.getRight()) {
                    lew.writeInt(eachd.intValue());
                }
            }
        }
    }

    private static int doubleToShortBits(double d) {
        return (int) (Double.doubleToLongBits(d) >> 48);
    }

    public static MaplePacket getNPCShop(MapleClient c, int sid, List<MapleShopItem> items, boolean pitches) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_NPC_SHOP.getValue());
        mplew.writeInt(sid);
        mplew.writeShort(items.size());
        for (MapleShopItem item : items) {
            mplew.writeInt(item.getItemId());
            mplew.writeInt(pitches ? 0 : item.getPrice());
            mplew.writeInt(pitches ? item.getPrice() : 0);
            mplew.write0(8);
            if (!InventoryConstants.isRechargable(item.getItemId())) {
                mplew.writeShort(1);
                mplew.writeShort(item.getBuyable());
            } else {
                mplew.writeShort(0);
                mplew.writeInt(0);
                mplew.writeShort(doubleToShortBits(ii.getPrice(item.getItemId())));
                mplew.writeShort(ii.getSlotMax(c, item.getItemId()));
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket confirmShopTransaction(byte code) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
        mplew.write(code);
        return mplew.getPacket();
    }

    public static MaplePacket addInventorySlot(MapleInventoryType type, IItem item) {
        return addInventorySlot(type, item, false);
    }

    public static MaplePacket addInventorySlot(MapleInventoryType type, IItem item, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(fromDrop ? 1 : 0);
        mplew.write(HexTool.getByteArrayFromHexString("01 00"));
        mplew.write(type.getType());
        mplew.write(item.getPosition());
        addItemInfo(mplew, item, true, false);
        return mplew.getPacket();
    }

    public static MaplePacket updateInventorySlot(MapleInventoryType type, IItem item) {
        return updateInventorySlot(type, item, false);
    }

    public static MaplePacket updateInventorySlot(MapleInventoryType type, IItem item, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        if (fromDrop) {
            mplew.write(1);
        } else {
            mplew.write(0);
        }
        mplew.write(HexTool.getByteArrayFromHexString("01 01"));
        mplew.write(type.getType());
        mplew.write(item.getPosition());
        mplew.write(0);
        mplew.writeShort(item.getQuantity());
        return mplew.getPacket();
    }

    public static MaplePacket moveInventoryItem(MapleInventoryType type, byte src, byte dst) {
        return moveInventoryItem(type, src, dst, (byte) -1);
    }

    public static MaplePacket moveInventoryItem(MapleInventoryType type, byte src, byte dst, byte equipIndicator) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01 02"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.writeShort(dst);
        if (equipIndicator != -1) {
            mplew.write(equipIndicator);
        }
        return mplew.getPacket();
    }

    public static MaplePacket moveAndMergeInventoryItem(MapleInventoryType type, byte src, byte dst, short total) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 02 03"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.write(1);
        mplew.write(type.getType());
        mplew.writeShort(dst);
        mplew.writeShort(total);
        return mplew.getPacket();
    }

    public static MaplePacket moveAndMergeWithRestInventoryItem(MapleInventoryType type, byte src, byte dst, short srcQ, short dstQ) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 02 01"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.writeShort(srcQ);
        mplew.write(0x01);
        mplew.write(type.getType());
        mplew.writeShort(dst);
        mplew.writeShort(dstQ);
        return mplew.getPacket();
    }

    public static MaplePacket clearInventoryItem(MapleInventoryType type, byte slot, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(fromDrop ? 1 : 0);
        mplew.write(HexTool.getByteArrayFromHexString("01 03"));
        mplew.write(type.getType());
        mplew.writeShort(slot);
        return mplew.getPacket();
    }

    public static MaplePacket scrolledItem(IItem scroll, IItem item, boolean destroyed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1);
        mplew.write(destroyed ? 2 : 3);
        mplew.write(scroll.getQuantity() > 0 ? 1 : 3);
        mplew.write(MapleInventoryType.USE.getType());
        mplew.writeShort(scroll.getPosition());
        if (scroll.getQuantity() > 0) {
            mplew.writeShort(scroll.getQuantity());
        }
        mplew.write(3);
        if (!destroyed) {
            mplew.write(MapleInventoryType.EQUIP.getType());
            mplew.writeShort(item.getPosition());
            mplew.write(0);
        }
        mplew.write(MapleInventoryType.EQUIP.getType());
        mplew.writeShort(item.getPosition());
        if (!destroyed) {
            addItemInfo(mplew, item, true, true);
        }
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket getScrollEffect(int chr, ScrollResult scrollSuccess, boolean legendarySpirit) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_SCROLL_EFFECT.getValue());
        mplew.writeInt(chr);
        switch (scrollSuccess) {
            case SUCCESS:
                mplew.writeShort(1);
                mplew.writeShort(legendarySpirit ? 1 : 0);
                break;
            case FAIL:
                mplew.writeShort(0);
                mplew.writeShort(legendarySpirit ? 1 : 0);
                break;
            case CURSE:
                mplew.write(0);
                mplew.write(1);
                mplew.writeShort(legendarySpirit ? 1 : 0);
                break;
        }
        return mplew.getPacket();
    }

    public static MaplePacket removePlayerFromMap(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        mplew.writeInt(cid);
        return mplew.getPacket();
    }

    public static MaplePacket removeItemFromMap(int oid, int animation, int cid) {
        return removeItemFromMap(oid, animation, cid, false, 0);
    }

    public static MaplePacket removeItemFromMap(int oid, int animation, int cid, boolean pet, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        mplew.write(animation);
        mplew.writeInt(oid);
        if (animation >= 2) {
            mplew.writeInt(cid);
            if (pet) {
                mplew.write(slot);
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateCharLook(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_LOOK.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        addCharLook(mplew, chr, false);
        addRingLook(mplew, chr, true);
        addRingLook(mplew, chr, false);
        addMarriageRingLook(mplew, chr);
        mplew.write0(3);
        return mplew.getPacket();
    }

    public static MaplePacket dropInventoryItem(MapleInventoryType type, short src) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01 03"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        if (src < 0) {
            mplew.write(1);
        }
        return mplew.getPacket();
    }

    public static MaplePacket dropInventoryItemUpdate(MapleInventoryType type, IItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01 01"));
        mplew.write(type.getType());
        mplew.writeShort(item.getPosition());
        mplew.writeShort(item.getQuantity());
        return mplew.getPacket();
    }

    public static MaplePacket damagePlayer(int skill, int monsteridfrom, int cid, int damage, int fake, int direction, boolean pgmr, int pgmr_1, boolean is_pg, int oid, int pos_x, int pos_y) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DAMAGE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.write(skill);
        mplew.writeInt(damage);
        mplew.writeInt(monsteridfrom);
        mplew.write(direction);
        if (pgmr) {
            mplew.write(pgmr_1);
            mplew.write(is_pg ? 1 : 0);
            mplew.writeInt(oid);
            mplew.write(6);
            mplew.writeShort(pos_x);
            mplew.writeShort(pos_y);
            mplew.write(0);
        } else {
            mplew.writeShort(0);
        }
        mplew.writeInt(damage);
        if (fake > 0) {
            mplew.writeInt(fake);
        }
        return mplew.getPacket();
    }

    public static MaplePacket charNameResponse(String charname, boolean nameUsed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHAR_NAME_RESPONSE.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket addNewCharEntry(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        mplew.write(0);
        addCharEntry(mplew, chr);
        return mplew.getPacket();
    }

    public static MaplePacket startQuest(MapleCharacter c, short quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.write(1);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket deleteCharResponse(int cid, int state) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DELETE_CHAR_RESPONSE.getValue());
        mplew.writeInt(cid);
        mplew.write(state);
        return mplew.getPacket();
    }

    public static MaplePacket charInfo(MapleCharacter chr, boolean isSelf) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHAR_INFO.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel());
        mplew.writeShort(chr.getJob().getId());
        mplew.writeShort(chr.getFame());
        mplew.write(chr.isMarried() ? 1 : 0);
        String guildName = "";
        String allianceName = "";
        MapleGuildSummary gs = chr.getClient().getChannelServer().getGuildSummary(chr.getGuildId());
        if (chr.getGuildId() > 0 && gs != null) {
            guildName = gs.getName();
            try {
                MapleAlliance alliance = chr.getClient().getChannelServer().getWorldInterface().getAlliance(gs.getAllianceId());
                if (alliance != null) {
                    allianceName = alliance.getName();
                }
            } catch (RemoteException re) {
                re.printStackTrace();
                chr.getClient().getChannelServer().reconnectWorld();
            }
        }
        mplew.writeMapleAsciiString(guildName);
        mplew.writeMapleAsciiString(allianceName);
        mplew.write(isSelf ? 1 : 0);
        MaplePet[] pets = chr.getPets();
        IItem inv = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -114);
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                mplew.write(pets[i].getUniqueId());
                mplew.writeInt(pets[i].getItemId());
                mplew.writeMapleAsciiString(pets[i].getName());
                mplew.write(pets[i].getLevel());
                mplew.writeShort(pets[i].getCloseness());
                mplew.write(pets[i].getFullness());
                mplew.writeShort(0);
                mplew.writeInt(inv != null ? inv.getItemId() : 0);
            }
        }
        mplew.write(0);
        if (chr.getMount() != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18) != null) {
            mplew.write(chr.getMount().getId());
            mplew.writeInt(chr.getMount().getLevel());
            mplew.writeInt(chr.getMount().getExp());
            mplew.writeInt(chr.getMount().getTiredness());
        } else {
            mplew.write(0);
        }
//        List<Integer> wishList = chr.getWishList();
        mplew.write(0);
//        for (int sn : wishList) {
//            mplew.writeInt(0);
////        }
        mplew.writeInt(0);//chr.getMonsterBook().getBookLevel());
        mplew.writeInt(0);//chr.getMonsterBook().getNormalCard());
        mplew.writeInt(0);//chr.getMonsterBook().getSpecialCard());
        mplew.writeInt(0);//chr.getMonsterBook().getTotalCards());
        mplew.writeInt(0);//chr.getMonsterBookCover() > 0 ? MapleItemInformationProvider.getInstance().getCardMobId(chr.getMonsterBookCover()) : 0);
        IItem i = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -49);
        mplew.writeInt(i == null ? 0 : i.getItemId());
        mplew.write0(2);
        return mplew.getPacket();
    }

    public static MaplePacket giveBuff(int buffid, int bufflength, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        long mask = getLongMask(statups);
        if (bufflength % 10000000 != 1004 && bufflength != 5221006) {
            mplew.writeLong(0);
        } else {
            mplew.writeInt(0);
        }
        mplew.writeLong(mask);
        if (bufflength % 10000000 == 1004 || bufflength == 5221006) {
            mplew.writeInt(0);
        }
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().shortValue());
            mplew.writeInt(buffid);
            mplew.writeInt(bufflength);
        }
        if (bufflength % 10000000 == 1004 || bufflength == 5221006) {
            mplew.writeInt(0);
        } else {
            mplew.writeShort(0);
        }
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        if (bufflength % 10000000 == 1004 || bufflength == 5221006) {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket giveBuff(int buffid, int bufflength, List<Pair<MapleBuffStat, Integer>> statups, boolean morph, boolean ismount, MapleMount mount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        long mask = getLongMask(statups);
        if (ismount) {
            mplew.writeInt(0);
        } else {
            mplew.writeLong(0);
        }
        mplew.writeLong(mask);
        if (!ismount) {
            for (Pair<MapleBuffStat, Integer> statup : statups) {
                mplew.writeShort(statup.getRight().shortValue());
                mplew.writeInt(buffid);
                mplew.writeInt(bufflength);
            }
            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);
        } else {
            mplew.writeInt(0);
            mplew.writeShort(0);
            mplew.writeInt(mount.getItemId());
            mplew.writeInt(mount.getSkillId());
            mplew.write0(8);
        }
        return mplew.getPacket();
    }

    public static MaplePacket showMonsterRiding(int cid, List<Pair<MapleBuffStat, Integer>> statups, MapleMount mount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.write0(6);
        mplew.write(0x20);
        mplew.write0(11);
        mplew.writeInt(mount.getItemId());
        mplew.writeInt(mount.getSkillId());
        mplew.write0(7);
        return mplew.getPacket();
    }

    public static MaplePacket forfeitQuest(MapleCharacter c, short quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket completeQuest(MapleCharacter c, short quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.write(HexTool.getByteArrayFromHexString("02 60 4C F8 80 8E D9 CA 01"));
        return mplew.getPacket();
    }

    public static MaplePacket updateQuestInfo(MapleCharacter c, short quest, int npc, byte progress) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(progress);
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    private static <E extends LongValueHolder> long getLongMask(List<Pair<E, Integer>> statups) {
        long mask = 0;
        for (Pair<E, Integer> statup : statups) {
            mask |= statup.getLeft().getValue();
        }
        return mask;
    }

    private static <E extends LongValueHolder> long getLongMaskFromList(List<E> statups) {
        long mask = 0;
        for (E statup : statups) {
            mask |= statup.getValue();
        }
        return mask;
    }

    private static <E extends LongValueHolder> long getLongMaskD(List<Pair<MapleDisease, Integer>> statups) {
        long mask = 0;
        for (Pair<MapleDisease, Integer> statup : statups) {
            mask |= statup.getLeft().getValue();
        }
        return mask;
    }

    private static <E extends LongValueHolder> long getLongMaskFromListD(List<MapleDisease> statups) {
        long mask = 0;
        for (MapleDisease statup : statups) {
            mask |= statup.getValue();
        }
        return mask;
    }

    public static MaplePacket giveDebuff(List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        long mask = getLongMaskD(statups);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        for (Pair<MapleDisease, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().shortValue());
            mplew.writeShort(skill.getSkillId());
            mplew.writeShort(skill.getSkillLevel());
            mplew.writeInt((int) skill.getDuration());
        }
        mplew.writeShort(0);
        mplew.writeShort(900);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket giveForeignDebuff(int cid, List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        long mask = getLongMaskD(statups);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        for (int i = 0; i < statups.size(); i++) {
            mplew.writeShort(skill.getSkillId());
            mplew.writeShort(skill.getSkillLevel());
        }
        mplew.writeShort(0);
        mplew.writeShort(900);
        return mplew.getPacket();
    }

    public static MaplePacket cancelForeignDebuff(int cid, List<MapleDisease> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        long mask = getLongMaskFromListD(statups);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        return mplew.getPacket();
    }

    public static MaplePacket giveForeignBuff(int cid, List<Pair<MapleBuffStat, Integer>> statups, boolean morph) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        long mask = getLongMask(statups);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            if (morph) {
                mplew.writeInt(statup.getRight().intValue());
            } else {
                mplew.writeShort(statup.getRight().shortValue());
            }
        }
        mplew.write(0);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    private static boolean isFirstLong(List<MapleBuffStat> statups) {
        for (MapleBuffStat stat : statups) {
            if (stat.isFirst()) {
                return true;
            }
        }
        return false;
    }

    public static MaplePacket cancelForeignBuff(int cid, List<MapleBuffStat> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        long mask = getLongMaskFromList(statups);
        if (!isFirstLong(statups)) {
            mplew.writeLong(0);
            mplew.writeLong(mask);
        } else {
            mplew.writeLong(mask);
            mplew.writeLong(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket cancelBuff(List<MapleBuffStat> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
        long mask = getLongMaskFromList(statups);
        if (!isFirstLong(statups)) {
            mplew.writeLong(0);
            mplew.writeLong(mask);
        } else {
            mplew.writeLong(mask);
            mplew.writeLong(0);
        }
        mplew.write(mask == MapleBuffStat.DASH.getValue() ? 4 : 3);
        return mplew.getPacket();
    }

    public static MaplePacket cancelDebuff(List<MapleDisease> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
        mplew.writeLong(0);
        mplew.writeLong(getLongMaskFromListD(statups));
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerShopChat(MapleCharacter c, String chat, boolean owner) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("06 08"));
        mplew.write(owner ? 0 : 1);
        mplew.writeMapleAsciiString(c.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerShopNewVisitor(MapleCharacter c, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x04);
        mplew.write(slot);
        addCharLook(mplew, c, false);
        mplew.writeMapleAsciiString(c.getName());
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerShopRemoveVisitor(int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x0A);
        if (slot > 0) {
            mplew.write(slot);
        }
        return mplew.getPacket();
    }

    public static MaplePacket getTradePartnerAdd(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("04 01"));
        mplew.write(1);
        addCharLook(mplew, c, false);
        mplew.writeMapleAsciiString(c.getName());
        return mplew.getPacket();
    }

    public static MaplePacket getTradeInvite(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("02 03"));
        mplew.write(3);
        mplew.writeMapleAsciiString(c.getName());
        mplew.write(HexTool.getByteArrayFromHexString("B7 50 00 00"));
        return mplew.getPacket();
    }

    public static MaplePacket getTradeMesoSet(byte number, int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xF);
        mplew.write(number);
        mplew.writeInt(meso);
        return mplew.getPacket();
    }

    public static MaplePacket getTradeItemAdd(byte number, IItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xE);
        mplew.write(number);
        addItemInfo(mplew, item);
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerShopItemUpdate(MaplePlayerShop shop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x17);
        mplew.write(shop.getItems().size());
        for (MaplePlayerShopItem item : shop.getItems()) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeInt(item.getPrice());
            addItemInfo(mplew, item.getItem(), true, true);
        }
        return mplew.getPacket();
    }

//    public static MaplePacket getPlayerShop(MapleClient c, MaplePlayerShop shop, boolean owner) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
//        mplew.write(HexTool.getByteArrayFromHexString("05 04 04"));
//        mplew.write(owner ? 0 : 1);
//        mplew.write(0);
//        addCharLook(mplew, shop.getOwner(), false);
//        mplew.writeMapleAsciiString(shop.getOwner().getName());
//        mplew.write(1);
//        addCharLook(mplew, shop.getOwner(), false);
//        mplew.writeMapleAsciiString(shop.getOwner().getName());
//        mplew.write(0xFF);
//        mplew.writeMapleAsciiString(shop.getDescription());
//        List<MaplePlayerShopItem> items = shop.getItems();
//        mplew.write(0x10);
//        mplew.write(items.size());
//        for (MaplePlayerShopItem item : items) {
//            mplew.writeShort(item.getBundles());
//            mplew.writeShort(item.getItem().getQuantity());
//            mplew.writeInt(item.getPrice());
//            addItemInfo(mplew, item.getItem(), true, true);
//        }
//        return mplew.getPacket();
//    }

    public static MaplePacket getTradeStart(MapleClient c, MapleTrade trade, byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("05 03 02"));
        mplew.write(number);
        if (number == 1) {
            mplew.write(0);
            addCharLook(mplew, trade.getPartner().getChr(), false);
            mplew.writeMapleAsciiString(trade.getPartner().getChr().getName());
        }
        mplew.write(number);
        addCharLook(mplew, c.getPlayer(), false);
        mplew.writeMapleAsciiString(c.getPlayer().getName());
        mplew.write(0xFF);
        return mplew.getPacket();
    }

    public static MaplePacket getTradeConfirmation() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x10);
        return mplew.getPacket();
    }

    public static MaplePacket getTradeCompletion(byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xA);
        mplew.write(number);
        mplew.write(7);
        return mplew.getPacket();
    }

    public static MaplePacket getTradeCancel(byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xA);
        mplew.write(number);
        mplew.write(2);
        return mplew.getPacket();
    }

    public static MaplePacket addCharBox(MapleCharacter c, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        addAnnounceBox(mplew, c.getInteraction());
        return mplew.getPacket();
    }

    public static MaplePacket removeCharBox(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalk(int npc, byte msgType, String talk, String endBytes) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.write(msgType);
        mplew.writeMapleAsciiString(talk);
        mplew.write(HexTool.getByteArrayFromHexString(endBytes));
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalk(int npc, byte msgType, String talk, String endBytes, int speaker) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.write(msgType);
        mplew.write(speaker);
        if (msgType == 14) {
            mplew.writeInt(0);
        }
        mplew.writeMapleAsciiString(talk);
        mplew.write(HexTool.getByteArrayFromHexString(endBytes));
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkStyle(int npc, String talk, int styles[]) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.write(7);
        mplew.write(0);
        mplew.writeMapleAsciiString(talk);
        mplew.write(styles.length);
        for (int i = 0; i < styles.length; i++) {
            mplew.writeInt(styles[i]);
        }
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkNum(int npc, String talk, int def, int min, int max) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.writeShort(3);
        mplew.writeMapleAsciiString(talk);
        mplew.writeInt(def);
        mplew.writeInt(min);
        mplew.writeInt(max);
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkText(int npc, String talk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.writeShort(2);
        mplew.writeMapleAsciiString(talk);
        mplew.write0(12);
        return mplew.getPacket();
    }

    public static MaplePacket showLevelup(int cid) {
        return showForeignEffect(cid, 0);
    }

    public static MaplePacket showJobChange(int cid) {
        return showForeignEffect(cid, 8);
    }

    public static MaplePacket showForeignEffect(int cid, int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(effect);
        return mplew.getPacket();
    }

    public static MaplePacket showBuffeffect(int cid, int skillid, int effectid) {
        return showBuffeffect(cid, skillid, effectid, (byte) 3);
    }

    public static MaplePacket showBuffeffect(int cid, int skillid, int effectid, int skillLevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(2);
        mplew.write(skillLevel);//todo update rest
        return mplew.getPacket();
    }

    public static MaplePacket showOwnBuffEffect(int skillid, int effectid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(1);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket showOwnBerserk(int skilllevel, boolean Berserk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(1);
        mplew.writeInt(1320006);
        mplew.write(0x7D);
        mplew.write(skilllevel);
        mplew.write(Berserk ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket showBerserk(int cid, int skilllevel, boolean Berserk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(1);
        mplew.writeInt(1320006);
        mplew.write(0x7D);
        mplew.write(skilllevel);
        mplew.write(Berserk ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket updateSkill(int skillid, int level, int masterlevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_SKILLS.getValue());
        mplew.write(1);
        mplew.writeShort(1);
        mplew.writeInt(skillid);
        mplew.writeInt(level);
        mplew.writeInt(masterlevel);
        mplew.write(ITEM_MAGIC);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket updateQuestMobKills(MapleQuestStatus status) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(status.getQuest().getId());
        mplew.write(1);
        String killStr = "";
        for (int kills : status.getMobKills().values()) {
            killStr += StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3);
        }
        mplew.writeMapleAsciiString(killStr);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket getShowQuestCompletion(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_QUEST_COMPLETION.getValue());
        mplew.writeShort(id);
        return mplew.getPacket();
    }

    public static MaplePacket getKeymap(Map<Integer, MapleKeyBinding> keybindings) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.KEYMAP.getValue());
        mplew.write(0);
        for (int x = 0; x < 90; x++) {
            MapleKeyBinding binding = keybindings.get(Integer.valueOf(x));
            if (binding != null) {
                mplew.write(binding.getType());
                mplew.writeInt(binding.getAction());
            } else {
                mplew.write(0);
                mplew.writeInt(0);
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket getWhisper(String sender, int channel, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(0x12);
        mplew.writeMapleAsciiString(sender);
        mplew.writeShort(channel - 1);
        mplew.writeMapleAsciiString(text);
        return mplew.getPacket();
    }

    public static MaplePacket getWhisperReply(String target, byte reply) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(0x0A);
        mplew.writeMapleAsciiString(target);
        mplew.write(reply);
        return mplew.getPacket();
    }

    public static MaplePacket getInventoryFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getShowInventoryFull() {
        return getShowInventoryStatus(0xff);
    }

    public static MaplePacket showItemUnavailable() {
        return getShowInventoryStatus(0xfe);
    }

    public static MaplePacket getShowInventoryStatus(int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0);
        mplew.write(mode);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket getStorage(int npcId, byte slots, Collection<IItem> items, int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x16);
        mplew.writeInt(npcId);
        mplew.write(slots);
        mplew.writeShort(0x7E);
        mplew.write0(6);
        mplew.writeInt(meso);
        mplew.writeShort(items.size() > 0 ? 0x100 : 0);
        boolean first = true;
        for (IItem item : items) {
            addItemInfo(mplew, item, true, true);
            if (first) {
                mplew.write(0);
                mplew.write(items.size() - 1);
                first = false;
            }
        }
        mplew.writeShort(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getStorageFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x11);
        return mplew.getPacket();
    }

    public static MaplePacket mesoStorage(byte slots, int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x13);
        mplew.write(slots);
        mplew.writeShort(2);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeInt(meso);
        return mplew.getPacket();
    }

    public static MaplePacket storeStorage(byte slots, MapleInventoryType type, Collection<IItem> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0xD);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(items.size());
        for (IItem item : items) {
            addItemInfo(mplew, item, true, true);
        }
        return mplew.getPacket();
    }

    public static MaplePacket takeOutStorage(byte slots, MapleInventoryType type, Collection<IItem> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x9);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(items.size());
        for (IItem item : items) {
            addItemInfo(mplew, item, true, true);
        }
        return mplew.getPacket();
    }

    public static MaplePacket showMonsterHP(int oid, int remhppercentage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_MONSTER_HP.getValue());
        mplew.writeInt(oid);
        mplew.write(remhppercentage);
        return mplew.getPacket();
    }

    public static MaplePacket showBossHP(int oid, int currHP, int maxHP, byte tagColor, byte tagBgColor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(5);
        mplew.writeInt(oid);
        mplew.writeInt(currHP);
        mplew.writeInt(maxHP);
        mplew.write(tagColor);
        mplew.write(tagBgColor);
        return mplew.getPacket();
    }

    public static MaplePacket giveFameResponse(int mode, String charname, int newfame) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(charname);
        mplew.write(mode);
        mplew.writeShort(newfame);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket giveFameErrorResponse(int status) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(status);
        return mplew.getPacket();
    }

    public static MaplePacket receiveFame(int mode, String charnameFrom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(5);
        mplew.writeMapleAsciiString(charnameFrom);
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static MaplePacket partyCreated() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(8);
        mplew.writeShort(0x8b);
        mplew.writeShort(2);
        mplew.write(CHAR_INFO_MAGIC);
        mplew.write(CHAR_INFO_MAGIC);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket partyInvite(MapleCharacter from) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(4);
        mplew.writeInt(from.getParty().getId());
        mplew.writeMapleAsciiString(from.getName());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket partyStatusMessage(int message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(message);
        return mplew.getPacket();
    }

    public static MaplePacket partyStatusMessage(int message, String charname) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(message);
        mplew.writeMapleAsciiString(charname);
        return mplew.getPacket();
    }

    private static void addPartyStatus(int forchannel, MapleParty party, LittleEndianWriter lew, boolean leaving) {
        List<MaplePartyCharacter> partymembers = new ArrayList<MaplePartyCharacter>(party.getMembers());
        while (partymembers.size() < 6) {
            partymembers.add(new MaplePartyCharacter());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeAsciiString(getRightPaddedStr(partychar.getName(), '\0', 13));
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getJobId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getLevel());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.isOnline()) {
                lew.writeInt(partychar.getChannel() - 1);
            } else {
                lew.writeInt(-2);
            }
        }
        lew.writeInt(party.getLeader().getId());
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel) {
                lew.writeInt(partychar.getMapid());
            } else {
                lew.writeInt(0);
            }
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel && !leaving) {
                lew.writeInt(partychar.getDoorTown());
                lew.writeInt(partychar.getDoorTarget());
                lew.writeInt(partychar.getDoorPosition().x);
                lew.writeInt(partychar.getDoorPosition().y);
            } else {
                lew.writeInt(0);
                lew.writeInt(0);
                lew.writeInt(0);
                lew.writeInt(0);
            }
        }
    }

    public static MaplePacket updateParty(int forChannel, MapleParty party, PartyOperation op, MaplePartyCharacter target) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        switch (op) {
            case DISBAND:
            case EXPEL:
            case LEAVE:
                mplew.write(0xC);
                mplew.writeInt(40546);
                mplew.writeInt(target.getId());
                if (op == PartyOperation.DISBAND) {
                    mplew.write(0);
                    mplew.writeInt(party.getId());
                } else {
                    mplew.write(1);
                    if (op == PartyOperation.EXPEL) {
                        mplew.write(1);
                    } else {
                        mplew.write(0);
                    }
                    mplew.writeMapleAsciiString(target.getName());
                    addPartyStatus(forChannel, party, mplew, false);
                }
                break;
            case JOIN:
                mplew.write(0xF);
                mplew.writeInt(40546);
                mplew.writeMapleAsciiString(target.getName());
                addPartyStatus(forChannel, party, mplew, false);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                mplew.write(0x7);
                mplew.writeInt(party.getId());
                addPartyStatus(forChannel, party, mplew, false);
                break;
            case CHANGE_LEADER:
                mplew.write(0x1A);
                mplew.writeInt(target.getId());
                mplew.write(0);
                break;
        }
        return mplew.getPacket();
    }

    public static MaplePacket partyPortal(int townId, int targetId, Point position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.writeShort(0x23);
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        mplew.writeShort(position.x);
        mplew.writeShort(position.y);
        return mplew.getPacket();
    }

    public static MaplePacket updatePartyMemberHP(int cid, int curhp, int maxhp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_PARTYMEMBER_HP.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(curhp);
        mplew.writeInt(maxhp);
        return mplew.getPacket();
    }

    public static MaplePacket multiChat(String name, String chattext, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MULTICHAT.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(chattext);
        return mplew.getPacket();
    }

    public static MaplePacket applyMonsterStatus(int oid, Map<MonsterStatus, Integer> stats, int skill, boolean monsterSkill, int delay) {
        return applyMonsterStatus(oid, stats, skill, monsterSkill, delay, null);
    }

    public static MaplePacket applyMonsterStatusTest(int oid, int mask, int delay, MobSkill mobskill, int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(mask);
        mplew.writeShort(1);
        mplew.writeShort(mobskill.getSkillId());
        mplew.writeShort(mobskill.getSkillLevel());
        mplew.writeShort(0);
        mplew.writeShort(delay);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket applyMonsterStatusTest2(int oid, int mask, int skill, int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(mask);
        mplew.writeShort(value);
        mplew.writeInt(skill);
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket applyMonsterStatus(int oid, Map<MonsterStatus, Integer> stats, int skill, boolean monsterSkill, int delay, MobSkill mobskill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        mplew.writeLong(0);
        int mask = 0;
        boolean isHero = false; // is hero move, should be legend
        for (MonsterStatus stat : stats.keySet()) {
            mask |= stat.getValue();
//            isHero = stat.isHero();
        }
        mplew.writeInt(isHero ? mask : 0);
        mplew.writeInt(isHero ? 0 : mask);
        for (Integer val : stats.values()) {
            mplew.writeShort(val);
            if (monsterSkill) {
                mplew.writeShort(mobskill.getSkillId());
                mplew.writeShort(mobskill.getSkillLevel());
            } else {
                mplew.writeInt(skill);
            }
            mplew.writeShort(-1);
        }
        mplew.writeInt(delay);
        mplew.write(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket cancelMonsterStatus(int oid, Map<MonsterStatus, Integer> stats) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        mplew.writeLong(0);
        int mask = 0;
        boolean isHero = false;
        for (MonsterStatus stat : stats.keySet()) {
            mask |= stat.getValue();
//            isHero = stat.isHero();
        }
        mplew.writeInt(isHero ? mask : 0);
        mplew.writeInt(isHero ? 0 : mask);
        if (isHero) {
            mplew.write(1); // amount to cancel
        } else {
            mplew.write(3);
            mplew.write(2);
        }
        mplew.write0(10);
        return mplew.getPacket();
    }

    public static MaplePacket getClock(int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(2);
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    public static MaplePacket getClockTime(int hour, int min, int sec) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(1);
        mplew.write(hour);
        mplew.write(min);
        mplew.write(sec);
        return mplew.getPacket();
    }

    public static MaplePacket spawnMist(int oid, int ownerCid, int skillId, Rectangle mistPosition, int level) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_MIST.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(oid);
        mplew.writeInt(ownerCid);
        mplew.writeInt(skillId);
        mplew.write(level);
        mplew.writeShort(8);
        mplew.writeInt(mistPosition.x);
        mplew.writeInt(mistPosition.y);
        mplew.writeInt(mistPosition.x + mistPosition.width);
        mplew.writeInt(mistPosition.y + mistPosition.height);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket removeMist(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_MIST.getValue());
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    public static MaplePacket damageSummon(int cid, int summonSkillId, int damage, int unkByte, int monsterIdFrom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DAMAGE_SUMMON.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(unkByte);
        mplew.writeInt(damage);
        mplew.writeInt(monsterIdFrom);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket damageMonster(int oid, int damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeInt(damage);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket healMonster(int oid, int heal) {
        return damageMonster(oid, -heal);
    }

    public static MaplePacket updateBuddylist(Collection<BuddylistEntry> buddylist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(7);
        mplew.write(buddylist.size());
        for (BuddylistEntry buddy : buddylist) {
            if (buddy.isVisible()) {
                mplew.writeInt(buddy.getCharacterId());
                mplew.writeAsciiString(getRightPaddedStr(buddy.getName(), '\0', 13));
                mplew.write(0);
                mplew.writeInt(buddy.getChannel() - 1);
                mplew.writeAsciiString(getRightPaddedStr(buddy.getGroup(), '\0', 13));
                mplew.writeInt(0);
            }
        }
        for (int x = 0; x < buddylist.size(); x++) {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket buddylistMessage(byte message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(message);
        return mplew.getPacket();
    }

    public static MaplePacket requestBuddylistAdd(int cidFrom, int cid, String nameFrom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(9);
        mplew.writeInt(cidFrom);
        mplew.writeMapleAsciiString(nameFrom);
        mplew.writeInt(cidFrom);
        mplew.writeAsciiString(getRightPaddedStr(nameFrom, '\0', 11));
        mplew.write(0x09);
        mplew.write(0xf0);
        mplew.write(0x01);
        mplew.writeInt(0x0f);
        mplew.writeNullTerminatedAsciiString("ZDev Group");
        mplew.writeInt(cid);
        return mplew.getPacket();
    }

    public static MaplePacket updateBuddyChannel(int characterid, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(0x14);
        mplew.writeInt(characterid);
        mplew.write(0);
        mplew.writeInt(channel);
        return mplew.getPacket();
    }

    public static MaplePacket itemEffect(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_EFFECT.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static MaplePacket updateBuddyCapacity(int capacity) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(0x15);
        mplew.write(capacity);
        return mplew.getPacket();
    }

    public static MaplePacket showChair(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_CHAIR.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static MaplePacket cancelChair() {
        return cancelChair(-1);
    }

    public static MaplePacket cancelChair(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_CHAIR.getValue());
        if (id == -1) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeShort(id);
        }
        return mplew.getPacket();
    }

    public static MaplePacket spawnReactor(MapleReactor reactor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();
        mplew.writeShort(SendPacketOpcode.REACTOR_SPAWN.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.writeInt(reactor.getId());
        mplew.write(reactor.getState());
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        mplew.write(0);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket triggerReactor(MapleReactor reactor, int stance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();
        mplew.writeShort(SendPacketOpcode.REACTOR_HIT.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        mplew.writeShort(stance);
        mplew.write(0);
        mplew.write(5);
        return mplew.getPacket();
    }

    public static MaplePacket destroyReactor(MapleReactor reactor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();
        mplew.writeShort(SendPacketOpcode.REACTOR_DESTROY.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        return mplew.getPacket();
    }

    public static MaplePacket musicChange(String song) {
        return environmentChange(song, 6);
    }

    public static MaplePacket showEffect(String effect) {
        return environmentChange(effect, 3);
    }

    public static MaplePacket playSound(String sound) {
        return environmentChange(sound, 4);
    }

    public static MaplePacket environmentChange(String env, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(env);
        return mplew.getPacket();
    }

    public static MaplePacket startMapEffect(String msg, int itemid, boolean active) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.write(active ? 0 : 1);
        mplew.writeInt(itemid);
        if (active) {
            mplew.writeMapleAsciiString(msg);
        }
        return mplew.getPacket();
    }

    public static MaplePacket removeMapEffect() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.write(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket showGuildInfo(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x1A);
        if (c == null) {
            mplew.write(0);
            return mplew.getPacket();
        }
        MapleGuild g = c.getClient().getChannelServer().getGuild(c.getMGC());
        if (g == null) {
            mplew.write(0);
            return mplew.getPacket();
        } else {
            c.setGuildRank(c.getGuildRank());
        }
        mplew.write(1);
        mplew.writeInt(g.getId());
        mplew.writeMapleAsciiString(g.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(g.getRankTitle(i));
        }
        Collection<MapleGuildCharacter> members = g.getMembers();
        mplew.write(members.size());
        for (MapleGuildCharacter mgc : members) {
            mplew.writeInt(mgc.getId());
        }
        for (MapleGuildCharacter mgc : members) {
            mplew.writeAsciiString(getRightPaddedStr(mgc.getName(), '\0', 13));
            mplew.writeInt(mgc.getJobId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeInt(g.getSignature());
            mplew.writeInt(mgc.getAllianceRank());
        }
        mplew.writeInt(g.getCapacity());
        mplew.writeShort(g.getLogoBG());
        mplew.write(g.getLogoBGColor());
        mplew.writeShort(g.getLogo());
        mplew.write(g.getLogoColor());
        mplew.writeMapleAsciiString(g.getNotice());
        mplew.writeInt(g.getGP());
        mplew.writeInt(g.getAllianceId());
        return mplew.getPacket();
    }

    public static MaplePacket guildMemberOnline(int gid, int cid, boolean bOnline) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3d);
        mplew.writeInt(gid);
        mplew.writeInt(cid);
        mplew.write(bOnline ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket guildInvite(int gid, String charName) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x05);
        mplew.writeInt(gid);
        mplew.writeMapleAsciiString(charName);
        return mplew.getPacket();
    }

    public static MaplePacket denyGuildInvitation(String charname) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x37);
        mplew.writeMapleAsciiString(charname);
        return mplew.getPacket();
    }

    public static MaplePacket genericGuildMessage(byte code) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(code);
        return mplew.getPacket();
    }

    public static MaplePacket newGuildMember(MapleGuildCharacter mgc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x27);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeAsciiString(getRightPaddedStr(mgc.getName(), '\0', 13));
        mplew.writeInt(mgc.getJobId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getGuildRank());
        mplew.writeInt(mgc.isOnline() ? 1 : 0);
        mplew.writeInt(1);
        mplew.writeInt(3);
        return mplew.getPacket();
    }

    public static MaplePacket memberLeft(MapleGuildCharacter mgc, boolean bExpelled) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(bExpelled ? 0x2f : 0x2c);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeMapleAsciiString(mgc.getName());
        return mplew.getPacket();
    }

    public static MaplePacket changeRank(MapleGuildCharacter mgc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x40);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.write(mgc.getGuildRank());
        return mplew.getPacket();
    }

    public static MaplePacket guildNotice(int gid, String notice) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x44);
        mplew.writeInt(gid);
        mplew.writeMapleAsciiString(notice);
        return mplew.getPacket();
    }

    public static MaplePacket guildMemberLevelJobUpdate(MapleGuildCharacter mgc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3C);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getJobId());
        return mplew.getPacket();
    }

    public static MaplePacket rankTitleChange(int gid, String[] ranks) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3E);
        mplew.writeInt(gid);
        for (int i = 0; i < 5; i++) {
            mplew.writeMapleAsciiString(ranks[i]);
        }
        return mplew.getPacket();
    }

    public static MaplePacket guildDisband(int gid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x32);
        mplew.writeInt(gid);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket guildEmblemChange(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x42);
        mplew.writeInt(gid);
        mplew.writeShort(bg);
        mplew.write(bgcolor);
        mplew.writeShort(logo);
        mplew.write(logocolor);
        return mplew.getPacket();
    }

    public static MaplePacket guildCapacityChange(int gid, int capacity) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3A);
        mplew.writeInt(gid);
        mplew.write(capacity);
        return mplew.getPacket();
    }

    public static void addThread(MaplePacketLittleEndianWriter mplew, ResultSet rs) throws SQLException {
        mplew.writeInt(rs.getInt("localthreadid"));
        mplew.writeInt(rs.getInt("postercid"));
        mplew.writeMapleAsciiString(rs.getString("name"));
        mplew.writeLong(getKoreanTimestamp(rs.getLong("timestamp")));
        mplew.writeInt(rs.getInt("icon"));
        mplew.writeInt(rs.getInt("replycount"));
    }

    public static MaplePacket BBSThreadList(ResultSet rs, int start) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BBS_OPERATION.getValue());
        mplew.write(0x06);
        if (!rs.last()) {
            mplew.write(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        int threadCount = rs.getRow();
        if (rs.getInt("localthreadid") == 0) {
            mplew.write(1);
            addThread(mplew, rs);
            threadCount--;
        } else {
            mplew.write(0);
        }
        if (!rs.absolute(start + 1)) {
            rs.first();
            start = 0;
        }
        mplew.writeInt(threadCount);
        mplew.writeInt(Math.min(10, threadCount - start));
        for (int i = 0; i < Math.min(10, threadCount - start); i++) {
            addThread(mplew, rs);
            rs.next();
        }
        return mplew.getPacket();
    }

    public static MaplePacket showThread(int localthreadid, ResultSet threadRS, ResultSet repliesRS) throws SQLException, RuntimeException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BBS_OPERATION.getValue());
        mplew.write(0x07);
        mplew.writeInt(localthreadid);
        mplew.writeInt(threadRS.getInt("postercid"));
        mplew.writeLong(getKoreanTimestamp(threadRS.getLong("timestamp")));
        mplew.writeMapleAsciiString(threadRS.getString("name"));
        mplew.writeMapleAsciiString(threadRS.getString("startpost"));
        mplew.writeInt(threadRS.getInt("icon"));
        if (repliesRS != null) {
            int replyCount = threadRS.getInt("replycount");
            mplew.writeInt(replyCount);
            int i;
            for (i = 0; i < replyCount && repliesRS.next(); i++) {
                mplew.writeInt(repliesRS.getInt("replyid"));
                mplew.writeInt(repliesRS.getInt("postercid"));
                mplew.writeLong(getKoreanTimestamp(repliesRS.getLong("timestamp")));
                mplew.writeMapleAsciiString(repliesRS.getString("content"));
            }
            if (i != replyCount || repliesRS.next()) {
                throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
            }
        } else {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket showGuildRanks(int npcid, ResultSet rs) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x49);
        mplew.writeInt(npcid);
        if (!rs.last()) {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(rs.getRow());
        rs.beforeFirst();
        while (rs.next()) {
            mplew.writeMapleAsciiString(rs.getString("name"));
            mplew.writeInt(rs.getInt("GP"));
            mplew.writeInt(rs.getInt("logo"));
            mplew.writeInt(rs.getInt("logoColor"));
            mplew.writeInt(rs.getInt("logoBG"));
            mplew.writeInt(rs.getInt("logoBGColor"));
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateGP(int gid, int GP) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x48);
        mplew.writeInt(gid);
        mplew.writeInt(GP);
        return mplew.getPacket();
    }

    public static MaplePacket skillEffect(MapleCharacter from, int skillId, int level, byte flags, int speed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);
        mplew.write(level);
        mplew.write(flags);
        mplew.write(speed);
        mplew.write0(5);
        return mplew.getPacket();
    }

    public static MaplePacket skillCancel(MapleCharacter from, int skillId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);
        return mplew.getPacket();
    }

    public static MaplePacket showMagnet(int mobid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_MAGNET.getValue());
        mplew.writeInt(mobid);
        mplew.write(success);
        return mplew.getPacket();
    }

    public static MaplePacket sendHint(String hint, int width, int height) {
        if (width < 1) {
            width = hint.length() * 10;
            if (width < 40) {
                width = 40;
            }
        }
        if (height < 5) {
            height = 5;
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_HINT.getValue());
        mplew.writeMapleAsciiString(hint);
        mplew.writeShort(width);
        mplew.writeShort(height);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket messengerInvite(String from, int messengerid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x03);
        mplew.writeMapleAsciiString(from);
        mplew.write(0);
        mplew.writeInt(messengerid);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket warpCS(MapleClient c) {
        return warpCS(c, false);
    }

    public static MaplePacket warpMTS(MapleClient c) {
        return warpCS(c, true);
    }

    public static MaplePacket warpCS(MapleClient c, boolean mts) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        MapleCharacter chr = c.getPlayer();
        if (!mts) {
            mplew.writeShort(SendPacketOpcode.CS_OPEN.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.MTS_OPEN.getValue());
        }
        mplew.writeLong(-1);
        mplew.write(0);
        addCharStats(mplew, chr);
        mplew.write0(2);
        addInventoryInfo(mplew, chr);
        mplew.write0(17);
        for (int i = 0; i < 15; i++) {
            mplew.write(CHAR_INFO_MAGIC);
        }
        mplew.write0(13);
        if (!mts) {
            mplew.write(1);
        }
        mplew.writeMapleAsciiString(chr.getClient().getAccountName());
        if (mts) {
            mplew.write(HexTool.getByteArrayFromHexString("88 13 00 00 07 00 00 00 F4 01 00 00 18 00 00 00 A8 00 00 00 70 AA A7 C5 4E C1 CA 01"));
        } else {
            mplew.write0(127);
            for (int i = 1; i <= 8; i++) {
                for (int j = 0; j < 2; j++) {
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50200004);
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50200069);
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50200117);
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50100008);
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(50000047);
                }
            }
            mplew.write0(7);
            mplew.writeInt(0x78);
        }
        return mplew.getPacket();
    }

    public static MaplePacket sendSpouseChat(String name, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPOUSE_CHAT.getValue());
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(msg);
        return mplew.getPacket();
    }

    public static MaplePacket addMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x00);
        mplew.write(position);
        addCharLook(mplew, chr, true);
        mplew.writeMapleAsciiString(from);
        mplew.write(channel);
        mplew.write(0x00);
        return mplew.getPacket();
    }

    public static MaplePacket removeMessengerPlayer(int position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x02);
        mplew.write(position);
        return mplew.getPacket();
    }

    public static MaplePacket updateMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x07);
        mplew.write(position);
        addCharLook(mplew, chr, true);
        mplew.writeMapleAsciiString(from);
        mplew.write(channel);
        mplew.write(0x00);
        return mplew.getPacket();
    }

    public static MaplePacket joinMessenger(int position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x01);
        mplew.write(position);
        return mplew.getPacket();
    }

    public static MaplePacket messengerChat(String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x06);
        mplew.writeMapleAsciiString(text);
        return mplew.getPacket();
    }

    public static MaplePacket messengerNote(String text, int mode, int mode2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(text);
        mplew.write(mode2);
        return mplew.getPacket();
    }

    public static MaplePacket updatePet(MaplePet pet) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("00 02 03 05"));
        mplew.write(pet.getPosition());
        mplew.writeShort(0);
        mplew.write(5);
        mplew.write(pet.getPosition());
        mplew.write(0);
        addPetInfo(mplew, pet);
        return mplew.getPacket();
    }

    public static MaplePacket showPet(MapleCharacter chr, MaplePet pet, boolean remove) {
        return showPet(chr, pet, remove, false);
    }

    public static MaplePacket showPet(MapleCharacter chr, MaplePet pet, boolean remove, boolean hunger) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_PET.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getPetIndex(pet));
        if (remove) {
            mplew.write(0);
            mplew.write(hunger ? 1 : 0);
        } else {
            mplew.writeShort(1);
            mplew.writeInt(pet.getItemId());
            mplew.writeMapleAsciiString(pet.getName());
            mplew.writeInt(pet.getUniqueId());
            mplew.writeInt(0);
            mplew.writeShort(pet.getPos().x);
            mplew.writeShort(pet.getPos().y);
            mplew.write(pet.getStance());
            mplew.writeInt(pet.getFh());
        }
        return mplew.getPacket();
    }

    public static MaplePacket movePet(int cid, int pid, int slot, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MOVE_PET.getValue());
        mplew.writeInt(cid);
        mplew.write(slot);
        mplew.writeInt(pid);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    public static MaplePacket petChat(int cid, int index, int act, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_CHAT.getValue());
        mplew.writeInt(cid);
        mplew.write(index);
        mplew.write(0);
        mplew.write(act);
        mplew.writeMapleAsciiString(text);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket commandResponse(int cid, int index, int animation, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_COMMAND.getValue());
        mplew.writeInt(cid);
        mplew.write(index);
        mplew.write((animation == 1 && success) ? 1 : 0);
        mplew.write(animation);
        if (animation == 1) {
            mplew.write(0);
        } else {
            mplew.writeShort(success ? 1 : 0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket showOwnPetLevelUp(int index) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(4);
        mplew.write(0);
        mplew.write(index);
        return mplew.getPacket();
    }

    public static MaplePacket showPetLevelUp(MapleCharacter chr, int index) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(4);
        mplew.write(0);
        mplew.write(index);
        return mplew.getPacket();
    }

    public static MaplePacket changePetName(MapleCharacter chr, String newname, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_NAMECHANGE.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(0);
        mplew.writeMapleAsciiString(newname);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket petStatUpdate(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_STATS.getValue());
        int mask = 0;
        mask |= MapleStat.PET.getValue();
        mplew.write(0);
        mplew.writeInt(mask);
        MaplePet[] pets = chr.getPets();
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                mplew.writeInt(pets[i].getUniqueId());
                mplew.writeInt(0);
            } else {
                mplew.writeLong(0);
            }
        }
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket showForcedEquip() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FORCED_MAP_EQUIP.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket summonSkill(int cid, int summonSkillId, int newStance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SUMMON_SKILL.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(newStance);
        return mplew.getPacket();
    }

    public static MaplePacket skillCooldown(int sid, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.COOLDOWN.getValue());
        mplew.writeInt(sid);
        mplew.writeShort(time);
        return mplew.getPacket();
    }

    public static MaplePacket skillBookSuccess(MapleCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.USE_SKILL_BOOK.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.writeInt(maxlevel);
        mplew.write(canuse ? 1 : 0);
        mplew.write(success ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket getMacros(SkillMacro[] macros) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SKILL_MACRO.getValue());
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (macros[i] != null) {
                count++;
            }
        }
        mplew.write(count);
        for (SkillMacro macro : macros) {
            if (macro != null) {
                mplew.writeMapleAsciiString(macro.getName());
                mplew.write(macro.getShout());
                mplew.writeInt(macro.getSkill1());
                mplew.writeInt(macro.getSkill2());
                mplew.writeInt(macro.getSkill3());
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerNPC(PlayerNPCs npc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_NPC.getValue());
        mplew.write(0x01);
        mplew.writeInt(npc.getId());
        mplew.writeMapleAsciiString(npc.getName());
        mplew.write(0);
        mplew.write(npc.getSkin());
        mplew.writeInt(npc.getFace());
        mplew.write(0);
        mplew.writeInt(npc.getHair());
        Map<Byte, Integer> equip = npc.getEquips();
        Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        for (byte position : equip.keySet()) {
            byte pos = (byte) (position * -1);
            if (pos > 100) {
                pos -= 100;
                myEquip.put(pos, equip.get(position));
            } else {
                if (myEquip.get(pos) == null) {
                    myEquip.put(pos, equip.get(position));
                }
            }
        }
        for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.writeShort(-1);
        Integer cWeapon = equip.get((byte) -111);
        if (cWeapon != null) {
            mplew.writeInt(cWeapon);
        } else {
            mplew.writeInt(0);
        }
        for (int i = 0; i < 12; i++) {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateAriantPQRanking(String name, int score, boolean empty) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        mplew.writeShort(SendPacketOpcode.ARIANT_SCORE.getValue());
        mplew.write(empty ? 0 : 1);
        if (!empty) {
            mplew.writeMapleAsciiString(name);
            mplew.writeInt(score);
        }
        return mplew.getPacket();
    }

    public static MaplePacket catchMonster(int monsobid, int itemid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (itemid == 2270002) {
            mplew.writeShort(SendPacketOpcode.CATCH_ARIANT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.CATCH_MOUNT.getValue());
        }
        mplew.writeInt(monsobid);
        mplew.writeInt(itemid);
        mplew.write(success);
        return mplew.getPacket();
    }

    public static MaplePacket showAllCharacter(int chars, int unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALL_CHARLIST.getValue());
        mplew.write(1);
        mplew.writeInt(chars);
        mplew.writeInt(unk);
        return mplew.getPacket();
    }

    public static MaplePacket showAllCharacterInfo(int worldid, List<MapleCharacter> chars) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALL_CHARLIST.getValue());
        mplew.write(0);
        mplew.write(worldid);
        mplew.write(chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr);
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateMount(int charid, MapleMount mount, boolean levelup) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_MOUNT.getValue());
        mplew.writeInt(charid);
        mplew.writeInt(mount.getLevel());
        mplew.writeInt(mount.getExp());
        mplew.writeInt(mount.getTiredness());
        mplew.write(levelup ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket boatPacket(boolean type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOAT_EFFECT.getValue());
        mplew.writeShort(type ? 1 : 2);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameReady(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x34);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameUnReady(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x35);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameStart(MapleMiniGame game, int loser) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("37 0" + loser));
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameSkipOwner(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x39);
        mplew.write(0x01);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameRequestTie(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x2C);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameDenyTie(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x2D);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("05 00"));
        mplew.write(2);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameSkipVisitor(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("39 00"));
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameMoveOmok(MapleMiniGame game, int move1, int move2, int move3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(12);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("3A"));
        mplew.writeInt(move1);
        mplew.writeInt(move2);
        mplew.write(move3);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameRemoveVisitor() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("0A 01"));
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameClose(byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xA);
        mplew.write(1);
        mplew.write(3);
        return mplew.getPacket();
    }

    public static MaplePacket getMatchCardStart(MapleMiniGame game, int loser) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("37 0" + loser));
        mplew.write(HexTool.getByteArrayFromHexString("0C"));
        int last = 13;
        if (game.getMatchesToWin() > 10) {
            last = 31;
        } else if (game.getMatchesToWin() > 6) {
            last = 21;
        }
        for (int i = 1; i < last; i++) {
            mplew.writeInt(game.getCardId(i));
        }
        return mplew.getPacket();
    }

    public static MaplePacket getMatchCardSelect(MapleMiniGame game, int turn, int slot, int firstslot, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("3E 0" + turn));
        if (turn == 1) {
            mplew.write(slot);
        } else if (turn == 0) {
            mplew.write(slot);
            mplew.write(firstslot);
            mplew.write(type);
        }
        return mplew.getPacket();
    }

    public static MaplePacket removeOmokBox(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket removeMatchcardBox(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerShopChat(MapleCharacter c, String chat, byte slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("06 08"));
        mplew.write(8);
        mplew.write(slot);
        mplew.writeMapleAsciiString(c.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static MaplePacket getTradeChat(MapleCharacter c, String chat, boolean owner) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("06 08"));
        mplew.write(8);
        mplew.write(owner ? 0 : 1);
        mplew.writeMapleAsciiString(c.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantBox() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0x2F);
        mplew.write(0x07);
        return mplew.getPacket();
    }

    public static MaplePacket getHiredMerchant(MapleCharacter chr, HiredMerchant hm, boolean firstTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("05 05 04"));
        mplew.write(hm.isOwner(chr) ? 0 : 1);
        mplew.write(0);
        mplew.writeInt(hm.getItemId());
        mplew.writeMapleAsciiString("Hired Merchant");
        for (int i = 0; i < 3; i++) {
            if (hm.getVisitors()[i] != null) {
                mplew.write(i + 1);
                addCharLook(mplew, hm.getVisitors()[i], false);
                mplew.writeMapleAsciiString(hm.getVisitors()[i].getName());
            }
        }
        mplew.write(0xFF);
        mplew.writeShort(0);
        mplew.writeMapleAsciiString(hm.getOwnerName());
        if (hm.isOwner(chr)) {
            mplew.writeInt(chr.getId());
            mplew.write(firstTime ? 1 : 0);
            mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00"));
        }
        mplew.writeMapleAsciiString(hm.getDescription());
        mplew.write(0x10);
        mplew.writeInt(0);
        mplew.write(hm.getItems().size());
        if (hm.getItems().isEmpty()) {
            mplew.write(0);
        } else {
            for (MaplePlayerShopItem item : hm.getItems()) {
                mplew.writeShort(item.getBundles());
                mplew.writeShort(item.getItem().getQuantity());
                mplew.writeInt(item.getPrice());
                addItemInfo(mplew, item.getItem(), true, true);
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateHiredMerchant(HiredMerchant hm) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x19);
        mplew.writeInt(0);
        mplew.write(hm.getItems().size());
        for (MaplePlayerShopItem item : hm.getItems()) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeInt(item.getPrice());
            addItemInfo(mplew, item.getItem(), true, true);
        }
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantChat(String message, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(6);
        mplew.write(8);
        mplew.write(slot);
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantVisitorLeave(int slot, boolean owner) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x0A);
        if (!owner) {
            mplew.write(slot);
        }
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantOwnerLeave() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x2A);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantMaintenanceMessage() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(5);
        mplew.write(0x00);
        mplew.write(0x12);
        return mplew.getPacket();
    }

    public static MaplePacket hiredMerchantVisitorAdd(MapleCharacter chr, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x04);
        mplew.write(slot);
        addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        return mplew.getPacket();
    }

    public static MaplePacket spawnHiredMerchant(HiredMerchant hm) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_HIRED_MERCHANT.getValue());
        mplew.writeInt(hm.getOwnerId());
        mplew.writeInt(hm.getItemId());
        mplew.writeShort((short) hm.getPosition().getX());
        mplew.writeShort((short) hm.getPosition().getY());
        mplew.writeShort(0);
        mplew.writeMapleAsciiString(hm.getOwnerName());
        mplew.write(0x05);
        mplew.writeInt(hm.getObjectId());
        mplew.writeMapleAsciiString(hm.getDescription());
        mplew.write(hm.getItemId() % 10);
        mplew.write(HexTool.getByteArrayFromHexString("00 04"));
        return mplew.getPacket();
    }

    public static MaplePacket destroyHiredMerchant(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DESTROY_HIRED_MERCHANT.getValue());
        mplew.writeInt(id);
        return mplew.getPacket();
    }

    public static MaplePacket leaveHiredMerchant(int slot, int status2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xA);
        mplew.write(slot);
        mplew.write(status2);
        return mplew.getPacket();
    }

    public static MaplePacket spawnPlayerNPC(PlayerNPCs npc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        mplew.write(1);
        mplew.writeInt(npc.getObjectId());
        mplew.writeInt(npc.getId());
        mplew.writeShort(npc.getPosition().x);
        mplew.writeShort(npc.getCY());
        mplew.write(1);
        mplew.writeShort(npc.getFH());
        mplew.writeShort(npc.getRX0());
        mplew.writeShort(npc.getRX1());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket sendYellowTip(String tip) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.YELLOW_TIP.getValue());
        mplew.write(0xFF);
        mplew.writeMapleAsciiString(tip);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket giveInfusion(int bufflength, int speed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        mplew.writeLong(0);
        mplew.writeLong(MapleBuffStat.MORPH.getValue());
        mplew.writeShort(speed);
        mplew.writeInt(5121009);
        mplew.writeLong(0);
        mplew.writeShort(bufflength);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket giveSpeedInfusion(int buffid, int bufflength, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        mplew.writeLong(getLongMask(statups));
        mplew.write0(10);
        mplew.writeInt(statups.get(0).getRight().intValue());
        mplew.writeInt(buffid);
        mplew.write0(10);
        mplew.write(bufflength);
        mplew.write(0x58);
        mplew.write(0x02);
        return mplew.getPacket();
    }

    public static MaplePacket givePirateBuff(int buffid, int bufflength, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        mplew.writeLong(getLongMask(statups));
        mplew.write0(10);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().shortValue());
            mplew.writeShort(0);
            mplew.writeInt(buffid);
            mplew.write0(5);
            mplew.writeShort(bufflength);
        }
        mplew.write0(3);
        return mplew.getPacket();
    }

    public static MaplePacket showPirateBuff(int cid, int skillid, int time, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        long mask = getLongMask(statups);
        mplew.writeLong(mask);
        mplew.write0(10);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeShort(statup.getRight());
            mplew.writeShort(0);
            mplew.writeInt(skillid);
            mplew.write0(5);
            mplew.writeShort(time);
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket showSpeedInfusion(int cid, int skillid, int time, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        long mask = getLongMask(statups);
        mplew.writeLong(mask);
        mplew.write0(10);
        mplew.writeInt(statups.get(0).getRight());
        mplew.writeInt(skillid);
        mplew.write0(10);
        mplew.writeShort(time);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket giveForeignInfusion(int cid, int speed, int duration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(0);
        mplew.writeLong(MapleBuffStat.MORPH.getValue());
        mplew.writeShort(0);
        mplew.writeInt(speed);
        mplew.writeInt(5121009);
        mplew.writeLong(0);
        mplew.writeInt(duration);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket sendMTS(List<MTSItemInfo> items, int tab, int type, int page, int pages) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x15);
        mplew.writeInt(pages * 16);
        mplew.writeInt(items.size());
        mplew.writeInt(tab);
        mplew.writeInt(type);
        mplew.writeInt(page);
        mplew.write(1);
        mplew.write(1);
        for (int i = 0; i < items.size(); i++) {
            MTSItemInfo item = items.get(i);
            addItemInfo(mplew, item.getItem(), true, true);
            mplew.writeInt(item.getID());
            mplew.writeInt(item.getTaxes());
            mplew.writeInt(item.getPrice());
            mplew.writeLong(0);
            mplew.writeInt(getQuestTimestamp(item.getEndingDate()));
            mplew.writeMapleAsciiString(item.getSeller());
            mplew.writeMapleAsciiString(item.getSeller());
            for (int j = 0; j < 28; j++) {
                mplew.write(0);
            }
        }
        mplew.write(0);
        return mplew.getPacket();
    }

    public static void sendUnkwnNote(String to, String msg, String from) throws SQLException {
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)");
        ps.setString(1, to);
        ps.setString(2, from);
        ps.setString(3, msg);
        ps.setLong(4, System.currentTimeMillis());
        ps.executeUpdate();
        ps.close();
    }

    public static MaplePacket showNotes(ResultSet notes, int count) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_NOTES.getValue());
        mplew.write(2);
        mplew.write(count);
        for (int i = 0; i < count; i++) {
            mplew.writeInt(notes.getInt("id"));
            mplew.writeMapleAsciiString(notes.getString("from"));
            mplew.writeMapleAsciiString(notes.getString("message"));
            mplew.writeLong(getKoreanTimestamp(notes.getLong("timestamp")));
            mplew.write(0);
            notes.next();
        }
        return mplew.getPacket();
    }

    public static MaplePacket useChalkboard(MapleCharacter chr, boolean close) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHALKBOARD.getValue());
        mplew.writeInt(chr.getId());
        if (close) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getChalkboard());
        }
        return mplew.getPacket();
    }

    public static MaplePacket trockRefreshMapList(int characterid, byte vip) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        int i = 10;
        mplew.writeShort(SendPacketOpcode.TROCK_LOCATIONS.getValue());
        mplew.write(0x03);
        mplew.write(vip);
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT mapid FROM trocklocations WHERE characterid = ? LIMIT 10");
            ps.setInt(1, characterid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                mplew.writeInt(rs.getInt("mapid"));
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
        }
        while (i > 0) {
            mplew.writeInt(999999999);
            i--;
        }
        return mplew.getPacket();
    }

    public static MaplePacket sendWishList(MapleCharacter mc, boolean update) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        if (update) {
            mplew.write(0x55);
        } else {
            mplew.write(0x4F);
        }
        byte i = 10;
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT sn FROM wishlist WHERE charid = ? LIMIT 10");
            ps.setInt(1, mc.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                mplew.writeInt(rs.getInt("sn"));
                i--;
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            System.out.println("Error getting wishlist data: " + se);
        }
        while (i > 0) {
            mplew.writeInt(0);
            i--;
        }
        return mplew.getPacket();
    }

    public static MaplePacket showMTSCash(MapleCharacter p) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION2.getValue());
        mplew.writeInt(p.getCSPoints(4));
        mplew.writeInt(p.getCSPoints(2));
        return mplew.getPacket();
    }

    public static MaplePacket MTSWantedListingOver(int nx, int items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x3D);
        mplew.writeInt(nx);
        mplew.writeInt(items);
        return mplew.getPacket();
    }

    public static MaplePacket MTSConfirmSell() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x1D);
        return mplew.getPacket();
    }

    public static MaplePacket MTSConfirmBuy() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x33);
        return mplew.getPacket();
    }

    public static MaplePacket MTSFailBuy() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x34);
        mplew.write(0x42);
        return mplew.getPacket();
    }

    public static MaplePacket MTSConfirmTransfer(int quantity, int pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x27);
        mplew.writeInt(quantity);
        mplew.writeInt(pos);
        return mplew.getPacket();
    }

    public static MaplePacket notYetSoldInv(List<MTSItemInfo> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x23);
        mplew.writeInt(items.size());
        if (!items.isEmpty()) {
            for (MTSItemInfo item : items) {
                addItemInfo(mplew, item.getItem(), true, true);
                mplew.writeInt(item.getID());
                mplew.writeInt(item.getTaxes());
                mplew.writeInt(item.getPrice());
                mplew.writeLong(0);
                mplew.writeInt(getQuestTimestamp(item.getEndingDate()));
                mplew.writeMapleAsciiString(item.getSeller());
                mplew.writeMapleAsciiString(item.getSeller());
                for (int i = 0; i < 28; i++) {
                    mplew.write(0);
                }
            }
        } else {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket transferInventory(List<MTSItemInfo> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x21);
        mplew.writeInt(items.size());
        if (!items.isEmpty()) {
            for (MTSItemInfo item : items) {
                addItemInfo(mplew, item.getItem(), true, true);
                mplew.writeInt(item.getID());
                mplew.writeInt(item.getTaxes());
                mplew.writeInt(item.getPrice());
                mplew.writeLong(0);
                mplew.writeInt(getQuestTimestamp(item.getEndingDate()));
                mplew.writeMapleAsciiString(item.getSeller());
                mplew.writeMapleAsciiString(item.getSeller());
                for (int i = 0; i < 28; i++) {
                    mplew.write(0);
                }
            }
        }
        mplew.write(0xD0 + items.size());
        mplew.write(HexTool.getByteArrayFromHexString("FF FF FF 00"));
        return mplew.getPacket();
    }

    public static MaplePacket showNXMapleTokens(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_UPDATE.getValue());
        for (int i = 1; i < 5; i *= 2) {
            mplew.writeInt(chr.getCSPoints(i));
        }
        return mplew.getPacket();
    }

    public static MaplePacket showBoughtCSItem(MapleClient c, CashItemInfo item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x57);
        addCashItemInformation(mplew, item, c.getAccID());
        return mplew.getPacket();
    }

    public static MaplePacket showBoughtCashPackage(List<CashItemInfo> cashPackage, int accountId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x89);
        mplew.write(cashPackage.size());
        for (CashItemInfo item : cashPackage) {
            addCashItemInformation(mplew, item, accountId);
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static void addCashItemInformation(MaplePacketLittleEndianWriter mplew, CashItemInfo item, int accountId) {
        mplew.writeLong(1337);
        mplew.writeInt(accountId);
        mplew.writeInt(0);
        mplew.writeInt(item.getId());
        mplew.writeInt(item.getId());
        mplew.writeShort(item.getCount());
        mplew.write0(13);
        mplew.write(HexTool.getByteArrayFromHexString("40 F0 81 DF 08 2E 0A CB 01"));
        mplew.write0(8);
    }

    public static MaplePacket wrongCouponCode() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x5C);
        mplew.write(0x9F);
        return mplew.getPacket();
    }

    public static MaplePacket showCouponRedeemedItem(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.writeShort(0x59);
        mplew.writeInt(0);
        mplew.writeInt(1);
        mplew.writeShort(1);
        mplew.writeShort(0x1A);
        mplew.writeInt(itemid);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket enableMTS() {
        return enableCSUse0();
    }

    public static MaplePacket enableCSUse0() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.write(0x12);
        mplew.write0(6);
        return mplew.getPacket();
    }

    public static MaplePacket enableCSUse1() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(9);
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x4B);
        mplew.writeShort(0);
        mplew.writeShort(4);
        mplew.writeShort(5);
        return mplew.getPacket();
    }

    public static MaplePacket enableCSUse2() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.writeShort(0x4D); //v83
        mplew.write0(2);
        return mplew.getPacket();
    }

    public static MaplePacket enableCSUse3() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(43);
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x4F);
        mplew.write0(40);
        return mplew.getPacket();
    }

    public static MaplePacket getFindReply(String target, int mapid, int MTSmapCSchannel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(9);
        mplew.writeMapleAsciiString(target);
        mplew.write(MTSmapCSchannel);
        mplew.writeInt(MTSmapCSchannel == 3 ? mapid - 1 : mapid);
        if (MTSmapCSchannel == 1) {
            mplew.write0(8);
        }
        return mplew.getPacket();
    }

    public static MaplePacket sendAutoHpPot(int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.AUTO_HP_POT.getValue());
        mplew.writeInt(itemId);
        return mplew.getPacket();
    }

    public static MaplePacket sendAutoMpPot(int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendPacketOpcode.AUTO_MP_POT.getValue());
        mplew.writeInt(itemId);
        return mplew.getPacket();
    }

    public static MaplePacket showOXQuiz(int questionSet, int questionId, boolean askQuestion) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendPacketOpcode.OX_QUIZ.getValue());
        mplew.write(askQuestion ? 1 : 0);
        mplew.write(questionSet);
        mplew.writeShort(questionId);
        return mplew.getPacket();
    }

    public static MaplePacket updateGender(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.GENDER.getValue());
        mplew.write(chr.getGender());
        return mplew.getPacket();
    }

    public static MaplePacket enableReport() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.ENABLE_REPORT.getValue());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket giveFinalAttack(int skillid, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.write(0);
        mplew.write(0x80);
        mplew.writeInt(0);
        mplew.writeShort(1);
        mplew.writeInt(skillid);
        mplew.writeInt(time);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket loadFamily(MapleCharacter player) {
        String[] title = {"Family Reunion", "Summon Family", "My Drop Rate 1.5x (15 min)", "My EXP 1.5x (15 min)", "Family Bonding (30 min)", "My Drop Rate 2x (15 min)", "My EXP 2x (15 min)", "My Drop Rate 2x (30 min)", "My EXP 2x (30 min)", "My Party Drop Rate 2x (30 min)", "My Party EXP 2x (30 min)"};
        String[] description = {"[Target] Me\n[Effect] Teleport directly to the Family member of your choice.", "[Target] 1 Family member\n[Effect] Summon a Family member of choice to the map you're in.", "[Target] Me\n[Time] 15 min.\n[Effect] Monster drop rate will be increased #c1.5x#.\n*  If the Drop Rate event is in progress, this will be nullified.", "[Target] Me\n[Time] 15 min.\n[Effect] EXP earned from hunting will be increased #c1.5x#.\n* If the EXP event is in progress, this will be nullified.", "[Target] At least 6 Family members online that are below me in the Pedigree\n[Time] 30 min.\n[Effect] Monster drop rate and EXP earned will be increased #c2x#. \n* If the EXP event is in progress, this will be nullified.", "[Target] Me\n[Time] 15 min.\n[Effect] Monster drop rate will be increased #c2x#.\n* If the Drop Rate event is in progress, this will be nullified.", "[Target] Me\n[Time] 15 min.\n[Effect] EXP earned from hunting will be increased #c2x#.\n* If the EXP event is in progress, this will be nullified.", "[Target] Me\n[Time] 30 min.\n[Effect] Monster drop rate will be increased #c2x#.\n* If the Drop Rate event is in progress, this will be nullified.", "[Target] Me\n[Time] 30 min.\n[Effect] EXP earned from hunting will be increased #c2x#. \n* If the EXP event is in progress, this will be nullified.", "[Target] My party\n[Time] 30 min.\n[Effect] Monster drop rate will be increased #c2x#.\n* If the Drop Rate event is in progress, this will be nullified.", "[Target] My party\n[Time] 30 min.\n[Effect] EXP earned from hunting will be increased #c2x#.\n* If the EXP event is in progress, this will be nullified."};
        int[] repCost = {3, 5, 7, 8, 10, 12, 15, 20, 25, 40, 50};
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LOAD_FAMILY.getValue());
        mplew.writeInt(11);
        for (int i = 0; i < 11; i++) {
            mplew.write(i > 4 ? (i % 2) + 1 : i);
            mplew.writeInt(repCost[i] * 100);
            mplew.writeInt(1);
            mplew.writeMapleAsciiString(title[i]);
            mplew.writeMapleAsciiString(description[i]);
        }
        return mplew.getPacket();
    }

    public static MaplePacket sendFamilyMessage() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendPacketOpcode.FAMILY_MESSAGE.getValue());
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket getFamilyInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_FAMILY.getValue());
//        mplew.writeInt(chr.getFamily().getReputation());
//        mplew.writeInt(chr.getFamily().getTotalReputation());
//        mplew.writeInt(chr.getFamily().getTodaysRep());
//        mplew.writeShort(chr.getFamily().getJuniors());
//        mplew.writeShort(chr.getFamily().getTotalJuniors());
//        mplew.writeShort(0);
//        mplew.writeInt(chr.getFamilyId());
//        mplew.writeMapleAsciiString(chr.getFamily().getFamilyName());
//        mplew.writeInt(0);
//        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket getStatusMsg(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(7);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static MaplePacket giveEnergyCharge(int barammount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        mplew.writeInt(0);
        mplew.writeLong(MapleBuffStat.ENERGY_CHARGE.getValue());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeShort(barammount);
        mplew.writeShort(0);
        mplew.writeLong(0);
        mplew.write(0);
        mplew.writeInt(50);
        return mplew.getPacket();
    }

    public static MaplePacket giveForeignEnergyCharge(int cid, int barammount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(0);
        mplew.writeLong(MapleBuffStat.ENERGY_CHARGE.getValue());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeShort(barammount);
        mplew.writeShort(0);
        mplew.writeLong(0);
        mplew.write(0);
        mplew.writeInt(50);
        return mplew.getPacket();
    }

    public static MaplePacket addCard(boolean full, int cardid, int level) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendPacketOpcode.MONSTERBOOK_ADD.getValue());
        mplew.write(full ? 0 : 1);
        mplew.writeInt(cardid);
        mplew.writeInt(level);
        return mplew.getPacket();
    }

    public static MaplePacket showGainCard() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x0D);
        return mplew.getPacket();
    }

    public static MaplePacket showForeginCardEffect(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(id);
        mplew.write(0x0D);
        return mplew.getPacket();
    }

    public static MaplePacket changeCover(int cardid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendPacketOpcode.MONSTER_BOOK_CHANGE_COVER.getValue());
        mplew.writeInt(cardid);
        return mplew.getPacket();
    }

    public static MaplePacket showCygnusIntro(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x12);
        mplew.writeMapleAsciiString("Effect/Direction.img/cygnus/Scene" + id);
        return mplew.getPacket();
    }

    public static MaplePacket cygnusIntroLock(boolean enable) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendPacketOpcode.TUTORIAL_INTRO_LOCK.getValue());
        mplew.write(enable ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket cygnusIntroDisableUI(boolean enable) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TUTORIAL_INTRO_DISABLE_UI.getValue());
        mplew.write(enable ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket itemMegaphone(String msg, boolean whisper, int channel, IItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(8);
        mplew.writeMapleAsciiString(msg);
        mplew.write(channel - 1);
        mplew.write(whisper ? 1 : 0);
        if (item == null) {
            mplew.write(0);
        } else {
            addItemInfo(mplew, item, false, false, true);
        }
        return mplew.getPacket();
    }

    public static MaplePacket removeNPC(int objid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_NPC.getValue());
        mplew.writeInt(objid);
        return mplew.getPacket();
    }

    public static MaplePacket reportResponse(byte mode, int remainingReports) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REPORT_RESPONSE.getValue());
        mplew.write(mode);
        if (mode == 2) {
            mplew.write(1);
            mplew.writeInt(remainingReports);
        }
        return mplew.getPacket();
    }

    public static MaplePacket sendHammerData(int hammerUsed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(0x34);
        mplew.write0(4);
        mplew.writeInt(hammerUsed);
        return mplew.getPacket();
    }

    public static MaplePacket sendHammerMessage() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(0x38);
        mplew.write0(14);
        return mplew.getPacket();
    }

    public static MaplePacket hammerItem(IItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0);
        mplew.write(2);
        mplew.write(3);
        mplew.write(1);
        mplew.write(item.getPosition());
        mplew.writeShort(0);
        mplew.write(1);
        mplew.write(item.getPosition());
        addItemInfo(mplew, item, true, false);
        return mplew.getPacket();
    }

    public static MaplePacket playPortalSound() {
        return showSpecialEffect(7);
    }

    public static MaplePacket showMonsterBookPickup() {
        return showSpecialEffect(14);
    }

    public static MaplePacket showEquipmentLevelUp() {
        return showSpecialEffect(17);
    }

    public static MaplePacket showItemLevelup() {
        return showSpecialEffect(17);
    }

    public static MaplePacket showSpecialEffect(int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effect);
        return mplew.getPacket();
    }

    public static MaplePacket startQuest(short quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.write(1);
        mplew.write0(10);
        return mplew.getPacket();
    }

    public static MaplePacket updateQuestFinish(short quest, int npc, short nextquest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(8);
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeShort(nextquest);
        return mplew.getPacket();
    }

    public static MaplePacket showQuestExpired(String expire) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(9);
        mplew.writeMapleAsciiString(expire);
        return mplew.getPacket();
    }

    public static MaplePacket questError(short quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(10);
        mplew.writeShort(quest);
        return mplew.getPacket();
    }

    public static MaplePacket getMultiMegaphone(String[] messages, int channel, boolean showEar) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(0x0A);
        if (messages[0] != null) {
            mplew.writeMapleAsciiString(messages[0]);
        }
        mplew.write(messages.length);
        for (int i = 1; i < messages.length; i++) {
            if (messages[i] != null) {
                mplew.writeMapleAsciiString(messages[i]);
            }
        }
        for (int i = 0; i < 10; i++) {
            mplew.write(channel - 1);
        }
        mplew.write(showEar ? 1 : 0);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket getGMEffect(int type, byte mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GM_PACKET.getValue());
        mplew.write(type);
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static MaplePacket sendFamilyInvite(int playerId, String inviter) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_INVITE.getValue());
        mplew.writeInt(playerId);
        mplew.writeMapleAsciiString(inviter);
        return mplew.getPacket();
    }

    public static MaplePacket showBoughtCSQuestItem(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("7E 01 00 00 00 01 00 19 00"));
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static MaplePacket updateEquipSlot(IItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0);
        mplew.write(HexTool.getByteArrayFromHexString("02 03 01"));
        mplew.writeShort(item.getPosition());
        mplew.write(0);
        mplew.write(item.getType());
        mplew.writeShort(item.getPosition());
        addItemInfo(mplew, item, true, true);
        mplew.writeMapleAsciiString("");
        return mplew.getPacket();
    }

    private static void getGuildInfo(MaplePacketLittleEndianWriter mplew, MapleGuild guild) {
        mplew.writeInt(guild.getId());
        mplew.writeMapleAsciiString(guild.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(guild.getRankTitle(i));
        }
        Collection<MapleGuildCharacter> members = guild.getMembers();
        mplew.write(members.size());
        for (MapleGuildCharacter mgc : members) {
            mplew.writeInt(mgc.getId());
        }
        for (MapleGuildCharacter mgc : members) {
            mplew.writeAsciiString(getRightPaddedStr(mgc.getName(), '\0', 13));
            mplew.writeInt(mgc.getJobId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeInt(guild.getSignature());
            mplew.writeInt(mgc.getAllianceRank());
        }
        mplew.writeInt(guild.getCapacity());
        mplew.writeShort(guild.getLogoBG());
        mplew.write(guild.getLogoBGColor());
        mplew.writeShort(guild.getLogo());
        mplew.write(guild.getLogoColor());
        mplew.writeMapleAsciiString(guild.getNotice());
        mplew.writeInt(guild.getGP());
        mplew.writeInt(guild.getAllianceId());
    }

    public static MaplePacket allianceCapacityChange(MapleAlliance alliance, int id, int capacity) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x17);
        mplew.writeInt(id);
        mplew.write(capacity);
        addAllianceInfo(mplew, alliance);
        return mplew.getPacket();
    }

    private static void addAllianceInfo(MaplePacketLittleEndianWriter mplew, MapleAlliance alliance) {
        mplew.writeInt(alliance.getId());
        mplew.writeMapleAsciiString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(alliance.getRankTitle(i));
        }
        mplew.write(alliance.getGuilds().size());
        for (Integer guild : alliance.getGuilds()) {
            mplew.writeInt(guild);
        }
        mplew.writeInt(alliance.getCapacity());
        mplew.writeMapleAsciiString(alliance.getNotice());
    }

    private static void addGuildInfo(MaplePacketLittleEndianWriter mplew, MapleAlliance alliance, MapleClient c, boolean player) {
        for (Integer guildd : alliance.getGuilds()) {
            try {
                getGuildInfo(mplew, c.getChannelServer().getWorldInterface().getGuild(guildd, player ? c.getPlayer().getMGC() : null));
            } catch (RemoteException re) {
                c.getChannelServer().reconnectWorld();
            }
        }
    }

    public static MaplePacket getAllianceInfo(MapleAlliance alliance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0C);
        mplew.write(1);
        addAllianceInfo(mplew, alliance);
        return mplew.getPacket();
    }

    public static MaplePacket makeNewAlliance(MapleAlliance alliance, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0F);
        addAllianceInfo(mplew, alliance);
        addGuildInfo(mplew, alliance, c, true);
        return mplew.getPacket();
    }

    public static MaplePacket getGuildAlliances(MapleAlliance alliance, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0D);
        mplew.writeInt(alliance.getGuilds().size());
        addGuildInfo(mplew, alliance, c, false);
        return mplew.getPacket();
    }

    public static MaplePacket addGuildToAlliance(MapleAlliance alliance, int newGuild, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x12);
        addAllianceInfo(mplew, alliance);
        mplew.writeInt(newGuild);
        addGuildInfo(mplew, alliance, c, false);
        return mplew.getPacket();
    }

    public static MaplePacket allianceInvitationDenied(String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x09);
        mplew.writeMapleAsciiString(name + " Guild has rejected the Guild Union invitation.");
        return mplew.getPacket();
    }

    public static MaplePacket allianceMemberOnline(MapleCharacter mc, boolean online) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0E);
        mplew.writeInt(mc.getGuild().getAllianceId());
        mplew.writeInt(mc.getGuildId());
        mplew.writeInt(mc.getId());
        mplew.write(online ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket allianceNotice(int id, String notice) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1C);
        mplew.writeInt(id);
        mplew.writeMapleAsciiString(notice);
        return mplew.getPacket();
    }

    public static MaplePacket changeAllianceRankTitle(int alliance, String[] ranks) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1A);
        mplew.writeInt(alliance);
        for (int i = 0; i < 5; i++) {
            mplew.writeMapleAsciiString(ranks[i]);
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateAllianceJobLevel(MapleCharacter mc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x18);
        mplew.writeInt(mc.getGuild().getAllianceId());
        mplew.writeInt(mc.getGuildId());
        mplew.writeInt(mc.getId());
        mplew.writeInt(mc.getLevel());
        mplew.writeInt(mc.getJob().getId());
        return mplew.getPacket();
    }

    public static MaplePacket removeGuildFromAlliance(MapleAlliance alliance, int expelledGuild, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x10);
        addAllianceInfo(mplew, alliance);
        mplew.writeInt(expelledGuild);
        addGuildInfo(mplew, alliance, c, false);
        mplew.write(0x01);
        return mplew.getPacket();
    }

    public static MaplePacket disbandAlliance(int alliance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1D);
        mplew.writeInt(alliance);
        return mplew.getPacket();
    }

    public static MaplePacket sendMesoLimit() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MESO_LIMIT.getValue());
        return mplew.getPacket();
    }

    public static MaplePacket sendEngagementRequest(String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_ACTION.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(name);
        mplew.writeInt(10);
        return mplew.getPacket();
    }

    public static MaplePacket sendGroomWishlist() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_ACTION.getValue());
        mplew.write(9);
        return mplew.getPacket();
    }

    public static MaplePacket sendFamilyJoinResponse(boolean accepted, String added) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_MESSAGE2.getValue());
        mplew.write(accepted ? 1 : 0);
        mplew.writeMapleAsciiString(added);
        return mplew.getPacket();
    }

    public static MaplePacket getSeniorMessage(String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_SENIOR_MESSAGE.getValue());
        mplew.writeMapleAsciiString(name);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket sendGainRep(int gain, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILY_GAIN_REP.getValue());
        mplew.writeInt(gain);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket removeItemFromDuey(boolean remove, int Package) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(0x17);
        mplew.writeInt(Package);
        mplew.write(remove ? 3 : 4);
        return mplew.getPacket();
    }

    public static MaplePacket sendDueyMSG(byte operation) {
        return sendDuey(operation, null);
    }

    public static MaplePacket sendDuey(byte operation, List<DueyPackages> packages) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(operation);
        if (operation == 8) {
            mplew.write(0);
            mplew.write(packages.size());
            for (DueyPackages dp : packages) {
                mplew.writeInt(dp.getPackageId());
                mplew.writeAsciiString(dp.getSender());
                for (int i = dp.getSender().length(); i < 13; i++) {
                    mplew.write(0);
                }
                mplew.writeInt(dp.getMesos());
                mplew.writeLong(getQuestTimestamp(dp.sentTimeInMilliseconds()));
                mplew.writeLong(0);
                for (int i = 0; i < 48; i++) {
                    mplew.writeInt(Randomizer.getInstance().nextInt(Integer.MAX_VALUE));
                }
                mplew.writeInt(0);
                mplew.write(0);
                if (dp.getItem() != null) {
                    mplew.write(1);
                    addItemInfo(mplew, dp.getItem(), true, true);
                } else {
                    mplew.write(0);
                }
            }
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket useHomingBeacon(MapleCharacter player, int skillid, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        for (int i = 0; i < statups.size(); i++) {
            mplew.write(0);
        }
        mplew.writeInt(0);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeShort(statup.getRight());
        }
        mplew.writeShort(0);
        mplew.writeInt(skillid);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket sendDojoAnimation(byte firstByte, String animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(firstByte);
        mplew.writeMapleAsciiString(animation);
        return mplew.getPacket();
    }

    public static MaplePacket getDojoInfo(String info) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(10);
        mplew.write(HexTool.getByteArrayFromHexString("B7 04"));
        mplew.writeMapleAsciiString(info);
        return mplew.getPacket();
    }

    public static MaplePacket getDojoInfoMessage(String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(9);
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }

    public static MaplePacket blockedMessage(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_BLOCKED.getValue());
        mplew.write(type);
        return mplew.getPacket();
    }

//    public static MaplePacket updateDojoStats(MapleCharacter chr, int belt) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
//        mplew.write(0x0a);
//        mplew.write(HexTool.getByteArrayFromHexString("B7 04")); //?
//        mplew.writeMapleAsciiString("pt=" + chr.getDojoPoints() + ";belt=" + belt + ";tuto=" + (chr.getFinishedDojoTutorial() ? "1" : "0"));
//        return mplew.getPacket();
//    }

    public static MaplePacket trembleEffect(int type, int delay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(1);
        mplew.write(type);
        mplew.writeInt(delay);
        return mplew.getPacket();
    }

    public static MaplePacket getEnergy(int level) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ENERGY.getValue());
        mplew.writeMapleAsciiString("energy");
        mplew.writeMapleAsciiString(Integer.toString(level));
        return mplew.getPacket();
    }

    public static MaplePacket dojoWarpUp() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DOJO_WARP_UP.getValue());
        mplew.write(0);
        mplew.write(6);
        return mplew.getPacket();
    }

    public static MaplePacket updateItemInSlot(IItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0);
        mplew.write(2);
        mplew.write(3);
        mplew.write(item.getType());
        mplew.write(item.getPosition());
        mplew.writeShort(0);
        mplew.write(1);
        mplew.write(item.getPosition());
        addItemInfo(mplew, item, true, false);
        return mplew.getPacket();
    }

    public static MaplePacket itemExpired(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(2);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    private static String getRightPaddedStr(String in, char padchar, int length) {
        StringBuilder builder = new StringBuilder(in);
        for (int x = in.length(); x < length; x++) {
            builder.append(padchar);
        }
        return builder.toString();
    }

    public static MaplePacket MobDamageMobFriendly(MapleMonster mob, int damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.write(1);
        mplew.writeInt(damage);
        int remainingHp = mob.getHp() - damage;
        if (remainingHp <= 0) {
            remainingHp = 0;
            mob.getMap().removeMapObject(mob);
        }
        mob.setHp(remainingHp);
        mplew.writeInt(remainingHp);
        mplew.writeInt(mob.getMaxHp());
        return mplew.getPacket();
    }

    public static MaplePacket shopErrorMessage(int error, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x0A);
        mplew.write(type);
        mplew.write(error);
        return mplew.getPacket();
    }

    private static void addRingInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.writeShort(0);
//        mplew.writeShort(chr.getCrushRings().size());
//        for (MapleRing ring : chr.getCrushRings()) {
//            mplew.writeInt(ring.getPartnerChrId());
//            mplew.writeAsciiString(getRightPaddedStr(ring.getPartnerName(), '\0', 13));
//            mplew.writeInt(ring.getRingId());
//            mplew.writeInt(0);
//            mplew.writeInt(ring.getPartnerRingId());
//            mplew.writeInt(0);
//        }
//        mplew.writeShort(chr.getFriendshipRings().size());
//        for (MapleRing ring : chr.getFriendshipRings()) {
//            mplew.writeInt(ring.getPartnerChrId());
//            mplew.writeAsciiString(getRightPaddedStr(ring.getPartnerName(), '\0', 13));
//            mplew.writeInt(ring.getRingId());
//            mplew.writeInt(0);
//            mplew.writeInt(ring.getPartnerRingId());
//            mplew.writeInt(0);
//            mplew.writeInt(ring.getItemId());
//        }
//        mplew.writeShort(chr.getMarriageRings().size());
//        int marriageId = 30000;
//        for (MapleRing ring : chr.getMarriageRings()) {
//            mplew.writeInt(marriageId);
//            mplew.writeInt(chr.getId());
//            mplew.writeInt(ring.getPartnerChrId());
//            mplew.writeShort(3);
//            mplew.writeInt(ring.getRingId());
//            mplew.writeInt(ring.getPartnerRingId());
//            mplew.writeAsciiString(getRightPaddedStr(chr.getName(), '\0', 13));
//            mplew.writeAsciiString(getRightPaddedStr(ring.getPartnerName(), '\0', 13));
//            marriageId++;
//        }
    }

    public static MaplePacket finishedSort(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FINISH_SORT.getValue());
        mplew.write(0);
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket finishedSort2(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FINISH_SORT2.getValue());
        mplew.write(0);
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket bunnyPacket() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(9);
        mplew.writeAsciiString("Protect the Moon Bunny!!!");
        return mplew.getPacket();
    }

    public static MaplePacket hpqMessage(String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.write(0);
        mplew.writeInt(5120016);
        mplew.writeAsciiString(text);
        return mplew.getPacket();
    }

    public static MaplePacket showHPQMoon() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(0x83);
        mplew.writeInt(-1);
        return mplew.getPacket();
    }

    public static MaplePacket showMCPQMessage(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_MESSAGE.getValue());
        mplew.writeShort(type);
        return mplew.getPacket();
    }

    public static MaplePacket sendOpenTreasure(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SILVER_BOX.getValue());
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static MaplePacket playerLeftMCPQ(int team, int spot, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_PLAYER_LEFT.getValue());
        mplew.write(spot);
        mplew.write(team);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    public static MaplePacket updatePlayerCP(int un, int total) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_PLAYER_CP.getValue());
        mplew.writeShort(un);
        mplew.writeShort(total);
        return mplew.getPacket();
    }

    public static MaplePacket updatePartyCP(int team, int un, int total) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_PARTY_CP.getValue());
        mplew.write(team);
        mplew.writeShort(un);
        mplew.writeShort(total);
        return mplew.getPacket();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static MaplePacket getPermBan(int reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeShort(2);
        mplew.writeInt(0);
        mplew.writeShort(reason);
        mplew.write(HexTool.getByteArrayFromHexString("8C 65 3F 8A CB D1 01"));

        return mplew.getPacket();
    }

    public static MaplePacket getTempBan(long timestampTill, byte reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(17);
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write(0x02);
        mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00"));
        mplew.write(reason);
        mplew.writeLong(timestampTill);
        return mplew.getPacket();
    }

    public static MaplePacket giveBuffTest(int buffid, int bufflength, long mask) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        mplew.writeLong(0);
        mplew.writeLong(mask);
        mplew.writeShort(1);
        mplew.writeInt(buffid);
        mplew.writeInt(bufflength);
        mplew.writeShort(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket updatePet(MaplePet pet, boolean alive) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0);
        mplew.write(2);
        mplew.write(3);
        mplew.write(5);
        mplew.write(pet.getPosition());
        mplew.writeShort(0);
        mplew.write(5);
        mplew.write(pet.getPosition());
        mplew.write(0);
        mplew.write(3);
        mplew.writeInt(pet.getItemId());
        mplew.write(1);
        mplew.writeInt(pet.getUniqueId());
        mplew.writeInt(0);
        mplew.write(HexTool.getByteArrayFromHexString("00 40 6f e5 0f e7 17 02"));
        String petname = pet.getName();
        if (petname.length() > 13) {
            petname = petname.substring(0, 13);
        }
        mplew.writeAsciiString(petname);
        for (int i = petname.length(); i < 13; i++) {
            mplew.write(0);
        }
        mplew.write(pet.getLevel());
        mplew.writeShort(pet.getCloseness());
        mplew.write(pet.getFullness());
        if (alive) {
            mplew.writeLong(getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
            mplew.writeInt(0);
        } else {
            mplew.write(0);
            mplew.write(ITEM_MAGIC);
            mplew.write(HexTool.getByteArrayFromHexString("bb 46 e6 17 02 00 00 00 00"));
        }
        return mplew.getPacket();
    }

    public static MaplePacket petChat(int cid, int un, String text, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_CHAT.getValue());
        mplew.writeInt(cid);
        mplew.write(slot);
        mplew.writeShort(un);
        mplew.writeMapleAsciiString(text);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket commandResponse(int cid, byte command, int slot, boolean success, boolean food) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_COMMAND.getValue());
        mplew.writeInt(cid);
        mplew.write(slot);
        if (!food) {
            mplew.write(0);
        }
        mplew.write(command);
        if (success) {
            mplew.write(1);
        } else {
            mplew.write(0);
        }
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket updateMount(int charid, int newlevel, int newexp, int tiredness, boolean levelup) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_MOUNT.getValue());
        mplew.writeInt(charid);
        mplew.writeInt(newlevel);
        mplew.writeInt(newexp);
        mplew.writeInt(tiredness);
        mplew.write(levelup ? (byte) 1 : (byte) 0);
        return mplew.getPacket();
    }

    public static MaplePacket reportReply(byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REPORTREPLY.getValue());
        mplew.write(type);
        return mplew.getPacket();
    }

    public static MaplePacket giveDash(List<Pair<MapleBuffStat, Integer>> statups, int x, int y, int duration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        mplew.writeLong(0);
        long mask = getLongMask(statups);
        mplew.writeLong(mask);
        mplew.writeShort(0);
        mplew.writeInt(x);
        mplew.writeInt(5001005);
        mplew.write(HexTool.getByteArrayFromHexString("1A 7C 8D 35"));
        mplew.writeShort(duration);
        mplew.writeInt(y);
        mplew.writeInt(5001005);
        mplew.write(HexTool.getByteArrayFromHexString("1A 7C 8D 35"));
        mplew.writeShort(duration);
        mplew.writeShort(0);
        mplew.write(2);
        return mplew.getPacket();
    }

    public static MaplePacket showDashEffecttoOthers(int cid, int x, int y, int duration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(0);
        mplew.write(HexTool.getByteArrayFromHexString("00 00 00 30 00 00 00 00"));
        mplew.writeShort(0);
        mplew.writeInt(x);
        mplew.writeInt(5001005);
        mplew.write(HexTool.getByteArrayFromHexString("1A 7C 8D 35"));
        mplew.writeShort(duration);
        mplew.writeInt(y);
        mplew.writeInt(5001005);
        mplew.write(HexTool.getByteArrayFromHexString("1A 7C 8D 35"));
        mplew.writeShort(duration);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket sendGMPolice(int reason, String sReason, int duration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GM_POLICE.getValue());
        mplew.writeInt(duration);
        mplew.write(4);
        mplew.write(reason);
        mplew.writeMapleAsciiString(sReason);
        return mplew.getPacket();
    }

    public static MaplePacket addToCartMessage(boolean fail, boolean remove) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION2.getValue());
        if (remove) {
            mplew.write(0x2B);
        } else {
            if (fail) {
                mplew.write(0x2A);
                mplew.writeInt(-1);
            } else {
                mplew.write(0x29);
            }
        }

        return mplew.getPacket();
    }

    public static MaplePacket sendInteractionBox(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        addAnnounceBox(mplew, c.getInteraction());
        return mplew.getPacket();
    }

    public static MaplePacket getInteraction(MapleCharacter chr, boolean firstTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        IPlayerInteractionManager ips = chr.getInteraction();
        int type = ips.getShopType();
        if (type == 1) {
            mplew.write(HexTool.getByteArrayFromHexString("05 05 04"));
        } else if (type == 2) {
            mplew.write(HexTool.getByteArrayFromHexString("05 04 04"));
        } else if (type == 3) {
            mplew.write(HexTool.getByteArrayFromHexString("05 02 02"));
        } else if (type == 4) {
            mplew.write(HexTool.getByteArrayFromHexString("05 01 02"));
        }
        mplew.write(ips.isOwner(chr) ? 0 : 1);
        mplew.write(0);
        if (type == 2 || type == 3 || type == 4) {
            addCharLook(mplew, ((MaplePlayerShop) ips).getMCOwner(), false);
            mplew.writeMapleAsciiString(ips.getOwnerName());
        } else {
            mplew.writeInt(((HiredMerchant) ips).getItemId());
            mplew.writeMapleAsciiString("Hired Merchant");
        }
        for (int i = 0; i < 3; i++) {
            if (ips.getVisitors()[i] != null) {
                mplew.write(i + 1);
                addCharLook(mplew, ips.getVisitors()[i], false);
                mplew.writeMapleAsciiString(ips.getVisitors()[i].getName());
            }
        }
        mplew.write(0xFF);
        if (type == 1) {
            mplew.writeShort(0);
            mplew.writeMapleAsciiString(ips.getOwnerName());
            if (ips.isOwner(chr)) {
                mplew.writeInt(Integer.MAX_VALUE);
                mplew.write(firstTime ? 1 : 0);
                mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00"));
            }
        } else if (type == 3 || type == 4) {
            MapleMiniGame minigame = (MapleMiniGame) ips;
            mplew.write(0);
            if (type == 4) {
                mplew.writeInt(1);
            } else {
                mplew.writeInt(2);
            }
            mplew.writeInt(minigame.getOmokPoints("wins", true));
            mplew.writeInt(minigame.getOmokPoints("ties", true));
            mplew.writeInt(minigame.getOmokPoints("losses", true));
            mplew.writeInt(2000);
            if (ips.getVisitors()[0] != null) {
                mplew.write(1);
                if (type == 4) {
                    mplew.writeInt(1);
                } else {
                    mplew.writeInt(2);
                }
                mplew.writeInt(minigame.getOmokPoints("wins", false));
                mplew.writeInt(minigame.getOmokPoints("ties", false));
                mplew.writeInt(minigame.getOmokPoints("losses", false));
                mplew.writeInt(2000);
            }
            mplew.write(0xFF);
        }
        mplew.writeMapleAsciiString(ips.getDescription());
        if (type == 3) {
            mplew.write(ips.getItemType());
            mplew.write(0);
        } else {
            mplew.write(0x10);
            if (type == 1) {
                mplew.writeInt(0);
            }
            mplew.write(ips.getItems().size());
            if (ips.getItems().isEmpty()) {
                if (type == 1) {
                    mplew.write(0);
                } else {
                    mplew.writeInt(0);
                }
            } else {
                for (MaplePlayerShopItem item : ips.getItems()) {
                    mplew.writeShort(item.getBundles());
                    mplew.writeShort(item.getItem().getQuantity());
                    mplew.writeInt(item.getPrice());
                    addItemInfo(mplew, item.getItem(), true, true);
                }
            }
        }
        return mplew.getPacket();
    }

    public static MaplePacket shopChat(String message, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("06 08"));
        mplew.write(slot);
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }

    public static MaplePacket shopItemUpdate(IPlayerInteractionManager shop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x17);
        if (shop.getShopType() == 1) {
            mplew.writeInt(0);
        }
        mplew.write(shop.getItems().size());
        for (MaplePlayerShopItem item : shop.getItems()) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeInt(item.getPrice());
            addItemInfo(mplew, item.getItem(), true, true);
        }
        return mplew.getPacket();
    }

    public static MaplePacket shopVisitorAdd(MapleCharacter chr, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x04);
        mplew.write(slot);
        addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        if (chr.getInteraction().getShopType() == 4 || chr.getInteraction().getShopType() == 3) {
            MapleMiniGame game = (MapleMiniGame) chr.getInteraction();
            mplew.writeInt(1);
            mplew.writeInt(game.getOmokPoints("wins", false));
            mplew.writeInt(game.getOmokPoints("ties", false));
            mplew.writeInt(game.getOmokPoints("losses", false));
            mplew.writeInt(2000);
        }
        System.out.println(mplew);
        return mplew.getPacket();
    }

    public static MaplePacket shopVisitorLeave(int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x0A);
        mplew.write(slot);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniBoxFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.writeShort(5);
        mplew.write(2);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameStart(int loser) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x37);
        mplew.write(loser);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameSkipTurn(int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x39);
        mplew.write(slot);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameReady() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x34);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameUnReady() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x35);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameRequestTie() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x2C);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameDenyTie() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x2D);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameMoveOmok(int move1, int move2, int move3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x3A);
        mplew.writeInt(move1);
        mplew.writeInt(move2);
        mplew.write(move3);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameWin(MapleMiniGame game, int person) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("38 00"));
        mplew.write(person);
        mplew.writeInt(1);
        mplew.writeInt(game.getOmokPoints("wins", true));
        mplew.writeInt(game.getOmokPoints("ties", true));
        mplew.writeInt(game.getOmokPoints("losses", true) + 1);
        mplew.writeInt(2000);
        mplew.writeInt(1);
        mplew.writeInt(game.getOmokPoints("wins", false) + 1);
        mplew.writeInt(game.getOmokPoints("ties", false));
        mplew.writeInt(game.getOmokPoints("losses", false));
        mplew.writeInt(2000);
        game.setOmokPoints(person + 1);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameTie(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("38 01"));
        mplew.writeInt(1);
        mplew.writeInt(game.getOmokPoints("wins", true));
        mplew.writeInt(game.getOmokPoints("ties", true) + 1);
        mplew.writeInt(game.getOmokPoints("losses", true));
        mplew.writeInt(2000);
        mplew.writeInt(1);
        mplew.writeInt(game.getOmokPoints("wins", false));
        mplew.writeInt(game.getOmokPoints("ties", false) + 1);
        mplew.writeInt(game.getOmokPoints("losses", false));
        mplew.writeInt(2000);
        game.setMatchCardPoints(3);
        return mplew.getPacket();
    }

    public static MaplePacket getMiniGameForfeit(MapleMiniGame game, int person) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("38 02"));
        mplew.write(person);
        mplew.writeInt(1);
        mplew.writeInt(game.getOmokPoints("wins", true));
        mplew.writeInt(game.getOmokPoints("ties", true));
        mplew.writeInt(game.getOmokPoints("losses", true) + 1); 
        mplew.writeInt(2000);
        mplew.writeInt(1);
        mplew.writeInt(game.getOmokPoints("wins", false) + 1);
        mplew.writeInt(game.getOmokPoints("ties", false));
        mplew.writeInt(game.getOmokPoints("losses", false));
        mplew.writeInt(2000);
        game.setOmokPoints(person + 1);
        return mplew.getPacket();
    }

    public static MaplePacket getMatchCardStart(MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x37);
        mplew.write(game.getLoser());
        int times;
        if (game.getMatchesToWin() > 10) {
            times = 30;
        } else if (game.getMatchesToWin() > 6) {
            times = 20;
        } else {
            times = 12;
        }
        mplew.write(times);
        for (int i = 1; i <= times; i++) {
            mplew.writeInt(game.getCardId(i));
        }
        return mplew.getPacket();
    }

    public static MaplePacket getMatchCardSelect(int turn, int slot, int firstslot, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x3E);
        mplew.write(turn);
        if (turn == 1) {
            mplew.write(slot);
        } else if (turn == 0) {
            mplew.write(slot);
            mplew.write(firstslot);
            mplew.write(type);
        }
        return mplew.getPacket();
    }

    public static MaplePacket enableRecommended(boolean enable) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ENABLE_RECOMMENDED.getValue());
        mplew.writeInt(enable ? 3 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket sendRecommended(int world, String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SEND_RECOMMENDED.getValue());
        mplew.write(world);
        mplew.writeInt(0);
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }
}