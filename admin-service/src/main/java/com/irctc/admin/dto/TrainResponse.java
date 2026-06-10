package com.irctc.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainResponse {
    private Long id;
    private String trainNumber;
    private String trainName;
    private String coachType;
    private Integer totalSeats;
    private String trainType;
    private Long routeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SeatResponse> seats;
}
