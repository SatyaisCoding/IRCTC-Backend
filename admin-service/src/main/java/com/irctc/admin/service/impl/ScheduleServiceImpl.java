package com.irctc.admin.service.impl;

import com.irctc.admin.dto.RouteResponse;
import com.irctc.admin.dto.RouteStationResponse;
import com.irctc.admin.dto.ScheduleRequest;
import com.irctc.admin.dto.ScheduleResponse;
import com.irctc.admin.dto.StationResponse;
import com.irctc.admin.entity.Route;
import com.irctc.admin.entity.Schedule;
import com.irctc.admin.entity.Train;
import com.irctc.admin.exception.BadRequestException;
import com.irctc.admin.exception.DuplicateResourceException;
import com.irctc.admin.exception.ResourceNotFoundException;
import com.irctc.admin.repository.ScheduleRepository;
import com.irctc.admin.repository.TrainRepository;
import com.irctc.admin.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final TrainRepository trainRepository;

    @Override
    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request) {
        Train train = trainRepository.findById(request.getTrainId())
                .orElseThrow(() -> new ResourceNotFoundException("Train with ID " + request.getTrainId() + " not found"));

        if (train.getRoute() == null) {
            throw new BadRequestException("Train " + train.getTrainNumber() + " is not associated with any route. Please assign a route first.");
        }

        // Validate departure date is in the future
        if (!request.getDepartureDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Departure date must be a future date");
        }

        // Validate unique train run per date
        if (scheduleRepository.existsByTrainIdAndDepartureDate(request.getTrainId(), request.getDepartureDate())) {
            throw new DuplicateResourceException("Train " + train.getTrainNumber() + " is already scheduled for departure date " + request.getDepartureDate());
        }

        Schedule schedule = Schedule.builder()
                .train(train)
                .departureDate(request.getDepartureDate())
                .status(request.getStatus())
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        return mapToResponse(savedSchedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAllSchedules() {
        return scheduleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ScheduleResponse mapToResponse(Schedule schedule) {
        Train train = schedule.getTrain();
        Route route = train.getRoute();

        RouteResponse routeResponse = null;
        if (route != null) {
            List<RouteStationResponse> stationsResponses = route.getRouteStations() != null ?
                    route.getRouteStations().stream()
                            .map(rs -> RouteStationResponse.builder()
                                    .id(rs.getId())
                                    .sequenceNumber(rs.getSequenceNumber())
                                    .arrivalTime(rs.getArrivalTime())
                                    .departureTime(rs.getDepartureTime())
                                    .distanceFromOrigin(rs.getDistanceFromOrigin())
                                    .station(StationResponse.builder()
                                            .id(rs.getStation().getId())
                                            .name(rs.getStation().getName())
                                            .city(rs.getStation().getCity())
                                            .state(rs.getStation().getState())
                                            .stationCode(rs.getStation().getStationCode())
                                            .createdAt(rs.getStation().getCreatedAt())
                                            .updatedAt(rs.getStation().getUpdatedAt())
                                            .build())
                                    .build())
                            .collect(Collectors.toList()) : new ArrayList<>();

            routeResponse = RouteResponse.builder()
                    .id(route.getId())
                    .routeName(route.getRouteName())
                    .trainId(route.getTrainId())
                    .routeStations(stationsResponses)
                    .build();
        }

        return ScheduleResponse.builder()
                .id(schedule.getId())
                .trainId(train.getId())
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .departureDate(schedule.getDepartureDate())
                .status(schedule.getStatus())
                .route(routeResponse)
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
