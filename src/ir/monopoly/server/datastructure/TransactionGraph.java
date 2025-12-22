package ir.monopoly.server.datastructure;

public class TransactionGraph {
    private MyHashTable<Integer, MyHashTable<Integer, Integer>> adjList;

    public TransactionGraph() {
        this.adjList = new MyHashTable<>();
    }

    public TransactionGraph(int numPlayers) {
        this.adjList = new MyHashTable<>();
    }

    public void recordTransaction(int from, int to, int amount) {
        if (from == to || amount <= 0) return;
        MyHashTable<Integer, Integer> neighbors = adjList.get(from);
        if (neighbors == null) {
            neighbors = new MyHashTable<>();
            adjList.put(from, neighbors);
        }
        Integer current = neighbors.get(to);
        neighbors.put(to, (current == null ? 0 : current) + amount);
    }

    public void printFinancialSummary(String[] playerNames) {
        System.out.println("--- Financial Summary (Graph) ---");
        for (int i = 0; i < playerNames.length; i++) {
            MyHashTable<Integer, Integer> neighbors = adjList.get(i);
            if (neighbors != null) {
                System.out.println(playerNames[i] + " -> ");
            }
        }
    }

    public String getTopInteraction(String[] playerNames) {
        int maxAmount = 0;
        String topPair = "None";
        return topPair;
    }
}
