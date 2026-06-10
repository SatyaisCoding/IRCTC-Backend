package com.irctc.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationRequest {

    @NotBlank(message = "Station name cannot be blank")
    private String name;

    @NotBlank(message = "City cannot be blank")
    private String city;

    @NotBlank(message = "State cannot be blank")
    private String state;

    @NotBlank(message = "Station code cannot be blank")
    @Pattern(regexp = "^[A-Za-z]+$", message = "Station code must contain only alphabetic characters")
    private String stationCode;
}
