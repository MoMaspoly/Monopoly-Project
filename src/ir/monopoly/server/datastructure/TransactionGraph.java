package ir.monopoly.server.datastructure;

/**
 * Tracks money flow between players using MyGraph (adjacency list).
 * Essential for generating reports on who is paying whom.
 */
public class TransactionGraph {
    private final MyGraph graph;
    private final int numPlayers;

    public TransactionGraph(int numPlayers) {
        this.numPlayers = numPlayers;
        this.graph = new MyGraph();
    }

    /**
     * Records a rent or trade payment.
     */
    public void recordTransaction(int fromId, int toId, int amount) {
        if (fromId > 0 && toId > 0 && fromId <= numPlayers && toId <= numPlayers && amount > 0) {
            graph.addEdge(fromId, toId, amount);
        }
    }

    public int getTotalPaidBy(int playerId) {
        if (playerId < 1 || playerId > numPlayers) return 0;
        int sum = 0;
        for (int j = 1; j <= numPlayers; j++) {
            sum += graph.getWeight(playerId, j);
        }
        return sum;
    }

    public int getTotalReceivedBy(int playerId) {
        if (playerId < 1 || playerId > numPlayers) return 0;
        int sum = 0;
        for (int i = 1; i <= numPlayers; i++) {
            sum += graph.getWeight(i, playerId);
        }
        return sum;
    }

    /**
     * Get net balance (received - paid)
     */
    public int getNetBalance(int playerId) {
        return getTotalReceivedBy(playerId) - getTotalPaidBy(playerId);
    }
}