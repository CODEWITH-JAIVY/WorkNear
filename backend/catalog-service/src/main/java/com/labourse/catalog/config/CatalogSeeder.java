package com.labourse.catalog.config;

import com.labourse.catalog.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

// Runs catalog-seed.sql on every startup. The seed file uses INSERT IGNORE on each row's natural
// key (code), so re-running it after the first time is a safe no-op — this is NOT a Flyway
// migration, just idempotent startup seeding appropriate for mostly-static reference data.
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogSeeder implements ApplicationRunner {

    private final DataSource dataSource;
    private final ServiceCategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/catalog-seed.sql"));
        }
        log.info("Catalog seeded: {} categories now in service_category", categoryRepository.count());
    }
}
