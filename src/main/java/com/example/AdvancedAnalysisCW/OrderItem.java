package com.example.AdvancedAnalysisCW;

public class OrderItem {
    private String chefName;
    private String itemName;
    private int itemQuantity;

    // Constructor
    public OrderItem(String chefName, String itemName, int itemQuantity) {
        this.chefName = chefName;
        this.itemName = itemName;
        this.itemQuantity = itemQuantity;
    }

    // Getters and setters
    public String getChefName() {
        return chefName;
    }

    public void setChefName(String chefName) {
        this.chefName = chefName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getItemQuantity() {
        return itemQuantity;
    }

    public void setItemQuantity(int itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    // Optional: Override toString() for debugging purposes
    @Override
    public String toString() {
        return "OrderItem{" +
                "chefName='" + chefName + '\'' +
                ", itemName='" + itemName + '\'' +
                ", itemQuantity=" + itemQuantity +
                '}';
    }
}

