package ir.monopoly.server.board;

import ir.monopoly.server.datastructure.MyHashTable;

public class Board {
    private Tile head;
    private Tile tail;
    private int size = 0;
    private final MyHashTable<Integer, Tile> tileMap;

    public Board() {
        this.tileMap = new MyHashTable<>();
    }

    public void addTile(Tile tile) {
        tileMap.put(tile.getTileId(), tile);

        if (head == null) {
            head = tile;
            tail = tile;
            tile.setNext(head);
        } else {
            tail.setNext(tile);
            tail = tile;
            tail.setNext(head);
        }
        size++;
    }

    public Tile getTileAt(int id) {
        return tileMap.get(id);
    }

    public Tile getHead() {
        return head;
    }

    public int getSize() {
        return size;
    }

    public Tile findTileByName(String name) {
        MyHashTable.Entry<Integer, Tile>[] allEntries = tileMap.getAllEntries();
        for (MyHashTable.Entry<Integer, Tile> entry : allEntries) {
            Tile t = entry.getValue();
            if (t.getName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    public MyHashTable.Entry<Integer, Tile>[] getAllTiles() {
        return tileMap.getAllEntries();
    }
}