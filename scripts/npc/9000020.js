var bossmaps = Array(100000005, 105070002, 105090900, 230040420, 280030000, 220080001, 240020402, 240020101, 801040100, 240060200, 610010005, 610010012, 610010013, 610010100, 610010101, 610010102, 610010103, 610010104); // Someone else's House, The Grave of Mushmom, The cursed Sanctuary, The Cave of Pianus, Zakums Altar, Origin of Clocktower, Manons Forest, Griffey Forest, The Nightmarish Last Days, Horntails Cave, Bigfoot- Phantom Forest-Forgotten Path, Phantom Forest-Evil Rising, Phantom Forest-The Evil Dead, Phantom Forest-Twisted Path 1, Phantom Forest-Twisted Path 2, Phantom Forest-Twisted Path 3, Phantom Forest-Twisted Path 4, Phantom Forest-Twisted Path 5
var monstermaps = Array(100040001, 101010100, 104040000, 103000101, 103000105, 101030110, 106000002, 101030103, 101040001, 101040003, 101030001, 104010001, 105070001, 105090300, 105040306, 230020000, 230010400, 211041400, 222010000, 220080000, 220070301, 220070201, 220050300, 220010500, 250020000, 251010000, 200040000, 200010301, 240020100, 240040500, 240040000, 600020300, 801040004, 800020130); // Dungeon Southern Forest I, Tree that Grew 1, Henesys Hunting Ground 1, Line 1 Area 1, Line 1 Area 4, Camp 1, Dangerous Valley II, Excavation Site III, Land of Wild Boar, Iron Boar Land, The Land of Wild Boar II, The Pig Beach, Ant Tunnel Park, Drakes Meal Table, The Forest of Golem, Forked Road: East Sea, Forked Road: West Sea, Forest of Dead Trees 4, Entrance to Black Mountain, Deep Inside the Clock Tower, Forbidden Time, Lost Time, Path of Time, Terrace Hall, Practice Field, Beginner, 10-Year-Old Herb Garden, Cloud Park 3, Garden of Darkness 1, Battlefield of Fire & Darkness, Entrance to Dragon Nest, The Dragon Canyon, Wolf Spider Cavern, Armory, Encounter with the Budda, 
var townmaps = Array(680000000, 230000000, 101000000, 211000000, 100000000, 251000000, 103000000, 222000000, 104000000, 240000000, 220000000, 250000000, 800000000, 600000000, 221000000, 200000000, 102000000, 801000000, 105040300, 610010004, 260000000, 540010000, 120000000); // Amoria, Aquarium, Ellinia, El Nath, Henesys, Herb Town, Kerning City, Korean Folk Town, Leafre, Lith Harbor, Ludibrium, Mu Lung, Mushroom Shrine, New Leaf City, Omega Sector, Orbis, Perion, Showa Town, Sleepywood, Crimsonwood, Ariant, Singapore, Nautilus Port
var chosenMap;
var typee;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == 1)
		status++;
	else {
		if (status == 3 && mode == 0)
			cm.sendNext("Please don't waste my time, I'm a busy whore.");
		cm.dispose();
		return;
	}
	
	if (status == 0) {
		cm.sendNext("Hi ! I can teleport you anywhere..");
	} if (status == 1) {
		cm.sendSimple("#fUI/UIWindow.img/QuestIcon/3/0#\r\n#L0#Towns#l\r\n#L1#Monster Maps#l\r\n#L2#Boss Maps#l");
	} else if (status == 2) {
		if (selection == 0)
			typee = townmaps;
		else if (selection == 1)
			typee = monstermaps;
		else
			typee = bossmaps;
			
		var selStr = "Select your destination.#b";
		for (var i = 0; i < typee.length; i++) {
			selStr += "\r\n#L" + i + "##m" + typee[i] + "#";
		}
		cm.sendSimple(selStr);
	} else if (status == 3) {
		chosenMap = typee[selection];
		cm.sendYesNo("Do you want to go to #m" + chosenMap + "#?");
	} else if (status == 4) {
		cm.warp(chosenMap, 0);
		cm.dispose();
	}
}