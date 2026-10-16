package com.labourse.catalog.controller;

import com.labourse.catalog.dto.CategoryWithItemsDto;
import com.labourse.catalog.dto.ItemDto;
import com.labourse.catalog.entity.ServiceCategory;
import com.labourse.catalog.entity.ServiceItem;
import com.labourse.catalog.repository.ServiceCategoryRepository;
import com.labourse.catalog.repository.ServiceItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock ServiceCategoryRepository categoryRepository;
    @Mock ServiceItemRepository itemRepository;
    @InjectMocks CatalogController controller;

    private ServiceItem item(String code, String name, String cat, String group, int order) {
        return new ServiceItem(code, name, cat, group, order);
    }

    @Test
    void getCategoryItems_groupsItemsUnderTheirUiSubHeading_inSeededOrder() {
        when(categoryRepository.findById("HOME_REPAIR"))
                .thenReturn(Optional.of(new ServiceCategory("HOME_REPAIR", "🏠 Home Repair & Maintenance", 0)));
        when(itemRepository.findByCategoryCodeOrderByDisplayOrderAsc("HOME_REPAIR")).thenReturn(List.of(
                item("HOME_REPAIR__ELECTRICIAN", "Electrician", "HOME_REPAIR", "Electrical", 0),
                item("HOME_REPAIR__WIRING", "Wiring", "HOME_REPAIR", "Electrical", 1),
                item("HOME_REPAIR__PLUMBER", "Plumber", "HOME_REPAIR", "Plumbing", 2)
        ));

        CategoryWithItemsDto result = controller.getCategoryItems("HOME_REPAIR");

        // "Electrical" must come before "Plumbing" — the order items were seeded in, not alphabetical
        assertThat(result.getGroupedItems().keySet()).containsExactly("Electrical", "Plumbing");
        assertThat(result.getGroupedItems().get("Electrical")).extracting(ItemDto::getName)
                .containsExactly("Electrician", "Wiring");
    }

    @Test
    void getCategoryItems_throws_forUnknownCategory() {
        when(categoryRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getCategoryItems("NOPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown category");
    }

    @Test
    void getAllItems_filtersBySearchTerm_caseInsensitively() {
        when(itemRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(
                item("A__ELECTRICIAN", "Electrician", "A", "g", 0),
                item("A__PLUMBER", "Plumber", "A", "g", 1)
        ));

        assertThat(controller.getAllItems("ELEC")).extracting(ItemDto::getCode)
                .containsExactly("A__ELECTRICIAN");
    }

    @Test
    void itemExists_delegatesToRepository() {
        when(itemRepository.existsByCode("HOME_REPAIR__ELECTRICIAN")).thenReturn(true);
        when(itemRepository.existsByCode("FAKE")).thenReturn(false);

        assertThat(controller.itemExists("HOME_REPAIR__ELECTRICIAN")).isTrue();
        assertThat(controller.itemExists("FAKE")).isFalse();
    }
}
