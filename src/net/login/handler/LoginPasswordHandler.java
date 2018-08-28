package net.login.handler;

import client.MapleCharacter;
import client.MapleClient;
import constants.ServerProperties;
import net.MaplePacketHandler;
import net.login.LoginWorker;
import server.AutoRegister;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.KoreanDateUtil;
import java.util.Calendar;

public class LoginPasswordHandler implements MaplePacketHandler {

    @Override
    public boolean validateState(MapleClient c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String login = slea.readMapleAsciiString();
        String pwd = slea.readMapleAsciiString();
        c.setAccountName(login);
        int loginok = 0;
        boolean ipBan = c.hasBannedIP();
        boolean macBan = c.hasBannedMac();
        if (AutoRegister.getAccountExists(login.toLowerCase())) {
            loginok = c.login(login, pwd, ipBan || macBan);
        } else if (ServerProperties.AutoRegister && (!ipBan || !macBan)) {
            if (AutoRegister.createAccount(login, pwd, c.getSession().getRemoteAddress().toString(), (byte) c.getMacs().toArray().length)) {
                loginok = c.login(login, pwd, ipBan || macBan);
            } else {
                loginok = 10;
            }
        } else {
            loginok = 5;
        }
        Calendar tempbannedTill = c.getTempBanCalendar();
        if (loginok == 0 && (ipBan || macBan)) {
            loginok = 3;
            if (macBan) {
                String[] ipSplit = c.getSession().getRemoteAddress().toString().split(":");
                MapleCharacter.ban(ipSplit[0], "Enforcing account ban, Account: " + login + ".", false);
            }
        }
        if (loginok == 3) {
            c.getSession().write(MaplePacketCreator.getPermBan(c.getBanReason()));
            return;
        } else if (loginok != 0) {
            c.getSession().write(MaplePacketCreator.getLoginFailed(loginok));
            return;
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            long tempban = KoreanDateUtil.getTempBanTimestamp(tempbannedTill.getTimeInMillis());
            byte reason = c.getBanReason();
            c.getSession().write(MaplePacketCreator.getTempBan(tempban, reason));
            return;
        }
        if (c.isGm()) {
            LoginWorker.getInstance().registerGMClient(c);
        } else {
            LoginWorker.getInstance().registerClient(c);
        }
    }
}