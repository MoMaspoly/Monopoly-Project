package ir.monopoly.server.datastructure;

public class MyGraph {
    private final MyHashTable<Integer, MyHashTable<Integer, Integer>> adjList;

    public MyGraph() {
        this.adjList = new MyHashTable<>();
    }

    public void addEdge(int from, int to, int amount) {
        MyHashTable<Integer, Integer> neighbors = adjList.get(from);
        if (neighbors == null) {
            neighbors = new MyHashTable<>();
            adjList.put(from, neighbors);
        }
        Integer current = neighbors.get(to);
        if (current == null) current = 0;
        neighbors.put(to, current + amount);
    }

    public int getWeight(int from, int to) {
        MyHashTable<Integer, Integer> neighbors = adjList.get(from);
        if (neighbors == null) return 0;
        Integer weight = neighbors.get(to);
        return weight == null ? 0 : weight;
    }

    public MyHashTable.Entry<Integer, Integer>[] getNeighbors(int playerId) {
        MyHashTable<Integer, Integer> neighbors = adjList.get(playerId);
        if (neighbors == null) return new MyHashTable.Entry[0];
        return neighbors.getAllEntries();
    }

    public boolean hasEdge(int from, int to) {
        return getWeight(from, to) > 0;
    }

    public String printGraph() {
        StringBuilder sb = new StringBuilder();
        MyHashTable.Entry<Integer, MyHashTable<Integer, Integer>>[] allEntries = adjList.getAllEntries();
        for (MyHashTable.Entry<Integer, MyHashTable<Integer, Integer>> entry : allEntries) {
            int from = entry.key;
            MyHashTable<Integer, Integer> neighbors = entry.value;
            MyHashTable.Entry<Integer, Integer>[] neighborEntries = neighbors.getAllEntries();
            for (MyHashTable.Entry<Integer, Integer> neighbor : neighborEntries) {
                int to = neighbor.key;
                int weight = neighbor.value;
                sb.append("Player ").append(from).append(" -> Player ").append(to)
                        .append(" : $").append(weight).append("\n");
            }
        }
        return sb.toString();
    }
}