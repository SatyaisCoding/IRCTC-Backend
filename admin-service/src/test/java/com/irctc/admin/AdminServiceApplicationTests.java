package com.irctc.admin;

import com.irctc.admin.dto.*;
import com.irctc.admin.entity.Seat;
import com.irctc.admin.entity.enums.SeatType;
import com.irctc.admin.exception.BadRequestException;
import com.irctc.admin.repository.SeatRepository;
import com.irctc.admin.service.RouteService;
import com.irctc.admin.service.ScheduleService;
import com.irctc.admin.service.StationService;
import com.irctc.admin.service.TrainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AdminServiceApplicationTests {

    @Autowired
    private StationService stationService;

    @Autowired
    private TrainService trainService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private SeatRepository seatRepository;

    @Test
    @Transactional
    void contextLoadsAndRunsScenario() {
        // 1. Create Station
        StationResponse stationGKP = stationService.createStation(StationRequest.builder()
                .name("Gorakhpur Test")
                .city("Gorakhpur")
                .state("UP")
                .stationCode("GKPT")
                .build());

        StationResponse stationNDLS = stationService.createStation(StationRequest.builder()
                .name("New Delhi Test")
                .city("Delhi")
                .state("Delhi")
                .stationCode("NDLST")
                .build());

        assertNotNull(stationGKP.getId());
        assertEquals("GKPT", stationGKP.getStationCode());

        // 2. Validate seat creation checks
        // 2a. Validate empty seat list throws BadRequestException
        assertThrows(BadRequestException.class, () -> {
            trainService.createTrain(TrainRequest.builder()
                    .trainNumber("11111")
                    .trainName("Empty Seat Train")
                    .coachType("2A")
                    .trainType("SUPERFAST")
                    .seats(new ArrayList<>())
                    .build());
        });

        // 2b. Validate duplicate seat numbers throw BadRequestException
        assertThrows(BadRequestException.class, () -> {
            trainService.createTrain(TrainRequest.builder()
                    .trainNumber("22222")
                    .trainName("Duplicate Seat Train")
                    .coachType("2A")
                    .trainType("SUPERFAST")
                    .seats(List.of(
                            SeatRequest.builder().seatNumber("S1").seatType(SeatType.LOWER).price(1550.0).build(),
                            SeatRequest.builder().seatNumber("S1").seatType(SeatType.MIDDLE).price(1500.0).build()
                    ))
                    .build());
        });

        // 2c. Create Train with valid custom seats
        List<SeatRequest> seatRequests = List.of(
                SeatRequest.builder().seatNumber("S1").seatType(SeatType.LOWER).price(1550.0).build(),
                SeatRequest.builder().seatNumber("S2").seatType(SeatType.MIDDLE).price(1500.0).build(),
                SeatRequest.builder().seatNumber("S3").seatType(SeatType.UPPER).price(1500.0).build(),
                SeatRequest.builder().seatNumber("S4").seatType(SeatType.SIDE_UPPER).price(1500.0).build(),
                SeatRequest.builder().seatNumber("S5").seatType(SeatType.SIDE_LOWER).price(1550.0).build()
        );

        TrainResponse trainResponse = trainService.createTrain(TrainRequest.builder()
                .trainNumber("99999")
                .trainName("Vande Bharat Test")
                .coachType("2A")
                .trainType("SUPERFAST")
                .seats(seatRequests)
                .build());

        assertNotNull(trainResponse.getId());
        assertEquals(5, trainResponse.getTotalSeats());
        assertNotNull(trainResponse.getSeats());
        assertEquals(5, trainResponse.getSeats().size());
        assertEquals(trainResponse.getId(), trainResponse.getSeats().get(0).getTrainId());

        // Verify seats are generated and associated in DB
        List<Seat> seats = seatRepository.findByTrainId(trainResponse.getId());
        assertEquals(5, seats.size());

        // Verify Seat Details
        assertEquals("S1", seats.get(0).getSeatNumber());
        assertEquals(SeatType.LOWER, seats.get(0).getSeatType());
        assertEquals(1550.0, seats.get(0).getPrice());

        assertEquals("S2", seats.get(1).getSeatNumber());
        assertEquals(SeatType.MIDDLE, seats.get(1).getSeatType());
        assertEquals(1500.0, seats.get(1).getPrice());

        // 3. Create Route
        RouteResponse routeResponse = routeService.createRoute(RouteRequest.builder()
                .routeName("GKP-NDLS")
                .routeStations(List.of(
                        RouteStationRequest.builder()
                                .stationId(stationGKP.getId())
                                .sequenceNumber(1)
                                .arrivalTime("06:00")
                                .departureTime("06:00")
                                .distanceFromOrigin(0.0)
                                .build(),
                        RouteStationRequest.builder()
                                .stationId(stationNDLS.getId())
                                .sequenceNumber(2)
                                .arrivalTime("14:30")
                                .departureTime("14:30")
                                .distanceFromOrigin(800.0)
                                .build()
                ))
                .build());

        assertNotNull(routeResponse.getId());
        assertEquals(2, routeResponse.getRouteStations().size());

        // 4. Assign Route
        TrainResponse trainWithRoute = trainService.assignRoute(trainResponse.getId(), routeResponse.getId());
        assertEquals(routeResponse.getId(), trainWithRoute.getRouteId());

        // 5. Create Schedule
        ScheduleResponse scheduleResponse = scheduleService.createSchedule(ScheduleRequest.builder()
                .trainId(trainResponse.getId())
                .departureDate(LocalDate.now().plusDays(10))
                .status(com.irctc.admin.entity.enums.ScheduleStatus.SCHEDULED)
                .build());

        assertNotNull(scheduleResponse.getId());
        assertEquals(trainResponse.getId(), scheduleResponse.getTrainId());
        assertEquals(2, scheduleResponse.getRoute().getRouteStations().size());
    }
}
