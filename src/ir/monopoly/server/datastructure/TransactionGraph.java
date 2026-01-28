package ir.monopoly.server.datastructure;

/**
 * Tracks money flow between players using an Adjacency Matrix.
 * Essential for generating reports on who is paying whom.
 */
public class TransactionGraph {
    private final int[][] matrix;
    private final int numPlayers;

    public TransactionGraph(int numPlayers) {
        this.numPlayers = numPlayers;
        // matrix[i][j] stores total money paid by player i to player j
        this.matrix = new int[numPlayers + 1][numPlayers + 1];
    }

    /**
     * Records a rent or trade payment.
     */
    public void recordTransaction(int fromId, int toId, int amount) {
        if (fromId > 0 && toId > 0 && fromId <= numPlayers && toId <= numPlayers) {
            matrix[fromId][toId] += amount;
        }
    }

    public int getTotalPaidBy(int playerId) {
        int sum = 0;
        for (int j = 1; j <= numPlayers; j++) sum += matrix[playerId][j];
        return sum;
    }

    public int getTotalReceivedBy(int playerId) {
        int sum = 0;
        for (int i = 1; i <= numPlayers; i++) sum += matrix[i][playerId];
        return sum;
    }
}
