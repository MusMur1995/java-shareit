package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    private Item findItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Не найден предмет с id: " + itemId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Не найден пользователь с id: " + userId));
    }

    @Override
    public List<ItemDto> getUserItems(Long userId) {
        findUserById(userId);

        List<Item> items = itemRepository.findByOwnerId(userId);
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .collect(Collectors.toList());

        Map<Long, List<CommentDto>> commentsByItemId = getCommentsForItems(itemIds);

        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> {
                    ItemDto itemDto = ItemMapper.toDto(item);

                    addBookingDatesToItemDto(itemDto, item.getId(), now);

                    itemDto.setComments(commentsByItemId.getOrDefault(item.getId(), Collections.emptyList()));

                    return itemDto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ItemDto getItemById(Long userId, Long itemId) {
        Item item = findItemById(itemId);
        ItemDto itemDto = ItemMapper.toDto(item);

        LocalDateTime now = LocalDateTime.now();

        if (userId != null && item.getOwner().getId().equals(userId)) {
            addBookingDatesToItemDto(itemDto, itemId, now);
        }

        List<Comment> comments = commentRepository.findByItemId(itemId);
        itemDto.setComments(CommentMapper.toDto(comments));

        return itemDto;
    }

    private void addBookingDatesToItemDto(ItemDto itemDto, Long itemId, LocalDateTime now) {
        bookingRepository.findFirstByItemIdAndStatusAndStartBeforeOrderByStartDesc(
                        itemId, BookingStatus.APPROVED, now)
                .ifPresent(booking -> itemDto.setLastBooking(BookingMapper.toShortDto(booking)));

        bookingRepository.findFirstByItemIdAndStatusAndStartAfterOrderByStartAsc(
                        itemId, BookingStatus.APPROVED, now)
                .ifPresent(booking -> itemDto.setNextBooking(BookingMapper.toShortDto(booking)));
    }

    private Map<Long, List<CommentDto>> getCommentsForItems(List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Comment> comments = commentRepository.findByItemIdIn(itemIds);

        return comments.stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toDto, Collectors.toList())
                ));
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
    @Transactional
    public ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto) {
        Item item = findItemById(itemId);

        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Пользователь не является владельцем этой вещи");
        }

        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            item.setName(itemDto.getName());
        }

        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            item.setDescription(itemDto.getDescription());
        }

        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        Item itemUpdated = itemRepository.save(item);
        return ItemMapper.toDto(itemUpdated);
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return ItemMapper.toDto(itemRepository.search(text));
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentCreateDto commentCreateDto) {
        User author = findUserById(userId);
        Item item = findItemById(itemId);

        LocalDateTime now = LocalDateTime.now();
        boolean hasBookedAndFinished = bookingRepository
                .existsByBookerIdAndItemIdAndStatusAndEndBefore(
                        userId, itemId, BookingStatus.APPROVED, now);

        if (!hasBookedAndFinished) {
            throw new ValidationException("Пользователь может оставить комментарий только после завершения аренды вещи");
        }

        Comment comment = new Comment();
        comment.setText(commentCreateDto.getText());
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(now);

        Comment savedComment = commentRepository.save(comment);
        return CommentMapper.toDto(savedComment);
    }
}