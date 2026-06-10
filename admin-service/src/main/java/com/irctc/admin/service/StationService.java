package com.irctc.admin.service;

import com.irctc.admin.dto.StationRequest;
import com.irctc.admin.dto.StationResponse;

import java.util.List;

public interface StationService {
    StationResponse createStation(StationRequest request);
    List<StationResponse> getAllStations();
    StationResponse updateStation(Long id, StationRequest request);
    void deleteStation(Long id);
    StationResponse getStationById(Long id);
    StationResponse getStationByCode(String code);
}
