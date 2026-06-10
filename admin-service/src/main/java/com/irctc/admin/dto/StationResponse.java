package com.irctc.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationResponse {
    private Long id;
    private String name;
    private String city;
    private String state;
    private String stationCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
