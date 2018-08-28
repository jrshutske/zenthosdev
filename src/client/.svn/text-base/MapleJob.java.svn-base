package client;

public enum MapleJob {
    BEGINNER(0),
    WARRIOR(100),
    FIGHTER(110),
    CRUSADER(111),
    HERO(112),
    PAGE(120),
    WHITEKNIGHT(121),
    PALADIN(122),
    SPEARMAN(130),
    DRAGONKNIGHT(131),
    DARKKNIGHT(132),
    MAGICIAN(200),
    FP_WIZARD(210),
    FP_MAGE(211),
    FP_ARCHMAGE(212),
    IL_WIZARD(220),
    IL_MAGE(221),
    IL_ARCHMAGE(222),
    CLERIC(230),
    PRIEST(231),
    BISHOP(232),
    BOWMAN(300),
    HUNTER(310),
    RANGER(311),
    BOWMASTER(312),
    CROSSBOWMAN(320),
    SNIPER(321),
    MARKSMAN(322),
    THIEF(400),
    ASSASSIN(410),
    HERMIT(411),
    NIGHTLORD(412),
    BANDIT(420),
    CHIEFBANDIT(421),
    SHADOWER(422),
    PIRATE(500),
    BRAWLER(510),
    MARAUDER(511),
    BUCCANEER(512),
    GUNSLINGER(520),
    OUTLAW(521),
    CORSAIR(522),
    MAPLELEAF_BRIGADIER(800),
    GM(900),
    SUPERGM(910),
    NOBLESSE(1000),
    DAWNWARRIOR1(1100),
    DAWNWARRIOR2(1110),
    DAWNWARRIOR3(1111),
    DAWNWARRIOR4(1112),
    BLAZEWIZARD1(1200),
    BLAZEWIZARD2(1210),
    BLAZEWIZARD3(1211),
    BLAZEWIZARD4(1212),
    WINDARCHER1(1300),
    WINDARCHER2(1310),
    WINDARCHER3(1311),
    WINDARCHER4(1312),
    NIGHTWALKER1(1400),
    NIGHTWALKER2(1410),
    NIGHTWALKER3(1411),
    NIGHTWALKER4(1412),
    THUNDERBREAKER1(1500),
    THUNDERBREAKER2(1510),
    THUNDERBREAKER3(1511),
    THUNDERBREAKER4(1512),
    LEGEND(2000),
    ARAN1(2100),
    ARAN2(2110),
    ARAN3(2111),
    ARAN4(2112);
    final int jobid;

    private MapleJob(int id) {
        jobid = id;
    }

    public int getId() {
        return jobid;
    }

    public static MapleJob getById(int id) {
        for (MapleJob l : MapleJob.values()) {
            if (l.getId() == id) {
                return l;
            }
        }
        return null;
    }

    public static MapleJob getBy5ByteEncoding(int encoded) {
        switch (encoded) {
            case 2:
                return WARRIOR;
            case 4:
                return MAGICIAN;
            case 8:
                return BOWMAN;
            case 16:
                return THIEF;
            case 32:
                return PIRATE;
            case 1024:
                return NOBLESSE;
            case 2048:
                return DAWNWARRIOR1;
            case 4096:
                return BLAZEWIZARD1;
            case 8192:
                return WINDARCHER1;
            case 16384:
                return NIGHTWALKER1;
            case 32768:
                return THUNDERBREAKER1;
            case 65536:
                 return LEGEND;
            case 131072:
                 return ARAN1;
            case 262144:
                 return ARAN2;
            case 524288:
                 return ARAN3;
            case 1049576:
                 return ARAN4;
            default:
                return BEGINNER;
        }
    }

    public boolean isA(MapleJob basejob) {
        return getId() >= basejob.getId() && getId() / 100 == basejob.getId() / 100;
    }

    public boolean hasNoJob() {
        return getId() == 0 || getId() == 1000 || getId() == 2000;
    }

    public int getJobClass() {
        if (getId() < 1000) {
            return 0;
        } else if (getId() > 999 && getId() < 2000) {
            return 1;
        } else if (getId() > 1999 && getId() != 2001 && getId() < 2113) {
            return 2;
        } else {
            return -1;
        }
    }
    
    public static String getJobName(int id) {
        switch (id) {
            case 0: return "Beginner";
            case 100: return "Warrior";
            case 110: return "Fighter";
            case 111: return "Crusader";
            case 112: return "Hero";
            case 120: return "Page";
            case 121: return "White Knight";
            case 122: return "Paladin";
            case 130: return "Spearman";
            case 131: return "Dragon Knight";
            case 132: return "Dark Knight";
            case 200: return "Magician";
            case 210: return "Fire/Poison Wizard";
            case 211: return "Fire/Posion Mage";
            case 212: return "Fire/Poison Archmage";
            case 220: return "Ice/Lightning Wizard";
            case 221: return "Ice/Lightning Mage";
            case 222: return "Ice/Lightning Archmage";
            case 230: return "Cleric";
            case 231: return "Priest";
            case 232: return "Bishop";
            case 300: return "Bowman";
            case 310: return "Hunter";
            case 320: return "Crossbowman";
            case 311: return "Ranger";
            case 321: return "Sniper";
            case 312: return "Bowmaster";
            case 322: return "Marksman";
            case 400: return "Thief";
            case 410: return "Assassin";
            case 420: return "Bandit";
            case 411: return "Hermit";
            case 421: return "Bandit";
            case 412: return "Night Lord";
            case 422: return "Shadower";
            case 500: return "Pirate";
            case 510: return "Brawler";
            case 511: return "Marauder";
            case 512: return "Buccaneer";
            case 520: return "Gunslinger";
            case 521: return "Outlaw";
            case 522: return "Corsair";
            case 800: return "MapleLeaf Brigadier";
            case 900: return "GM";
            case 910: return "Super GM";
            case 1000: return "Noblesse";
            case 1100: return "Dawn Warrior (1)";
            case 1110: return "Dawn Warrior (2)";
            case 1111: return "Dawn Warrior (3)";
            case 1112: return "Dawn Warrior (4)";
            case 1200: return "Blaze Wizard (1)";
            case 1210: return "Blaze Wizard (2)";
            case 1211: return "Blaze Wizard (3)";
            case 1212: return "Blaze Wizard (4)";
            case 1300: return "Wind Archer (1)";
            case 1310: return "Wind Archer (2)";
            case 1311: return "Wind Archer (3)";
            case 1312: return "Wind Archer (4)";
            case 1400: return "Night Walker (1)";
            case 1410: return "Night Walker (2)";
            case 1411: return "Night Walker (3)";
            case 1412: return "Night Walker (4)";
            case 1500: return "Thunder Breaker (1)";
            case 1510: return "Thunder Breaker (2)";
            case 1511: return "Thunder Breaker (3)";
            case 1512: return "Thunder Breaker (4)";
            case 2000: return "Legend";
            case 2100: return "Aran (1)";
            case 2110: return "Aran (2)";
            case 2111: return "Aran (3)";
            case 2112: return "Aran (4)";
            default: return "(Class not found)";
        }
    }
}