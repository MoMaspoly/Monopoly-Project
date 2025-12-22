package ir.monopoly.server.game;

import ir.monopoly.server.datastructure.MyQueue;

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
        chanceCards.enqueue(new Card("Get Out of Jail Free (Chance)", (p, gs) -> {
            p.addGetOutOfJailFreeCard(true);
            gs.addEvent(p.getName() + " received a Get Out of Jail Free (Chance) card.");
        }));
    }

    private void initializeCommunityCards() {
        communityChestCards.enqueue(new Card("Get Out of Jail Free (Community Chest)", (p, gs) -> {
            p.addGetOutOfJailFreeCard(false);
            gs.addEvent(p.getName() + " received a Get Out of Jail Free (Community Chest) card.");
        }));
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