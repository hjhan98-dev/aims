package com.aims.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 실제 DataSource로 컨텍스트를 띄우고 부팅 시 Flyway migration을 실행하므로
// 로컬에 PostgreSQL이 떠 있어야 함 (infra/docker-compose.yml 참고)
@SpringBootTest
class AimsCoreApplicationTests {

    @Test
    void contextLoads() {
    }
}
