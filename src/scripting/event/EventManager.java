package scripting.event;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import javax.script.Invocable;
import javax.script.ScriptException;
import net.channel.ChannelServer;
import net.world.MapleParty;
import server.TimerManager;
import server.maps.MapleMap;

public class EventManager {
    private Invocable iv;
    private ChannelServer cserv;
    private Map<String, EventInstanceManager> instances = new HashMap<String, EventInstanceManager>();
    private Properties props = new Properties();
    private String name;

    public EventManager(ChannelServer cserv, Invocable iv, String name) {
        this.iv = iv;
        this.cserv = cserv;
        this.name = name;
    }

    public void cancel() {
        try {
            iv.invokeFunction("cancelSchedule", (Object) null);
        } catch (ScriptException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public void schedule(String methodName, long delay) {
        schedule(methodName, null, delay);
    }

    public void schedule(final String methodName, final EventInstanceManager eim, long delay) {
        TimerManager.getInstance().schedule(new Runnable() {

            public void run() {
                try {
                    iv.invokeFunction(methodName, eim);
                } catch (ScriptException ex) {
                    ex.printStackTrace();
                } catch (NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
            }
        }, delay);
    }

    public ScheduledFuture<?> scheduleAtTimestamp(final String methodName, long timestamp) {
        return TimerManager.getInstance().scheduleAtTimestamp(new Runnable() {

            public void run() {
                try {
                    iv.invokeFunction(methodName, (Object) null);
                } catch (ScriptException ex) {
                    ex.printStackTrace();
                } catch (NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
            }
        }, timestamp);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(final String methodName, long delay) { // Ehh Fully copied and pasted, im lazy.
        return TimerManager.getInstance().register(new Runnable() {

            public void run() {
                try {
                    iv.invokeFunction(methodName, (Object) null);
                } catch (ScriptException ex) {
                    ex.printStackTrace();
                } catch (NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
            }
        }, delay);
    }

    public ChannelServer getChannelServer() {
        return cserv;
    }

    public EventInstanceManager getInstance(String name) {
        return instances.get(name);
    }

    public Collection<EventInstanceManager> getInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public EventInstanceManager newInstance(String name) {
        EventInstanceManager ret = new EventInstanceManager(this, name);
        instances.put(name, ret);
        return ret;
    }

    public void disposeInstance(String name) {
        try {
            iv.invokeFunction("dispose", (Object) null);
        } catch (ScriptException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
        instances.remove(name);
    }

    public Invocable getIv() {
        return iv;
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public String getName() {
        return name;
    }

    public void startInstance(MapleParty party, MapleMap map) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (iv.invokeFunction("setup", (Object) null));
            eim.registerParty(party, map);
        } catch (ScriptException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public void startInstance(EventInstanceManager eim) {
        try {
            iv.invokeFunction("setup", eim);
        } catch (ScriptException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }
}