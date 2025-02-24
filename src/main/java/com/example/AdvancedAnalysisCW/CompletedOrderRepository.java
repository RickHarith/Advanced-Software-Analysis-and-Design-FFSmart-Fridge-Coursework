package com.example.AdvancedAnalysisCW;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompletedOrderRepository extends JpaRepository<CompletedOrder, Long> {
    CompletedOrder findByDeliveryIdAndItemNameAndChefIdAndDeliveryPersonId(Long deliveryId, String itemName, Long chefId, Long deliveryPersonId);

    List<CompletedOrder> findByDeliveryPersonId(Long deliveryPersonId);
}
