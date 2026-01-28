package ir.monopoly.server.game;

import ir.monopoly.server.datastructure.PlayerMaxHeap;
import ir.monopoly.server.player.Player;

public class LeaderboardManager {

    /**
     * Uses a Max-Heap to extract the top players by total wealth.
     * @return A formatted string for GUI display.
     */
    public static String getTopKReport(GameState gameState) {
        StringBuilder sb = new StringBuilder();

        // 1. Top 3 by Wealth
        sb.append("🏆 MONOPOLY LEADERBOARD 🏆\n");
        sb.append("══════════════════════════\n\n");

        sb.append("💰 TOP 3 BY WEALTH:\n");
        sb.append("-------------------\n");
        String wealthReport = getTopByWealth(gameState);
        if (wealthReport.isEmpty()) {
            sb.append("No players yet.\n");
        } else {
            sb.append(wealthReport);
        }
        sb.append("\n");

        // 2. Top by Properties Count
        sb.append("🏠 TOP BY PROPERTIES:\n");
        sb.append("---------------------\n");
        String propertiesReport = getTopByProperties(gameState);
        sb.append(propertiesReport);
        sb.append("\n");

        // 3. Financial Interactions
        sb.append("💸 FINANCIAL INTERACTIONS:\n");
        sb.append("--------------------------\n");
        String financialReport = getFinancialStats(gameState);
        sb.append(financialReport);

        return sb.toString();
    }

    private static String getTopByWealth(GameState gameState) {
        // استفاده از PlayerMaxHeap دستی به جای MyHeap عمومی
        PlayerMaxHeap maxHeap = new PlayerMaxHeap(gameState.getPlayers().length);
        StringBuilder sb = new StringBuilder();

        for (Player p : gameState.getPlayers()) {
            if (p != null) {
                maxHeap.insert(p);
            }
        }

        int count = 1;
        // استخراج سه بازیکن ثروتمند اول
        while (count <= 3) {
            Player p = maxHeap.extractMax();
            if (p == null) break;
            sb.append(count).append(". ").append(p.getName())
                    .append(" - Total: $").append(p.getTotalWealth())
                    .append(" (Cash: $").append(p.getBalance()).append(")\n");
            count++;
        }

        if (sb.length() == 0) {
            sb.append("No players with wealth data.\n");
        }

        return sb.toString();
    }

    private static String getTopByProperties(GameState gameState) {
        StringBuilder sb = new StringBuilder();
        Player[] players = gameState.getPlayers();
        Player topPlayer = null;
        int maxProperties = 0;

        for (Player p : players) {
            if (p != null) {
                // Count properties
                final int[] count = {0};
                p.getOwnedProperties().forEach(prop -> count[0]++);

                sb.append(p.getName()).append(": ").append(count[0]).append(" properties\n");

                if (count[0] > maxProperties) {
                    maxProperties = count[0];
                    topPlayer = p;
                }
            }
        }

        if (topPlayer != null && maxProperties > 0) {
            sb.append("\n👑 Leader: ").append(topPlayer.getName())
                    .append(" with ").append(maxProperties).append(" properties!\n");
        } else {
            sb.append("No properties owned yet.\n");
        }

        return sb.toString();
    }

    private static String getFinancialStats(GameState gameState) {
        StringBuilder sb = new StringBuilder();
        boolean hasTransactions = false;

        for (Player p : gameState.getPlayers()) {
            if (p != null) {
                int paid = gameState.getTransactionGraph().getTotalPaidBy(p.getPlayerId());
                int received = gameState.getTransactionGraph().getTotalReceivedBy(p.getPlayerId());

                if (paid > 0 || received > 0) {
                    hasTransactions = true;
                    sb.append(p.getName()).append(":\n");
                    sb.append("  Paid out: $").append(paid).append("\n");
                    sb.append("  Received: $").append(received).append("\n");
                    sb.append("  Net: $").append(received - paid).append("\n\n");
                }
            }
        }

        if (!hasTransactions) {
            sb.append("No financial transactions yet.\n");
        }

        return sb.toString();
    }
}