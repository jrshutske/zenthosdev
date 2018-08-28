package net.channel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import client.MapleCharacter;

public class PlayerStorage implements IPlayerStorage {
    private final Map<String, MapleCharacter> nameToChar = new LinkedHashMap<String, MapleCharacter>();
    private final Map<Integer, MapleCharacter> idToChar = new LinkedHashMap<Integer, MapleCharacter>();

    public void registerPlayer(MapleCharacter chr) {
        nameToChar.put(chr.getName().toLowerCase(), chr);
        idToChar.put(chr.getId(), chr);
    }

    public void deregisterPlayer(MapleCharacter chr) {
        nameToChar.remove(chr.getName().toLowerCase());
        idToChar.remove(chr.getId());
    }

    public MapleCharacter getCharacterByName(String name) {
        return nameToChar.get(name.toLowerCase());
    }

    public MapleCharacter getCharacterById(int id) {
        return idToChar.get(Integer.valueOf(id));
    }

    public Collection<MapleCharacter> getAllCharacters() {
        return nameToChar.values();
    }
}