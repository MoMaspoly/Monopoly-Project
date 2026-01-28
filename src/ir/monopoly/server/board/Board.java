package ir.monopoly.server.board;

import java.util.HashMap;
import java.util.Map;

public class Board {
    private Tile head;
    private Tile tail;
    private int size = 0;
    private final Map<Integer, Tile> tileMap;

    public Board() {
        this.tileMap = new HashMap<>();
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
        for (Tile t : tileMap.values()) {
            if (t.getName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }
}
