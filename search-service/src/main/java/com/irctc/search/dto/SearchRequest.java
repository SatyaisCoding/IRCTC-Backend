package com.irctc.search.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @NotBlank(message = "Source station (stationFrom) cannot be blank")
    private String stationFrom;

    @NotBlank(message = "Destination station (stationTo) cannot be blank")
    private String stationTo;
}
