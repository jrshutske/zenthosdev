package scripting;

import java.io.File;
import java.io.FileReader;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import client.MapleClient;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractScriptManager {
    protected ScriptEngine engine;
    private ScriptEngineManager sem;

    protected AbstractScriptManager() {
        sem = new ScriptEngineManager();
    }

    protected Invocable getInvocable(String path, MapleClient c) {
        try {
            path = "scripts/" + path;
            engine = null;
            if (c != null) {
                engine = c.getScriptEngine(path);
            }
            if (engine == null) {
                File scriptFile = new File(path);
                if (!scriptFile.exists())
                    return null;
                engine = sem.getEngineByName("javascript");
                if (c != null) {
                    c.setScriptEngine(path, engine);
                }
                try (Stream<String> stream = Files.lines(scriptFile.toPath())) { 
                String lines = "load('nashorn:mozilla_compat.js');"; 
                lines += stream.collect(Collectors.joining(System.lineSeparator())); 
                engine.eval(lines); 
            } catch (final ScriptException t) { 
                if (ServerConstants.VPS) 
                    System.out.println("Idontknow"); 
                else  
                    System.out.println(t); 
                   return null; 
            }  
            }
            return (Invocable) engine;
        } catch (Exception e) {
            System.out.println("Error executing script this is it. " + e);
            return null;
        }
    }

    protected void resetContext(String path, MapleClient c) {
        path = "scripts/" + path;
        c.removeScriptEngine(path);
    }
}