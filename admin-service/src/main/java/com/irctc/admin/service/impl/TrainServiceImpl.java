package com.irctc.admin.service.impl;

import com.irctc.admin.dto.SeatRequest;
import com.irctc.admin.dto.SeatResponse;
import com.irctc.admin.dto.TrainRequest;
import com.irctc.admin.dto.TrainResponse;
import com.irctc.admin.entity.Route;
import com.irctc.admin.entity.Seat;
import com.irctc.admin.entity.Train;
import com.irctc.admin.exception.BadRequestException;
import com.irctc.admin.exception.DuplicateResourceException;
import com.irctc.admin.exception.ResourceNotFoundException;
import com.irctc.admin.kafka.KafkaProducerService;
import com.irctc.admin.repository.RouteRepository;
import com.irctc.admin.repository.TrainRepository;
import com.irctc.admin.service.TrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainServiceImpl implements TrainService {

    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final KafkaProducerService kafkaProducerService;

    @Override
    @Transactional
    public TrainResponse createTrain(TrainRequest request) {
        if (trainRepository.existsByTrainNumber(request.getTrainNumber())) {
            throw new DuplicateResourceException("Train with number " + request.getTrainNumber() + " already exists");
        }

        // Validate seats list presence
        if (request.getSeats() == null || request.getSeats().isEmpty()) {
            throw new BadRequestException("At least one seat must be defined");
        }

        // Validate duplicate seat numbers
        Set<String> uniqueSeatNumbers = new HashSet<>();
        List<String> duplicateSeatNumbers = new ArrayList<>();
        for (SeatRequest seatReq : request.getSeats()) {
            if (!uniqueSeatNumbers.add(seatReq.getSeatNumber())) {
                duplicateSeatNumbers.add(seatReq.getSeatNumber());
            }
        }
        if (!duplicateSeatNumbers.isEmpty()) {
            throw new BadRequestException("Duplicate seat numbers are not allowed: " + duplicateSeatNumbers);
        }

        // Build train entity
        Train train = Train.builder()
                .trainNumber(request.getTrainNumber())
                .trainName(request.getTrainName())
                .coachType(request.getCoachType())
                .totalSeats(request.getSeats().size())
                .trainType(request.getTrainType())
                .build();

        // Build custom seat entities
        List<Seat> seats = request.getSeats().stream()
                .map(seatReq -> Seat.builder()
                        .seatNumber(seatReq.getSeatNumber())
                        .seatType(seatReq.getSeatType())
                        .price(seatReq.getPrice())
                        .train(train)
                        .build())
                .collect(Collectors.toList());

        train.setSeats(seats);

        Train savedTrain = trainRepository.save(train);
        TrainResponse response = mapToResponse(savedTrain);
        
        // Publish to Kafka
        kafkaProducerService.sendTrainCreated(response);

        return response;
    }

    @Override
    @Transactional
    public TrainResponse assignRoute(Long trainId, Long routeId) {
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new ResourceNotFoundException("Train with ID " + trainId + " not found"));

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route with ID " + routeId + " not found"));

        train.setRoute(route);
        route.setTrainId(trainId);
        
        routeRepository.save(route);
        Train savedTrain = trainRepository.save(train);
        return mapToResponse(savedTrain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainResponse> getAllTrains() {
        return trainRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TrainResponse mapToResponse(Train train) {
        List<SeatResponse> seatResponses = train.getSeats() != null ? train.getSeats().stream()
                .map(seat -> SeatResponse.builder()
                        .id(seat.getId())
                        .trainId(train.getId())
                        .seatNumber(seat.getSeatNumber())
                        .seatType(seat.getSeatType())
                        .price(seat.getPrice())
                        .build())
                .collect(Collectors.toList()) : new ArrayList<>();

        return TrainResponse.builder()
                .id(train.getId())
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .coachType(train.getCoachType())
                .totalSeats(train.getTotalSeats())
                .trainType(train.getTrainType())
                .routeId(train.getRoute() != null ? train.getRoute().getId() : null)
                .createdAt(train.getCreatedAt())
                .updatedAt(train.getUpdatedAt())
                .seats(seatResponses)
                .build();
    }
}
