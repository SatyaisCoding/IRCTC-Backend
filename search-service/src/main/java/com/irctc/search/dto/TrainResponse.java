package com.irctc.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainResponse {
    private Long id;
    private String trainNumber;
    private String trainName;
    private String stationFrom;
    private String stationTo;
    private String departureTime;
    private String arrivalTime;
    private Integer availableSeats;
    private Double fare;
}
