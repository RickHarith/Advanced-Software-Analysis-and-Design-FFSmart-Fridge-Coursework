package com.example.AdvancedAnalysisCW;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    @Autowired
    private CompletedOrderRepository completedOrderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${randomnumberapi.url}")
    private String randomNumberApiUrl;

    @Autowired
    private DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;

    @Autowired
    public DeliveryController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Delivery>> getAllDeliveries() {
        List<Delivery> deliveries = deliveryRepository.findAll();
        return ResponseEntity.ok(deliveries);
    }

    @PostMapping("/generate-unique-id")
    public ResponseEntity<Long> generateUniqueIdOnServer() {
        Long uniqueId = generateUniqueId(); // Call your existing unique ID generation logic
        if (uniqueId != null) {
            return ResponseEntity.ok(uniqueId);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addDelivery(@RequestBody Delivery delivery, HttpSession session) {
        // Check if the user is logged in as a Chef
        String role = (String) session.getAttribute("role");
        if (role == null || !"Chef".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"Only chefs are allowed to make orders.\"}");
        }

        try {
            // Generate a unique ID for the entire order
            Long generatedId = generateUniqueId();
            if (generatedId == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"error\":\"Failed to generate a unique ID.\"}");
            }

            // Set the generated ID for the delivery
            delivery.setDeliveryId(generatedId);

            // Retrieve the chef's ID from the session and set it in delivery
            Long chefId = (Long) session.getAttribute("userId");
            delivery.setChefId(chefId);

            // Save the delivery
            deliveryRepository.save(delivery);

            return ResponseEntity.ok("{\"message\":\"Order placed successfully.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"An error occurred while placing the order.\"}");
        }
    }

    private Long generateUniqueId() {
        int maxRetries = 5;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            ResponseEntity<Integer[]> responseEntity = restTemplate.getForEntity(randomNumberApiUrl, Integer[].class);
            Integer[] response = responseEntity.getBody();
            if (response != null && response.length > 0) {
                Long generatedId = Long.valueOf(response[0]);
                if (!deliveryRepository.existsById(generatedId)) {
                    return generatedId;
                }
            }
        }
        System.err.println("Failed to generate a unique ID after " + maxRetries + " attempts.");
        return null;
    }

    @PostMapping("/take-orders")
    public ResponseEntity<?> takeOrders(@RequestBody List<Long> orderIds, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"Delivery Person".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"Only delivery personnel are allowed to take orders.\"}");
        }

        try {
            for (Long orderId : orderIds) {
                Optional<Delivery> optionalDelivery = deliveryRepository.findById(orderId);
                if (optionalDelivery.isPresent()) {
                    Delivery delivery = optionalDelivery.get();

                    // Check if the order is available (deliverypersonid is empty)
                    if (delivery.getDeliverypersonId() == null) {
                        // Set the deliverypersonid to the current user's ID
                        Long deliveryPersonId = (Long) session.getAttribute("userId");
                        System.out.println("UserId: " + deliveryPersonId); // Logging added here

                        // Set the deliverypersonid for the order
                        delivery.setDeliverypersonId(deliveryPersonId);
                        deliveryRepository.save(delivery); // Save the order with the updated deliverypersonid
                    }
                }
            }

            return ResponseEntity.ok("{\"message\":\"Orders taken successfully.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"An error occurred while taking orders.\"}");
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/ongoing")
    public ResponseEntity<List<Delivery>> getOngoingOrdersByDeliveryPersonId(HttpSession session) {
        // Get the user ID from the session
        Long deliveryPersonId = (Long) session.getAttribute("userId");

        // Check if the user is logged in as a Delivery Person
        String role = (String) session.getAttribute("role");
        if (deliveryPersonId != null && "Delivery Person".equals(role)) {
            List<Delivery> ongoingOrders = deliveryRepository.findByDeliverypersonId(deliveryPersonId);
            return ResponseEntity.ok(ongoingOrders);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @GetMapping("/current-delivery-orders")
    public ResponseEntity<List<OrderItem>> getCurrentDeliveryOrders(HttpSession session) {
        Long deliveryPersonId = (Long) session.getAttribute("userId");

        if (deliveryPersonId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        List<Delivery> deliveries = deliveryRepository.findByDeliverypersonId(deliveryPersonId);
        List<OrderItem> orderItems = new ArrayList<>();

        for (Delivery delivery : deliveries) {
            User chef = userRepository.findById(delivery.getChefId()).orElse(null);
            if (chef != null) {
                orderItems.add(new OrderItem(chef.getFullname(), delivery.getItemName(), delivery.getItemQuantity()));
            }
        }

        return ResponseEntity.ok(orderItems);
    }

    @PostMapping("/finish-order")
    @Transactional
    public ResponseEntity<?> finishOrder(@RequestBody List<ExpiryDateInput> expiryDateInputs, HttpSession session) {
        try {
            // Get the delivery person's ID from the session
            Long deliveryPersonId = (Long) session.getAttribute("userId");

            if (deliveryPersonId != null) {
                for (ExpiryDateInput input : expiryDateInputs) {
                    String itemName = input.getItemName();
                    Date itemExpiry = input.getItemExpiry();

                    // Check if an item with the same name and expiry date already exists in inventorydb
                    Optional<Inventory> existingItemOptional = inventoryRepository.findByItemNameAndItemExpiry(itemName, itemExpiry);

                    if (existingItemOptional.isPresent()) {
                        // Item already exists, increment its quantity
                        Inventory existingItem = existingItemOptional.get();
                        existingItem.setItemQuantity(existingItem.getItemQuantity() + 1);
                        inventoryRepository.save(existingItem);
                    } else {
                        // Item doesn't exist, generate a unique ID and save it as a new item
                        Long generatedId = generateUniqueId();
                        if (generatedId != null) {
                            Inventory newItem = new Inventory();
                            newItem.setItemId(generatedId);
                            newItem.setItemName(itemName);
                            newItem.setItemQuantity(1); // Set initial quantity to 1
                            newItem.setItemExpiry(itemExpiry);
                            inventoryRepository.save(newItem);
                        } else {
                            // Handle the case where a unique ID couldn't be generated
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("{\"error\":\"Failed to generate a unique ID for the item.\"}");
                        }
                    }

                    // Find the corresponding delivery record in deliverydb
                    Delivery deliveryRecord = deliveryRepository.findByDeliverypersonIdAndItemName(deliveryPersonId, itemName);

                    if (deliveryRecord != null) {
                        // Check if the item already exists in CompletedOrder for the same person, chef, and delivery
                        CompletedOrder existingCompletedOrder = completedOrderRepository
                                .findByDeliveryIdAndItemNameAndChefIdAndDeliveryPersonId(
                                        deliveryRecord.getDeliveryId(), itemName, deliveryRecord.getChefId(), deliveryPersonId);

                        if (existingCompletedOrder != null) {
                            // Increment the quantity if a matching CompletedOrder entry exists
                            existingCompletedOrder.setItemQuantity(existingCompletedOrder.getItemQuantity() + 1);
                            completedOrderRepository.save(existingCompletedOrder);
                        } else {
                            // Create a new CompletedOrder entry if it doesn't exist
                            CompletedOrder completedOrder = new CompletedOrder();
                            completedOrder.setDeliveryId(deliveryRecord.getDeliveryId());
                            completedOrder.setItemName(input.getItemName());
                            completedOrder.setItemQuantity(1); // Always 1 when completing an order
                            completedOrder.setChefId(deliveryRecord.getChefId());
                            completedOrder.setDeliveryPersonId(deliveryPersonId);
                            completedOrder.setDeliveryDate(new Date(System.currentTimeMillis())); // Current date and time
                            completedOrderRepository.save(completedOrder);
                        }
                    } else {
                        // Handle the case where the corresponding delivery record is not found
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("{\"error\":\"Corresponding delivery record not found.\"}");
                    }
                }

                // After processing all items, delete the corresponding rows from deliverydb
                for (ExpiryDateInput input : expiryDateInputs) {
                    System.out.println("deliveryPersonId: " + deliveryPersonId);
                    deliveryRepository.deleteByDeliverypersonId(deliveryPersonId);
                }

                // Sample response (update with actual logic)
                return ResponseEntity.ok("{\"message\":\"Items have been inserted, order finished successfully!.\"}");
            } else {
                // Handle the case where the delivery person is not logged in
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"Authentication failed.\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"An error occurred while finishing the order.\"}");
        }
    }

    @GetMapping("/delivery-history")
    public ResponseEntity<List<CompletedOrder>> getDeliveryHistory(HttpSession session) {
        Long deliveryPersonId = (Long) session.getAttribute("userId");

        if (deliveryPersonId != null) {
            // Fetch the delivery history for the delivery person based on their ID
            List<CompletedOrder> deliveryHistory = completedOrderRepository.findByDeliveryPersonId(deliveryPersonId);
            return ResponseEntity.ok(deliveryHistory);
        } else {
            // Handle the case where the delivery person is not logged in
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
}
