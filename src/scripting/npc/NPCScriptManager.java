package scripting.npc;

import java.util.HashMap;
import java.util.Map;
import javax.script.Invocable;
import client.MapleClient;
import client.MapleCharacter;
import scripting.AbstractScriptManager;

public class NPCScriptManager extends AbstractScriptManager {
    private Map<MapleClient, NPCConversationManager> cms = new HashMap<MapleClient, NPCConversationManager>();
    private Map<MapleClient, NPCScript> scripts = new HashMap<MapleClient, NPCScript>();
    private static NPCScriptManager instance = new NPCScriptManager();

    public synchronized static NPCScriptManager getInstance() {
        return instance;
    }

    public void start(MapleClient c, int npc) {
        start(c, npc, null, null);
    }

    public void start(MapleClient c, int npc, String filename, MapleCharacter chr) {
        try {
            NPCConversationManager cm = new NPCConversationManager(c, npc, chr, filename);
            if (cms.containsKey(c)) {
                dispose(c);
                return;
            }
            cms.put(c, cm);
            Invocable iv = getInvocable("npc/" + npc + ".js", c);
            if (filename != null) {
                iv = getInvocable("npc/" + filename + ".js", c);
            }
            if (iv == null || NPCScriptManager.getInstance() == null) {
                if (iv == null) {
                    cm.sendOk("Hi I'm an uncoded npc.\r\nMy ID is #r" + npc + "#k.");
                }
                cm.dispose();
                return;
            }
            engine.put("cm", cm);
            NPCScript ns = iv.getInterface(NPCScript.class);
            scripts.put(c, ns);
            ns.start();
        } catch (Exception e) {
            System.out.println("Error executing NPC script. " + e);
            dispose(c);
            cms.remove(c);
        }
    }

    public void action(MapleClient c, byte mode, byte type, int selection) {
        NPCScript ns = scripts.get(c);
        if (ns != null) {
            try {
                ns.action(mode, type, selection);
            } catch (Exception e) {
                System.out.println("Error executing NPC script. " + e);
                dispose(c);
            }
        }
    }

    public void dispose(NPCConversationManager cm) {
        cms.remove(cm.getC());
        scripts.remove(cm.getC());
        if (cm.getFileName() != null) {
            resetContext("npc/" + cm.getFileName() + ".js", cm.getC());
        } else {
            resetContext("npc/" + cm.getNpc() + ".js", cm.getC());
        }
    }

    public void dispose(MapleClient c) {
        NPCConversationManager npccm = cms.get(c);
        if (npccm != null) {
            dispose(npccm);
        }
    }

    public NPCConversationManager getCM(MapleClient c) {
        return cms.get(c);
    }
}