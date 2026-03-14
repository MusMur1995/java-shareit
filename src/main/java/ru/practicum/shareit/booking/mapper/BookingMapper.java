package ru.practicum.shareit.booking.mapper;

import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.user.mapper.UserMapper;

import java.util.List;
import java.util.stream.Collectors;

public class BookingMapper {

    public static BookingDto toDto(Booking booking) {
        return BookingDto.builder()
                .id(booking.getId())
                .start(booking.getStart())
                .end(booking.getEnd())
                .status(booking.getStatus())
                .item(ItemMapper.toShortDto(booking.getItem()))
                .booker(UserMapper.toShortDto(booking.getBooker()))
                .build();
    }

    public static BookingShortDto toShortDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        return BookingShortDto.builder()
                .id(booking.getId())
                .bookerId(booking.getBooker().getId())
                .build();
    }

    public static List<BookingDto> toDto(List<Booking> bookings) {
        return bookings.stream()
                .map(BookingMapper::toDto)
                .collect(Collectors.toList());
    }
}