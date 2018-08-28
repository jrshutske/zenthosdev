var setupTask;

function init() {
    scheduleNew();
}

function scheduleNew() {
	setupTask = em.scheduleAtFixedRate("start", 1000 * 60 * 10);
}

function cancelSchedule() {
    setupTask.cancel(true);
}

function start() {
    var Message = new Array("Thank you for using and supporting ZenthosDev.","@Commands will show a list of commands!");
    em.getChannelServer().broadcastPacket(Packages.tools.MaplePacketCreator.sendYellowTip("[ZDev-Tip] " + Message[Math.floor(Math.random() * Message.length)]));
}