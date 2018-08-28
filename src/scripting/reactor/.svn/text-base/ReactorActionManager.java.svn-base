package scripting.reactor;

import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import client.Equip;
import client.IItem;
import client.Item;
import client.MapleClient;
import client.MapleInventoryType;
import constants.ServerProperties;
import scripting.AbstractPlayerInteraction;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider.DropEntry;
import server.maps.BossMapMonitor;
import server.maps.MapleMap;
import server.maps.MapleReactor;

public class ReactorActionManager extends AbstractPlayerInteraction {

    private MapleReactor reactor;

    public ReactorActionManager(MapleClient c, MapleReactor reactor) {
        super(c);
        this.reactor = reactor;
    }

    public void dropItems() {
        dropItems(false, 0, 0, 0, 0);
    }

    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso) {
        dropItems(meso, mesoChance, minMeso, maxMeso, 0);
    }

    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        List<DropEntry> chances = getDropChances();
        List<DropEntry> items = new LinkedList<DropEntry>();
        int numItems = 0;
        if (meso && Math.random() < (1 / (double) mesoChance)) {
            items.add(new DropEntry(0, mesoChance));
        }
        Iterator<DropEntry> iter = chances.iterator();
        while (iter.hasNext()) {
            DropEntry d = (DropEntry) iter.next();
            if (Math.random() < (1 / (double) d.chance)) {
                numItems++;
                items.add(d);
            }
        }
        while (items.size() < minItems) {
            items.add(new DropEntry(0, mesoChance));
            numItems++;
        }
        java.util.Collections.shuffle(items);
        final Point dropPos = reactor.getPosition();
        dropPos.x -= (12 * numItems);
        for (DropEntry d : items) {
            if (d.itemId == 0) {
                int range = maxMeso - minMeso;
                int displayDrop = (int) (Math.random() * range) + minMeso;
                int mesoDrop = displayDrop * ServerProperties.getMesoRate;
                reactor.getMap().spawnMesoDrop(mesoDrop, displayDrop, dropPos, reactor, getPlayer(), meso);
            } else {
                IItem drop;
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (ii.getInventoryType(d.itemId) != MapleInventoryType.EQUIP) {
                    drop = new Item(d.itemId, (byte) 0, (short) 1);
                } else {
                    drop = ii.randomizeStats(getClient(), (Equip) ii.getEquipById(d.itemId));
                }
                reactor.getMap().spawnItemDrop(reactor, getPlayer(), drop, dropPos, false, true);
            }
            dropPos.x += 25;
        }
    }

    private List<DropEntry> getDropChances() {
        return ReactorScriptManager.getInstance().getDrops(reactor.getId());
    }

    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPosition());
    }

    public void spawnMonster(int id, int x, int y) {
        spawnMonster(id, 1, new Point(x, y));
    }

    public void spawnMonster(int id, int qty) {
        spawnMonster(id, qty, getPosition());
    }

    public void spawnMonster(int id, int qty, int x, int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    private void spawnMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            MapleMonster mob = MapleLifeFactory.getMonster(id);
            reactor.getMap().spawnMonsterOnGroudBelow(mob, pos);
        }
    }

    public Point getPosition() {
        Point pos = reactor.getPosition();
        pos.y -= 10;
        return pos;
    }

    public void spawnNpc(int npcId) {
        spawnNpc(npcId, getPosition());
    }

    public void spawnNpc(int npcId, int x, int y) {
        spawnNpc(npcId, new Point(x, y));
    }

    public MapleReactor getReactor() {
        return reactor;
    }

    public void spawnFakeMonster(int id) {
        spawnFakeMonster(id, 1, getPosition());
    }

    public void spawnFakeMonster(int id, int x, int y) {
        spawnFakeMonster(id, 1, new Point(x, y));
    }

    public void spawnFakeMonster(int id, int qty) {
        spawnFakeMonster(id, qty, getPosition());
    }

    public void spawnFakeMonster(int id, int qty, int x, int y) {
        spawnFakeMonster(id, qty, new Point(x, y));
    }

    private void spawnFakeMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            MapleMonster mob = MapleLifeFactory.getMonster(id);
            reactor.getMap().spawnFakeMonsterOnGroundBelow(mob, pos);
        }
    }

    public void killAll() {
        reactor.getMap().killAllMonsters(false);
    }

    public void killMonster(int monsId) {
        reactor.getMap().killMonster(monsId);
    }

    public void closePortal(int mapid, String pName) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).getPortal(pName).setPortalState(MaplePortal.CLOSE);
    }

    public void openPortal(int mapid, String pName) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).getPortal(pName).setPortalState(MaplePortal.OPEN);
    }

    public void closeDoor(int mapid) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).setReactorState();
    }

    public void openDoor(int mapid) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).resetReactors();
    }

    public void createMapMonitor(int pMapId, String pName) {
        MapleMap pMap = getClient().getChannelServer().getMapFactory().getMap(pMapId);
        MaplePortal portal = pMap.getPortal(pName);
        BossMapMonitor bmm = new BossMapMonitor(getPlayer().getMap(), pMap, portal);
    }
}