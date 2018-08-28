/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
var status;
var nx = Array(500, 700, 1200, 5000, 10000, 30000, 60000);
var price = Array(500000, 650000, 7000000, 14000000, 28000000, 56000000, 112000000);

function start() {
	status = -1;
	action(1, 0, -1);
}

function action(mode, type, selection) {
	if (mode == 1)
		status++;
	else {
		if (status == 0 && mode == 0)
			cm.sendOk("You're ugly ! go away");
		cm.dispose();
		return;
	}
	
	if (cm.getPlayer().getMapId() == 200000000) {
		cm.getChar().getStorage().sendStorage(cm.getC(), 2010006);
		cm.dispose();
	} else {
		if (status == 0)
			cm.sendYesNo("Hi there ! I sell NX cash. Would you like to buy some ?");
		else if (status == 1) {
			var text = "Please select an option#b\r\n";
			for (var i = 0; i < nx.length; i++)
				text += "\r\n#L"+i+"#"+nx[i]+" for "+price[i]+" mesos.#l";
			cm.sendSimple(text);
		} else if (status == 2) {
			if (cm.getMeso() >= price[selection]) {
				cm.gainMeso(-price[selection]);
				cm.modifyNx(nx[selection]);
				cm.sendNext("Go make yourself look pretty..");
			} else {
				cm.sendOk("Not enough mesos ! fuck you :P");
		}
		cm.dispose();
	}
}}