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
    private boolean auctionActive = false;
    private final GameState gameState;

    public AuctionManager(Property property, Player[] players, GameState gameState) {
        this.property = property;
        this.activeBidders = new ArrayList<>();
        for (Player p : players) {
            if (p.getStatus() != PlayerStatus.BANKRUPT) {
                activeBidders.add(p);
            }
        }
        this.currentHighestBid = 10;
        this.auctionActive = true;
        this.gameState = gameState;
    }

    public boolean isAuctionActive() {
        return auctionActive && activeBidders.size() > 1;
    }

    public boolean placeBid(Player player, int amount) {
        if (!auctionActive || !activeBidders.contains(player)) return false;
        if (amount <= currentHighestBid) return false;
        if (player.getBalance() < amount) {
            BankruptcyManager.processBankruptcy(player, null, gameState);
            return false;
        }

        currentHighestBid = amount;
        currentWinner = player;
        roundFinished = false;

        currentBidderIndex = (activeBidders.indexOf(player) + 1) % activeBidders.size();
        if (currentBidderIndex == 0) roundFinished = true;

        return true;
    }

    public boolean passBid(Player player) {
        if (!auctionActive || !activeBidders.contains(player)) return false;

        activeBidders.remove(player);
        if (activeBidders.isEmpty()) {
            endAuction();
            return true;
        }

        if (currentBidderIndex >= activeBidders.size()) {
            currentBidderIndex = 0;
            roundFinished = true;
        }

        if (activeBidders.size() == 1) {
            endAuction();
            return true;
        }

        return true;
    }

    private void endAuction() {
        auctionActive = false;
        if (currentWinner != null) {
            currentWinner.changeBalance(-currentHighestBid);
            property.setOwner(currentWinner.getPlayerId());
            currentWinner.addProperty(property);
        }
    }

    public void forceEndAuction() {
        auctionActive = false;
        if (currentWinner != null) {
            endAuction();
        } else if (!activeBidders.isEmpty()) {
            Player winner = activeBidders.get(0);
            if (winner.getBalance() >= 10) {
                winner.changeBalance(-10);
                property.setOwner(winner.getPlayerId());
                winner.addProperty(property);
            }
        }
    }

    public boolean isFinished() {
        return !auctionActive || activeBidders.size() <= 1;
    }

    public Property getProperty() {
        return this.property;
    }

    public int getCurrentHighestBid() {
        return currentHighestBid;
    }

    public Player getCurrentWinner() {
        return currentWinner;
    }

    public Player getCurrentBidder() {
        if (activeBidders.isEmpty() || !auctionActive) return null;
        return activeBidders.get(currentBidderIndex);
    }

    public List<Player> getActiveBidders() {
        return new ArrayList<>(activeBidders);
    }

    public String getAuctionStatus() {
        if (!auctionActive) return "Auction finished";
        return "Property: " + property.getName() +
                " | Current bid: $" + currentHighestBid +
                " | Leader: " + (currentWinner != null ? currentWinner.getName() : "None") +
                " | Current turn: " + (getCurrentBidder() != null ? getCurrentBidder().getName() : "None");
    }
}