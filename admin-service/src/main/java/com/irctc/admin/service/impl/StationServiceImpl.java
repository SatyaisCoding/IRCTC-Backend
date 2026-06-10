package com.irctc.admin.service.impl;

import com.irctc.admin.dto.StationRequest;
import com.irctc.admin.dto.StationResponse;
import com.irctc.admin.entity.Station;
import com.irctc.admin.exception.DuplicateResourceException;
import com.irctc.admin.exception.ResourceNotFoundException;
import com.irctc.admin.kafka.KafkaProducerService;
import com.irctc.admin.repository.StationRepository;
import com.irctc.admin.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;
    private final KafkaProducerService kafkaProducerService;

    @Override
    @Transactional
    public StationResponse createStation(StationRequest request) {
        String code = request.getStationCode().toUpperCase();
        if (stationRepository.existsByStationCode(code)) {
            throw new DuplicateResourceException("Station with code " + code + " already exists");
        }

        Station station = Station.builder()
                .name(request.getName())
                .city(request.getCity())
                .state(request.getState())
                .stationCode(code)
                .build();

        Station savedStation = stationRepository.save(station);
        StationResponse response = mapToResponse(savedStation);
        kafkaProducerService.sendStationCreated(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StationResponse> getAllStations() {
        return stationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StationResponse updateStation(Long id, StationRequest request) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station with ID " + id + " not found"));

        String newCode = request.getStationCode().toUpperCase();
        if (!station.getStationCode().equals(newCode) && stationRepository.existsByStationCode(newCode)) {
            throw new DuplicateResourceException("Station with code " + newCode + " already exists");
        }

        station.setName(request.getName());
        station.setCity(request.getCity());
        station.setState(request.getState());
        station.setStationCode(newCode);

        Station savedStation = stationRepository.save(station);
        return mapToResponse(savedStation);
    }

    @Override
    @Transactional
    public void deleteStation(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station with ID " + id + " not found"));
        stationRepository.delete(station);
    }

    @Override
    @Transactional(readOnly = true)
    public StationResponse getStationById(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station with ID " + id + " not found"));
        return mapToResponse(station);
    }

    @Override
    @Transactional(readOnly = true)
    public StationResponse getStationByCode(String code) {
        Station station = stationRepository.findByStationCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Station with code " + code + " not found"));
        return mapToResponse(station);
    }

    private StationResponse mapToResponse(Station station) {
        return StationResponse.builder()
                .id(station.getId())
                .name(station.getName())
                .city(station.getCity())
                .state(station.getState())
                .stationCode(station.getStationCode())
                .createdAt(station.getCreatedAt())
                .updatedAt(station.getUpdatedAt())
                .build();
    }
}
