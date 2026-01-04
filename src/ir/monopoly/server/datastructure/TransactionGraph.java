package ir.monopoly.server.datastructure;

public class TransactionGraph {
    private final MyHashTable<Integer, MyHashTable<Integer, Integer>> adjList;

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

    @SuppressWarnings("unchecked")
    public void printFinancialSummary(String[] playerNames) {
        System.out.println("--- Financial Transaction Summary (Directed Graph) ---");

        Object[] playerEntriesRaw = adjList.getAllEntries();

        for (Object rawEntry : playerEntriesRaw) {
            MyHashTable.Entry<Integer, MyHashTable<Integer, Integer>> playerEntry =
                    (MyHashTable.Entry<Integer, MyHashTable<Integer, Integer>>) rawEntry;

            int fromPlayer = playerEntry.key;
            MyHashTable<Integer, Integer> neighbors = playerEntry.value;

            if (neighbors != null && neighbors.size() > 0) {
                System.out.print(playerNames[fromPlayer] + " paid to → ");

                Object[] transactionEntriesRaw = neighbors.getAllEntries();

                for (int j = 0; j < transactionEntriesRaw.length; j++) {
                    MyHashTable.Entry<Integer, Integer> transEntry =
                            (MyHashTable.Entry<Integer, Integer>) transactionEntriesRaw[j];

                    int toPlayer = transEntry.key;
                    int amount = transEntry.value;

                    System.out.print(playerNames[toPlayer] + ": $" + amount);
                    if (j < transactionEntriesRaw.length - 1) {
                        System.out.print(" | ");
                    }
                }
                System.out.println();
            }
        }
        System.out.println("---------------------------------------------------");
    }

    @SuppressWarnings("unchecked")
    public String getTopInteraction(String[] playerNames) {
        int maxAmount = 0;
        String topPair = "No transactions recorded";

        Object[] playerEntriesRaw = adjList.getAllEntries();

        for (Object rawEntry : playerEntriesRaw) {
            MyHashTable.Entry<Integer, MyHashTable<Integer, Integer>> playerEntry =
                    (MyHashTable.Entry<Integer, MyHashTable<Integer, Integer>>) rawEntry;

            int from = playerEntry.key;
            MyHashTable<Integer, Integer> neighbors = playerEntry.value;

            if (neighbors == null || neighbors.size() == 0) continue;

            Object[] edgesRaw = neighbors.getAllEntries();

            for (Object rawEdge : edgesRaw) {
                MyHashTable.Entry<Integer, Integer> edge =
                        (MyHashTable.Entry<Integer, Integer>) rawEdge;

                int to = edge.key;
                int amount = edge.value;

                if (amount > maxAmount) {
                    maxAmount = amount;
                    topPair = playerNames[from] + " and " + playerNames[to] + " ($" + amount + ")";
                }
            }
        }

        return "Highest Financial Interaction: " + topPair;
    }
}