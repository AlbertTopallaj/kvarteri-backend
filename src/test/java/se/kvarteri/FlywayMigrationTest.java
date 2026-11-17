package se.kvarteri;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Boots with Flyway enabled and Hibernate validating: fails if migrations and entities drift apart. */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:flywaytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class FlywayMigrationTest {

    @Test
    void migrationsMatchEntities() {
    }
}
