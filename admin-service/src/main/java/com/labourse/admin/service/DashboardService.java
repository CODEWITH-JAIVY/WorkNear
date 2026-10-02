package com.labourse.admin.service;

import com.labourse.admin.dto.AdminDtos.DashboardResponse;
import com.labourse.admin.entity.DisputeStatus;
import com.labourse.admin.entity.RefundStatus;
import com.labourse.admin.entity.StaffStatus;
import com.labourse.admin.repository.DisputeRepository;
import com.labourse.admin.repository.RefundRequestRepository;
import com.labourse.admin.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DisputeRepository disputeRepository;
    private final RefundRequestRepository refundRepository;
    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (DisputeStatus s : DisputeStatus.values()) {
            byStatus.put(s.name(), disputeRepository.countByStatus(s));
        }
        return new DashboardResponse(byStatus,
                refundRepository.countByStatus(RefundStatus.PENDING),
                staffRepository.countByStatus(StaffStatus.ACTIVE));
    }
}
