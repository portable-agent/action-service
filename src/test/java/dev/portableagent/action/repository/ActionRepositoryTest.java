package dev.portableagent.action.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.action.model.Action;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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

    assertThat(repository.saveIfMissing(action)).isTrue();

    var saved = repository.findById(tenantId, action.getId());
    assertThat(saved).isPresent();
    assertThat(saved.orElseThrow().getRequestKey()).isEqualTo("request-123");
  }

  @Test
  void saveIfMissing_whenRequestsRunTogether_shouldInsertOnce() throws Exception {
    var tenantId = UUID.randomUUID();
    var first = action(tenantId, "same-request");
    var second = action(tenantId, "same-request");
    var start = new CountDownLatch(1);

    try (var pool = Executors.newFixedThreadPool(2)) {
      var firstSave = pool.submit(() -> saveAfterStart(first, start));
      var secondSave = pool.submit(() -> saveAfterStart(second, start));
      start.countDown();

      assertThat(List.of(firstSave.get(), secondSave.get())).containsExactlyInAnyOrder(true, false);
    }

    assertThat(new ActionRepository(db).findByRequestKey(tenantId, "same-request")).isPresent();
  }

  private boolean saveAfterStart(Action action, CountDownLatch start) throws Exception {
    start.await();
    try (var taskConnection =
        DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
      return new ActionRepository(DSL.using(taskConnection, SQLDialect.POSTGRES))
          .saveIfMissing(action);
    }
  }

  private Action action(UUID tenantId, String requestKey) {
    return Action.create(
        tenantId,
        UUID.randomUUID(),
        requestKey,
        "calendar.create_event",
        "fake-calendar",
        "a".repeat(64),
        Instant.parse("2026-08-28T10:00:00Z"));
  }
}
