package com.irctc.admin.service;

import com.irctc.admin.dto.TrainRequest;
import com.irctc.admin.dto.TrainResponse;

import java.util.List;

public interface TrainService {
    TrainResponse createTrain(TrainRequest request);
    TrainResponse assignRoute(Long trainId, Long routeId);
    List<TrainResponse> getAllTrains();
}
