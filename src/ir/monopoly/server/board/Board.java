package ir.monopoly.server.board;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the Monopoly board as a Circular Linked List.
 * This structure allows for infinite movement loops and easy tile retrieval.
 */
public class Board {
    private Tile head; // Tile 0 (GO)
    private Tile tail; // The last added tile
    private int size = 0;
    private final Map<Integer, Tile> tileMap; // Fast lookup for IDs

    public Board() {
        this.tileMap = new HashMap<>();
    }

    /**
     * Adds a new tile to the board and maintains the circular connection.
     * @param tile The tile to be added.
     */
    public void addTile(Tile tile) {
        tileMap.put(tile.getTileId(), tile);

        if (head == null) {
            head = tile;
            tail = tile;
            tile.setNext(head); // Points to itself to start the circle
        } else {
            tail.setNext(tile);
            tail = tile;
            tail.setNext(head); // Closes the circle by pointing back to GO
        }
        size++;
    }

    /**
     * Retrieves a tile by its index (0-39).
     * Used by GameState to find properties or resolve landings.
     */
    public Tile getTileAt(int id) {
        return tileMap.get(id);
    }

    /**
     * Standard getter for the starting tile.
     */
    public Tile getHead() {
        return head;
    }

    public int getSize() {
        return size;
    }

    /**
     * Utility to find a tile by name (useful for debugging or specific card effects).
     */
    public Tile findTileByName(String name) {
        for (Tile t : tileMap.values()) {
            if (t.getName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }
}
