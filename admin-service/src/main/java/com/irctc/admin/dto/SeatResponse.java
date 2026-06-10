package com.irctc.admin.dto;

import com.irctc.admin.entity.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private Long trainId;
    private String seatNumber;
    private SeatType seatType;
    private Double price;
}
