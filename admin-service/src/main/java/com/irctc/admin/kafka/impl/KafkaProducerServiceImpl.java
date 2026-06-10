package com.irctc.admin.kafka.impl;

import com.irctc.admin.dto.StationResponse;
import com.irctc.admin.dto.TrainResponse;
import com.irctc.admin.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.station-created}")
    private String stationCreatedTopic;

    @Value("${app.kafka.topics.train-created}")
    private String trainCreatedTopic;

    @Override
    public void sendStationCreated(StationResponse station) {
        String key = "station-" + station.getId();
        log.info("[KafkaProducer] Publishing station-created event → topic: [{}] | key: [{}]", 
                stationCreatedTopic, key);

        CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(stationCreatedTopic, key, station);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[KafkaProducer] Station created event delivery FAILED for key [{}]: {}", 
                        key, ex.getMessage());
            } else {
                log.info("[KafkaProducer] Station created event delivered | key: [{}] | partition: {} | offset: {}", 
                        key, 
                        result.getRecordMetadata().partition(), 
                        result.getRecordMetadata().offset());
            }
        });
    }

    @Override
    public void sendTrainCreated(TrainResponse train) {
        String key = "train-" + train.getId();
        log.info("[KafkaProducer] Publishing train-created event → topic: [{}] | key: [{}]", 
                trainCreatedTopic, key);

        CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(trainCreatedTopic, key, train);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[KafkaProducer] Train created event delivery FAILED for key [{}]: {}", 
                        key, ex.getMessage());
            } else {
                log.info("[KafkaProducer] Train created event delivered | key: [{}] | partition: {} | offset: {}", 
                        key, 
                        result.getRecordMetadata().partition(), 
                        result.getRecordMetadata().offset());
            }
        });
    }
}
