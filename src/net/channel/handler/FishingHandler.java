package net.channel.handler;

import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import tools.Randomizer;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class FishingHandler extends AbstractMaplePacketHandler {
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        IItem item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (item.getItemId() != itemId || item == null || item.getQuantity() < 1) {
            return;
        }
        for (MapleFish fish : MapleItemInformationProvider.getInstance().getFishReward(itemId)) {
            if (fish.getProb() >= Randomizer.getInstance().nextInt(9) + 1) {
                MapleInventoryManipulator.addById(c, fish.getItemId(), (short) fish.getCount());
                c.getSession().write(MaplePacketCreator.sendOpenTreasure(fish.getItemId()));
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemId, 1, false, true);
                return;
            }
        }
    }

    public static final class MapleFish {
        private int itemId, prob, count;
        private String effect;

        public MapleFish(int itemId, int prob, int count, String effect) {
            this.itemId = itemId;
            this.prob = prob;
            this.count = count;
            this.effect = effect;
        }

        public int getItemId() {
            return itemId;
        }

        public int getProb() {
            return prob;
        }

        public int getCount() {
            return count;
        }

        public String getEffect() {
            return effect;
        }
    }
}