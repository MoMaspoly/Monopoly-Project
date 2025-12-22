package ir.monopoly.server.game;

import java.util.Random;

public class Dice {

    private static final int MIN = 1;
    private static final int MAX = 6;

    private final Random random;

    public Dice() {
        this.random = new Random();
    }

    public int roll() {
        int die1 = random.nextInt(MAX) + MIN;
        int die2 = random.nextInt(MAX) + MIN;
        return die1 + die2;
    }
}