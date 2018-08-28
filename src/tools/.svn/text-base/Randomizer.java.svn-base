package tools;

import java.security.SecureRandom;
import java.util.Random;

public class Randomizer {
    private static Randomizer instance = new Randomizer();
    private SecureRandom secureRandom;
    private Random rand;
    private int callCount;

    public static Randomizer getInstance() {
        return instance;
    }

    private Randomizer() {
        secureRandom = new SecureRandom();
        rand = new Random(secureRandom.nextLong());
        callCount = 0;
    }

    private void callRandom() {
        if (callCount > 9) {
            secureRandom.setSeed(rand.nextLong());
            rand.setSeed(secureRandom.nextLong());
        } else {
            callCount++;
        }
    }

    public int nextInt() {
        return rand.nextInt();
    }

    public int nextInt(int i) {
        callRandom();
        return rand.nextInt(i);
    }

    public double nextDouble() {
        return rand.nextDouble();
    }
}