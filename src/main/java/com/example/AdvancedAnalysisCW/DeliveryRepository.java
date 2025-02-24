package com.example.AdvancedAnalysisCW;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByDeliverypersonId(Long deliveryPersonId);
    void deleteByDeliverypersonId(Long deliveryPersonId);
    Delivery findByDeliverypersonIdAndItemName(Long deliveryPersonId, String itemName);
}
