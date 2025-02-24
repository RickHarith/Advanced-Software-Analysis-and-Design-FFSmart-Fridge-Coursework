package com.example.AdvancedAnalysisCW;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@RestController
public class UserController {
    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private RestTemplate restTemplate;
    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Value("${randomnumberapi.url}")
    private String randomNumberApiUrl;
    private Long generateUniqueId() {
        int maxRetries = 5;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            ResponseEntity<Integer[]> responseEntity = restTemplate.getForEntity(randomNumberApiUrl, Integer[].class);
            Integer[] response = responseEntity.getBody();
            if (response != null && response.length > 0) {
                Long generatedId = Long.valueOf(response[0]);
                if (!userRepository.existsById(generatedId)) {
                    return generatedId;
                }
            }
        }
        System.err.println("Failed to generate a unique trip ID after " + maxRetries + " attempts.");
        return null;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body("{\"error\":\"Username already exists.\"}");
        }
        Long uniqueId = generateUniqueId();
        if (uniqueId == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"Failed to generate a unique ID.\"}");
        }
        user.setId(uniqueId);
        userRepository.save(user);
        return ResponseEntity.ok("{\"message\":\"User registered successfully\"}");
    }

    @PostMapping("/login")
    public ResponseEntity<?> logIn(@RequestBody User loginUser, HttpSession session) {
        try {
            User user = userRepository.findByUsername(loginUser.getUsername())
                    .orElse(null);

            if (user != null && user.getPassword().equals(loginUser.getPassword())) {
                session.setAttribute("userId", user.getId());
                session.setAttribute("role", user.getRole());
                session.setAttribute("fullname", user.getFullname());

                String role = user.getRole();
                if ("Chef".equals(role) || "Head Chef".equals(role)) {
                    return ResponseEntity.ok(Collections.singletonMap("redirect", "chefmain.html"));
                } else if ("Delivery Person".equals(role)) {
                    return ResponseEntity.ok(Collections.singletonMap("redirect", "deliverymain.html"));
                } else if ("Admin".equals(role)) {
                    return ResponseEntity.ok(Collections.singletonMap("redirect", "adminmain.html"));
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Collections.singletonMap("error", "Invalid role"));
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("error", "Invalid username or password"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "An error occurred."));
        }
    }


    @GetMapping("/is-logged-in")
    public ResponseEntity<Map<String, Boolean>> isLoggedIn(HttpSession session) {
        boolean isLoggedIn = session.getAttribute("userId") != null;
        return ResponseEntity.ok(Collections.singletonMap("loggedIn", isLoggedIn));
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logOut(HttpSession session) {
        Object userId = session.getAttribute("userId");
        session.removeAttribute("userId");
        session.removeAttribute("role"); // Remove the user role as well
        session.removeAttribute("fullname");
        Object userIdAfterLogout = session.getAttribute("userId");

        String message;
        if (userIdAfterLogout == null) {
            message = "Log out successful.";
        } else {
            message = "Error: User ID attribute was not cleared properly.";
        }

        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    @GetMapping("/fetch-users")
    public ResponseEntity<List<User>> fetchUsers(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"Admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.emptyList()); // Return an empty list or appropriate error response.
        }

        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/update-user-role/{userId}")
    public ResponseEntity<?> updateUserRole(@PathVariable Long userId, @RequestBody Map<String, String> roleData, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"Admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"Only admins are allowed to make changes.\"}");
        }

        String newRole = roleData.get("role");

        // Check if the user with the provided userId exists
        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "User not found"));
        }

        User user = userOptional.get();
        user.setRole(newRole);

        // Save the updated user with the new role
        userRepository.save(user);

        return ResponseEntity.ok(Collections.singletonMap("message", "User role updated successfully"));
    }


    @PostMapping("/send-health-report")
    public ResponseEntity<?> sendHealthReport(@RequestBody HealthReport healthReport, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"Admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"Only admins are allowed to send health reports.\"}");
        }

        try {
            System.out.println("Received a health report request.");
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);

            helper.setSubject("Health Report");

            String emailContent = "<h2>Health Report</h2><br>";
            emailContent += "Owner Name: " + healthReport.getOwnerName() + "<br>";
            emailContent += "Owner Email: " + healthReport.getOwnerEmail() + "<br>";
            emailContent += "Business Phone: " + healthReport.getBusinessPhone() + "<br>";
            emailContent += "Business Name: " + healthReport.getBusinessName() + "<br>";
            emailContent += "Restaurant Name: " + healthReport.getRestaurantName() + "<br>";
            emailContent += "Restaurant Address: " + healthReport.getRestaurantAddress() + "<br>";
            emailContent += "City: " + healthReport.getCity() + "<br>";
            emailContent += "Zip Code: " + healthReport.getZipCode() + "<br>";
            emailContent += "Certified Food Handler: " + healthReport.getCertifiedFoodHandler() + "<br>";
            emailContent += "Report Details: " + healthReport.getReportDetails() + "<br>";

            helper.setText(emailContent, true);

            // Set the recipient email address
            helper.setTo(healthReport.getReceivingEmail());

            // Send the email
            javaMailSender.send(message);
            System.out.println("Health report sent successfully.");

            return ResponseEntity.ok(Collections.singletonMap("message", "Health report sent successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "An error occurred while sending the health report."));
        }
    }
}

