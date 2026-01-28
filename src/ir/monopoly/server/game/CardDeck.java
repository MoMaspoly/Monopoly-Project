package ir.monopoly.server.game;

import ir.monopoly.server.datastructure.MyQueue;

public class CardDeck {
    private final MyQueue<Card> chanceCards = new MyQueue<>();
    private final MyQueue<Card> communityChestCards = new MyQueue<>();

    public CardDeck() {
        // کارت‌های شانس (Chance)
        chanceCards.enqueue(new Card("Advance to GO (Collect $200)", (p, gs) -> {
            p.setCurrentPosition(0);
            p.changeBalance(200);
            // در Controller دستور ROLL_UPDATE صادر می‌شود
        }));

        chanceCards.enqueue(new Card("Go to Jail! Move directly to Jail.", (p, gs) -> {
            p.setStatus(ir.monopoly.server.player.PlayerStatus.IN_JAIL);
            p.setCurrentPosition(10);
        }));

        chanceCards.enqueue(new Card("Speeding fine $15", (p, gs) -> p.changeBalance(-15)));

        // کارت‌های صندوق (Community Chest)
        communityChestCards.enqueue(new Card("Bank error! Collect $200", (p, gs) -> p.changeBalance(200)));
        communityChestCards.enqueue(new Card("Doctor's fee. Pay $50", (p, gs) -> p.changeBalance(-50)));
    }

    public Card drawChance() {
        Card c = chanceCards.dequeue();
        chanceCards.enqueue(c);
        return c;
    }

    public Card drawCommunityChest() {
        Card c = communityChestCards.dequeue();
        communityChestCards.enqueue(c);
        return c;
    }
}
