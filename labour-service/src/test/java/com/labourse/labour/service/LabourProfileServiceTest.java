package com.labourse.labour.service;

import com.labourse.labour.client.MatchingServiceClient;
import com.labourse.labour.dto.LocationUpdateDto;
import com.labourse.labour.entity.LabourProfile;
import com.labourse.labour.repository.LabourProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabourProfileServiceTest {

    @Mock LabourProfileRepository repository;
    @Mock MatchingServiceClient matchingServiceClient;
    @InjectMocks LabourProfileService service;

    @Test
    void updateLocation_savesLocallyAndPushesToMatchingService() {
        LabourProfile profile = new LabourProfile();
        profile.setUserId(1L);

        LocationUpdateDto dto = new LocationUpdateDto();
        dto.setLat(28.5);
        dto.setLon(77.3);

        when(repository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(repository.save(any(LabourProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateLocation(1L, dto);

        // Both must happen — local DB is the source of truth, Redis (via matching-service) is
        // the fast-lookup cache. If only one updates, matching-service silently uses stale location.
        verify(repository).save(argThat(p -> p.getCurrentLat() == 28.5 && p.getCurrentLon() == 77.3));
        verify(matchingServiceClient).updateLocation(1L, 28.5, 77.3);
    }

    @Test
    void setAvailability_syncsBothLocalFlagAndMatchingServiceCache() {
        LabourProfile profile = new LabourProfile();
        profile.setUserId(2L);

        when(repository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(repository.save(any(LabourProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setAvailability(2L, true);

        assertThat(profile.isAvailableToday()).isTrue();
        verify(matchingServiceClient).setAvailability(2L, true);
    }
}
