package provider;

import java.io.File;

public class MapleWZProvider {
    private final static String wzPath = System.getProperty("wzpath");
    public final static MapleDataProvider equipWZ;
    public final static MapleDataProvider etcWZ;
    public final static MapleDataProvider itemWZ;
    public final static MapleDataProvider mapWZ;
    public final static MapleDataProvider mobWZ;
    public final static MapleDataProvider questWZ;
    public final static MapleDataProvider reactorWZ;
    public final static MapleDataProvider skillWZ;
    public final static MapleDataProvider stringWZ;

    static {
        equipWZ = MapleDataProviderFactory.getDataProvider(new File(wzPath + "/Character.wz"));
        etcWZ = MapleDataProviderFactory.getDataProvider(new File(wzPath + "/Etc.wz"));
        itemWZ = MapleDataProviderFactory.getDataProvider(new File(wzPath + "/Item.wz"));
        mapWZ = MapleDataProviderFactory.getDataProvider(new File(wzPath + "/Map.wz"));
        mobWZ = MapleDataProviderFactory.getDataProvider(new File(wzPath + "/Mob.wz"));
        questWZ = MapleDataProviderFactory.getDataProvider(new File(wzPath + "/Quest.wz"));
        reactorWZ = MapleDataProviderFactory.getDataProvider(new File(wzPath + "/Reactor.wz"));
        skillWZ = MapleDataProviderFactory.getDataProvider(new File(wzPath + "/Skill.wz"));
        stringWZ = MapleDataProviderFactory.getDataProvider(new File(wzPath + "/String.wz"));
    }
}