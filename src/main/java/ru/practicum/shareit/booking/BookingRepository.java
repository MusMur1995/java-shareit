package ru.practicum.shareit.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByBookerIdOrderByStartDesc(Long bookerId);


    List<Booking> findByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(
            Long bookerId, LocalDateTime start, LocalDateTime end
    );

    default List<Booking> findCurrentBookings(Long bookerId, LocalDateTime time) {
        return findByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(bookerId, time, time);
    }


    List<Booking> findByBookerIdAndEndBeforeOrderByStartDesc(Long bookerId, LocalDateTime time);

    List<Booking> findByBookerIdAndStartAfterOrderByStartAsc(Long bookerId, LocalDateTime time);

    List<Booking> findByBookerIdAndStatusOrderByStartDesc(Long bookerId, BookingStatus status);

    List<Booking> findByItemOwnerIdOrderByStartDesc(Long ownerId);


    List<Booking> findByItemOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(
            Long ownerId,
            LocalDateTime start,
            LocalDateTime end
    );

    default List<Booking> findCurrentBookingsByItemOwnerId(Long ownerId, LocalDateTime time) {
        return findByItemOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(
                ownerId, time, time
        );
    }


    List<Booking> findByItemOwnerIdAndEndBeforeOrderByStartDesc(Long ownerId, LocalDateTime time);

    List<Booking> findByItemOwnerIdAndStartAfterOrderByStartDesc(Long ownerId, LocalDateTime time);

    List<Booking> findByItemOwnerIdAndStatusOrderByStartDesc(Long ownerId, BookingStatus status);

    List<Booking> findByItemIdAndStartBeforeAndStatusOrderByStartDesc(
            Long itemId, LocalDateTime time, BookingStatus status
    );

    List<Booking> findByItemIdAndStartAfterAndStatusOrderByStartAsc(
            Long itemId, LocalDateTime time, BookingStatus status
    );

    boolean existsByItemIdAndBookerIdAndEndBefore(
            Long itemId, Long bookerId, LocalDateTime time
    );
}