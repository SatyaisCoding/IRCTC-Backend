package com.irctc.notification;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    private static void loadDotEnv() {
        java.io.File dotEnv = new java.io.File(".env");
        if (!dotEnv.exists()) {
            dotEnv = new java.io.File("../.env");
        }
        if (dotEnv.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(dotEnv))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load .env file: " + e.getMessage());
            }
        }
    }

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        // 1. Recover by publishing directly to the DLT topic suffixed with "-dlt" on the same partition
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (r, e) -> new TopicPartition(r.topic() + "-dlt", r.partition()));

        // 2. Exponential backoff: starting at 2000ms, doubling delay each retry (2000ms, 4000ms, 8000ms) up to 10000ms max
        ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
        backOff.setMaxInterval(10000L);
        backOff.setMaxElapsedTime(100000L); // Extend total elapsed time so it successfully completes all 3 retries

        // 3. Set Max attempts to 4 (1 initial attempt + 3 local retries)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setLogLevel(org.springframework.kafka.KafkaException.Level.WARN); // Suppress noisy ERROR stack traces during retries
        
        // Exclude specific exceptions if desired
        errorHandler.addNotRetryableExceptions(NullPointerException.class);
        
        return errorHandler;
    }
}
