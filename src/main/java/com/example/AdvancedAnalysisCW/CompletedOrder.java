package com.example.AdvancedAnalysisCW;

import jakarta.persistence.*;

import java.sql.Date;

@Entity
@Table(name = "completeddb")
public class CompletedOrder {
    @Id
    @Column(name = "deliveryid")
    private Long deliveryId;

    @Column(name = "itemname")
    private String itemName;

    @Column(name = "itemquantity")
    private int itemQuantity;

    @Column(name = "chefid")
    private Long chefId;

    @Column(name = "deliverypersonid")
    private Long deliveryPersonId;

    @Column(name = "deliverydate")
    private Date deliveryDate;

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

    public Long getDeliveryPersonId() {
        return deliveryPersonId;
    }

    public void setDeliveryPersonId(Long deliveryPersonId) {
        this.deliveryPersonId = deliveryPersonId;
    }

    public Date getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Date deliveryDate) {
        this.deliveryDate = deliveryDate;
    }
}

