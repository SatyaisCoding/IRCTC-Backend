package com.irctc.search.service.impl;

import com.irctc.search.dto.SearchRequest;
import com.irctc.search.dto.TrainResponse;
import com.irctc.search.entity.Train;
import com.irctc.search.exception.SearchException;
import com.irctc.search.repository.TrainRepository;
import com.irctc.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final TrainRepository trainRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TrainResponse> searchTrains(SearchRequest request) {
        List<Train> trains = trainRepository.findByStationFromAndStationTo(
                request.getStationFrom().toUpperCase(),
                request.getStationTo().toUpperCase()
        );
        return trains.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainResponse getTrainByNumber(String trainNumber) {
        Train train = trainRepository.findByTrainNumber(trainNumber)
                .orElseThrow(() -> new SearchException("Train not found with Number: " + trainNumber, "TRAIN_NOT_FOUND"));
        return mapToResponse(train);
    }

    @Override
    @Transactional
    public TrainResponse addTrain(TrainResponse dto) {
        if (trainRepository.findByTrainNumber(dto.getTrainNumber()).isPresent()) {
            throw new SearchException("Train already exists with number: " + dto.getTrainNumber(), "TRAIN_ALREADY_EXISTS");
        }

        Train train = Train.builder()
                .trainNumber(dto.getTrainNumber())
                .trainName(dto.getTrainName())
                .stationFrom(dto.getStationFrom().toUpperCase())
                .stationTo(dto.getStationTo().toUpperCase())
                .departureTime(dto.getDepartureTime())
                .arrivalTime(dto.getArrivalTime())
                .availableSeats(dto.getAvailableSeats())
                .fare(dto.getFare())
                .build();

        Train savedTrain = trainRepository.save(train);
        return mapToResponse(savedTrain);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void populateInitialTrains() {
        if (trainRepository.count() == 0) {
            trainRepository.save(Train.builder()
                    .trainNumber("12626")
                    .trainName("Kerala Express")
                    .stationFrom("NDLS")
                    .stationTo("TVC")
                    .departureTime("20:10")
                    .arrivalTime("14:20")
                    .availableSeats(120)
                    .fare(2450.0)
                    .build());

            trainRepository.save(Train.builder()
                    .trainNumber("12952")
                    .trainName("Mumbai Rajdhani")
                    .stationFrom("NDLS")
                    .stationTo("MMCT")
                    .departureTime("16:55")
                    .arrivalTime("08:35")
                    .availableSeats(45)
                    .fare(3200.0)
                    .build());

            trainRepository.save(Train.builder()
                    .trainNumber("12302")
                    .trainName("Howrah Rajdhani")
                    .stationFrom("NDLS")
                    .stationTo("HWH")
                    .departureTime("16:50")
                    .arrivalTime("09:55")
                    .availableSeats(60)
                    .fare(3100.0)
                    .build());
        }
    }

    private TrainResponse mapToResponse(Train train) {
        return TrainResponse.builder()
                .id(train.getId())
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .stationFrom(train.getStationFrom())
                .stationTo(train.getStationTo())
                .departureTime(train.getDepartureTime())
                .arrivalTime(train.getArrivalTime())
                .availableSeats(train.getAvailableSeats())
                .fare(train.getFare())
                .build();
    }
}
