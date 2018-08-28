package server.maps;

import java.util.Collection;
import client.MapleCharacter;
import client.MapleJob;
import net.StringValueHolder;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import tools.MaplePacketCreator;

public class FourthJobQuestsPortalHandler {
    public enum FourthJobQuests implements StringValueHolder {
        RUSH("s4rush"),
        BERSERK("s4berserk");
        private final String name;

        private FourthJobQuests(String Newname) {
            this.name = Newname;
        }

        @Override
        public String getValue() {
            return name;
        }
    }

    public static boolean handlePortal(String name, MapleCharacter c) {
        if (name.equals(FourthJobQuests.RUSH.getValue())) {
            if (!checkPartyLeader(c) && !checkRush(c)) {
                c.dropMessage("You step into the portal, but it swiftly kicks you out.");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
            }
            if (!checkPartyLeader(c) && checkRush(c)) {
                c.dropMessage("You're not the party leader.");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            if (!checkRush(c)) {
                c.dropMessage("Someone in your party is not a 4th Job warrior.");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            c.getClient().getChannelServer().getEventSM().getEventManager("4jrush").startInstance(c.getParty(), c.getMap());
            return true;
        } else if (name.equals(FourthJobQuests.BERSERK.getValue())) {
            if (!checkBerserk(c)) {
                c.dropMessage("The portal to the Forgotten Shrine is locked.");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            c.getClient().getChannelServer().getEventSM().getEventManager("4jberserk").startInstance(c.getParty(), c.getMap());
            return true;
        }
        return false;
    }

    private static boolean checkRush(MapleCharacter c) {
        MapleParty CsParty = c.getParty();
        Collection<MaplePartyCharacter> CsPartyMembers = CsParty.getMembers();
        for (MaplePartyCharacter mpc : CsPartyMembers) {
            if (!MapleJob.getById(mpc.getJobId()).isA(MapleJob.WARRIOR)) return false;
            if (!(MapleJob.getById(mpc.getJobId()).isA(MapleJob.HERO) || MapleJob.getById(mpc.getJobId()).isA(MapleJob.PALADIN) || MapleJob.getById(mpc.getJobId()).isA(MapleJob.DARKKNIGHT)))
                return false;
        }
        return true;
    }

    private static boolean checkPartyLeader(MapleCharacter c) {
        return c.getParty().getLeader().getId() == c.getId();
    }

    private static boolean checkBerserk(MapleCharacter c) {
        return c.haveItem(4031475, 1, false, true);
    }
}