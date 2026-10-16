package com.labourse.labour.service;

import com.labourse.labour.client.CatalogServiceClient;
import com.labourse.labour.client.MatchingServiceClient;
import com.labourse.labour.dto.LabourProfileDto;
import com.labourse.labour.dto.LocationUpdateDto;
import com.labourse.labour.entity.Gender;
import com.labourse.labour.entity.LabourProfile;
import com.labourse.labour.repository.LabourProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabourProfileServiceTest {

    @Mock LabourProfileRepository repository;
    @Mock MatchingServiceClient matchingServiceClient;
    @Mock CatalogServiceClient catalogServiceClient;
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

    @Test
    void update_validatesCodesWithCatalogService_beforeSaving() {
        LabourProfile profile = new LabourProfile();
        profile.setUserId(3L);

        LabourProfileDto dto = new LabourProfileDto();
        dto.setName("Ramesh Kumar");
        dto.setLabourType("CLEANING");
        dto.setServiceCodes(List.of("CLEANING__KITCHEN_CLEANING", "CLEANING__BATHROOM_CLEANING"));
        dto.setGender(Gender.MALE);
        dto.setRate(400.0);

        when(repository.findByUserId(3L)).thenReturn(Optional.of(profile));
        when(repository.save(any(LabourProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        LabourProfile result = service.update(3L, dto);

        // Validation must happen before save — not after, and not skipped
        verify(catalogServiceClient).validateCodesOrThrow(dto.getServiceCodes());
        assertThat(result.getServiceCodesCsv()).isEqualTo("CLEANING__KITCHEN_CLEANING,CLEANING__BATHROOM_CLEANING");

        verify(matchingServiceClient).syncProfile(eq(3L), argThat(p ->
                "CLEANING__KITCHEN_CLEANING,CLEANING__BATHROOM_CLEANING".equals(p.getServiceCodesCsv())
                && p.getGender() == Gender.MALE
        ));
    }

    @Test
    void update_throwsAndNeverSaves_whenCatalogRejectsAnUnknownCode() {
        LabourProfile profile = new LabourProfile();
        profile.setUserId(4L);

        LabourProfileDto dto = new LabourProfileDto();
        dto.setServiceCodes(List.of("TOTALLY_MADE_UP_CODE"));

        when(repository.findByUserId(4L)).thenReturn(Optional.of(profile));
        doThrow(new IllegalArgumentException("Unknown service code(s): TOTALLY_MADE_UP_CODE"))
                .when(catalogServiceClient).validateCodesOrThrow(dto.getServiceCodes());

        assertThatThrownBy(() -> service.update(4L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown service code");

        // A typo'd code must never reach the DB or matching-service — that's the whole point
        verify(repository, never()).save(any());
        verifyNoInteractions(matchingServiceClient);
    }

    @Test
    void update_skipsCatalogValidation_whenNoServiceCodesProvided() {
        // Updating e.g. just the "about" text shouldn't force re-validating skills every time
        LabourProfile profile = new LabourProfile();
        profile.setUserId(5L);

        LabourProfileDto dto = new LabourProfileDto();
        dto.setAbout("Updated bio only");

        when(repository.findByUserId(5L)).thenReturn(Optional.of(profile));
        when(repository.save(any(LabourProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(5L, dto);

        verify(catalogServiceClient).validateCodesOrThrow(null);
    }

    @Test
    void getPublicProfile_exposesCustomerFacingFieldsAndSplitsCsvLists() {
        LabourProfile p = new LabourProfile();
        p.setUserId(9L);
        p.setName("Sita Devi");
        p.setServiceCodesCsv("CLEANING__KITCHEN_CLEANING, CLEANING__BATHROOM_CLEANING");
        p.setLanguagesCsv("HINDI,ENGLISH");
        p.setRating(4.6);
        p.setRatingCount(12);
        p.setKycVerified(true);
        p.setGender(Gender.FEMALE);
        p.setCurrentLat(28.5);
        p.setCurrentLon(77.3);
        when(repository.findByUserId(9L)).thenReturn(Optional.of(p));

        var dto = service.getPublicProfile(9L);

        assertThat(dto.serviceCodes()).containsExactly("CLEANING__KITCHEN_CLEANING", "CLEANING__BATHROOM_CLEANING");
        assertThat(dto.languages()).containsExactly("HINDI", "ENGLISH");
        assertThat(dto.rating()).isEqualTo(4.6);
        assertThat(dto.kycVerified()).isTrue();
        // No location or gender field exists on the public DTO at all — guard against it being added back
        assertThat(java.util.Arrays.stream(dto.getClass().getRecordComponents()).map(rc -> rc.getName()))
                .doesNotContain("currentLat", "currentLon", "gender", "latitude", "longitude");
    }

    @Test
    void getPublicProfile_returnsEmptyListsRatherThanNull_whenNothingSetYet() {
        LabourProfile p = new LabourProfile();
        p.setUserId(10L);
        when(repository.findByUserId(10L)).thenReturn(Optional.of(p));

        var dto = service.getPublicProfile(10L);

        assertThat(dto.serviceCodes()).isEmpty();
        assertThat(dto.languages()).isEmpty();
    }
}
