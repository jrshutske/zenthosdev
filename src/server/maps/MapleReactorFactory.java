package server.maps;

import java.util.HashMap;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataTool;
import provider.MapleWZProvider;
import tools.Pair;
import tools.StringUtil;

public class MapleReactorFactory {
    private final static Map<Integer, MapleReactorStats> reactorStats = new HashMap<Integer, MapleReactorStats>();

    public static MapleReactorStats getReactor(int rid) {
        MapleReactorStats stats = reactorStats.get(Integer.valueOf(rid));
        if (stats == null) {
            int infoId = rid;
            MapleData reactorData = MapleWZProvider.reactorWZ.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) + ".img", '0', 11));
            MapleData link = reactorData.getChildByPath("info/link");
            if (link != null) {
                infoId = MapleDataTool.getIntConvert("info/link",reactorData);
                stats = reactorStats.get(Integer.valueOf(infoId));
            }
            if (stats == null) {
                reactorData = MapleWZProvider.reactorWZ.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) + ".img", '0', 11));
                MapleData reactorInfoData = reactorData.getChildByPath("0/event/0");
                stats = new MapleReactorStats();
                if (reactorInfoData != null) {
                    boolean areaSet = false;
                    int i = 0;
                    while (reactorInfoData != null) {
                        Pair<Integer,Integer> reactItem = null;
                        int type = MapleDataTool.getIntConvert("type",reactorInfoData);
                        if (type == 100) {
                            reactItem = new Pair<Integer,Integer>(MapleDataTool.getIntConvert("0",reactorInfoData),MapleDataTool.getIntConvert("1",reactorInfoData));
                            if (!areaSet) {
                                stats.setTL(MapleDataTool.getPoint("lt",reactorInfoData));
                                stats.setBR(MapleDataTool.getPoint("rb",reactorInfoData));
                                areaSet = true;
                            }
                        }
                        byte nextState = (byte)MapleDataTool.getIntConvert("state",reactorInfoData);
                        stats.addState((byte) i, type, reactItem, nextState);
                        i++;
                        reactorInfoData = reactorData.getChildByPath(i + "/event/0");
                    }
                } else {
                    stats.addState((byte) 0, 999, null, (byte) 0);
                }

                reactorStats.put(Integer.valueOf(infoId), stats);
                if (rid != infoId) {
                    reactorStats.put(Integer.valueOf(rid), stats);
                }
            } else {
                reactorStats.put(Integer.valueOf(rid), stats);
            }
        }
        return stats;
    }
}