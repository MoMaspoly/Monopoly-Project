package ir.monopoly.server.player;

import ir.monopoly.server.datastructure.MyStack;
import ir.monopoly.server.datastructure.PropertyTree;
import ir.monopoly.server.property.Property;

public class Player implements Comparable<Player> {
    private final int playerId;
    private final String name;
    private int balance;
    private int currentPosition;
    private PlayerStatus status;
    private int jailTurns = 0;
    private boolean hasChanceJailCard = false;
    private boolean hasCommunityJailCard = false;
    private PropertyTree ownedProperties;
    private MyStack<String> actionHistory;

    public Player(int playerId, String name, int initialBalance) {
        this.playerId = playerId;
        this.name = name;
        this.balance = initialBalance;
        this.currentPosition = 0;
        this.status = PlayerStatus.ACTIVE;
        this.ownedProperties = new PropertyTree();
        this.actionHistory = new MyStack<>();
    }

    public void addGetOutOfJailFreeCard(boolean isChance) {
        if (isChance) hasChanceJailCard = true;
        else hasCommunityJailCard = true;
    }

    public boolean useGetOutOfJailFreeCard(boolean isChance) {
        if (isChance && hasChanceJailCard) {
            hasChanceJailCard = false;
            return true;
        } else if (!isChance && hasCommunityJailCard) {
            hasCommunityJailCard = false;
            return true;
        }
        return false;
    }

    public boolean hasGetOutOfJailFreeCard() {
        return hasChanceJailCard || hasCommunityJailCard;
    }

    public void releaseFromJail() {
        this.status = PlayerStatus.ACTIVE;
        this.jailTurns = 0;
    }

    public int getJailTurns() { return jailTurns; }
    public void incrementJailTurns() { this.jailTurns++; }
    public void resetJailTurns() { this.jailTurns = 0; }

    public int getPlayerId() { return playerId; }
    public String getName() { return name; }
    public int getBalance() { return balance; }
    public void changeBalance(int amount) { this.balance += amount; }
    public int getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(int position) { this.currentPosition = position; }
    public PlayerStatus getStatus() { return status; }
    public void setStatus(PlayerStatus status) { this.status = status; }
    public PropertyTree getOwnedProperties() { return ownedProperties; }

    public void addProperty(Property property) {
        ownedProperties.insert(property);
        property.setOwner(playerId);
    }

    public void removeProperty(int propertyId) {
        ownedProperties.remove(propertyId);
    }

    @Override
    public int compareTo(Player other) {
        return Integer.compare(this.getTotalWealth(), other.getTotalWealth());
    }

    @Override
    public String toString() {
        return name + " ($" + balance + ")";
    }

    public int getTotalWealth() {
        final int[] sum = {0};
        ownedProperties.forEach(p -> {
            sum[0] += p.getPurchasePrice() + (p.getHouseCount() * p.getHouseCost());
            if (p.hasHotel()) sum[0] += p.getHouseCost();
        });
        return this.balance + sum[0];
    }
}