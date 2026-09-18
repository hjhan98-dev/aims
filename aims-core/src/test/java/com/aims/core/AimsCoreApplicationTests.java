package com.aims.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Requires PostgreSQL running locally (see infra/docker-compose.yml) since
// the context boots a real DataSource and runs Flyway migrations on startup.
@SpringBootTest
class AimsCoreApplicationTests {

    @Test
    void contextLoads() {
    }
}
