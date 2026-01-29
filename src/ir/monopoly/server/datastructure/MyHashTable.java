package ir.monopoly.server.datastructure;

public class MyHashTable<K, V> {
    private static final int CAPACITY = 100;
    private Entry<K, V>[] table;
    private int size = 0;

    @SuppressWarnings("unchecked")
    public MyHashTable() {
        table = new Entry[CAPACITY];
    }

    public static class Entry<K, V> {
        K key;
        V value;
        Entry<K, V> next;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() { return key; }
        public V getValue() { return value; }
        public void setValue(V value) { this.value = value; }
    }

    private int hash(K key) {
        return Math.abs(key.hashCode()) % CAPACITY;
    }

    public void put(K key, V value) {
        int index = hash(key);
        if (table[index] == null) {
            table[index] = new Entry<>(key, value);
            size++;
        } else {
            Entry<K, V> current = table[index];
            while (true) {
                if (current.key.equals(key)) {
                    current.value = value;
                    return;
                }
                if (current.next == null) break;
                current = current.next;
            }
            current.next = new Entry<>(key, value);
            size++;
        }
    }

    public V get(K key) {
        int index = hash(key);
        Entry<K, V> current = table[index];
        while (current != null) {
            if (current.key.equals(key)) return current.value;
            current = current.next;
        }
        return null;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public Entry<K, V>[] getAllEntries() {
        @SuppressWarnings("unchecked")
        Entry<K, V>[] all = new Entry[size];
        int j = 0;
        for (int i = 0; i < CAPACITY; i++) {
            Entry<K, V> current = table[i];
            while (current != null) {
                all[j++] = current;
                current = current.next;
            }
        }
        return all;
    }

    public int size() {
        return size;
    }
}