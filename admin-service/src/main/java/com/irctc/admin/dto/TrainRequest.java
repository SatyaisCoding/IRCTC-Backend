package com.irctc.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainRequest {

    @NotBlank(message = "Train number cannot be blank")
    private String trainNumber;

    @NotBlank(message = "Train name cannot be blank")
    private String trainName;

    @NotBlank(message = "Coach type cannot be blank")
    @Pattern(regexp = "^(3A|2A|1A|sleeper)$", message = "Coach type must be one of: 3A, 2A, 1A, sleeper")
    private String coachType;

    @NotBlank(message = "Train type cannot be blank")
    private String trainType; // e.g. EXPRESS, SUPERFAST, LOCAL

    @NotEmpty(message = "At least one seat must be defined")
    private List<@Valid SeatRequest> seats;
}
