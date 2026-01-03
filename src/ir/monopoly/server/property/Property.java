package ir.monopoly.server.property;

public class Property {
    private final int propertyId;
    private final String name;
    private final int purchasePrice;
    private final String colorGroup;
    private final int houseCost;
    private int houseCount = 0;
    private boolean hasHotel = false;
    private boolean mortgaged = false;
    private Integer ownerId = null;
    private final int mortgageValue;

    public Property(int propertyId, String name, int purchasePrice, String colorGroup, int houseCost, int mortgageValue) {
        this.propertyId = propertyId;
        this.name = name;
        this.purchasePrice = purchasePrice;
        this.colorGroup = colorGroup;
        this.houseCost = houseCost;
        this.mortgageValue = mortgageValue;
    }

    public int getPropertyId() { return propertyId; }
    public String getName() { return name; }
    public int getPurchasePrice() { return purchasePrice; }
    public String getColorGroup() { return colorGroup; }
    public int getHouseCost() { return houseCost; }
    public int getHouseCount() { return houseCount; }
    public boolean hasHotel() { return hasHotel; }
    public boolean isMortgaged() { return mortgaged; }
    public Integer getOwnerId() { return ownerId; }
    public int getMortgageValue() { return mortgageValue; }
    public int getUnmortgagePrice() { return (int) (mortgageValue * 1.1); }

    public void setOwner(int ownerId) { this.ownerId = ownerId; }
    public void clearOwner() { this.ownerId = null; }
    public void setMortgaged(boolean mortgaged) { this.mortgaged = mortgaged; }

    public void addHouse() { if (houseCount < 4) houseCount++; }
    public void addHotel() { hasHotel = true; }
    public void removeHouses(int count) { houseCount = Math.max(0, houseCount - count); }
    public void removeHotel() { hasHotel = false; }

    public int calculateRent(boolean hasFullGroup) {
        if (isMortgaged()) return 0;
        int baseRent = purchasePrice / 10;
        if (hasFullGroup && houseCount == 0) return baseRent * 2;
        switch (houseCount) {
            case 1: return baseRent * 5;
            case 2: return baseRent * 15;
            case 3: return baseRent * 40;
            case 4: return baseRent * 70;
            default:
                if (hasHotel) return baseRent * 100;
        }
        return baseRent;
    }
}
