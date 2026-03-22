package ru.practicum.shareit.item.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.comment.Comment;
import ru.practicum.shareit.item.comment.CommentMapper;
import ru.practicum.shareit.item.comment.CommentRepository;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    public ItemDto createItem(Long ownerId, ItemDto dto) {
        User owner = userService.getUserById(ownerId);
        Item item = ItemMapper.toItem(dto);
        item.setOwner(owner);
        Item savedItem = itemRepository.save(item);

        return ItemMapper.toDto(savedItem);
    }

    @Override
    public CommentDto addComment(Long userId, Long itemId, CommentCreateDto dto) {
        User user = userService.getUserById(userId);
        Item item = getItemById(itemId);
        LocalDateTime now = LocalDateTime.now();

        boolean hasBooking = bookingRepository.existsByItemIdAndBookerIdAndEndBefore(itemId, userId, now);

        if (!hasBooking) {
            throw new ValidationException("Невозможно оставить комментарий");
        }

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setAuthor(user);
        comment.setItem(item);
        comment.setCreated(now);

        Comment saved = commentRepository.save(comment);

        return CommentMapper.toDto(saved);
    }

    @Override
    public ItemDto updateItem(Long userId, Long itemId, ItemDto dto) {

        Item item = getItemById(itemId);

        if (!Objects.equals(userId, item.getOwner().getId())) {
            throw new AccessDeniedException("Пользователь не является владельцем");
        }
        if (dto.getName() != null) {
            item.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription());
        }
        if (dto.getAvailable() != null) {
            item.setAvailable(dto.getAvailable());
        }
        Item updatedItem = itemRepository.save(item);

        return ItemMapper.toDto(updatedItem);
    }

    @Override
    public Item getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Данная вещь не зарегистрирована в каталоге"));
    }

    @Override
    public ItemDto getAboutItem(Long itemId) {
        Item item = getItemById(itemId);
        ItemDto dto = ItemMapper.toDto(item);

        List<CommentDto> comments = commentRepository.findByItemIdOrderByCreatedAsc(itemId)
                .stream()
                .map(CommentMapper::toDto)
                .toList();

        dto.setComments(comments);

        return dto;
    }

    @Override
    public List<ItemDto> getUserItems(Long userId) {
        userService.getUserById(userId);
        List<Item> items = itemRepository.findByOwnerId(userId);
        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> {
                    ItemDto dto = ItemMapper.toDto(item);

                    Booking last = bookingRepository
                            .findByItemIdAndStartBeforeAndStatusOrderByStartDesc(
                                    item.getId(), now, BookingStatus.APPROVED)
                            .stream()
                            .findFirst()
                            .orElse(null);

                    Booking next = bookingRepository
                            .findByItemIdAndStartAfterAndStatusOrderByStartAsc(
                                    item.getId(), now, BookingStatus.APPROVED)
                            .stream()
                            .findFirst()
                            .orElse(null);

                    List<CommentDto> comments = commentRepository
                            .findByItemIdOrderByCreatedAsc(item.getId())
                            .stream()
                            .map(CommentMapper::toDto)
                            .toList();

                    dto.setLastBooking(BookingMapper.toShortDto(last));
                    dto.setNextBooking(BookingMapper.toShortDto(next));
                    dto.setComments(comments);

                    return dto;
                })
                .toList();
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<ItemDto> items = itemRepository.search(text).stream()
                .map(ItemMapper::toDto)
                .toList();

        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        return items;
    }
}