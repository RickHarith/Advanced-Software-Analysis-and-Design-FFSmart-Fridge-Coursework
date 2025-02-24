package com.example.AdvancedAnalysisCW;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
public class InventoryLoggerController {

    @GetMapping("/log")
    public ResponseEntity<String> getInventoryLog() {
        try {
            String logFilePath = Paths.get(".").toAbsolutePath().normalize().toString() + "/inventory_log.txt";
            String content = new String(Files.readAllBytes(Paths.get(logFilePath)));
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading log file");
        }
    }
}
