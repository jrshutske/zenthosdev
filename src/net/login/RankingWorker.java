package net.login;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import client.MapleJob;
import database.DatabaseConnection;

public class RankingWorker implements Runnable {
    private Connection con;
    private long lastUpdate = System.currentTimeMillis();
	
    public void run() {
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            updateRanking(null);
            short[] ranks = {0, 100, 200, 300, 400, 500, 1000, 1100, 1200, 1300, 1400, 1500, 2000, 2100};
            for (byte j = 0; j < ranks.length; j++) {
                updateRanking(MapleJob.getById(ranks[j]));
            }
            con.commit();
            con.setAutoCommit(true);
            lastUpdate = System.currentTimeMillis();
        } catch (SQLException ex) {
            try {
                con.rollback();
                con.setAutoCommit(true);
                System.out.println("Could not update rankings: " + ex);
            } catch (SQLException ex2) {
                System.out.println("Could not rollback unfinished ranking transaction: " + ex2);
            }
        }
    }
	
    private void updateRanking(MapleJob job) throws SQLException {
        String sqlCharSelect = "SELECT c.id, " + (job != null ? "c.jobRank, c.jobRankMove" : "c.rank, c.rankMove") + ", a.lastlogin AS lastlogin, a.loggedin FROM characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id WHERE c.gm = 0 ";
        if (job != null) {
            sqlCharSelect += "AND c.job DIV 100 = ? ";
        }
        sqlCharSelect += "ORDER BY c.reborns DESC , c.level DESC , c.exp DESC , c.fame DESC , c.meso DESC";
        PreparedStatement charSelect = con.prepareStatement(sqlCharSelect);
        if (job != null) {
            charSelect.setInt(1, job.getId() / 100);
        }
        ResultSet rs = charSelect.executeQuery();
        PreparedStatement ps = con.prepareStatement("UPDATE characters SET " + (job != null ? "jobRank = ?, jobRankMove = ? " : "rank = ?, rankMove = ? ") + "WHERE id = ?");
        int rank = 0;
        while (rs.next()) {
            int rankMove = 0;
            rank++;
            if (rs.getLong("lastlogin") < lastUpdate || rs.getInt("loggedin") > 0) {
                rankMove = rs.getInt((job != null ? "jobRankMove" : "rankMove"));
            }
            rankMove += rs.getInt((job != null ? "jobRank" : "rank")) - rank;
            ps.setInt(1, rank);
            ps.setInt(2, rankMove);
            ps.setInt(3, rs.getInt("id"));
            ps.executeUpdate();
        }
        rs.close();
        charSelect.close();
        ps.close();
    }
}