package ir.monopoly.server.property;

public class Property {
    private final int propertyId;
    private final String name;
    private final String colorGroup;
    private final int purchasePrice;
    private final int[] rentLevels = new int[6];
    private final int houseCost;

    private int houseCount = 0;
    private boolean hasHotel = false;
    private boolean isMortgaged = false;
    private Integer ownerId = null;

    public Property(int propertyId, String name, String colorGroup, int purchasePrice,
                    int rentNoHouse, int rent1, int rent2, int rent3, int rent4, int rentHotel, int houseCost) {
        this.propertyId = propertyId;
        this.name = name;
        this.colorGroup = colorGroup;
        this.purchasePrice = purchasePrice;
        this.rentLevels[0] = rentNoHouse;
        this.rentLevels[1] = rent1;
        this.rentLevels[2] = rent2;
        this.rentLevels[3] = rent3;
        this.rentLevels[4] = rent4;
        this.rentLevels[5] = rentHotel;
        this.houseCost = houseCost;
    }

    public int calculateRent(boolean hasFullGroup) {
        if (isMortgaged || ownerId == null) return 0;
        int level = hasHotel ? 5 : houseCount;
        int rent = rentLevels[level];
        if (level == 0 && hasFullGroup) rent *= 2;
        return rent;
    }


    public int getPurchasePrice() { return purchasePrice; }
    public int getHouseCost() { return houseCost; }
    public int getHouseCount() { return houseCount; }
    public boolean hasHotel() { return hasHotel; }
    public void addHouse() { if (houseCount < 4) houseCount++; }
    public void addHotel() { hasHotel = true; houseCount = 0; }
    public void removeHouses(int count) { houseCount = Math.max(0, houseCount - count); }
    public void removeHotel() { hasHotel = false; }
    public boolean isMortgaged() { return isMortgaged; }
    public Integer getOwnerId() { return ownerId; }
    public String getColorGroup() { return colorGroup; }
    public int getPropertyId() { return propertyId; }
    public String getName() { return name; }

    public void setOwner(int playerId) { this.ownerId = playerId; }
    public void clearOwner() { this.ownerId = null; }
    public void setMortgaged(boolean mortgaged) { this.isMortgaged = mortgaged; }
    public int getMortgageValue() { return purchasePrice / 2; }
    public int getUnmortgagePrice() { return (int) (getMortgageValue() * 1.1); }
}