package scripting.reactor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import client.MapleClient;
import database.DatabaseConnection;
import scripting.AbstractScriptManager;
import server.life.MapleMonsterInformationProvider.DropEntry;
import server.maps.MapleReactor;

public class ReactorScriptManager extends AbstractScriptManager {
    private static ReactorScriptManager instance = new ReactorScriptManager();
    private Map<Integer, List<DropEntry>> drops = new HashMap<Integer, List<DropEntry>>();

    public synchronized static ReactorScriptManager getInstance() {
        return instance;
    }

    public void act(MapleClient c, MapleReactor reactor) {
        try {
            ReactorActionManager rm = new ReactorActionManager(c, reactor);
            Invocable iv = getInvocable("reactor/" + reactor.getId() + ".js", c);
            if (iv == null) {
                return;
            }
            engine.put("rm", rm);
            ReactorScript rs = iv.getInterface(ReactorScript.class);
            rs.act();
        } catch (Exception e) {
            System.out.println("Error executing reactor script. " + e);
        }
    }

    public List<DropEntry> getDrops(int rid) {
        List<DropEntry> ret = drops.get(rid);
        if (ret == null) {
            ret = new LinkedList<DropEntry>();
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT itemid, chance FROM reactordrops WHERE reactorid = ? AND chance >= 0");
                ps.setInt(1, rid);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ret.add(new DropEntry(rs.getInt("itemid"), rs.getInt("chance")));
                }
                rs.close();
                ps.close();
            } catch (Exception e) {
                System.out.println("Could not retrieve drops for reactor " + rid + e);
            }
            drops.put(rid, ret);
        }
        return ret;
    }

    public void clearDrops() {
        drops.clear();
    }
}