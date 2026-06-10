package com.irctc.admin.dto;

import com.irctc.admin.entity.enums.ScheduleStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

    @NotNull(message = "Train ID cannot be null")
    private Long trainId;

    @NotNull(message = "Departure date cannot be null")
    @Future(message = "Departure date must be a future date")
    private LocalDate departureDate;

    @NotNull(message = "Status cannot be null")
    private ScheduleStatus status;
}
