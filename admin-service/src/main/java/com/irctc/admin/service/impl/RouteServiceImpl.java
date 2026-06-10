package com.irctc.admin.service.impl;

import com.irctc.admin.dto.RouteRequest;
import com.irctc.admin.dto.RouteResponse;
import com.irctc.admin.dto.RouteStationResponse;
import com.irctc.admin.dto.StationResponse;
import com.irctc.admin.entity.Route;
import com.irctc.admin.entity.RouteStation;
import com.irctc.admin.entity.Station;
import com.irctc.admin.exception.BadRequestException;
import com.irctc.admin.exception.ResourceNotFoundException;
import com.irctc.admin.repository.RouteRepository;
import com.irctc.admin.repository.StationRepository;
import com.irctc.admin.service.RouteService;
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
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;

    @Override
    @Transactional
    public RouteResponse createRoute(RouteRequest request) {
        // Validation 1: Check unique sequence numbers
        Set<Integer> seqNumbers = new HashSet<>();
        for (var rs : request.getRouteStations()) {
            if (!seqNumbers.add(rs.getSequenceNumber())) {
                throw new BadRequestException("Sequence number " + rs.getSequenceNumber() + " cannot be repeated in a single route");
            }
        }

        // Validation 2: Check contiguous sequence starting from 1
        int n = request.getRouteStations().size();
        for (int i = 1; i <= n; i++) {
            if (!seqNumbers.contains(i)) {
                throw new BadRequestException("Sequence numbers must be contiguous starting from 1 to " + n);
            }
        }

        // Validation 3: Check unique stations
        Set<Long> stationIds = new HashSet<>();
        for (var rs : request.getRouteStations()) {
            if (!stationIds.add(rs.getStationId())) {
                throw new BadRequestException("Station ID " + rs.getStationId() + " cannot be repeated in a single route");
            }
        }

        Route route = Route.builder()
                .routeName(request.getRouteName())
                .build();

        List<RouteStation> routeStations = new ArrayList<>();
        for (var rsReq : request.getRouteStations()) {
            Station station = stationRepository.findById(rsReq.getStationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Station with ID " + rsReq.getStationId() + " not found"));

            RouteStation routeStation = RouteStation.builder()
                    .route(route)
                    .station(station)
                    .sequenceNumber(rsReq.getSequenceNumber())
                    .arrivalTime(rsReq.getArrivalTime())
                    .departureTime(rsReq.getDepartureTime())
                    .distanceFromOrigin(rsReq.getDistanceFromOrigin())
                    .build();
            routeStations.add(routeStation);
        }
        route.setRouteStations(routeStations);

        Route savedRoute = routeRepository.save(route);
        return mapToResponse(savedRoute);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getRouteById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route with ID " + id + " not found"));
        return mapToResponse(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponse> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private RouteResponse mapToResponse(Route route) {
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

        return RouteResponse.builder()
                .id(route.getId())
                .routeName(route.getRouteName())
                .trainId(route.getTrainId())
                .routeStations(stationsResponses)
                .build();
    }
}
