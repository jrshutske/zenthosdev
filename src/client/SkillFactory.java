package client;

import java.util.HashMap;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataTool;
import provider.MapleWZProvider;
import tools.StringUtil;

public class SkillFactory {
    private final static Map<Integer, ISkill> skills = new HashMap<Integer, ISkill>();

    public static ISkill getSkill(int id) {
        ISkill ret = skills.get(Integer.valueOf(id));
        if (ret != null) {
            return ret;
        }
        synchronized (skills) {
            ret = skills.get(Integer.valueOf(id));
            if (ret == null) {
                int job = id / 10000;
                final MapleData skillroot = MapleWZProvider.skillWZ.getData(StringUtil.getLeftPaddedStr(String.valueOf(job), '0', 3) + ".img");
                final MapleData skillData = skillroot.getChildByPath("skill/" + StringUtil.getLeftPaddedStr(String.valueOf(id), '0', 7));
                if (skillData != null) {
                    ret = Skill.loadFromData(id, skillData);
                }
                skills.put(Integer.valueOf(id), ret);
            }
            return ret;
        }
    }

    public static String getSkillName(int id) {
        String strId = Integer.toString(id);
        strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
        final MapleData skillroot = MapleWZProvider.skillWZ.getData("Skill.img").getChildByPath(strId);
        if (skillroot != null) {
            return MapleDataTool.getString(skillroot.getChildByPath("name"), "");
        }
        return null;
    }
}