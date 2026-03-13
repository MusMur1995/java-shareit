package ru.practicum.shareit.booking.mapper;

import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.user.mapper.UserMapper;

import java.util.List;
import java.util.stream.Collectors;

public class BookingMapper {

    public static BookingDto toDto(Booking booking) {
        if (booking == null) {
            return null;
        }
        return new BookingDto(
                booking.getId(),
                booking.getStart(),
                booking.getEnd(),
                booking.getItem().getId(),
                booking.getBooker().getId(),
                booking.getStatus()
        );
    }

    public static BookingResponseDto toResponseDto(Booking booking) {
        if (booking == null) {
            return null;
        }
        return new BookingResponseDto(
                booking.getId(),
                booking.getStart(),
                booking.getEnd(),
                ItemMapper.toDto(booking.getItem()),
                UserMapper.toDto(booking.getBooker()),
                booking.getStatus()
        );
    }

    public static BookingShortDto toShortDto(Booking booking) {
        if (booking == null) {
            return null;
        }
        return new BookingShortDto(
                booking.getId(),
                booking.getBooker().getId(),
                booking.getStart(),
                booking.getEnd()
        );
    }

    public static List<BookingDto> toDto(List<Booking> bookings) {
        if (bookings == null) {
            return List.of();
        }
        return bookings.stream()
                .map(BookingMapper::toDto)
                .collect(Collectors.toList());
    }

    public static List<BookingResponseDto> toResponseDto(List<Booking> bookings) {
        if (bookings == null) {
            return List.of();
        }
        return bookings.stream()
                .map(BookingMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}