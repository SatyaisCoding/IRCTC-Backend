package com.irctc.booking.service;

import com.irctc.booking.dto.BookingRequest;
import com.irctc.booking.dto.BookingResponse;

import java.util.List;

public interface BookingService {
    BookingResponse createBooking(BookingRequest request);
    BookingResponse getBookingById(Long id);
    List<BookingResponse> getBookingsByUserId(Long userId);
    BookingResponse cancelBooking(Long id);
}
