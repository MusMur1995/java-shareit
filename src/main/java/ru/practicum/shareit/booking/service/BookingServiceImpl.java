package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    private static final Sort SORT_BY_START_DESC = Sort.by(Sort.Direction.DESC, "start");

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден с id: " + userId));
    }

    private Item findItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена с id: " + itemId));
    }

    private Booking findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено с id: " + bookingId));
    }

    private void validateBookingDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ValidationException("Даты начала и окончания должны быть указаны");
        }
        if (!start.isBefore(end)) {
            throw new ValidationException("Дата начала должна быть раньше даты окончания");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Дата начала не может быть в прошлом");
        }
    }

    private void checkItemAvailability(Item item, LocalDateTime start, LocalDateTime end) {
        if (!item.isAvailable()) {
            throw new UnavailableItemException("Вещь недоступна для бронирования");
        }

        List<Booking> existingBookings = bookingRepository.findByItemIdAndStatus(item.getId(),
                BookingStatus.APPROVED);

        boolean hasOverlap = existingBookings.stream()
                .anyMatch(booking ->
                        !(end.isBefore(booking.getStart()) || start.isAfter(booking.getEnd()))
                );

        if (hasOverlap) {
            throw new UnavailableItemException("Вещь уже забронирована на указанные даты");
        }
    }

    @Override
    @Transactional
    public BookingDto createBooking(Long userId, BookingCreateDto bookingCreateDto) {
        User booker = findUserById(userId);
        Item item = findItemById(bookingCreateDto.getItemId());

        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Владелец не может бронировать свою вещь");
        }

        validateBookingDates(bookingCreateDto.getStart(), bookingCreateDto.getEnd());
        checkItemAvailability(item, bookingCreateDto.getStart(), bookingCreateDto.getEnd());

        Booking booking = new Booking();
        booking.setStart(bookingCreateDto.getStart());
        booking.setEnd(bookingCreateDto.getEnd());
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        Booking savedBooking = bookingRepository.save(booking);
        return BookingMapper.toDto(savedBooking);
    }

    @Override
    @Transactional
    public BookingDto approveBooking(Long userId, Long bookingId, Boolean approved) {
        Booking booking = findBookingById(bookingId);

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Только владелец вещи может подтвердить или отклонить бронирование");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование уже обработано");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);

        Booking updatedBooking = bookingRepository.save(booking);
        return BookingMapper.toDto(updatedBooking);
    }

    @Override
    public BookingDto getBookingById(Long userId, Long bookingId) {
        Booking booking = findBookingById(bookingId);

        if (!booking.getBooker().getId().equals(userId) &&
                !booking.getItem().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("У вас нет доступа к этому бронированию");
        }

        return BookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getUserBookings(Long userId, BookingState state, Integer from, Integer size) {
        findUserById(userId);

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (state) {
            case ALL:
                bookings = bookingRepository.findByBookerId(userId, SORT_BY_START_DESC);
                break;
            case CURRENT:
                bookings = bookingRepository.findByBookerIdAndStartBeforeAndEndAfter(
                        userId, now, now, SORT_BY_START_DESC);
                break;
            case PAST:
                bookings = bookingRepository.findByBookerIdAndEndBefore(userId, now, SORT_BY_START_DESC);
                break;
            case FUTURE:
                bookings = bookingRepository.findByBookerIdAndStartAfter(userId, now, SORT_BY_START_DESC);
                break;
            case WAITING:
                bookings = bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.WAITING, SORT_BY_START_DESC);
                break;
            case REJECTED:
                bookings = bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.REJECTED, SORT_BY_START_DESC);
                break;
            default:
                throw new ValidationException("Неизвестное состояние: " + state);
        }

        List<Booking> pagedBookings = applyPagination(bookings, from, size);

        return BookingMapper.toDto(pagedBookings);
    }

    @Override
    public List<BookingDto> getOwnerBookings(Long userId, BookingState state, Integer from, Integer size) {
        findUserById(userId);

        List<Item> userItems = itemRepository.findByOwnerId(userId);
        if (userItems.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (state) {
            case ALL:
                bookings = bookingRepository.findByItemOwnerId(userId, SORT_BY_START_DESC);
                break;
            case CURRENT:
                bookings = bookingRepository.findCurrentByItemOwnerId(userId, now, SORT_BY_START_DESC);
                break;
            case PAST:
                bookings = bookingRepository.findPastByItemOwnerId(userId, now, SORT_BY_START_DESC);
                break;
            case FUTURE:
                bookings = bookingRepository.findFutureByItemOwnerId(userId, now, SORT_BY_START_DESC);
                break;
            case WAITING:
                bookings = bookingRepository.findByItemOwnerIdAndStatus(userId, BookingStatus.WAITING, SORT_BY_START_DESC);
                break;
            case REJECTED:
                bookings = bookingRepository.findByItemOwnerIdAndStatus(userId, BookingStatus.REJECTED, SORT_BY_START_DESC);
                break;
            default:
                throw new ValidationException("Неизвестное состояние: " + state);
        }

        List<Booking> pagedBookings = applyPagination(bookings, from, size);
        return BookingMapper.toDto(pagedBookings);
    }

    private List<Booking> applyPagination(List<Booking> bookings, Integer from, Integer size) {
        if (from == null || size == null) {
            return bookings;
        }

        if (from < 0 || size <= 0) {
            throw new ValidationException("Параметры пагинации должны быть положительными");
        }

        int toIndex = Math.min(from + size, bookings.size());
        if (from >= bookings.size()) {
            return List.of();
        }

        return bookings.subList(from, toIndex);
    }
}