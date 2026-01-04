package ir.monopoly.server.game;

import ir.monopoly.server.board.Tile;
import ir.monopoly.server.datastructure.MyQueue;
import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;
import ir.monopoly.server.property.Property;

public class CardDeck {
    private final MyQueue<Card> chanceCards;
    private final MyQueue<Card> communityChestCards;

    public CardDeck() {
        this.chanceCards = new MyQueue<>();
        this.communityChestCards = new MyQueue<>();
        initializeChanceCards();
        initializeCommunityCards();
    }

    private void initializeChanceCards() {

        chanceCards.enqueue(new Card("Advance to GO (Collect $200)", (p, gs) -> {
            moveToTile(p, gs, 0, true);
            gs.addEvent(p.getName() + " advanced to GO and collected $200.");
        }));

        chanceCards.enqueue(new Card("Advance to Mayfair", (p, gs) -> {
            moveToTile(p, gs, 39, true);
            gs.addEvent(p.getName() + " advanced to Mayfair.");
        }));

        chanceCards.enqueue(new Card("Advance to Old Kent Road", (p, gs) -> {
            moveToTile(p, gs, 1, true);
            gs.addEvent(p.getName() + " advanced to Old Kent Road.");
        }));

        chanceCards.enqueue(new Card("Advance to nearest Railroad", (p, gs) -> {
            int current = p.getCurrentPosition();
            int[] railroads = {5, 15, 25, 35};
            int nearest = findNearest(current, railroads);
            moveToTile(p, gs, nearest, true);
            Tile tile = gs.getBoard().getTileAt(nearest);
            TileResolver.resolveTile(tile, gs); // اجاره دو برابر اگر صاحب داشت
            gs.addEvent(p.getName() + " advanced to nearest Railroad.");
        }));

        chanceCards.enqueue(new Card("Advance to nearest Utility", (p, gs) -> {
            int current = p.getCurrentPosition();
            int[] utilities = {12, 28};
            int nearest = findNearest(current, utilities);
            moveToTile(p, gs, nearest, true);
            gs.addEvent(p.getName() + " advanced to nearest Utility.");
        }));

        chanceCards.enqueue(new Card("Bank pays you dividend of $50", (p, gs) -> {
            p.changeBalance(50);
            gs.addEvent(p.getName() + " received $50 dividend.");
        }));

        chanceCards.enqueue(new Card("Get Out of Jail Free", (p, gs) -> {
            p.addGetOutOfJailFreeCard(true);
            gs.addEvent(p.getName() + " received Get Out of Jail Free (Chance).");
        }));

        chanceCards.enqueue(new Card("Go Back 3 Spaces", (p, gs) -> {
            int newPos = (p.getCurrentPosition() - 3 + 40) % 40;
            p.setCurrentPosition(newPos);
            Tile tile = gs.getBoard().getTileAt(newPos);
            TileResolver.resolveTile(tile, gs);
            gs.addEvent(p.getName() + " went back 3 spaces.");
        }));

        chanceCards.enqueue(new Card("Go to Jail – Do not pass GO", (p, gs) -> {
            p.setStatus(PlayerStatus.IN_JAIL);
            p.setCurrentPosition(10);
            gs.addEvent(p.getName() + " went directly to Jail!");
        }));

        chanceCards.enqueue(new Card("General repairs: $25/house, $100/hotel", (p, gs) -> {
            int[] cost = {0};
            p.getOwnedProperties().forEach(prop -> {
                cost[0] += prop.getHouseCount() * 25;
                if (prop.hasHotel()) cost[0] += 100;
            });
            p.changeBalance(-cost[0]);
            gs.addEvent(p.getName() + " paid $" + cost[0] + " for general repairs.");
        }));

        chanceCards.enqueue(new Card("Pay poor tax of $15", (p, gs) -> {
            p.changeBalance(-15);
            gs.addEvent(p.getName() + " paid $15 poor tax.");
        }));

        chanceCards.enqueue(new Card("Advance to Whitechapel Road", (p, gs) -> {
            moveToTile(p, gs, 3, true);
        }));

        chanceCards.enqueue(new Card("Building loan matures – Collect $150", (p, gs) -> {
            p.changeBalance(150);
            gs.addEvent(p.getName() + " collected $150 from building loan.");
        }));

        chanceCards.enqueue(new Card("Chairman of the Board – Pay $50 to each player", (p, gs) -> {
            for (Player op : gs.getPlayers()) {
                if (op != p && op.getStatus() != PlayerStatus.BANKRUPT) {
                    p.changeBalance(-50);
                    op.changeBalance(50);
                    gs.getTransactionGraph().recordTransaction(p.getPlayerId(), op.getPlayerId(), 50);
                }
            }
            gs.addEvent(p.getName() + " paid $50 to each player as Chairman.");
        }));

        chanceCards.enqueue(new Card("Advance to nearest Railroad (again)", (p, gs) -> {
            int current = p.getCurrentPosition();
            int[] railroads = {5, 15, 25, 35};
            int nearest = findNearest(current, railroads);
            moveToTile(p, gs, nearest, true);
        }));

        chanceCards.enqueue(new Card("You are assessed for street repairs – $40/house, $115/hotel", (p, gs) -> {
            int[] cost = {0};
            p.getOwnedProperties().forEach(prop -> {
                cost[0] += prop.getHouseCount() * 40;
                if (prop.hasHotel()) cost[0] += 115;
            });
            p.changeBalance(-cost[0]);
            gs.addEvent(p.getName() + " paid $" + cost[0] + " for street repairs.");
        }));
    }

