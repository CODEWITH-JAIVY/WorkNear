package com.labourse.customer.service;

import com.labourse.customer.dto.CustomerProfileDto;
import com.labourse.customer.entity.CustomerProfile;
import com.labourse.customer.repository.CustomerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    @Mock CustomerProfileRepository repository;
    @InjectMocks CustomerProfileService service;

    @Test
    void getByUserId_throws_whenProfileShellNotYetCreated() {
        when(repository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByUserId(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void update_overwritesOnlyProvidedFields() {
        CustomerProfile existing = new CustomerProfile();
        existing.setUserId(1L);
        existing.setName("Old Name");

        CustomerProfileDto dto = new CustomerProfileDto();
        dto.setName("New Name");
        dto.setCity("Noida");
        dto.setPincode("201301");

        when(repository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(CustomerProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerProfile result = service.update(1L, dto);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getCity()).isEqualTo("Noida");
        assertThat(result.getPincode()).isEqualTo("201301");
    }
}
