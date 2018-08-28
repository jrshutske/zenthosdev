/*
 Made by XiuzSu
 Credits to watzmename
*/
importPackage(Packages.server);

var status;
var choice;

function start() {
	status = -1;
	action(1, 0, 0);
} 

function action(mode, type, selection) {
	if (mode == 1)
		status++;
	else {
		cm.dispose();
		return;
	}
	if (status == 0)
		cm.sendNext("Hi, I'm the store banker!");
	else if (status == 1)
		if (cm.hasTemp()) {
			if (cm.getHiredMerchantItems(true)) {
				cm.sendOk("These items where saved from the last server shutdown!");
				cm.dispose();
			} else {
				cm.sendOk("Please make some space to receive all your items.");
				cm.dispose();
			}
		} else {
			cm.sendSimple("Would you like to withdraw\r\n\r\n#b#L0#Mesos#l\r\n#L1#Items#l");
		}
	else if (status == 2) {
		cm.sendNext("Let me pull up your files...");
		choice = selection;
	} else {
		if (choice == 0) {
			if (status == 3) {
				var mesoEarnt = cm.getHiredMerchantMesos();
				if (mesoEarnt > 0)
					cm.sendYesNo("You have made "+mesoEarnt+" mesos in your store so far. Would you like to withdraw them?");
				else {
					cm.sendOk("You have not made any mesos.");
					cm.dispose();
				}
			} else if (status == 4) {
				cm.sendNext("Thank you for using my services, your mesos has been recieved.");
				cm.gainMeso(cm.getHiredMerchantMesos());
				cm.setHiredMerchantMesos(0);
				cm.dispose();
			}
		} else {
			if (cm.getHiredMerchantItems(false)) {
				cm.sendOk("Thank you for using my services, your item has been recieved.");
				cm.dispose();
			} else {
				cm.sendOk("Please make some space to receive all your items.");
				cm.dispose();
			}
		}
	}
}