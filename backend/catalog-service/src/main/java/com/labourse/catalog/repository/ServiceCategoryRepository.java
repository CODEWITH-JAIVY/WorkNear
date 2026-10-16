package com.labourse.catalog.repository;

import com.labourse.catalog.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, String> {
    List<ServiceCategory> findAllByOrderByDisplayOrderAsc();
}
