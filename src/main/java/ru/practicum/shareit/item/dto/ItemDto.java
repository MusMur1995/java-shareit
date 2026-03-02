package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * TODO Sprint add-controllers.
 */
@Data
@AllArgsConstructor
public class ItemDto {
    private Long id;

    @NotBlank(message = "Наименование товара не может быть пустым")
    private String name;

    @NotBlank(message = "Описание товара не может быть пустым")
    private String description;

    @NotNull(message = "Статус товара должен быть указан")
    private Boolean available;
    private Long requestId;
}