package com.example.AdvancedAnalysisCW;

import jakarta.persistence.*;
import java.sql.Date;

@Entity
@Table(name = "inventorydb")
public class Inventory {
    @Id
    @Column(name = "itemid")
    private Long itemId;

    @Column(name = "itemname")
    private String itemName;

    @Column(name = "itemquantity")
    private Integer itemQuantity;

    @Column(name = "itemexpiry") // Add a new column for item expiry.
    private Date itemExpiry;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getItemQuantity() {
        return itemQuantity;
    }

    public void setItemQuantity(Integer itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    public Date getItemExpiry() {
        return itemExpiry;
    }

    public void setItemExpiry(Date itemExpiry) {
        this.itemExpiry = itemExpiry;
    }
}
