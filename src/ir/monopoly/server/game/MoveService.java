package ir.monopoly.server.game;

import ir.monopoly.server.board.Board;
import ir.monopoly.server.board.Tile;
import ir.monopoly.server.player.Player;

public class MoveService {

    public static Tile movePlayer(Player player, Board board, int steps) {
        Tile currentTile = board.getHead();
        while (currentTile.getTileId() != player.getCurrentPosition()) {
            currentTile = currentTile.getNext();
        }

        Tile destination = currentTile;
        for (int i = 0; i < steps; i++) {
            destination = destination.getNext();

            if (destination == board.getHead()) {
                GoHandler.handlePassingGo(player);
                System.out.println(player.getName() + " passed GO and collected $200");
            }
        }

        player.setCurrentPosition(destination.getTileId());

        return destination;
    }
}