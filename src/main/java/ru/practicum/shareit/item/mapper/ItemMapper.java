package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.stream.Collectors;

public class ItemMapper {

    public static ItemDto toDto(Item item) {
        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setAvailable(item.isAvailable());

        if (item.getRequest() != null) {
            dto.setRequestId(item.getRequest().getId());
        }

        return dto;
    }

    public static Item toEntity(ItemDto itemDto, User owner) {
        Item item = new Item();
        item.setName(itemDto.getName());
        item.setDescription(itemDto.getDescription());
        item.setAvailable(itemDto.getAvailable() != null ? itemDto.getAvailable() : true);
        item.setOwner(owner);
        return item;
    }

    public static List<ItemDto> toDto(List<Item> items) {
        return items.stream()
                .map(ItemMapper::toDto)
                .collect(Collectors.toList());
    }

    public static ItemShortDto toShortDto(Item item) {
        return new ItemShortDto(item.getId(), item.getName());
    }
}