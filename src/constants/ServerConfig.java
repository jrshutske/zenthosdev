package constants;

public class ServerConfig {
    public enum ChannelKeys {
        ONE("2062e90b3ea10a86ff666a76c41aa0d9e9d88f4e", 0, 1),
        TWO("5dfc64fff3b07c7c01ebd39706ec3cf3e6c37464", 0, 2),
        THREE("f47ef28d4a014d8de91de9f28ae6fcd52dfb5f77", 0, 3),
        FOUR("33fd56a7b827c7b0b2df8ea1224521cd7c00e4e4", 0, 4),
        FIVE("113f78f519e010e65853241bfcb14450c4fccb66", 0, 5),
        SIX("4abba5486022346a2b309c1c2ea6a0da41a88090", 0, 6),
        SEVEN("76134d11fe0c2b337e2b786bfcc738b975fcf40a", 0, 7),
        EIGHT("5688c244c56a884a50984130a17d0b61d06743a3", 0, 8),
        NINE("6e59a6559033c70b98148f1bd67e1b63aaeedf30", 0, 9),
        TEN("603dd499e4b134bf9925600b7f150644f9e9a50b", 0, 10),
        ELEVEN("b48f4c3c803f58950b005d785cf828027a83eac4", 0, 11),
        TWELVE("52a9458618abed6a42e228b33ade9cdf5ded10b4", 0, 12),
        THIRTEEN("190535a9ffb4d4d688ac1f3fa7dc09a6c81c3b86", 0, 13),
        FOURTEEN("5ce2b432ac85290b411ef0975b96712c1c35591a", 0, 14),
        FIFTEEN("7d8bae4945561008426174be907142196ed84275", 0, 15),
        SIXTEEN("da0517603d42ce6f9d9bdf4871bc1ecbf7a20c3c", 0, 16),
        SEVENTEEN("87c56d1e33cf26f48ac76f1bd76b6637cddd9548", 0, 17),
        EIGHTEEN("fbce35ee8db37d9bf02f444c65e49fb8a9685c28", 0, 18),
        NINETEEN("51a2bb10ecf4e2e28fe62b405106baadb0d11090", 0, 19),
        TWEENTY("9a071c700e4c051c354817f7e2482d148380d574", 0, 20);

        private final int number;
        private final int world;
        private final String s;

        private ChannelKeys(String s, int world, int number) {
            this.number = number;
            this.world = world;
            this.s = s;
        }

        public String getValue() {
            return s;
        }

        public int getNumber() {
            return number;
        }

        public int getWorld() {
            return world;
        }
    }

    public enum LoginServer {
        ONE("d3703816f23fdee7fce6ba061244736b83c88fc5", 0, 1);

        private final int number;
        private final int world;
        private final String s;

        private LoginServer(String s, int world, int number) {
            this.number = number;
            this.world = world;
            this.s = s;
        }

        public String getValue() {
            return s;
        }

        public int getNumber() {
            return number;
        }

        public int getWorld() {
            return world;
        }
    }
}