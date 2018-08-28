package server;

import client.MapleCharacter;
import client.MapleClient;
import constants.ServerProperties;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import tools.MaplePacketCreator;

public class AntiCheats {
    private WeakReference<MapleCharacter> player;
    private static AntiCheats instance = new AntiCheats();
    private final String[] tokenReason = {"God-Mode","Fast Attack","Item Vac","Healing Undead","Same Damage","Etc Exploding","Fast HP Regen","Fast MP Regen"};
    private byte[] cheatToken = new byte[11];
    private long[] lastTime = new long[11];
    private long lastDamage = 0;
    private long summonSummonTime = 0;
    private int sequentialAttacks = 0;
    private int sequentialSummonAttack = 0;
    private int sameDamage = 0;
    private long lastAttackTime;
    private long attackingSince;
    private long regenHPSince;
    private long regenMPSince;
    private int numHPRegens;
    private int numMPRegens;
    private int lastHit;

    public AntiCheats(MapleCharacter player) {
        this.player = new WeakReference<MapleCharacter>(player);
        this.attackingSince = regenHPSince = regenMPSince = System.currentTimeMillis();
        for (byte i = 0; i < cheatToken.length; i++) {
            cheatToken[i] = 0;
            lastTime[i] = 0;
        }
    }

    public static AntiCheats getInstance() {
        return instance;
    }

    private AntiCheats() {
        instance = this;
    }

    /**
     * Type 0 - Default.
     * Type 1 - Save limit.
     * Type 2 - Contact GM.
     * Type 3 - Mesos drop.
     * Type 4 - Smega.
     * Type 5 - NPC.
     * Type 6 - Change map.
     * Type 7 - Commands.
     * Type 8 - Use of NX Items.
     * Type 9 - Chat Control.
     * Type 10 - Guild Control.
     *
     * @param limit
     * @param type
     * @return
     */
    public synchronized boolean Spam(int limit, int type) {
        if (type < 0 || lastTime.length < type || type > 10) {
            type = 0;
        }
        if (!player.get().isGM()) {
            if (System.currentTimeMillis() < limit + lastTime[type]) {
                return true;
            }
        }
        lastTime[type] = System.currentTimeMillis();
        return false;
    }

    public void addCheatToken(int type) {
        if (type < 0 || type > 10) {
            System.out.println("Misuse of CheatToken Type: " + type + ".");
        } else if (cheatToken[type] >= 10) {
            autoBan(player.get().getClient(), tokenReason[type]);
        } else {
            cheatToken[type]++;
        }
    }

    public byte getTokenCounters(int type) {
        if (type < 0 || type > 10) {
            return -1;
        } else {
            return cheatToken[type];
        }
    }

    public String getTokenType(int type) {
        if (type < 0 || type > 10) {
            return "This type does not exist.";
        } else {
            return tokenReason[type];
        }
    }

    public void resetSummonAttack() {
        summonSummonTime = System.currentTimeMillis();
        sequentialSummonAttack = 0;
    }

    public boolean checkSummonAttack() {
        sequentialSummonAttack++;
        long allowedAttacks = (System.currentTimeMillis() - summonSummonTime) / 2000 + 1;
        if (allowedAttacks < sequentialAttacks) {
            addCheatToken(1);
            return false;
        }
        return true;
    }

    public boolean checkAttack(int skillId) {
        sequentialAttacks++;
        long oldLastAttackTime = lastAttackTime;
        lastAttackTime = System.currentTimeMillis();
        long attackTime = lastAttackTime - attackingSince;
        if (sequentialAttacks > 10) {
            final int divisor;
            if (skillId == 3121004 || skillId == 5221004) {
                divisor = 30;
            } else {
                divisor = 300;
            }
            if (attackTime / divisor < sequentialAttacks) {
                addCheatToken(1);
                return false;
            }
        }
        if (lastAttackTime - oldLastAttackTime > 1500) {
            attackingSince = lastAttackTime;
            sequentialAttacks = 0;
        }
        return true;
    }

    public int checkSameDamage(long damage) {
        if (damage > 1 && lastDamage == damage) {
            sameDamage++;
        } else {
            lastDamage = damage;
            sameDamage = 0;
        }
        return sameDamage;
    }

    public boolean checkHPRegen() {
        numHPRegens++;
        if ((System.currentTimeMillis() - regenHPSince) / 100000 < numHPRegens) {
            addCheatToken(6);
            return false;
        }
        return true;
    }

    public boolean checkMPRegen() {
        numMPRegens++;
        if ((System.currentTimeMillis() - regenMPSince) / 100000 < numMPRegens) {
            addCheatToken(7);
            return false;
        }
        return true;
    }

    public boolean checkLastHit(boolean playerHit) {
        if (playerHit) {
            lastHit = 0;
        } else {
            lastHit++;
        }
        if (lastHit > 500) {
            addCheatToken(0);
            return false;
        }
        return true;
    }

    public static void autoBan(MapleClient c, String reason) {
        if (c.getPlayer().isGM()) {
            return;
        }
        if (ServerProperties.AutoBan) {
            String name = c.getPlayer().getName();
            String banReason = name + " has been banned by the system. (Reason: " + reason + ")";
            c.getPlayer().ban("ZDev| " + banReason + ".", true);
            try {
                c.getChannelServer().getWorldInterface().broadcastGMMessage(null, MaplePacketCreator.serverNotice(6, banReason).getBytes());
            } catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }
        }
    }
}