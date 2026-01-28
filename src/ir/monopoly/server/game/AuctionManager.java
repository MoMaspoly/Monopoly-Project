package ir.monopoly.server.game;

import ir.monopoly.server.player.Player;
import ir.monopoly.server.player.PlayerStatus;
import ir.monopoly.server.property.Property;

public class AuctionManager {
    private final Property property;
    private final Player[] activeBidders;
    private int bidderCount;
    private int currentHighestBid = 0;
    private Player currentWinner = null;
    private int currentBidderIndex = 0;
    private boolean roundFinished = false;
    private boolean auctionActive = false;
    private final GameState gameState;

    public AuctionManager(Property property, Player[] players, GameState gameState) {
        this.property = property;
        this.activeBidders = new Player[4];
        this.bidderCount = 0;

        for (Player p : players) {
            if (p.getStatus() != PlayerStatus.BANKRUPT) {
                activeBidders[bidderCount] = p;
                bidderCount++;
            }
        }
        this.currentHighestBid = 10;
        this.auctionActive = true;
        this.gameState = gameState;
    }

    public boolean isAuctionActive() {
        return auctionActive && bidderCount > 1;
    }

    public boolean placeBid(Player player, int amount) {
        if (!auctionActive || !containsPlayer(player)) return false;
        if (amount <= currentHighestBid) return false;
        if (player.getBalance() < amount) {
            BankruptcyManager.processBankruptcy(player, null, gameState);
            return false;
        }

        currentHighestBid = amount;
        currentWinner = player;
        roundFinished = false;

        int playerIndex = getPlayerIndex(player);
        if (playerIndex != -1) {
            currentBidderIndex = (playerIndex + 1) % bidderCount;
            if (currentBidderIndex == 0) roundFinished = true;
        }

        return true;
    }

    public boolean passBid(Player player) {
        if (!auctionActive || !containsPlayer(player)) return false;

        removePlayer(player);
        if (bidderCount == 0) {
            endAuction();
            return true;
        }

        if (currentBidderIndex >= bidderCount) {
            currentBidderIndex = 0;
            roundFinished = true;
        }

        if (bidderCount == 1) {
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
        } else if (bidderCount > 0) {
            Player winner = activeBidders[0];
            if (winner.getBalance() >= 10) {
                winner.changeBalance(-10);
                property.setOwner(winner.getPlayerId());
                winner.addProperty(property);
            }
        }
    }

    public boolean isFinished() {
        return !auctionActive || bidderCount <= 1;
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
        if (bidderCount == 0 || !auctionActive) return null;
        return activeBidders[currentBidderIndex];
    }

    public Player[] getActiveBidders() {
        Player[] result = new Player[bidderCount];
        for (int i = 0; i < bidderCount; i++) {
            result[i] = activeBidders[i];
        }
        return result;
    }

    public String getAuctionStatus() {
        if (!auctionActive) return "Auction finished";
        return "Property: " + property.getName() +
                " | Current bid: $" + currentHighestBid +
                " | Leader: " + (currentWinner != null ? currentWinner.getName() : "None") +
                " | Current turn: " + (getCurrentBidder() != null ? getCurrentBidder().getName() : "None");
    }

    private boolean containsPlayer(Player player) {
        for (int i = 0; i < bidderCount; i++) {
            if (activeBidders[i] == player) {
                return true;
            }
        }
        return false;
    }

    private int getPlayerIndex(Player player) {
        for (int i = 0; i < bidderCount; i++) {
            if (activeBidders[i] == player) {
                return i;
            }
        }
        return -1;
    }

    private void removePlayer(Player player) {
        int index = getPlayerIndex(player);
        if (index != -1) {
            for (int i = index; i < bidderCount - 1; i++) {
                activeBidders[i] = activeBidders[i + 1];
            }
            bidderCount--;

            if (currentBidderIndex > index) {
                currentBidderIndex--;
            }
        }
    }
}