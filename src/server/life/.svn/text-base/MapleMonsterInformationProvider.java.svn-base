package server.life;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import database.DatabaseConnection;
import java.util.ArrayList;

public class MapleMonsterInformationProvider {
    public static class DropEntry {
        public int itemId;
        public int chance;
        public int assignedRangeStart;
        public int assignedRangeLength;

        public DropEntry(int itemId, int chance) {
            this.itemId = itemId;
            this.chance = chance;
        }

        @Override
        public String toString() {
            return itemId + " chance: " + chance;
        }
    }

    public static final int APPROX_FADE_DELAY = 90;
    private static MapleMonsterInformationProvider instance = null;
    private static final Map<Integer,List<DropEntry>> drops = new HashMap<Integer, List<DropEntry>>();
    private static final List<Integer> dontExist = new ArrayList<Integer>();

    public static MapleMonsterInformationProvider getInstance() {
        if (instance == null) instance = new MapleMonsterInformationProvider();
        return instance;
    }

    public List<DropEntry> retrieveDropChances(int monsterId) {
        if (dontExist.contains(monsterId)) {
            return null;
        }
        if (drops.containsKey(monsterId)) {
            return drops.get(monsterId);
        }
        List<DropEntry> ret = new LinkedList<DropEntry>();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT itemid, chance, monsterid FROM monsterdrops WHERE (monsterid = ? AND chance >= 0) OR (monsterid <= 0)");
            ps.setInt(1, monsterId);
            ResultSet rs = ps.executeQuery();
            MapleMonster theMonster = null;
            if (!rs.first()) {
                dontExist.add(monsterId);
                rs.close();
                ps.close();
                return null;
            }
            while (rs.next()) {
                int rowMonsterId = rs.getInt("monsterid");
                int chance = rs.getInt("chance");
                if (rowMonsterId != monsterId && rowMonsterId != 0) {
                    if (theMonster == null) {
                        theMonster = MapleLifeFactory.getMonster(monsterId);
                    }
                    chance += theMonster.getLevel() * rowMonsterId;
                }
                ret.add(new DropEntry(rs.getInt("itemid"), chance));
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            System.out.println("Error retrieving drop: " + e);
        }
        drops.put(monsterId, ret);
        return ret;
    }
}