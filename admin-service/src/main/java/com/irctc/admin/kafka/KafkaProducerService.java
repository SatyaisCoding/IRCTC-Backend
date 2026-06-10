package com.irctc.admin.kafka;

import com.irctc.admin.dto.StationResponse;
import com.irctc.admin.dto.TrainResponse;

public interface KafkaProducerService {
    void sendStationCreated(StationResponse station);
    void sendTrainCreated(TrainResponse train);
}
