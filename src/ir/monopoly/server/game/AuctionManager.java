package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;
import ir.monopoly.server.property.Property;

import java.util.ArrayList;
import java.util.List;

public class AuctionManager {
    private final Property property;
    private final List<Player> activeBidders;
    private int currentHighestBid = 0;
    private Player currentWinner = null;
    private int currentBidderIndex = 0;
    private boolean roundFinished = false;

    public AuctionManager(Property property, Player[] players) {
        this.property = property;
        this.activeBidders = new ArrayList<>();
        for (Player p : players) {
            if (p.getStatus() != PlayerStatus.BANKRUPT) {
                activeBidders.add(p);
            }
        }
        this.currentHighestBid = 10;
    }

    public boolean placeBid(Player player, int amount) {
        if (!activeBidders.get(currentBidderIndex).equals(player)) return false;
        if (amount <= currentHighestBid) return false;

        currentHighestBid = amount;
        currentWinner = player;
        roundFinished = false;
        nextBidder();
        return true;
    }

    public void passBid(Player player) {
        if (activeBidders.get(currentBidderIndex).equals(player)) {
            activeBidders.remove(currentBidderIndex);
            if (currentBidderIndex >= activeBidders.size()) {
                currentBidderIndex = 0;
                roundFinished = true;
            }
            if (activeBidders.isEmpty() || (roundFinished && activeBidders.size() == 1)) {
                finalizeAuction();
            }
        }
    }

    private void nextBidder() {
        currentBidderIndex = (currentBidderIndex + 1) % activeBidders.size();
        if (currentBidderIndex == 0) roundFinished = true;
    }

    private void finalizeAuction() {
        if (currentWinner != null) {
            currentWinner.changeBalance(-currentHighestBid);
            property.setOwner(currentWinner.getPlayerId());
            currentWinner.addProperty(property);
        }
    }

    public int getCurrentHighestBid() { return currentHighestBid; }
    public Player getCurrentWinner() { return currentWinner; }
    public Player getCurrentBidder() {
        return activeBidders.isEmpty() ? null : activeBidders.get(currentBidderIndex);
    }
}