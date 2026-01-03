package ir.monopoly.server.network;

import ir.monopoly.server.board.Board;
import ir.monopoly.server.board.Tile;
import ir.monopoly.server.board.TileType;
import ir.monopoly.server.game.GameState;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.property.Property;

public class GameInitializer {

    // این متد کل صفحه بازی (۴۰ خانه) را طبق استاندارد مونوپولی می‌سازد
    public static Board initializeBoard() {
        Board board = new Board();

        // 0: GO (شروع)
        board.addTile(new Tile(0, TileType.GO, "GO"));

        // 1: Old Kent Road (Brown) - قیمت: 60 - هزینه خانه: 50 - رهن: 30
        addProperty(board, 1, "Old Kent Road", "Brown", 60, 50, 30);

        // 2: Community Chest
        board.addTile(new Tile(2, TileType.CARD, "Community Chest"));

        // 3: Whitechapel Road (Brown)
        addProperty(board, 3, "Whitechapel Road", "Brown", 60, 50, 30);

        // 4: Income Tax (مالیات)
        board.addTile(new Tile(4, TileType.TAX, "Income Tax"));

        // 5: King's Cross Station (Railroad)
        addProperty(board, 5, "King's Cross Station", "Railroad", 200, 0, 100);

        // 6: The Angel Islington (Light Blue)
        addProperty(board, 6, "The Angel Islington", "Light Blue", 100, 50, 50);

        // 7: Chance
        board.addTile(new Tile(7, TileType.CARD, "Chance"));

        // 8: Euston Road (Light Blue)
        addProperty(board, 8, "Euston Road", "Light Blue", 100, 50, 50);

        // 9: Pentonville Road (Light Blue)
        addProperty(board, 9, "Pentonville Road", "Light Blue", 120, 50, 60);

        // 10: Jail (زندان)
        board.addTile(new Tile(10, TileType.JAIL, "Jail"));

        // 11: Pall Mall (Pink)
        addProperty(board, 11, "Pall Mall", "Pink", 140, 100, 70);

        // 12: Electric Company (Utility)
        addProperty(board, 12, "Electric Company", "Utility", 150, 0, 75);

        // 13: Whitehall (Pink)
        addProperty(board, 13, "Whitehall", "Pink", 140, 100, 70);

        // 14: Northumberland Avenue (Pink)
        addProperty(board, 14, "Northumberland Avenue", "Pink", 160, 100, 80);

        // 15: Marylebone Station (Railroad)
        addProperty(board, 15, "Marylebone Station", "Railroad", 200, 0, 100);

        // 16: Bow Street (Orange)
        addProperty(board, 16, "Bow Street", "Orange", 180, 100, 90);

        // 17: Community Chest
        board.addTile(new Tile(17, TileType.CARD, "Community Chest"));

        // 18: Marlborough Street (Orange)
        addProperty(board, 18, "Marlborough Street", "Orange", 180, 100, 90);

        // 19: Vine Street (Orange)
        addProperty(board, 19, "Vine Street", "Orange", 200, 100, 100);

        // 20: Free Parking (پارکینگ مجانی)
        board.addTile(new Tile(20, TileType.GO, "Free Parking"));

        // 21: Strand (Red)
        addProperty(board, 21, "Strand", "Red", 220, 150, 110);

        // 22: Chance
        board.addTile(new Tile(22, TileType.CARD, "Chance"));

        // 23: Fleet Street (Red)
        addProperty(board, 23, "Fleet Street", "Red", 220, 150, 110);

        // 24: Trafalgar Square (Red)
        addProperty(board, 24, "Trafalgar Square", "Red", 240, 150, 120);

        // 25: Fenchurch St Station (Railroad)
        addProperty(board, 25, "Fenchurch St Station", "Railroad", 200, 0, 100);

        // 26: Leicester Square (Yellow)
        addProperty(board, 26, "Leicester Square", "Yellow", 260, 150, 130);

        // 27: Coventry Street (Yellow)
        addProperty(board, 27, "Coventry Street", "Yellow", 260, 150, 130);

        // 28: Water Works (Utility)
        addProperty(board, 28, "Water Works", "Utility", 150, 0, 75);

        // 29: Piccadilly (Yellow)
        addProperty(board, 29, "Piccadilly", "Yellow", 280, 150, 140);

        // 30: Go To Jail (برو به زندان)
        board.addTile(new Tile(30, TileType.JAIL, "Go To Jail"));

        // 31: Regent Street (Green)
        addProperty(board, 31, "Regent Street", "Green", 300, 200, 150);

        // 32: Oxford Street (Green)
        addProperty(board, 32, "Oxford Street", "Green", 300, 200, 150);

        // 33: Community Chest
        board.addTile(new Tile(33, TileType.CARD, "Community Chest"));

        // 34: Bond Street (Green)
        addProperty(board, 34, "Bond Street", "Green", 320, 200, 160);

        // 35: Liverpool St Station (Railroad)
        addProperty(board, 35, "Liverpool St Station", "Railroad", 200, 0, 100);

        // 36: Chance
        board.addTile(new Tile(36, TileType.CARD, "Chance"));

        // 37: Park Lane (Dark Blue)
        addProperty(board, 37, "Park Lane", "Dark Blue", 350, 200, 175);

        // 38: Luxury Tax
        board.addTile(new Tile(38, TileType.TAX, "Luxury Tax"));

        // 39: Mayfair (Dark Blue)
        addProperty(board, 39, "Mayfair", "Dark Blue", 400, 200, 200);

        return board;
    }

    // متد کمکی برای اضافه کردن راحت‌تر املاک به برد
    private static void addProperty(Board board, int id, String name, String color, int price, int houseCost, int mortgage) {
        Property prop = new Property(id, name, price, color, houseCost, mortgage);
        // اینجا Property را به عنوان دیتای Tile ذخیره می‌کنیم
        board.addTile(new Tile(id, TileType.PROPERTY, prop));
    }

    // متد اصلی برای ساخت کل بازی (برد + ۴ بازیکن)
    public static GameState createGame() {
        Board board = initializeBoard();
        Player[] players = new Player[4];

        // ساخت ۴ بازیکن با پول اولیه ۱۵۰۰ دلار
        for (int i = 0; i < 4; i++) {
            players[i] = new Player(i + 1, "Player " + (i + 1), 1500);
        }
        return new GameState(players, board);
    }
}
