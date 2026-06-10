package com.irctc.admin.dto;

import com.irctc.admin.entity.enums.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatRequest {

    @NotBlank(message = "Seat number cannot be blank")
    private String seatNumber;

    @NotNull(message = "Seat type cannot be null")
    private SeatType seatType;

    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be positive")
    private Double price;
}
