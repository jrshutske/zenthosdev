/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
					   Matthias Butz <matze@odinms.de>
					   Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/* Mia Warp
	So people aren't stuck in Nath.
*/

var status = 0;
var maps = Array(100000000, 101000000, 102000000, 103000000, 104000000, 105040300, 200000000, 211000000, 230000000, 220000000, 221000000, 222000000, 240000000, 250000000, 251000000, 600000000, 800000000, 801000000, 801040000);
var names = Array("Henesys", "Ellinia", "Perion", "Kerning", "Lith Harbor", "Sleepywood", "Orbis", "El Nath", "Aqua Road", "Ludibrium", "Omega Sector", "Korean Folk Town", "Leafre", "Mu Lung", "Herb Town", "New Leaf City", "Mushroom Shrine", "Showa Town", "Hideout");
// var cost = Array(1000, 1000, 1000, 1000);
var selectedMap = -1;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (status >= 2 && mode == 0) {
			cm.sendOk("Alright, see you next time.");
			cm.dispose();
			return;
		}
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			cm.sendNext("Hi, I'm Mia.");
		} else if (status == 1) {
			cm.sendNextPrev("I can take you anywhere!")
		} else if (status == 2) {
			var selStr = "Select your destination.#b";
			for (var i = 0; i < maps.length; i++) {
				selStr += "\r\n#L" + i + "#" + names[i] + "#l";
			}
			cm.sendSimple(selStr);
		} else if (status == 3) {
// 			   if (cm.getMeso() < cost[selection]) {
			   if (cm.getMeso() < 0) {
				cm.sendOk("You do not have enough mesos.")
				cm.dispose();
			} else {
				cm.sendYesNo("So you have nothing left to do here? Do you want to go to " + names[selection] + "?");
				selectedMap = selection;
			}
		} else if (status == 4) {
// 			cm.gainMeso(-cost[selectedMap]);
			cm.warp(maps[selectedMap], 0);
			cm.dispose();
		}
	}
}	