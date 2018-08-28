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

/* Rupi
Happyville Warp NPC
*/
var status = 0;

function start() {
    cm.sendSimple ("What would you like from me today ?\r\n#b#L0#I want to go to Happyville !#l\r\n\#L1#I want to get out of Happyville !#l");
}

function action(mode, type, selection) {
    if (mode == 1)
        status++;
    else {
        cm.dispose();
        return;
    }
    if (status == 1) {
        if (selection == 0) {
            cm.gainItem(1472063, 1);
            cm.warp(209000000);
        } else
            cm.warp(101000000);
        cm.sendOk("Have fun!");
        cm.dispose();
    }
}