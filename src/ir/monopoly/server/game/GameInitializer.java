package ir.monopoly.server.game;

import ir.monopoly.server.board.Board;
import ir.monopoly.server.board.Tile;
import ir.monopoly.server.board.TileType;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;
import java.util.List;

public class GameInitializer {

    // این متد توسط سرور صدا زده می‌شود
    public static GameState initializeGame(List<Player> connectedPlayers) {
        // ۱. تبدیل لیست بازیکنان شبکه به آرایه
        Player[] playersArray = connectedPlayers.toArray(new Player[0]);

        // ۲. ساخت برد کامل (پر کردن خانه‌ها)
        Board board = createFullBoard();

        // ۳. ساخت بازی
        return new GameState(playersArray, board);
    }

    // متد ساخت ۴۰ خانه بازی
    public static Board createFullBoard() {
        Board board = new Board();

        // 0: GO
        board.addTile(new Tile(0, TileType.GO, "GO"));

        // 1: Old Kent Road
        addProperty(board, 1, "Old Kent Road", "Brown", 60, 50, 30);
        // 2: Community Chest
        board.addTile(new Tile(2, TileType.CARD, "Community Chest"));
        // 3: Whitechapel Road
        addProperty(board, 3, "Whitechapel Road", "Brown", 60, 50, 30);
        // 4: Income Tax
        board.addTile(new Tile(4, TileType.TAX, "Income Tax"));
        // 5: Kings Cross Station
        addProperty(board, 5, "Kings Cross Station", "Railroad", 200, 0, 100);
        // 6: The Angel Islington
        addProperty(board, 6, "The Angel Islington", "Light Blue", 100, 50, 50);
        // 7: Chance
        board.addTile(new Tile(7, TileType.CARD, "Chance"));
        // 8: Euston Road
        addProperty(board, 8, "Euston Road", "Light Blue", 100, 50, 50);
        // 9: Pentonville Road
        addProperty(board, 9, "Pentonville Road", "Light Blue", 120, 50, 60);

        // 10: Jail
        board.addTile(new Tile(10, TileType.JAIL, "Jail"));

        // 11: Pall Mall
        addProperty(board, 11, "Pall Mall", "Pink", 140, 100, 70);
        // 12: Electric Company
        addProperty(board, 12, "Electric Company", "Utility", 150, 0, 75);
        // 13: Whitehall
        addProperty(board, 13, "Whitehall", "Pink", 140, 100, 70);
        // 14: Northumberland Avenue
        addProperty(board, 14, "Northumberland Avenue", "Pink", 160, 100, 80);
        // 15: Marylebone Station
        addProperty(board, 15, "Marylebone Station", "Railroad", 200, 0, 100);
        // 16: Bow Street
        addProperty(board, 16, "Bow Street", "Orange", 180, 100, 90);
        // 17: Community Chest
        board.addTile(new Tile(17, TileType.CARD, "Community Chest"));
        // 18: Marlborough Street
        addProperty(board, 18, "Marlborough Street", "Orange", 180, 100, 90);
        // 19: Vine Street
        addProperty(board, 19, "Vine Street", "Orange", 200, 100, 100);

        // 20: Free Parking
        board.addTile(new Tile(20, TileType.GO, "Free Parking")); // رفتار مشابه GO دارد (بدون جایزه)

        // 21: Strand
        addProperty(board, 21, "Strand", "Red", 220, 150, 110);
        // 22: Chance
        board.addTile(new Tile(22, TileType.CARD, "Chance"));
        // 23: Fleet Street
        addProperty(board, 23, "Fleet Street", "Red", 220, 150, 110);
        // 24: Trafalgar Square
        addProperty(board, 24, "Trafalgar Square", "Red", 240, 150, 120);
        // 25: Fenchurch St Station
        addProperty(board, 25, "Fenchurch St Station", "Railroad", 200, 0, 100);
        // 26: Leicester Square
        addProperty(board, 26, "Leicester Square", "Yellow", 260, 150, 130);
        // 27: Coventry Street
        addProperty(board, 27, "Coventry Street", "Yellow", 260, 150, 130);
        // 28: Water Works
        addProperty(board, 28, "Water Works", "Utility", 150, 0, 75);
        // 29: Piccadilly
        addProperty(board, 29, "Piccadilly", "Yellow", 280, 150, 140);

        // 30: Go To Jail
        board.addTile(new Tile(30, TileType.JAIL, "Go To Jail"));

        // 31: Regent Street
        addProperty(board, 31, "Regent Street", "Green", 300, 200, 150);
        // 32: Oxford Street
        addProperty(board, 32, "Oxford Street", "Green", 300, 200, 150);
        // 33: Community Chest
        board.addTile(new Tile(33, TileType.CARD, "Community Chest"));
        // 34: Bond Street
        addProperty(board, 34, "Bond Street", "Green", 320, 200, 160);
        // 35: Liverpool St Station
        addProperty(board, 35, "Liverpool St Station", "Railroad", 200, 0, 100);
        // 36: Chance
        board.addTile(new Tile(36, TileType.CARD, "Chance"));
        // 37: Park Lane
        addProperty(board, 37, "Park Lane", "Dark Blue", 350, 200, 175);
        // 38: Luxury Tax
        board.addTile(new Tile(38, TileType.TAX, "Luxury Tax"));
        // 39: Mayfair
        addProperty(board, 39, "Mayfair", "Dark Blue", 400, 200, 200);

        return board;
    }

    private static void addProperty(Board board, int id, String name, String color, int price, int houseCost, int mortgage) {
        Property prop = new Property(id, name, price, color, houseCost, mortgage);
        board.addTile(new Tile(id, TileType.PROPERTY, prop));
    }
}
