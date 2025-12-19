package ir.monopoly.server.board;

public class Tile {

    private final int tileId;
    private final TileType tileType;
    private Object tileData;
    private Tile next;

    public Tile(int tileId, TileType tileType, Object tileData) {
        this.tileId = tileId;
        this.tileType = tileType;
        this.tileData = tileData;
        this.next = null;
    }

    public int getTileId() {
        return tileId;
    }

    public TileType getTileType() {
        return tileType;
    }

    public Object getTileData() {
        return tileData;
    }

    public Tile getNext() {
        return next;
    }

    public void setNext(Tile next) {
        this.next = next;
    }
}