package ir.monopoly.server.game;

import ir.monopoly.server.datastructure.MyHeap;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;

public class LeaderboardManager {

    public static void printTopPlayers(GameState gameState) {
        MyHeap<Player> maxHeap = new MyHeap<>(true);

        for (Player p : gameState.getPlayers()) {
            if (p.getStatus() != PlayerStatus.BANKRUPT) {
                maxHeap.insert(p);
            }
        }

        System.out.println("--- LEADERBOARD (Top Wealthy Players - Using MyHeap) ---");
        int count = 1;
        while (count <= 3 && !maxHeap.isEmpty()) {
            Player top = maxHeap.extract();
            if (top == null) break;
            System.out.println(count + ". " + top.getName() + " | Wealth: $" + top.getBalance());
            count++;
        }

        System.out.println("\n" + gameState.getWealthReport());
    }
}