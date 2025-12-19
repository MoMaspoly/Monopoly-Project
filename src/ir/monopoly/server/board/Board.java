package ir.monopoly.server.board;

public class Board {

    private Tile head;
    private int size;

    public Board() {
        this.head = null;
        this.size = 0;
    }

    public void addTile(Tile tile) {
        if (head == null) {
            head = tile;
            tile.setNext(tile);
        } else {
            Tile current = head;
            while (current.getNext() != head) {
                current = current.getNext();
            }
            current.setNext(tile);
            tile.setNext(head);
        }
        size++;
    }

    public Tile move(Tile start, int steps) {
        Tile current = start;
        for (int i = 0; i < steps; i++) {
            current = current.getNext();
        }
        return current;
    }

    public Tile getTileAt(int position) {

        Tile current = head;
        for (int i = 0; i < position; i++) {
            current = current.getNext();
        }
        return current;
    }

    public Tile getHead() {
        return head;
    }

    public int getSize() {
        return size;
    }
}