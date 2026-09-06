package com.example.evshare;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FlywayMigrationIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Flyway bean should be injected and configured")
    void testFlywayInjected() {
        assertNotNull(flyway, "Flyway bean must be present in Spring ApplicationContext");
    }

    @Test
    @DisplayName("Flyway schema version should be at version 7 with all migrations applied")
    void testFlywayMigrationsApplied() {
        MigrationInfo current = flyway.info().current();
        assertNotNull(current, "Current Flyway migration info must not be null");
        assertEquals("7", current.getVersion().getVersion(), "Current database version should be 7");
        assertTrue(current.getState().isApplied(), "Current migration must be in APPLIED state");

        MigrationInfo[] applied = flyway.info().applied();
        assertEquals(7, applied.length, "Exactly 7 migration scripts should be applied");

        for (MigrationInfo info : applied) {
            assertTrue(info.getState().isApplied(), "Migration " + info.getScript() + " must be applied");
            assertNotNull(info.getInstalledOn(), "Installation timestamp must be present");
        }
    }
}
