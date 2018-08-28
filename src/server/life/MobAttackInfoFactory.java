package server.life;

import java.util.HashMap;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataTool;
import provider.MapleWZProvider;
import tools.Pair;
import tools.StringUtil;

public class MobAttackInfoFactory {
    private final static Map<Pair<Integer, Integer>, MobAttackInfo> mobAttacks = new HashMap<Pair<Integer, Integer>, MobAttackInfo>();

    public static MobAttackInfo getMobAttackInfo(MapleMonster mob, int attack) {
        MobAttackInfo ret = mobAttacks.get(new Pair<Integer, Integer>(Integer.valueOf(mob.getId()), Integer.valueOf(attack)));
        if (ret != null) {
            return ret;
        }
        synchronized (mobAttacks) {
            ret = mobAttacks.get(new Pair<Integer, Integer>(Integer.valueOf(mob.getId()), Integer.valueOf(attack)));
            if (ret == null) {
                MapleData mobData = MapleWZProvider.mobWZ.getData(StringUtil.getLeftPaddedStr(Integer.toString(mob.getId()) + ".img", '0', 11));
                if (mobData != null) {
                    //MapleData infoData = mobData.getChildByPath("info");
                    String linkedmob = MapleDataTool.getString("link", mobData, "");
                    if(linkedmob.length() != 0) {
                        mobData = MapleWZProvider.mobWZ.getData(StringUtil.getLeftPaddedStr(linkedmob + ".img", '0', 11));
                    }
                    MapleData attackData = mobData.getChildByPath("attack" + (attack + 1) + "/info");
                    if (attackData != null) {
                        MapleData deadlyAttack = attackData.getChildByPath("deadlyAttack");
                        int mpBurn = MapleDataTool.getInt("mpBurn", attackData, 0);
                        int disease = MapleDataTool.getInt("disease", attackData, 0);
                        int level = MapleDataTool.getInt("level", attackData, 0);
                        int mpCon = MapleDataTool.getInt("conMP", attackData, 0);
                        ret = new MobAttackInfo(mob.getId(), attack);
                        ret.setDeadlyAttack(deadlyAttack != null);
                        ret.setMpBurn(mpBurn);
                        ret.setDiseaseSkill(disease);
                        ret.setDiseaseLevel(level);
                        ret.setMpCon(mpCon);
                    }
                }
                mobAttacks.put(new Pair<Integer, Integer>(Integer.valueOf(mob.getId()), Integer.valueOf(attack)), ret);
            }
            return ret;
        }
    }
}