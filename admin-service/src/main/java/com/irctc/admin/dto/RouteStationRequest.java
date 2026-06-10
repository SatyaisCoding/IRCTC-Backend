package com.irctc.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStationRequest {

    @NotNull(message = "Station ID cannot be null")
    private Long stationId;

    @NotNull(message = "Sequence number cannot be null")
    @Min(value = 1, message = "Sequence number must be at least 1")
    private Integer sequenceNumber;

    @NotBlank(message = "Arrival time cannot be blank")
    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Arrival time must be in HH:mm format")
    private String arrivalTime;

    @NotBlank(message = "Departure time cannot be blank")
    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Departure time must be in HH:mm format")
    private String departureTime;

    @NotNull(message = "Distance from origin cannot be null")
    @Min(value = 0, message = "Distance from origin cannot be negative")
    private Double distanceFromOrigin;
}
