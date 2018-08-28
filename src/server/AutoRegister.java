package server;

import client.LoginCrypto;
import constants.ServerProperties;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AutoRegister {
    private static final List<String> accountList = new ArrayList<String>();

    public static boolean getAccountExists(String login) {
        if (accountList.isEmpty()) {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT name, LastLoginInMilliseconds FROM accounts ORDER BY LastLoginInMilliseconds DESC");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    long lastTime = rs.getLong("LastLoginInMilliseconds");
                    if (System.currentTimeMillis() < (lastTime + 604800000)) {
                        accountList.add(rs.getString("name").toLowerCase());
                    } else {
                        break;
                    }
                }
                rs.close();
                ps.close();
            } catch (Exception e) {
                System.out.println("ZDev: Error loading the account list: " + e + ".");
            }
        }
        if (accountList.contains(login)) {
            return true;
        } else {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT name FROM accounts WHERE name = ? LIMIT 1");
                ps.setString(1, login);
                ResultSet rs = ps.executeQuery();
                if (rs.first()) {
                    accountList.add(rs.getString("name").toLowerCase());
                    rs.close();
                    ps.close();
                    return true;
                }
                rs.close();
                ps.close();
            } catch (Exception e) {
                System.out.println("ZDev: Error checking for an account: " + e + ".");
            }
            return false;
        }
    }

    public static boolean createAccount(String login, String pwd, String eip, byte macs) {
        boolean success = false;
        if (ServerProperties.autoRegLimit > macs) {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO accounts (name, password, email, macs, lastknownip) VALUES (?, ?, ?, ?, ?)");
                ps.setString(1, login);
                ps.setString(2, LoginCrypto.hexSha1(pwd));
                ps.setString(3, "no@email.provided");
                ps.setString(4, "00-00-00-00-00-00");
                ps.setString(5, eip);
                ps.executeUpdate();
                ps.close();
                success = true;
                accountList.add(login.toLowerCase());
            } catch (Exception e) {
                System.out.println("ZDev: Error creating an account (" + login + " | " + pwd + " | " + eip + ").");
            }
        }
        return success;
    }
}