package net;

import net.handler.KeepAliveHandler;
import net.channel.handler.*;
import net.login.handler.*;

public final class PacketProcessor {
    private MaplePacketHandler[] handlers;
    private static PacketProcessor instance;
    public enum Mode {LOGINSERVER, CHANNELSERVER};

    private PacketProcessor() {
        int maxRecvOp = 0;
        for (RecvPacketOpcode op : RecvPacketOpcode.values()) {
            if (op.getValue() > maxRecvOp) {
                maxRecvOp = op.getValue();
            }
        }
        handlers = new MaplePacketHandler[maxRecvOp + 1];
    }

    public MaplePacketHandler getHandler(short packetId) {
        if (packetId > handlers.length) {
            return null;
        }
        MaplePacketHandler handler = handlers[packetId];
        if (handler != null) {
            return handler;
        }
        return null;
    }

    public void registerHandler(RecvPacketOpcode code, MaplePacketHandler handler) {
        try {
            handlers[code.getValue()] = handler;
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            System.out.println("Error registering handler - " + code.name());
        }
    }

    public synchronized static PacketProcessor getProcessor(Mode mode) {
        if (instance == null) {
            instance = new PacketProcessor();
            instance.reset(mode);
        }
        return instance;
    }

    public void reset(Mode mode) {
        handlers = new MaplePacketHandler[handlers.length];
        registerHandler(RecvPacketOpcode.PONG, new KeepAliveHandler());
        if (mode == Mode.LOGINSERVER) {
            registerHandler(RecvPacketOpcode.CHARLIST_REQUEST, new CharlistRequestHandler());
            registerHandler(RecvPacketOpcode.CHAR_SELECT_WITH_PIC, new CharSelectedHandler());
            registerHandler(RecvPacketOpcode.CHECK_CHAR_NAME, new CheckCharNameHandler());
            registerHandler(RecvPacketOpcode.CREATE_CHAR, new CreateCharHandler());
            registerHandler(RecvPacketOpcode.DELETE_CHAR, new DeleteCharHandler());
            registerHandler(RecvPacketOpcode.LOGIN_PASSWORD, new LoginPasswordHandler());
            registerHandler(RecvPacketOpcode.PICK_ALL_CHAR, new PickCharHandler());
            registerHandler(RecvPacketOpcode.REGISTER_PIC, new RegisterPicHandler());
            registerHandler(RecvPacketOpcode.RELOG, new RelogRequestHandler());
            registerHandler(RecvPacketOpcode.SERVERLIST_REQUEST, new ServerlistRequestHandler());
            registerHandler(RecvPacketOpcode.SERVERLIST_REREQUEST, new ServerlistRequestHandler());
            registerHandler(RecvPacketOpcode.SERVERSTATUS_REQUEST, new ServerStatusRequestHandler());
            registerHandler(RecvPacketOpcode.VIEW_ALL_CHAR, new ViewCharHandler());
        } else if (mode == Mode.CHANNELSERVER) {
            registerHandler(RecvPacketOpcode.ACCEPT_FAMILY, new AcceptFamilyHandler());
            registerHandler(RecvPacketOpcode.ADD_FAMILY, new FamilyAddHandler());
            registerHandler(RecvPacketOpcode.ALLIANCE_OPERATION, new AllianceOperationHandler());
            registerHandler(RecvPacketOpcode.AUTO_AGGRO, new AutoAggroHandler());
            registerHandler(RecvPacketOpcode.AUTO_DISTRIBUTE_AP, new AutoAssignHandler());
            registerHandler(RecvPacketOpcode.BBS_OPERATION, new BBSOperationHandler());
            registerHandler(RecvPacketOpcode.BEHOLDER, new BeholderHandler());
            registerHandler(RecvPacketOpcode.BUDDYLIST_MODIFY, new BuddylistModifyHandler());
            registerHandler(RecvPacketOpcode.BUY_CS_ITEM, new BuyCSItemHandler());
            registerHandler(RecvPacketOpcode.CANCEL_BUFF, new CancelBuffHandler());
            registerHandler(RecvPacketOpcode.CANCEL_CHAIR, new CancelChairHandler());
            registerHandler(RecvPacketOpcode.CANCEL_DEBUFF, new CancelDebuffHandler());
            registerHandler(RecvPacketOpcode.CANCEL_ITEM_EFFECT, new CancelItemEffectHandler());
            registerHandler(RecvPacketOpcode.CHANGE_CHANNEL, new ChangeChannelHandler());
            registerHandler(RecvPacketOpcode.CHANGE_KEYMAP, new KeymapChangeHandler());
            registerHandler(RecvPacketOpcode.CHANGE_MAP, new ChangeMapHandler());
            registerHandler(RecvPacketOpcode.CHANGE_MAP_SPECIAL, new ChangeMapSpecialHandler());
            registerHandler(RecvPacketOpcode.CHAR_INFO_REQUEST, new CharInfoRequestHandler());
            registerHandler(RecvPacketOpcode.CLOSE_CHALKBOARD, new CloseChalkboardHandler());
            registerHandler(RecvPacketOpcode.CLOSE_RANGE_ATTACK, new CloseRangeDamageHandler());
            registerHandler(RecvPacketOpcode.COUPON_CODE, new CouponCodeHandler());
            registerHandler(RecvPacketOpcode.DAMAGE_REACTOR, new ReactorHitHandler());
            registerHandler(RecvPacketOpcode.DAMAGE_SUMMON, new DamageSummonHandler());
            registerHandler(RecvPacketOpcode.DENY_GUILD_REQUEST, new DenyGuildRequestHandler());
            registerHandler(RecvPacketOpcode.DENY_PARTY_REQUEST, new DenyPartyRequestHandler());
            registerHandler(RecvPacketOpcode.DISTRIBUTE_AP, new DistributeAPHandler());
            registerHandler(RecvPacketOpcode.DISTRIBUTE_SP, new DistributeSPHandler());
            registerHandler(RecvPacketOpcode.DUEY_ACTION, new DueyHandler());
            registerHandler(RecvPacketOpcode.ENERGY_ORB_ATTACK, new EnergyOrbDamageHandler());
            registerHandler(RecvPacketOpcode.ENTER_CASH_SHOP, new EnterCashShopHandler());
            registerHandler(RecvPacketOpcode.ENTER_MTS, new EnterMTSHandler());
            registerHandler(RecvPacketOpcode.FACE_EXPRESSION, new FaceExpressionHandler());
            registerHandler(RecvPacketOpcode.GENERAL_CHAT, new GeneralchatHandler());
            registerHandler(RecvPacketOpcode.GIVE_FAME, new GiveFameHandler());
            registerHandler(RecvPacketOpcode.GUILD_OPERATION, new GuildOperationHandler());
            registerHandler(RecvPacketOpcode.HEAL_OVER_TIME, new HealOvertimeHandler());
            registerHandler(RecvPacketOpcode.HIRED_MERCHANT_REQUEST, new HiredMerchantRequest());
            registerHandler(RecvPacketOpcode.ITEM_MOVE, new ItemMoveHandler());
            registerHandler(RecvPacketOpcode.ITEM_PICKUP, new ItemPickupHandler());
            registerHandler(RecvPacketOpcode.ITEM_SORT, new ItemSortHandler());
            registerHandler(RecvPacketOpcode.ITEM_SORT2, new ItemIdSortHandler());
            registerHandler(RecvPacketOpcode.MAGIC_ATTACK, new MagicDamageHandler());
            registerHandler(RecvPacketOpcode.MAKER_SKILL, new MakerSkillHandler());
            registerHandler(RecvPacketOpcode.MESO_DROP, new MesoDropHandler());
            registerHandler(RecvPacketOpcode.MESSENGER, new MessengerHandler());
            registerHandler(RecvPacketOpcode.MOB_DAMAGE_MOB, new MobDamageMobHandler());
            registerHandler(RecvPacketOpcode.MOB_DAMAGE_MOB_FRIENDLY, new MobDamageMobFriendlyHandler());
            registerHandler(RecvPacketOpcode.MONSTER_BOMB, new MonsterBombHandler());
            registerHandler(RecvPacketOpcode.MONSTER_BOOK_COVER, new MonsterBookCoverHandler());
            registerHandler(RecvPacketOpcode.MOVE_LIFE, new MoveLifeHandler());
            registerHandler(RecvPacketOpcode.MOVE_PET, new MovePetHandler());
            registerHandler(RecvPacketOpcode.MOVE_PLAYER, new MovePlayerHandler());
            registerHandler(RecvPacketOpcode.MOVE_SUMMON, new MoveSummonHandler());
            registerHandler(RecvPacketOpcode.MTS_OP, new MTSHandler());
            registerHandler(RecvPacketOpcode.NOTE_ACTION, new NoteActionHandler());
            registerHandler(RecvPacketOpcode.NPC_ACTION, new NPCAnimation());
            registerHandler(RecvPacketOpcode.NPC_SHOP, new NPCShopHandler());
            registerHandler(RecvPacketOpcode.NPC_TALK, new NPCTalkHandler());
            registerHandler(RecvPacketOpcode.NPC_TALK_MORE, new NPCMoreTalkHandler());
            registerHandler(RecvPacketOpcode.OPEN_TREASURE, new FishingHandler());
            registerHandler(RecvPacketOpcode.PARTYCHAT, new PartyChatHandler());
            registerHandler(RecvPacketOpcode.PARTY_OPERATION, new PartyOperationHandler());
            registerHandler(RecvPacketOpcode.PARTY_SEARCH_REGISTER, new PartySearchRegisterHandler());
            registerHandler(RecvPacketOpcode.PARTY_SEARCH_START, new PartySearchStartHandler());
            registerHandler(RecvPacketOpcode.PET_AUTO_POT, new PetAutoPotHandler());
            registerHandler(RecvPacketOpcode.PET_CHAT, new PetChatHandler());
            registerHandler(RecvPacketOpcode.PET_COMMAND, new PetCommandHandler());
            registerHandler(RecvPacketOpcode.PET_EXCLUDE_ITEMS, new PetExcludeItemsHandler());
            registerHandler(RecvPacketOpcode.PET_FOOD, new PetFoodHandler());
            registerHandler(RecvPacketOpcode.PET_LOOT, new PetLootHandler());
            registerHandler(RecvPacketOpcode.PLAYER_INTERACTION, new PlayerInteractionHandler());
            registerHandler(RecvPacketOpcode.PLAYER_LOGGEDIN, new PlayerLoggedinHandler());
            registerHandler(RecvPacketOpcode.PLAYER_UPDATE, new PlayerUpdateHandler());
            registerHandler(RecvPacketOpcode.QUEST_ACTION, new QuestActionHandler());
            registerHandler(RecvPacketOpcode.RANGED_ATTACK, new RangedAttackHandler());
            registerHandler(RecvPacketOpcode.REPORT, new ReportHandler());
            registerHandler(RecvPacketOpcode.RING_ACTION, new RingActionHandler());
            registerHandler(RecvPacketOpcode.SCRIPTED_ITEM, new ScriptedItemHandler());
            registerHandler(RecvPacketOpcode.SKILL_EFFECT, new SkillEffectHandler());
            registerHandler(RecvPacketOpcode.SKILL_MACRO, new SkillMacroHandler());
            registerHandler(RecvPacketOpcode.SPAWN_PET, new SpawnPetHandler());
            registerHandler(RecvPacketOpcode.SPECIAL_MOVE, new SpecialMoveHandler());
            registerHandler(RecvPacketOpcode.SPOUSE_CHAT, new SpouseChatHandler());
            registerHandler(RecvPacketOpcode.STORAGE, new StorageHandler());
            registerHandler(RecvPacketOpcode.SUMMON_ATTACK, new SummonDamageHandler());
            registerHandler(RecvPacketOpcode.TAKE_DAMAGE, new TakeDamageHandler());
            registerHandler(RecvPacketOpcode.TOUCHING_CS, new TouchingCashShopHandler());
            registerHandler(RecvPacketOpcode.TOUCHING_REACTOR, new TouchReactorHandler());
            registerHandler(RecvPacketOpcode.TROCK_ADD_MAP, new TrockAddMapHandler());
            registerHandler(RecvPacketOpcode.USE_CASH_ITEM, new UseCashItemHandler());
            registerHandler(RecvPacketOpcode.USE_CATCH_ITEM, new UseCatchItemHandler());
            registerHandler(RecvPacketOpcode.USE_CHAIR, new UseChairHandler());
            registerHandler(RecvPacketOpcode.USE_DEATHITEM, new UseDeathItemHandler());
            registerHandler(RecvPacketOpcode.USE_DOOR, new DoorHandler());
            registerHandler(RecvPacketOpcode.USE_FAMILY, new FamilyUseHandler());
            registerHandler(RecvPacketOpcode.USE_HAMMER, new UseHammerHandler());
            registerHandler(RecvPacketOpcode.USE_INNER_PORTAL, new InnerPortalHandler());
            registerHandler(RecvPacketOpcode.USE_ITEM, new UseItemHandler());
            registerHandler(RecvPacketOpcode.USE_ITEMEFFECT, new UseItemEffectHandler());
            registerHandler(RecvPacketOpcode.USE_MAPLELIFE, new UseMapleLifeHandler());
            registerHandler(RecvPacketOpcode.USE_MOUNT_FOOD, new UseMountFoodHandler());
            registerHandler(RecvPacketOpcode.USE_REMOTE, new RemoteGachaponHandler());
            registerHandler(RecvPacketOpcode.USE_REMOTE, new UseRemoteHandler());
            registerHandler(RecvPacketOpcode.USE_RETURN_SCROLL, new UseItemHandler());
            registerHandler(RecvPacketOpcode.USE_SKILL_BOOK, new SkillBookHandler());
            registerHandler(RecvPacketOpcode.USE_SOLOMON_ITEM, new UseSolomonHandler());
            registerHandler(RecvPacketOpcode.USE_SUMMON_BAG, new UseSummonBag());
            registerHandler(RecvPacketOpcode.USE_UPGRADE_SCROLL, new ScrollHandler());
            registerHandler(RecvPacketOpcode.WHISPER, new WhisperHandler());
        } else {
            throw new RuntimeException("Unknown packet processor mode: " + mode.name() + ".");
        }
    }
}