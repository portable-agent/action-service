package dev.portableagent.action.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.action.model.Action;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import tools.jackson.databind.json.JsonMapper;

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
    var repository = repository(db);
    var tenantId = UUID.randomUUID();
    var payload =
        Map.<String, Object>of("title", "Demo", "attendees", List.of("person@example.test"));
    var action =
        Action.create(
            tenantId,
            UUID.randomUUID(),
            "request-123",
            "calendar.create_event",
            "calendar",
            payload,
            "a".repeat(64),
            Instant.parse("2026-08-28T10:00:00Z"));

    assertThat(repository.saveIfMissing(action)).isTrue();

    var saved = repository.findById(tenantId, action.getId());
    assertThat(saved).isPresent();
    assertThat(saved.orElseThrow().getRequestKey()).isEqualTo("request-123");
    assertThat(saved.orElseThrow().getPayload()).isEqualTo(payload);
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

    assertThat(repository(db).findByRequestKey(tenantId, "same-request")).isPresent();
  }

  private boolean saveAfterStart(Action action, CountDownLatch start) throws Exception {
    start.await();
    try (var taskConnection =
        DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
      return repository(DSL.using(taskConnection, SQLDialect.POSTGRES)).saveIfMissing(action);
    }
  }

  private Action action(UUID tenantId, String requestKey) {
    return Action.create(
        tenantId,
        UUID.randomUUID(),
        requestKey,
        "calendar.create_event",
        "fake-calendar",
        Map.of("title", "Demo"),
        "a".repeat(64),
        Instant.parse("2026-08-28T10:00:00Z"));
  }

  private ActionRepository repository(DSLContext context) {
    return new ActionRepository(context, JsonMapper.builder().build());
  }
}
