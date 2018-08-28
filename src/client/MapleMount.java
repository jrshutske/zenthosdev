package client;

import java.util.concurrent.ScheduledFuture;
import server.TimerManager;
import tools.MaplePacketCreator;

public class MapleMount {

    private int itemid;
    private int skillid;
    private int tiredness;
    private int exp;
    private int level;
    private ScheduledFuture<?> tirednessSchedule;
    private MapleCharacter owner;
    private boolean active;

    public MapleMount(MapleCharacter owner, int id, int skillid) {
        this.itemid = id;
        this.skillid = skillid;
        this.tiredness = 0;
        this.level = 1;
        this.exp = 0;
        this.owner = owner;
        active = true;
    }

    public int getItemId() {
        return itemid;
    }

    public int getSkillId() {
        return skillid;
    }

    public int getId() {
        return itemid < 1903000 ? itemid - 1901999 : 5;
    }

    public int getTiredness() {
        return tiredness;
    }

    public int getExp() {
        return exp;
    }

    public int getLevel() {
        return level;
    }

    public void setTiredness(int newtiredness) {
        this.tiredness = newtiredness;
        if (tiredness < 0) {
            tiredness = 0;
        }
    }

    public void increaseTiredness() {
        this.tiredness++;
        owner.getMap().broadcastMessage(MaplePacketCreator.updateMount(owner.getId(), this, false));
        if (tiredness > 99) {
            owner.dispelSkill((owner.getJob().getJobClass() * 10000000) + 1004);
        }
    }

    public void setExp(int newexp) {
        this.exp = newexp;
    }

    public void setLevel(int newlevel) {
        this.level = newlevel;
    }

    public void setItemId(int newitemid) {
        this.itemid = newitemid;
    }

    public void setSkillId(int skillid) {
        this.skillid = skillid;
    }

    public void startSchedule() {
        this.tirednessSchedule = TimerManager.getInstance().register(new Runnable() {

            public void run() {
                increaseTiredness();
            }
        }, 60000, 60000);
    }

    public void cancelSchedule() {
        if (this.tirednessSchedule != null) {
            this.tirednessSchedule.cancel(false);
        }
    }

    public void setActive(boolean set) {
        active = set;
    }

    public boolean isActive() {
        return active;
    }
}