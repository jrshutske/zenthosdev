package scripting.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.script.ScriptException;
import client.MapleCharacter;
import database.DatabaseConnection;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import provider.MapleWZProvider;
import server.TimerManager;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;

public class EventInstanceManager {
    private EventManager em;
    private List<MapleCharacter> chars = new LinkedList<MapleCharacter>();
    private List<MapleMonster> mobs = new LinkedList<MapleMonster>();
    private long eventTime = 0;
    private long timeStarted = 0;
    private Map<MapleCharacter,Integer> killCount = new HashMap<MapleCharacter,Integer>();
    private MapleMapFactory mapFactory;
    private Properties props = new Properties();
    private String name;

    public EventInstanceManager(EventManager em, String name) {
        this.em = em;
        this.name = name;
        mapFactory = new MapleMapFactory(MapleWZProvider.mapWZ, MapleWZProvider.stringWZ);
        mapFactory.setChannel(em.getChannelServer().getChannel());
    }

    public void registerPlayer(MapleCharacter chr) {
        try {
            chars.add(chr);
            chr.setEventInstance(this);
            em.getIv().invokeFunction("playerEntry", this, chr);
        } catch (ScriptException ex) {
            System.out.println("Error " + ex);
        } catch (NoSuchMethodException ex) {
            System.out.println("Error " + ex);
        }
    }

    public void startEventTimer(long time) {
        timeStarted = System.currentTimeMillis();
        eventTime = time;
    }

    public boolean isTimerStarted() {
        return eventTime > 0 && timeStarted > 0;
    }

    public long getTimeLeft() {
        return eventTime - (System.currentTimeMillis() - timeStarted);
    }

    public void registerParty(MapleParty party, MapleMap map) {
        for (MaplePartyCharacter pc : party.getMembers()) {
            MapleCharacter c = map.getCharacterById(pc.getId());
            registerPlayer(c);
        }
    }

    public void unregisterPlayer(MapleCharacter chr) {
        chars.remove(chr);
        chr.setEventInstance(null);
    }

    public int getPlayerCount() {
        return chars.size();
    }

    public List<MapleCharacter> getPlayers() {
        return new ArrayList<MapleCharacter>(chars);
    }

    public void registerMonster(MapleMonster mob) {
        mobs.add(mob);
        mob.setEventInstance(this);
    }

    public void unregisterMonster(MapleMonster mob) {
        mobs.remove(mob);
        mob.setEventInstance(null);
        if (mobs.size() == 0) {
            try {
                em.getIv().invokeFunction("allMonstersDead", this);
            } catch (ScriptException ex) {
                ex.printStackTrace();
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void playerKilled(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerDead", this, chr);
        } catch (ScriptException ex) {
            System.out.println("Error " + ex);
        } catch (NoSuchMethodException ex) {
            System.out.println("Error " + ex);
        }
    }

    public boolean revivePlayer(MapleCharacter chr) {
        try {
            Object b = em.getIv().invokeFunction("playerRevive", this, chr);
            if (b instanceof Boolean) {
                return (Boolean) b;
            }
        } catch (ScriptException ex) {
            System.out.println("Error " + ex);
        } catch (NoSuchMethodException ex) {
            System.out.println("Error " + ex);
        }
        return true;
    }

    public void playerDisconnected(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerDisconnected", this, chr);
        } catch (ScriptException ex) {
            System.out.println("Error " + ex);
        } catch (NoSuchMethodException ex) {
            System.out.println("Error " + ex);
        }
    }

    public void monsterKilled(MapleCharacter chr, MapleMonster mob) {
        try {
            Integer kc = killCount.get(chr);
            int inc = ((Double) em.getIv().invokeFunction("monsterValue", this, mob.getId())).intValue();
            if (kc == null) {
                kc = inc;
            } else {
                kc += inc;
            }
            killCount.put(chr, kc);
        } catch (ScriptException ex) {
            System.out.println("Error " + ex);
        } catch (NoSuchMethodException ex) {
            System.out.println("Error " + ex);
        }
    }

    public int getKillCount(MapleCharacter chr) {
        Integer kc = killCount.get(chr);
        if (kc == null)
            return 0;
        else
            return kc;
    }

    public void dispose() {
        chars.clear();
        mobs.clear();
        killCount.clear();
        mapFactory = null;
        em.disposeInstance(name);
        em = null;
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public void schedule(final String methodName, long delay) {
        TimerManager.getInstance().schedule(new Runnable() {
            public void run() {
                try {
                    em.getIv().invokeFunction(methodName, EventInstanceManager.this);
                } catch (NullPointerException npe) {
                } catch (ScriptException ex) {
                    ex.printStackTrace();
                } catch (NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
            }
        }, delay);
    }

    public String getName() {
        return name;
    }

    public void killByCount(MapleCharacter player) {
        try {
            em.getIv().invokeFunction("addKillCount", this, player);
        } catch (ScriptException ex) {
            System.out.println("Error " + ex);
        } catch (NoSuchMethodException ex) {
            System.out.println("Error " + ex);
        }
    }

    public void saveWinner(MapleCharacter chr) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO eventstats (event, instance, characterid, channel) VALUES (?, ?, ?, ?)");
            ps.setString(1, em.getName());
            ps.setString(2, getName());
            ps.setInt(3, chr.getId());
            ps.setInt(4, chr.getClient().getChannel());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("Error " + ex);
        }
    }

    public MapleMap getMapInstance(int mapId) {
        boolean wasLoaded = mapFactory.isMapLoaded(mapId);
        MapleMap map = mapFactory.getMap(mapId);
        if (!wasLoaded) {
            if (em.getProperty("shuffleReactors") != null && em.getProperty("shuffleReactors").equals("true")) {
                map.shuffleReactors();
            }
        }
        return map;
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public Object setProperty(String key, String value, boolean prev) {
        return props.setProperty(key, value);
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public void leftParty(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("leftParty", this, chr);
        } catch (ScriptException ex) {
            System.out.println("Error " + ex);
        } catch (NoSuchMethodException ex) {
            System.out.println("Error " + ex);
        }
    }

    public void disbandParty() {
        try {
            em.getIv().invokeFunction("disbandParty", this);
        } catch (ScriptException ex) {
            System.out.println("Error " + ex);
        } catch (NoSuchMethodException ex) {
            System.out.println("Error " + ex);
        }
    }

    public void finishPQ() {
        try {
            em.getIv().invokeFunction("clearPQ", this);
        } catch (ScriptException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public void removePlayer(MapleCharacter chr) {
        try {
            em.getIv().invokeFunction("playerExit", this, chr);
        } catch (ScriptException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isLeader(MapleCharacter chr) {
        return (chr.getParty().getLeader().getId() == chr.getId());
    }
}