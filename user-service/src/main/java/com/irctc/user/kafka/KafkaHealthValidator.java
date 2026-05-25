package com.irctc.user.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaHealthValidator {

    private final AdminClient kafkaAdminClient;

    private static final long CACHE_TTL_MS     = 30_000L;
    private static final long PROBE_TIMEOUT_SEC = 5L;

    private final AtomicLong    lastHealthyTimestamp = new AtomicLong(0L);
    private final AtomicBoolean brokerHealthy        = new AtomicBoolean(false);

    // Runs once at startup — does NOT block boot if Kafka is down
    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        log.info("[KafkaHealthValidator] Checking Kafka broker connectivity...");
        try {
            pingBroker();
            log.info("[KafkaHealthValidator] ✅ Kafka broker is reachable.");
        } catch (KafkaBrokerUnavailableException ex) {
            log.error("[KafkaHealthValidator] ❌ Kafka broker unreachable on startup: {}", ex.getMessage());
            log.error("[KafkaHealthValidator] Notification events will fail until Kafka is available.");
        }
    }

    // Called before every send — result cached for 30s to avoid repeated broker probes
    public void ensureConnected() {
        long now = System.currentTimeMillis();
        if (brokerHealthy.get() && (now - lastHealthyTimestamp.get()) < CACHE_TTL_MS) {
            return;
        }
        pingBroker();
    }

    private void pingBroker() {
        try {
            DescribeClusterResult result = kafkaAdminClient.describeCluster();
            String clusterId = result.clusterId().get(PROBE_TIMEOUT_SEC, TimeUnit.SECONDS);
            brokerHealthy.set(true);
            lastHealthyTimestamp.set(System.currentTimeMillis());
            log.debug("[KafkaHealthValidator] Broker probe OK — clusterId: {}", clusterId);
        } catch (Exception ex) {
            brokerHealthy.set(false);
            lastHealthyTimestamp.set(0L);
            throw new KafkaBrokerUnavailableException(
                    "Kafka broker unreachable: " + ex.getMessage(), ex);
        }
    }
}
