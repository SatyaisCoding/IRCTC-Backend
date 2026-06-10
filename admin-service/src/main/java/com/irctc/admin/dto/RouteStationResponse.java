package com.irctc.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStationResponse {
    private Long id;
    private Integer sequenceNumber;
    private String arrivalTime;
    private String departureTime;
    private Double distanceFromOrigin;
    private StationResponse station;
}
