package com.irctc.booking.service.impl;

import com.irctc.booking.dto.BookingRequest;
import com.irctc.booking.dto.BookingResponse;
import com.irctc.booking.entity.Booking;
import com.irctc.booking.entity.BookingStatus;
import com.irctc.booking.exception.BookingException;
import com.irctc.booking.repository.BookingRepository;
import com.irctc.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final Random random = new Random();

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        // Simple mock of booking logic
        String generatedSeat = "Coach-S" + (random.nextInt(10) + 1) + "/Seat-" + (random.nextInt(72) + 1);

        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .trainId(request.getTrainId())
                .passengerName(request.getPassengerName())
                .journeyDate(request.getJourneyDate())
                .seatNumber(generatedSeat)
                .bookingStatus(BookingStatus.CONFIRMED) // Defaulting to confirmed for demo
                .fare(request.getFare())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        return mapToResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingException("Booking not found with ID: " + id, "BOOKING_NOT_FOUND"));
        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingException("Booking not found with ID: " + id, "BOOKING_NOT_FOUND"));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled", "ALREADY_CANCELLED");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        Booking updatedBooking = bookingRepository.save(booking);
        return mapToResponse(updatedBooking);
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .trainId(booking.getTrainId())
                .passengerName(booking.getPassengerName())
                .journeyDate(booking.getJourneyDate())
                .seatNumber(booking.getSeatNumber())
                .bookingStatus(booking.getBookingStatus())
                .fare(booking.getFare())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
