package database;

import constants.ServerProperties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

public class DatabaseConnection {
    private static ThreadLocal<Connection> con = new ThreadLocalConnection();
    private final static String dbUserName = ServerProperties.getDBusername;
    private final static String dbPassword = ServerProperties.getDBpassword;

    public static Connection getConnection() {
        return con.get();
    }

    public static void closeAll() throws SQLException {
        for (Connection conn : ThreadLocalConnection.allConnections) {
            conn.close();
        }
    }

    private static class ThreadLocalConnection extends ThreadLocal<Connection> {
        public static Collection<Connection> allConnections = new LinkedList<Connection>();

        @Override
        protected Connection initialValue() {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                System.out.println("ERROR: " + e);
            }
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/zenthosdev?autoReconnect=true", dbUserName, dbPassword);
                allConnections.add(con);
                return con;
            } catch (SQLException e) {
                System.out.println("ERROR: " + e);
                return null;
            }
        }
    }
}