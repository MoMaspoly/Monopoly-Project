package ir.monopoly.server.board;

/**
 * Represents a single cell on the Monopoly board.
 */
public class Tile {
    private final int tileId;
    private final TileType tileType;
    private final String name; // This was likely the missing piece
    private Object tileData; // Stores the Property object if it's a property tile
    private Tile next; // For the circular linked list board

    public Tile(int tileId, TileType tileType, String name) {
        this.tileId = tileId;
        this.tileType = tileType;
        this.name = name;
    }

    /**
     * Overloaded constructor to support tiles that carry Property data.
     */
    public Tile(int tileId, TileType tileType, Object tileData) {
        this.tileId = tileId;
        this.tileType = tileType;
        this.tileData = tileData;
        // If it's a property, use the property's name
        if (tileData instanceof ir.monopoly.server.property.Property) {
            this.name = ((ir.monopoly.server.property.Property) tileData).getName();
        } else {
            this.name = tileType.toString();
        }
    }

    public int getTileId() {
        return tileId;
    }

    public TileType getTileType() {
        return tileType;
    }

    public String getName() {
        return name;
    }

    public Object getTileData() {
        return tileData;
    }

    public void setTileData(Object tileData) {
        this.tileData = tileData;
    }

    public Tile getNext() {
        return next;
    }

    public void setNext(Tile next) {
        this.next = next;
    }
}
