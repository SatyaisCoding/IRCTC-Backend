package com.irctc.booking.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class BookingRequest {

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "Train ID cannot be null")
    private Long trainId;

    @NotBlank(message = "Passenger name cannot be blank")
    private String passengerName;

    @NotNull(message = "Journey date cannot be null")
    @FutureOrPresent(message = "Journey date must be in the present or future")
    private LocalDate journeyDate;

    @NotNull(message = "Fare cannot be null")
    @Min(value = 0, message = "Fare must be non-negative")
    private Double fare;
}
