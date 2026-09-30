package com.labourse.matching.service;

import com.labourse.matching.dto.JobMatchRequest;
import com.labourse.matching.dto.NearbyLabourDto;
import java.util.List;

public interface MatchingService {
    List<NearbyLabourDto> findMatchingLabour(JobMatchRequest job);
    void updateLabourLocation(String labourId, double lat, double lon);
    void setLabourAvailability(String labourId, boolean available);
    void setLabourKycVerified(String labourId, boolean verified);
}
