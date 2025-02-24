// Delivery.java
package com.example.AdvancedAnalysisCW;

import jakarta.persistence.*;

@Entity
@Table(name = "deliverydb")
public class Delivery {
    @Id
    @Column(name = "deliveryid")
    private Long deliveryId;
    @Column(name = "itemname")
    private String itemName;
    @Column(name = "itemquantity")
    private int itemQuantity;
    @Column(name = "chefid") // New field to store chef's ID
    private Long chefId;
    @Column(name = "deliverypersonid") // Corrected field name
    private Long deliverypersonId;

    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
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

    public Long getChefId() {
        return chefId;
    }

    public void setChefId(Long chefId) {
        this.chefId = chefId;
    }

    public Long getDeliverypersonId() {
        return deliverypersonId;
    }

    public void setDeliverypersonId(Long deliverypersonId) {
        this.deliverypersonId = deliverypersonId;
    }
}
