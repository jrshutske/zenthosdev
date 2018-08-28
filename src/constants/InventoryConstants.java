package constants;

public final class InventoryConstants {
    public final static int LOCK = 0x01;
    public final static int SPIKES = 0x02;
    public final static int COLD = 0x04;
    public final static int UNTRADEABLE = 0x08;
    public final static int KARMA = 0x10;
    public final static int PET_COME = 0x80;
    public final static int UNKNOWN_SKILL = 0x100;
    public final static float ITEM_ARMOR_EXP = 1 / 350000;
    public static final float ITEM_WEAPON_EXP = 1 / 700000;

    public static int getFlagByInt(int type) {
        if (type == 128) {
            return PET_COME;
        } else if (type == 256) {
            return UNKNOWN_SKILL;
        }
        return 0;
    }

    public static boolean isThrowingStar(int itemId) {
        return itemId / 10000 == 207;
    }

    public static boolean isBullet(int itemId) {
        return itemId / 10000 == 233;
    }

    public static boolean isRechargable(int itemId) {
        return itemId / 10000 == 233 || itemId / 10000 == 207;
    }

    public static boolean isArrowForCrossBow(int itemId) {
        return itemId / 1000 == 2061;
    }

    public static boolean isArrowForBow(int itemId) {
        return itemId / 1000 == 2060;
    }

    public static boolean isArrow(int itemId) {
        return itemId / 1000 == 2060 || itemId / 1000 == 2061;
    }

    public static boolean isOverall(int itemId) {
        return itemId / 10000 == 1050000;
    }

    public static boolean isWeapon(int itemId) {
        return itemId >= 1302000 && itemId < 1492024;
    }
}