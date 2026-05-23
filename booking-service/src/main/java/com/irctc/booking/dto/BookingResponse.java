package com.irctc.booking.dto;

import com.irctc.booking.entity.BookingStatus;
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
public class BookingResponse {
    private Long id;
    private Long userId;
    private Long trainId;
    private String passengerName;
    private LocalDate journeyDate;
    private String seatNumber;
    private BookingStatus bookingStatus;
    private Double fare;
    private LocalDateTime createdAt;
}
