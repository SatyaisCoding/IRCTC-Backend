package com.irctc.admin.dto;

import com.irctc.admin.entity.enums.ScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    private Long id;
    private Long trainId;
    private String trainNumber;
    private String trainName;
    private LocalDate departureDate;
    private ScheduleStatus status;
    private RouteResponse route;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
