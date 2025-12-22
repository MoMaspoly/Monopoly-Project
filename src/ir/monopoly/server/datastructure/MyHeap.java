package ir.monopoly.server.datastructure;

public class MyHeap<T extends Comparable<T>> {
    private T[] heap;
    private int size;
    private boolean isMaxHeap;

    public MyHeap(boolean isMaxHeap) {
        this(10, isMaxHeap);
    }

    public MyHeap(int capacity, boolean isMaxHeap) {
        this.heap = (T[]) new Comparable[capacity];
        this.size = 0;
        this.isMaxHeap = isMaxHeap;
    }

    private void heapifyUp(int i) {
        while (i > 0 && compare(heap[i], heap[(i - 1) / 2]) > 0) {
            swap(i, (i - 1) / 2);
            i = (i - 1) / 2;
        }
    }

    private void heapifyDown(int i) {
        int target = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;
        if (left < size && compare(heap[left], heap[target]) > 0) target = left;
        if (right < size && compare(heap[right], heap[target]) > 0) target = right;
        if (target != i) {
            swap(i, target);
            heapifyDown(target);
        }
    }

    public void update(T oldItem, T newItem) {
        for (int i = 0; i < size; i++) {
            if (heap[i].equals(oldItem)) {
                heap[i] = newItem;
                if (compare(heap[i], heap[(i - 1)/2]) > 0) {
                    heapifyUp(i);
                } else {
                    heapifyDown(i);
                }
                return;
            }
        }
    }

    private int compare(T a, T b) {
        return isMaxHeap ? a.compareTo(b) : b.compareTo(a);
    }

    private void swap(int i, int j) {
        T temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    public void insert(T item) {
        if (size == heap.length) resize();
        heap[size] = item;
        heapifyUp(size++);
    }

    private void resize() {
        T[] newHeap = (T[]) new Comparable[heap.length * 2];
        System.arraycopy(heap, 0, newHeap, 0, heap.length);
        heap = newHeap;
    }

    public T extract() {
        if (size == 0) return null;
        T root = heap[0];
        heap[0] = heap[--size];
        heapifyDown(0);
        return root;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }
}