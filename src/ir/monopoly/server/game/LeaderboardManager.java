package ir.monopoly.server.game;

import ir.monopoly.server.datastructure.MyHeap;
import ir.monopoly.server.player.Player;

public class LeaderboardManager {

    /**
     * Uses a Max-Heap to extract the top players by total wealth.
     * @return A formatted string for GUI display.
     */
    public static String getTopKReport(GameState gameState) {
        MyHeap<Player> maxHeap = new MyHeap<>(true);

        for (Player p : gameState.getPlayers()) {
            maxHeap.insert(p);
        }

        StringBuilder sb = new StringBuilder("🏆 MONOPOLY LEADERBOARD 🏆\n\n");
        int count = 1;
        while (!maxHeap.isEmpty() && count <= 3) {
            Player p = maxHeap.extract();
            sb.append(count).append(". ").append(p.getName())
                    .append(" - Wealth: $").append(p.getTotalWealth()).append("\n");
            count++;
        }
        return sb.toString();
    }
}
