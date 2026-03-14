package ru.practicum.shareit.item.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Не найден пользователь с id: " + userId));
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
    public Item getItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Не найден предмет с id: " + itemId));
    }

    @Override
    @Transactional
    public ItemDto createItem(Long userId, ItemDto itemDto) {
        User owner = findUserById(userId);
        Item item = ItemMapper.toEntity(itemDto, owner);
        Item createdItem = itemRepository.save(item);
        return ItemMapper.toDto(createdItem);
    }

    @Override
    public ItemDto updateItem(Long userId, Long itemId, ItemDto dto) {

        Item item = getItemById(itemId);

        if (!Objects.equals(userId, item.getOwner().getId())) {
            throw new AccessDeniedException("Пользователь не является владельцем вещи");
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
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return ItemMapper.toDto(itemRepository.search(text));
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
}