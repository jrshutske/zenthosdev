var status = -1;
function start (){
    action(1,0,0);
}

function action(Mode,Type,Selection){
    if(Mode != 1){
        cm.dispose();
        return;
    }else
        status++;
    if(status == 0)
        cm.sendNext("Valentin Day's.. awwhhh the flower rise with a powerful smell and the birds sings more beautiful than ever... ohh the love can be feel on the air! OMG OMG #r<3#k.");
    else if(status == 1)
        cm.sendYesNo("Oh.. hi #h #, sorry.. I distracted. Do you wants a heart box with nice chocolate inside? :D");
    else if(status == 2)
        cm.sendNext("So then, you have to give me materias and mesos to do it. :)\r\n\r\nAll I need is:\r\n\r\n1 #i4031111# #b#t4031111# \r\n1 #i4031112# #b#t4031112# \r\n1 #i4031109# #b#t4031109# \r\n1 #i4031110# #b#t4031110# \r\n 1000 Mesos");
    else if(status == 3){
        if(cm.haveItem(4031111) && cm.haveItem(4031112) && cm.haveItem(4031110) && cm.haveItem(4031109) && cm.getMeso() > 999){
            cm.sendOk("Nice, there you got #i4140100#");
            cm.gainItem(4140100,1);
        }else
            cm.sendNext("Double-check you inventory hunny <3");
        cm.dispose();
    }
}