package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ItemShortDto {
    private Long id;

    @NotBlank(message = "Наименование товара не может быть пустым")
    private String name;
}
