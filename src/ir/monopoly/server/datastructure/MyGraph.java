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
}