package ir.monopoly.server.game;

import java.util.Random;

public class Dice {
    private static final int MIN = 1;
    private static final int MAX = 6;
    private final Random random;

    private int die1;
    private int die2;

    public Dice() {
        this.random = new Random();
    }

    public int[] roll() {
        die1 = random.nextInt(MAX) + MIN;
        die2 = random.nextInt(MAX) + MIN;
        return new int[]{die1, die2};
    }

    public int getSum() {
        return die1 + die2;
    }

    public boolean isDoubles() {
        return die1 == die2;
    }

    public int getDie1() {
        return die1;
    }

    public int getDie2() {
        return die2;
    }
}