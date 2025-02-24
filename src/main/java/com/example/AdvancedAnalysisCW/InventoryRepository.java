package com.example.AdvancedAnalysisCW;

import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Date;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findById(Long id);
    Optional<Inventory> findByItemName(String itemName);
    Optional<Inventory> findByItemNameAndItemExpiry(String itemName, Date itemExpiry);
}
