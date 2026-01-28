package ir.monopoly.server.datastructure;

import ir.monopoly.server.player.Player;

public class PlayerMaxHeap {
    private Player[] heap;
    private int size;
    private int capacity;

    public PlayerMaxHeap(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.heap = new Player[capacity];
    }

    private int getParent(int i) { return (i - 1) / 2; }
    private int getLeftChild(int i) { return (2 * i) + 1; }
    private int getRightChild(int i) { return (2 * i) + 2; }

    public void insert(Player player) {
        if (size == capacity) return;

        heap[size] = player;
        int current = size;
        size++;

        while (current != 0 && getWealth(heap[current]) > getWealth(heap[getParent(current)])) {
            swap(current, getParent(current));
            current = getParent(current);
        }
    }

    public Player extractMax() {
        if (size <= 0) return null;
        if (size == 1) {
            size--;
            return heap[0];
        }

        Player root = heap[0];
        heap[0] = heap[size - 1];
        size--;
        heapifyDown(0);

        return root;
    }

    private void heapifyDown(int i) {
        int left = getLeftChild(i);
        int right = getRightChild(i);
        int largest = i;

        if (left < size && getWealth(heap[left]) > getWealth(heap[largest])) largest = left;
        if (right < size && getWealth(heap[right]) > getWealth(heap[largest])) largest = right;

        if (largest != i) {
            swap(i, largest);
            heapifyDown(largest);
        }
    }

    private void swap(int i, int j) {
        Player temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    private int getWealth(Player p) {
        return p.getTotalWealth();
    }

    // متدهای جدید برای کامل‌تر شدن کلاس
    public boolean isEmpty() {
        return size == 0;
    }

    public int getSize() {
        return size;
    }

    public Player peekMax() {
        if (size == 0) return null;
        return heap[0];
    }
}