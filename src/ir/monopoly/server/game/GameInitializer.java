package ir.monopoly.server.game;

import ir.monopoly.server.board.*;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;
import java.util.List;

/**
 * FIXED: Strictly maps all 40 tiles. Ensures Jail (10) and Go To Jail (30)
 * are correctly indexed and not treated as properties.
 */
public class GameInitializer {
    public static GameState initializeGame(List<Player> players) {
        Board board = new Board();
        for (int i = 0; i < 40; i++) {
            Tile tile;
            switch (i) {
                case 0 -> tile = new Tile(i, TileType.GO, "GO");
                case 10 -> tile = new Tile(i, TileType.JAIL, "Jail");
                case 20 -> tile = new Tile(i, TileType.GO, "Free Parking");
                case 30 -> tile = new Tile(i, TileType.JAIL, "Go To Jail");
                case 2, 17, 33 -> tile = new Tile(i, TileType.CARD, "Community Chest");
                case 7, 22, 36 -> tile = new Tile(i, TileType.CARD, "Chance");
                case 4, 38 -> tile = new Tile(i, TileType.TAX, "Tax");
                case 5, 15, 25, 35 -> tile = new Tile(i, TileType.PROPERTY, new Property(i, "Station", 200, "Railroad", 0, 100));
                case 12, 28 -> tile = new Tile(i, TileType.PROPERTY, new Property(i, "Utility", 150, "Utility", 0, 75));
                default -> {
                    String color = getGroup(i);
                    Property p = new Property(i, "Street " + i, 60 + (i * 10), color, 50, 30);
                    tile = new Tile(i, TileType.PROPERTY, p);
                }
            }
            board.addTile(tile);
        }
        return new GameState(players.toArray(new Player[0]), board);
    }

    private static String getGroup(int i) {
        if (i < 10) return "Brown"; if (i < 20) return "Pink";
        if (i < 30) return "Red"; return "Blue";
    }
}
