package com.irctc.search.service;

import com.irctc.search.dto.SearchRequest;
import com.irctc.search.dto.TrainResponse;

import java.util.List;

public interface SearchService {
    List<TrainResponse> searchTrains(SearchRequest request);
    TrainResponse getTrainByNumber(String trainNumber);
    TrainResponse addTrain(TrainResponse trainResponse);
}
