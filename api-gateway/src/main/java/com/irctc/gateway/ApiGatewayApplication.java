package com.irctc.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.FileReader;

@SpringBootApplication
@Slf4j
public class ApiGatewayApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    /**
     * Loads .env file from the project root into system properties.
     * Spring Boot does not load .env automatically — this bridges that gap.
     */
    private static void loadDotEnv() {
        String[] locations = {"../.env", ".env"};
        for (String path : locations) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts.length > 1 ? parts[1].trim() : "";
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                }
                log.info("[ApiGateway] Loaded environment from: {}", path);
                return;
            } catch (Exception ignored) {}
        }
    }
}
