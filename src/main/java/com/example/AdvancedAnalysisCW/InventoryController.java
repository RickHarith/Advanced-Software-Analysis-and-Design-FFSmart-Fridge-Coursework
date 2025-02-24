package com.example.AdvancedAnalysisCW;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${randomnumberapi.url}")
    private String randomNumberApiUrl;

    @PostMapping("/add")
    public ResponseEntity<?> addInventoryItem(@RequestBody Inventory inventory, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"Chef".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"Only chefs are allowed to make orders.\"}");
        }

        String itemName = inventory.getItemName().toLowerCase();
        Optional<Inventory> existingItem = inventoryRepository.findByItemNameAndItemExpiry(itemName, inventory.getItemExpiry());

        if (existingItem.isPresent()) {
            Inventory existingInventory = existingItem.get();
            existingInventory.setItemQuantity(existingInventory.getItemQuantity() + inventory.getItemQuantity());
            inventoryRepository.save(existingInventory);
            InventoryLogger.log(session.getAttribute("userId") + " | " + session.getAttribute("fullname") +
                    " has updated item quantity: " + inventory.getItemName() +
                    " to " + existingInventory.getItemQuantity());
            return ResponseEntity.ok("{\"message\":\"Item quantity updated successfully.\"}");
        } else {
            Long uniqueId = generateUniqueId();
            if (uniqueId == null) {
                return ResponseEntity.badRequest().body("{\"error\":\"Failed to generate a unique ID.\"}");
            }

            inventory.setItemId(uniqueId);
            inventory.setItemName(itemName);

            // Check if the quantity is valid (0 and above)
            if (inventory.getItemQuantity() >= 0) {
                // Save the inventory item
                inventoryRepository.save(inventory);
                InventoryLogger.log(session.getAttribute("userId") + " | " + session.getAttribute("fullname") +
                        " has added a new item: " + itemName +
                        " with quantity " + inventory.getItemQuantity());
                return ResponseEntity.ok("{\"message\":\"Item added to the inventory successfully\"}");
            } else {
                return ResponseEntity.badRequest().body("{\"error\":\"Item quantity must be 0 or greater.\"}");
            }
        }
    }

    private Long generateUniqueId() {
        int maxRetries = 5;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            ResponseEntity<Integer[]> responseEntity = restTemplate.getForEntity(randomNumberApiUrl, Integer[].class);
            Integer[] response = responseEntity.getBody();
            if (response != null && response.length > 0) {
                Long generatedId = Long.valueOf(response[0]);
                if (!inventoryRepository.existsById(generatedId)) {
                    return generatedId;
                }
            }
        }
        System.err.println("Failed to generate a unique ID after " + maxRetries + " attempts.");
        return null;
    }


    @GetMapping("/all")
    public ResponseEntity<List<Inventory>> getAllInventory() {
        List<Inventory> inventory = inventoryRepository.findAll();
        return ResponseEntity.ok(inventory);
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateInventory(@RequestBody List<Inventory> updatedInventory) {
        try {
            for (Inventory updatedItem : updatedInventory) {
                Optional<Inventory> existingItem = inventoryRepository.findByItemName(updatedItem.getItemName());
                if (existingItem.isPresent()) {
                    Inventory existingInventory = existingItem.get();
                    existingInventory.setItemQuantity(updatedItem.getItemQuantity());
                    inventoryRepository.save(existingInventory);
                }
            }
            return ResponseEntity.ok("{\"message\":\"Inventory updated successfully.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"An error occurred while updating the inventory.\"}");
        }
    }

    @DeleteMapping("/delete/{itemId}")
    public ResponseEntity<?> deleteInventoryItem(@PathVariable Long itemId, HttpSession session) {
        // Check if the user is logged in as a Chef
        String role = (String) session.getAttribute("role");
        if (role == null || !"Chef".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"Only chefs are allowed to delete items.\"}");
        }

        return inventoryRepository.findById(itemId)
                .map(item -> {
                    inventoryRepository.delete(item);
                    InventoryLogger.log(session.getAttribute("userId") + " | " + session.getAttribute("fullname") +
                            " has deleted item: " + item.getItemName());
                    return ResponseEntity.ok("{\"message\":\"Item deleted successfully.\"}");
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"error\":\"Item not found.\"}"));
    }


    @PostMapping("/loadinventory")
    public ResponseEntity<List<Inventory>> loadInventory(HttpSession session) {
        // Check if the user is logged in as a Chef
        String role = (String) session.getAttribute("role");
        if (role == null || !"Chef".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.emptyList());
        }

        List<Inventory> inventory = inventoryRepository.findAll();
        return ResponseEntity.ok(inventory);
    }

    @PostMapping("/adjustquantity")
    public ResponseEntity<?> adjustQuantity(@RequestParam String itemName, @RequestParam int change, HttpSession session) {
        // Check if the user is logged in as a Chef
        String role = (String) session.getAttribute("role");
        if (role == null || !"Chef".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\":\"Only Chef users are allowed to adjust quantities.\"}");
        }

        itemName = itemName.toLowerCase(); // Normalize item name to lowercase

        Optional<Inventory> existingItem = inventoryRepository.findByItemName(itemName);
        if (existingItem.isPresent()) {
            Inventory existingInventory = existingItem.get();
            int currentQuantity = existingInventory.getItemQuantity();
            int updatedQuantity = currentQuantity + change;

            if (updatedQuantity >= 0) {
                existingInventory.setItemQuantity(updatedQuantity);
                inventoryRepository.save(existingInventory);
                InventoryLogger.log(session.getAttribute("userId") + " | " + session.getAttribute("fullname") +
                        " has " + (change > 0 ? "increased" : "decreased") + " item quantity: " + itemName +
                        " by " + Math.abs(change) + " to " + updatedQuantity);
                return ResponseEntity.ok("{\"message\":\"Quantity adjusted successfully.\"}");
            } else {
                return ResponseEntity.badRequest().body("{\"message\":\"Quantity cannot go below 0.\"}");
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/submitchanges")
    public ResponseEntity<?> submitChanges(@RequestBody List<Inventory> updatedInventory, HttpSession session) {
        // Check if the user is logged in as a Chef
        String role = (String) session.getAttribute("role");
        if (role == null || !"Chef".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"Only chefs are allowed to submit changes.\"}");
        }

        try {
            for (Inventory updatedItem : updatedInventory) {
                inventoryRepository.findById(updatedItem.getItemId()).ifPresent(existingInventory -> {
                    existingInventory.setItemQuantity(updatedItem.getItemQuantity());
                    inventoryRepository.save(existingInventory);
                    InventoryLogger.log(session.getAttribute("userId") + " | " + session.getAttribute("fullname") +
                            " has updated item quantity: " + existingInventory.getItemName() + " to " + updatedItem.getItemQuantity());
                });
            }
            return ResponseEntity.ok("{\"message\":\"Inventory updated successfully.\"}");
        } catch (Exception e) {
            e.printStackTrace(); // Log the error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"An error occurred while updating the inventory: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications(HttpSession session) {
        // Check if the user is logged in as a Chef
        String role = (String) session.getAttribute("role");
        if (role == null || !"Chef".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        List<Inventory> inventory = inventoryRepository.findAll();
        List<Notification> notifications = new ArrayList<>();

        for (Inventory item : inventory) {
            Date currentDate = new Date();
            Date expiryDate = item.getItemExpiry();

            long timeDifference = expiryDate.getTime() - currentDate.getTime();
            long daysLeft = timeDifference / (1000 * 60 * 60 * 24);

            if (daysLeft >= 0 && daysLeft <= 3) {
                String notificationText = item.getItemName() + " is expiring soon!\n" + expiryDate.toString();
                Notification notification = new Notification(notificationText);
                notifications.add(notification);
            }
        }

        // Log the generated notifications
        for (Notification notification : notifications) {
            System.out.println("Notification: " + notification.getMessage());
        }

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/running-out")
    public ResponseEntity<List<Inventory>> getItemsRunningOut(HttpSession session) {
        // Check if the user is logged in as a Chef
        String role = (String) session.getAttribute("role");
        if (role == null || !"Chef".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        // Get all inventory items
        List<Inventory> inventory = inventoryRepository.findAll();

        // Filter items with quantity below 3
        List<Inventory> itemsRunningOut = inventory.stream()
                .filter(item -> item.getItemQuantity() < 3)
                .collect(Collectors.toList());

        return ResponseEntity.ok(itemsRunningOut);
    }
}
