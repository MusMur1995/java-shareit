package ru.practicum.shareit.booking.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.UnavailableItemException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;


    @Override
    @Transactional
    public BookingDto create(Long userId, BookingCreateDto dto) {

        User booker = userService.getUserById(userId);

        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Предмет с ID=%d не найден в каталоге", dto.getItemId())));

        if (!item.getAvailable()) {
            throw new UnavailableItemException(
                    String.format("Предмет с ID=%d временно недоступен для бронирования", item.getId()));
        }
        if (item.getOwner().getId().equals(userId)) {
            throw new ValidationException(String.format("Пользователь с ID=%d не может забронировать собственную вещь с ID=%d",
                    userId, item.getId()));
        }
        if (!dto.getEnd().isAfter(dto.getStart())) {
            throw new ValidationException(
                    String.format("Дата завершения бронирования (%s) должна быть позже даты начала (%s)",
                            dto.getEnd(), dto.getStart()));
        }

        Booking booking = BookingMapper.toEntity(dto);

        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        bookingRepository.save(booking);

        return BookingMapper.toDto(booking);
    }

    @Override
    public BookingDto approveBooking(Long bookingId, Long userId, Boolean approved) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Запрос на бронирование с ID=%d не найден", bookingId)));

        if (!Objects.equals(booking.getItem().getOwner().getId(), userId)) {
            throw new AccessDeniedException(
                    String.format("Пользователь с ID=%d не является владельцем вещи с ID=%d. " +
                                    "Только владелец может подтверждать или отклонять бронирование",
                            userId, booking.getItem().getId()));
        }

        userService.getUserById(userId);

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException(
                    String.format("Невозможно изменить статус бронирования с ID=%d - текущий статус: %s",
                            bookingId, booking.getStatus()));
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        bookingRepository.save(booking);

        return BookingMapper.toDto(booking);
    }

    @Override
    public BookingDto getAboutBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Информация о бронировании с ID=%d отсутствует", bookingId)));

        if (!Objects.equals(booking.getItem().getOwner().getId(), userId)
                && !Objects.equals(booking.getBooker().getId(), userId)) {
            throw new AccessDeniedException(
                    String.format("Пользователь с ID=%d не имеет доступа к просмотру бронирования с ID=%d. " +
                                    "Доступ только для владельца вещи (ID=%d) или автора запроса (ID=%d)",
                            userId, bookingId, booking.getItem().getOwner().getId(), booking.getBooker().getId()));
        }

        return BookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> findBookingsByUserId(Long userId, BookingState state) {
        userService.getUserById(userId);

        List<Booking> bookings;
        LocalDateTime now = LocalDateTime.now();

        switch (state) {
            case ALL -> bookings = bookingRepository.findByBookerIdOrderByStartDesc(userId);
            case CURRENT -> bookings = bookingRepository.findCurrentBookings(userId, now);
            case PAST -> bookings = bookingRepository.findByBookerIdAndEndBeforeOrderByStartDesc(userId, now);
            case FUTURE -> bookings = bookingRepository.findByBookerIdAndStartAfterOrderByStartAsc(userId, now);
            case WAITING -> bookings =
                    bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
            case REJECTED -> bookings =
                    bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);

            default -> throw new ValidationException(
                    String.format("Передан неподдерживаемый статус: '%s'. Допустимые значения: ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED",
                            state));
        }

        return BookingMapper.toListDto(bookings);
    }

    @Override
    public List<BookingDto> findBookingsByItemOwnerId(Long userId, BookingState state) {
        userService.getUserById(userId);

        List<Booking> bookings;
        LocalDateTime now = LocalDateTime.now();

        switch (state) {
            case ALL -> bookings = bookingRepository.findByItemOwnerIdOrderByStartDesc(userId);
            case CURRENT -> bookings =
                    bookingRepository.findCurrentBookingsByItemOwnerId(userId, now);
            case PAST -> bookings =
                    bookingRepository.findByItemOwnerIdAndEndBeforeOrderByStartDesc(userId, now);
            case FUTURE -> bookings =
                    bookingRepository.findByItemOwnerIdAndStartAfterOrderByStartDesc(userId, now);
            case WAITING -> bookings =
                    bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
            case REJECTED -> bookings =
                    bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);

            default -> throw new ValidationException(
                    String.format("Передан неподдерживаемый статус: '%s'. Допустимые значения: ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED",
                            state));
        }

        return BookingMapper.toListDto(bookings);
    }
}