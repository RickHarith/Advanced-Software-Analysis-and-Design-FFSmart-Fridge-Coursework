package com.example.AdvancedAnalysisCW;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InventoryLogger {

    private static final String LOG_FILE = "inventory_log.txt";

    public static void log(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.write(timeStamp + " - " + message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}