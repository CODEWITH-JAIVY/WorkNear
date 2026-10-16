package com.labourse.catalog.controller;

import com.labourse.catalog.dto.CategoryWithItemsDto;
import com.labourse.catalog.dto.ItemDto;
import com.labourse.catalog.entity.ServiceCategory;
import com.labourse.catalog.entity.ServiceItem;
import com.labourse.catalog.repository.ServiceCategoryRepository;
import com.labourse.catalog.repository.ServiceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceItemRepository itemRepository;

    // "What service do you need?" screen — the 25-category grid
    @GetMapping("/categories")
    public List<ServiceCategory> getCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc();
    }

    // Tapping a category — items grouped by their UI sub-heading (e.g. "Electrical", "Plumbing")
    @GetMapping("/categories/{categoryCode}/items")
    public CategoryWithItemsDto getCategoryItems(@PathVariable String categoryCode) {
        ServiceCategory category = categoryRepository.findById(categoryCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + categoryCode));

        List<ServiceItem> items = itemRepository.findByCategoryCodeOrderByDisplayOrderAsc(categoryCode);

        // LinkedHashMap preserves displayOrder-derived group sequence — first group encountered
        // stays first, matching the order items were seeded in rather than re-sorting alphabetically
        Map<String, List<ItemDto>> grouped = new LinkedHashMap<>();
        for (ServiceItem item : items) {
            grouped.computeIfAbsent(item.getGroupLabel(), k -> new java.util.ArrayList<>())
                    .add(new ItemDto(item.getCode(), item.getName()));
        }

        return new CategoryWithItemsDto(category.getCode(), category.getName(), grouped);
    }

    // Flat list — used by labour-service's profile-setup search/autocomplete across ALL 336 items
    @GetMapping("/items")
    public List<ItemDto> getAllItems(@RequestParam(required = false) String search) {
        List<ServiceItem> items = itemRepository.findAllByOrderByDisplayOrderAsc();
        return items.stream()
                .filter(i -> search == null || i.getName().toLowerCase().contains(search.toLowerCase()))
                .map(i -> new ItemDto(i.getCode(), i.getName()))
                .collect(Collectors.toList());
    }

    // Internal — labour-service/job-service call this to validate a code before accepting it
    @GetMapping("/items/{code}/exists")
    public boolean itemExists(@PathVariable String code) {
        return itemRepository.existsByCode(code);
    }
}