    private void initializeCommunityCards() {

        communityChestCards.enqueue(new Card("Advance to GO (Collect $200)", (p, gs) -> {
            moveToTile(p, gs, 0, true);
        }));

        communityChestCards.enqueue(new Card("Bank error in your favor – Collect $200", (p, gs) -> {
            p.changeBalance(200);
            gs.addEvent(p.getName() + " collected $200 from bank error.");
        }));

        communityChestCards.enqueue(new Card("Doctor’s fee – Pay $50", (p, gs) -> {
            p.changeBalance(-50);
            gs.addEvent(p.getName() + " paid $50 doctor's fee.");
        }));

        communityChestCards.enqueue(new Card("Sale of stock – Collect $50", (p, gs) -> {
            p.changeBalance(50);
            gs.addEvent(p.getName() + " collected $50 from stock sale.");
        }));

        communityChestCards.enqueue(new Card("Get Out of Jail Free", (p, gs) -> {
            p.addGetOutOfJailFreeCard(false);
            gs.addEvent(p.getName() + " received Get Out of Jail Free (Community).");
        }));

        communityChestCards.enqueue(new Card("Go to Jail – Directly", (p, gs) -> {
            p.setStatus(PlayerStatus.IN_JAIL);
            p.setCurrentPosition(10);
            gs.addEvent(p.getName() + " went directly to Jail!");
        }));

        communityChestCards.enqueue(new Card("Holiday fund matures – Collect $100", (p, gs) -> {
            p.changeBalance(100);
        }));

        communityChestCards.enqueue(new Card("Income tax refund – Collect $20", (p, gs) -> {
            p.changeBalance(20);
        }));

        communityChestCards.enqueue(new Card("Birthday – Collect $10 from each player", (p, gs) -> {
            for (Player op : gs.getPlayers()) {
                if (op != p && op.getStatus() != PlayerStatus.BANKRUPT) {
                    op.changeBalance(-10);
                    p.changeBalance(10);
                    gs.getTransactionGraph().recordTransaction(op.getPlayerId(), p.getPlayerId(), 10);
                }
            }
        }));

        communityChestCards.enqueue(new Card("Life insurance matures – Collect $100", (p, gs) -> {
            p.changeBalance(100);
        }));

        communityChestCards.enqueue(new Card("Hospital fees – Pay $100", (p, gs) -> {
            p.changeBalance(-100);
        }));

        communityChestCards.enqueue(new Card("School fees – Pay $50", (p, gs) -> {
            p.changeBalance(-50);
        }));

        communityChestCards.enqueue(new Card("Consultancy fee – Collect $25", (p, gs) -> {
            p.changeBalance(25);
        }));

        communityChestCards.enqueue(new Card("Street repairs: $40/house, $115/hotel", (p, gs) -> {
            int[] cost = {0};
            p.getOwnedProperties().forEach(prop -> {
                cost[0] += prop.getHouseCount() * 40;
                if (prop.hasHotel()) cost[0] += 115;
            });
            p.changeBalance(-cost[0]);
            gs.addEvent(p.getName() + " paid $" + cost[0] + " for street repairs.");
        }));

        communityChestCards.enqueue(new Card("You inherit $100", (p, gs) -> {
            p.changeBalance(100);
        }));

        communityChestCards.enqueue(new Card("Beauty contest prize – Collect $10", (p, gs) -> {
            p.changeBalance(10);
        }));
    }

    private void moveToTile(Player p, GameState gs, int tileId, boolean collectIfPassGo) {
        int oldPos = p.getCurrentPosition();
        p.setCurrentPosition(tileId);

        if (collectIfPassGo && oldPos > tileId) {
            p.changeBalance(200);
            gs.addEvent(p.getName() + " passed GO and collected $200.");
        }

        Tile tile = gs.getBoard().getTileAt(tileId);
        TileResolver.resolveTile(tile, gs);
    }

    private int findNearest(int current, int[] positions) {
        int minDist = 40;
        int nearest = positions[0];
        for (int pos : positions) {
            int dist = (pos - current + 40) % 40;
            if (dist < minDist) {
                minDist = dist;
                nearest = pos;
            }
        }
        return nearest;
    }

    public Card drawChance() {
        Card card = chanceCards.dequeue();
        if (card != null) chanceCards.enqueue(card);
        return card;
    }

    public Card drawCommunityChest() {
        Card card = communityChestCards.dequeue();
        if (card != null) communityChestCards.enqueue(card);
        return card;
    }
}