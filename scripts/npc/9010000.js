var status;
var questions = new Array("In space it is possible to cry?",
"Is 'Copyrightable' the longest word in the English language that can be written without repeating a letter?",
"Is it true that slug's don't have any noses?",
"Do most Eskimoes have fridges?",
"Does the owner of the server like pie?",
"Is an Ostrich's eye bigger than it's brain?",
"Was Thomas Edison, who invented the lightbulb, afraid of the dark?",
"Is the letter 't' is the second most common letter used in the English language?",
"Is the weakest monster in MapleStory the Blue Snail ?",
"Is the most powerful monster in MapleStory the JR. Balrog ?",
"Is the weakest among the mushrooms the Orange Mushroom ?",
"When you defeat the Pig, Wild Boar, or the Horned Mushroom, you'll obtain [leather].",
"The JR. Balrog that lives in the cursed altar is a Level. 80 monster.",
"The fastest monster in MapleStory is the Stone Gollem.",
"Mushmom, a humongous mushroom monster, is basically a giant version of the Orange Mushroom.",
"Bubblings that reside in subways attack by firing bubbles from long range.",
"Beginners need to pay only 100 mesos, a major discount, to use the cabs that are placed on every town on the island."
);
var answers = new Array(false, false, false, true, true, true, true, true, false, false, false, false, true, false, true, false, false);
var rOutput = new Array("There is no gravity, so tears cannot flow",
"It's 'uncopyrightable'",
"They actually have four",
"They use fridges to keep their food from going frozen",
"Yes he does ! A!SDLASDASLD OMG BBQHAX.",
"According to some faggots on the internet, the eye is larger",
"This guy was actually a pussy, you cant blame him *COUGH NERD COUGH*",
"No idea why but yeah t is the second most command letter used.",
"The weakest monster is the GREEN SNAIL. (LV. 1)",
"Currently the most powerful monster is the CRIMSON BALROG.",
"The weakest among the mushrooms is the SPORE. (LV. 2)",
"HORNED MUSHROOM does not provide [leather].",
"JR. BALROG is a LV. 80 monster.",
"Stone Gollem and Dark Stone Gollem is considered the slowest monsters in the game.",
"MUSHMOM is a giant version of the ORANGE MUSHROOM.",
"BUBBLINGS only attack up-close.",
"Beginners only need to pay 50 mesos to use the cab."
);
var asked = new Array();
var currentQuestion;

function start() {
	status = -1;
	action(1, 0, -1);
}

function action(mode, type, selection) {
	if (status == 3 && mode == 1) { // continue quiz.
		status = 2;
		selection = 0;
	} else if (mode == 1 || (mode == 0 && type == 1)) // answering / accepting
		status++;
	else {
		if (type == 12 && mode == 0) // declining.
			cm.sendOk("I have great prizes to reward you !");
		cm.dispose();
		return;
	}
	
	if (status == 0)
		cm.sendAcceptDecline("Hey I'm #p"+cm.getNpc()+"#.\r\n I'm the quiz guy of #b"+cm.serverName()+"MS.#k How would you like to participate in this awesome true or false game and earn some prizes ?");
	else if (status == 1)
		cm.sendSimple("Excited to start ? Pick your choice.#b\r\n#L0#Start quiz !#l\r\n#L1#Explain how the quiz works#l\r\n#L2#What prizes can I obtain#l\r\n#L3#Where did the questions come from.#l");
	else if (status == 2) {
		if (selection == 0) {
			if (questions.length == asked.length) {
				cm.sendNext("You have finished the whole quiz.. Please continue to obtain your prize");
				getPrize();
				cm.dispose();
			} else {
				currentQuestion = -1;
				while (contains(currentQuestion) || currentQuestion == -1) {
					currentQuestion = Math.floor(Math.random() * questions.length);
				}
				asked.push(currentQuestion);
				cm.sendYesNo("\t\t\t\t\t\t\t\#bQUESTION NUMBER "+asked.length+":#k\r\n"+questions[currentQuestion]);
			}
		} else if (selection == 1) {
			cm.sendNext("Basically you have to answer questions that I will display for you, the more you get correct the better your prize will be.\r\nTo answer questions just press #rYes#k or #rNo#k. Have fun nigger !");
			status = 0;
		} else if (selection == 2) {
			cm.sendNext("In construction");
			status = 0;
		} else if (selection == 3) {
			cm.sendNext("OxQuiz.xml and google.. Because they're my friend ^_______^ (helped me through exams alot !  google while inside my exam ftw");
			status = 0;
		}
	} else if (status == 3) {
			var answer = mode == 0 ? false : true;
			if (answers[currentQuestion] == answer) {
				cm.sendYesNo("Correct. Do you want to continue and get better prizes ?");
			} else {
				cm.sendOk("\t\t\t\t\t\t\t\#rIncorrect answer !#k\r\n"+rOutput[currentQuestion]);
				cm.dispose();
			}
	} else if (status == 4) {
		// create random prizes etc.
		getPrize();
		cm.sendOk("Well done, you got "+asked.length+" questions correct..");
		cm.dispose();
	}
}

