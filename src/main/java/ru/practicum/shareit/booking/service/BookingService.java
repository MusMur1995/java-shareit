package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;

import java.util.List;

public interface BookingService {
    BookingDto create(Long userId, BookingCreateDto dto);

    BookingDto approveBooking(Long bookingId, Long userId, Boolean approved);

    BookingDto getAboutBooking(Long userId, Long bookingId);

    List<BookingDto> findBookingsByUserId(Long userId, BookingState state);

    List<BookingDto> findBookingsByItemOwnerId(Long userId, BookingState state);
}
