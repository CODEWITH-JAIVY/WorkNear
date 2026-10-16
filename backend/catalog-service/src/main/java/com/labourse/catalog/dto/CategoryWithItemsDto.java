package com.labourse.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;
import java.util.List;

// Category screen response — items pre-grouped by groupLabel so the app can render
// "Electrical / Plumbing / Carpenter ..." sections directly without client-side grouping logic
@Data
@AllArgsConstructor
public class CategoryWithItemsDto {
    private String categoryCode;
    private String categoryName;
    private Map<String, List<ItemDto>> groupedItems; // groupLabel -> items
}
