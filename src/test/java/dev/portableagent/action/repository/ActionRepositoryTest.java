package dev.portableagent.action.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.action.model.Action;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ActionRepositoryTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

  private static DSLContext db;
  private static Connection connection;

  @BeforeAll
  static void setUpDatabase() throws SQLException {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .load()
        .migrate();
    connection =
        DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    db = DSL.using(connection, SQLDialect.POSTGRES);
  }

  @AfterAll
  static void closeDatabase() throws SQLException {
    connection.close();
  }

  @Test
  void save_whenActionIsValid_shouldReadSameAction() {
    var repository = new ActionRepository(db);
    var tenantId = UUID.randomUUID();
    var action =
        Action.create(
            tenantId,
            UUID.randomUUID(),
            "request-123",
            "calendar.create_event",
            "calendar",
            "a".repeat(64),
            Instant.parse("2026-08-28T10:00:00Z"));

    repository.save(action);

    var saved = repository.findById(tenantId, action.getId());
    assertThat(saved).isPresent();
    assertThat(saved.orElseThrow().getRequestKey()).isEqualTo("request-123");
  }
}
