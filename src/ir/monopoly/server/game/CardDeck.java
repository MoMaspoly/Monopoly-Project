package ir.monopoly.server.game;

import ir.monopoly.server.datastructure.MyQueue;

public class CardDeck {
    private final MyQueue<Card> chanceCards = new MyQueue<>();
    private final MyQueue<Card> communityChestCards = new MyQueue<>();

    public CardDeck() {
        // Chance Cards
        chanceCards.enqueue(new Card("Advance to GO!", (p, gs) -> { p.setCurrentPosition(0); p.changeBalance(200); }));
        chanceCards.enqueue(new Card("Speeding fine $15", (p, gs) -> p.changeBalance(-15)));
        chanceCards.enqueue(new Card("Go to Jail!", (p, gs) -> { p.setStatus(ir.monopoly.server.player.PlayerStatus.IN_JAIL); p.setCurrentPosition(10); }));
        chanceCards.enqueue(new Card("Bank dividend pays you $50", (p, gs) -> p.changeBalance(50)));
        chanceCards.enqueue(new Card("Go back 3 spaces", (p, gs) -> p.setCurrentPosition((p.getCurrentPosition() - 3 + 40) % 40)));

        // Community Chest
        communityChestCards.enqueue(new Card("Bank error! Collect $200", (p, gs) -> p.changeBalance(200)));
        communityChestCards.enqueue(new Card("Doctor's fee. Pay $50", (p, gs) -> p.changeBalance(-50)));
        communityChestCards.enqueue(new Card("You inherit $100", (p, gs) -> p.changeBalance(100)));
        communityChestCards.enqueue(new Card("Birthday! Collect $10 from each player", (p, gs) -> {
            for(var other : gs.getPlayers()) { if(other != p) { other.changeBalance(-10); p.changeBalance(10); }}
        }));
    }

    public Card drawChance() {
        Card c = chanceCards.dequeue();
        chanceCards.enqueue(c); // Put back to bottom
        return c;
    }

    public Card drawCommunityChest() {
        Card c = communityChestCards.dequeue();
        communityChestCards.enqueue(c); // Put back to bottom
        return c;
    }
}