function contains(quest) {
    for (var i = 0; i < asked.length; i++) {
        if (asked[i] == quest)
            return true;
    }
    return false;
}

function getPrize() {
	var hasQuant = false;
	var junk = new Array(4000009, 4000006, 4000005, 4000014, 4000016, 4000023, 4000022, 4000030, 4000029, 4000036, 4000038, 4000422);
	var junkWeap = new Array(1432043, 1432000, 1432001, 1432009, 1432024, 1432042, 1432002, 1442039, 1442048, 1442007, 1442061, 1442035, 1442024, 1442025, 1382000, 1382003,
						1382018, 1382042, 1382004, 1382015, 1382012, 1382055, 1382019, 1382019, 1412001, 1412000, 1412005, 1412013, 1412018, 1412005, 1412008, 1412027, 1422000,
						1422006, 1422003, 1422004, 1422033, 1402013, 1402029, 1402007, 1402044, 1402006, 1402002, 1402010, 1402014, 1402009, 1402018, 1372005, 1372006, 1372043, 1372022, 1372001,
						1452023, 1452001, 1452032, 1472066, 1472030, 1472003, 1472000, 1462047, 1462023, 1462000, 1462034, 1462005, 1332021, 1332032, 1332007, 1332070, 1332067, 1332006,
						1312033, 1312005, 1312018, 1322051, 1322004, 1322010, 1322053
						
						);
	var useable = new Array(2022280, 2022073, 2022112, 2022089, 2010000, 2022180, 2022178, 2100016, 2100000, 2102006, 2100001, 2100007);
	var goodEqWeap = new Array(1432039, 1432007, 1432040, 1432045, 1432018, 1432011, 1432030, 1442034, 1442020, 1442019, 1442045, 1442044, 1382053, 1382007, 1382034, 1382024, 1382056,
						1382008, 1382016, 1382035, 1382037, 1412018, 1412007, 1412019, 1412027, 1412008, 1412025, 1412032, 1412009, 1412010, 1412021, 1422027, 1422013, 1422022, 1422010, 
						1422029, 1422009, 1422005, 1422025, 1402037, 1402035, 1402016, 1402034, 1402004, 1402012, 1402039, 1372010, 1372016, 1372008, 1372015, 1372033, 1372025, 1452017,
						1452019, 1452020, 1452014, 1452012, 1452052, 1472028, 1472031, 1472062, 1472053, 1472033, 1462017, 1462015, 1462021, 1462013, 1332069, 1332072, 1332026, 1332051,
						1332052, 1312030, 1312015, 1312010, 1312004, 1312016, 1322045, 1322059, 1322020, 1322019, 1322029, 4001013
						);
	var Rare = new Array(1432038, 1442002, 1382036, 1412026, 1422028, 1402036, 1372032, 1452044, 1472052, 1472051, 1462039, 1332050, 1332049, 1312031, 1322052, 1302059, 2022118);
	var rand = Math.floor(Math.random() * 100)+(asked.length*2);
	var curArray;
	if (rand < 20) {
		curArray = junk;
		hasQuant = true;
	} else if (rand >= 20 && rand <= 40) {
		curArray = junkWeap;
	} else if (rand > 40 && rand <= 60) {
		curArray = useable;
		hasQuant = true;
	} else if (rand > 60 && rand <= 80) {
		curArray = goodEqWeap;
	} else if (rand > 80 && rand <= 95) {
		curArray = goodEqWeap;
	} else {
		curArray = Rare;
	}
	cm.gainItem(curArray[Math.floor(Math.random() * curArray.length)], hasQuant ? Math.floor(Math.random() * 20) : 1);
}