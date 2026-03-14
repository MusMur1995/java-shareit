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
                .orElseThrow(() -> new NotFoundException("Данная вещь не зарегистрирована в каталоге"));

        if (!item.getAvailable()) {
            throw new IllegalStateException("Вещь недоступна для бронирования");
        }
        if (item.getOwner().getId().equals(userId)) {
            throw new jakarta.validation.ValidationException("Владелец не может бронировать свою вещь");
        }
        if (!dto.getEnd().isAfter(dto.getStart())) {
            throw new ValidationException("Дата окончания должна быть позже даты начала");
        }

        Booking booking = BookingMapper.toEntity(dto);

        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        bookingRepository.save(booking);

        return BookingMapper.toDto(booking);
    }

    @Override
    public BookingDto confirmBooking(Long bookingId, Long userId, Boolean approved) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не обнаружено"));

        if (!Objects.equals(booking.getItem().getOwner().getId(), userId)) {
            throw new AccessDeniedException("Подтвердить или отклонить бронирование может только владелец");
        }

        userService.getUserById(userId);

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new IllegalStateException("Бронирование уже обработано");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        bookingRepository.save(booking);

        return BookingMapper.toDto(booking);
    }

    @Override
    public BookingDto getAboutBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не обнаружено"));

        if (!Objects.equals(booking.getItem().getOwner().getId(), userId)
                && !Objects.equals(booking.getBooker().getId(), userId)) {
            throw new AccessDeniedException("Запрос возможен только для владельца или арендатора");
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

            default -> throw new IllegalArgumentException("Некорректный параметр состояния бронирования");
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

            default -> throw new IllegalArgumentException("Некорректный параметр состояния бронирования");
        }

        return BookingMapper.toListDto(bookings);
    }
}