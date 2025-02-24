package com.example.AdvancedAnalysisCW;

import java.sql.Date;

public class ExpiryDateInput {
    private String itemName;
    private Date itemExpiry;

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Date getItemExpiry() {
        return itemExpiry;
    }

    public void setItemExpiry(Date itemExpiry) {
        this.itemExpiry = itemExpiry;
    }
}
