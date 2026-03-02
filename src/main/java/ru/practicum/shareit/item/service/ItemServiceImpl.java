package ru.practicum.shareit.item.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.InMemoryUserRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final InMemoryUserRepository userRepository;

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
        return ItemMapper.toDto(itemRepository.findByOwnerId(userId));
    }

    @Override
    public ItemDto getItemById(Long itemId) {
        Item item = findItemById(itemId);
        return ItemMapper.toDto(item);
    }

    @Override
    public ItemDto createItem(Long userId, ItemDto itemDto) {
        User owner = findUserById(userId);
        Item item = ItemMapper.toEntity(itemDto, owner);
        Item createdItem = itemRepository.save(item);
        return ItemMapper.toDto(createdItem);
    }

    @Override
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

        Item itemUpdated = itemRepository.update(item);

        return ItemMapper.toDto(itemUpdated);
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        return ItemMapper.toDto(itemRepository.search(text));
    }
}