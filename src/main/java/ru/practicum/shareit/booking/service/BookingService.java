package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;

import java.util.List;

public interface BookingService {
    BookingDto createBooking(Long userId, BookingCreateDto bookingCreateDto);

    BookingDto approveBooking(Long userId, Long bookingId, Boolean approved);

    BookingDto getBookingById(Long userId, Long bookingId);

    List<BookingDto> getUserBookings(Long userId, BookingState state, Integer from, Integer size);

    List<BookingDto> getOwnerBookings(Long userId, BookingState state, Integer from, Integer size);
}
