package com.labourse.catalog.repository;

import com.labourse.catalog.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, String> {
    List<ServiceItem> findByCategoryCodeOrderByDisplayOrderAsc(String categoryCode);
    List<ServiceItem> findAllByOrderByDisplayOrderAsc();

    // Used by labour-service / job-service to validate a code actually exists before accepting it
    boolean existsByCode(String code);
}
