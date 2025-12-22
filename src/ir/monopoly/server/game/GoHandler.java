package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;

public class GoHandler {

    public static final int GO_REWARD = 200;

    public static void handlePassingGo(Player player) {
        player.changeBalance(GO_REWARD);
    }
}

