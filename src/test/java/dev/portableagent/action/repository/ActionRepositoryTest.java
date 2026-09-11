package dev.portableagent.action.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.portableagent.action.exception.ActionChanged;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionDecision;
import dev.portableagent.action.model.ActionStatus;
import dev.portableagent.action.model.OutboxItem;
import dev.portableagent.action.model.OutboxType;
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
        connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
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
        var payload = Map.<String, Object>of("title", "Demo", "attendees", List.of("person@example.test"));
        var action = Action.create(
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
    void update_whenActionSucceeded_shouldStoreResult() {
        var repository = repository(db);
        var tenantId = UUID.randomUUID();
        var action = action(tenantId, "result-request");
        assertThat(repository.saveIfMissing(action)).isTrue();
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), Instant.parse("2026-08-28T10:00:01Z"));
        repository.update(action);
        action.startExecution(Instant.parse("2026-08-28T10:00:02Z"));
        repository.update(action);
        action.succeed("event-123", Instant.parse("2026-08-28T10:00:03Z"));

        repository.update(action);

        var saved = repository.findById(tenantId, action.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ActionStatus.SUCCEEDED);
        assertThat(saved.getResult().eventId()).isEqualTo("event-123");
    }

    @Test
    void update_whenActionFailed_shouldStoreFailedStateWithoutResult() {
        var repository = repository(db);
        var action = action(UUID.randomUUID(), "failed-request");
        assertThat(repository.saveIfMissing(action)).isTrue();
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), Instant.parse("2026-08-28T10:00:01Z"));
        repository.update(action);
        action.startExecution(Instant.parse("2026-08-28T10:00:02Z"));
        repository.update(action);
        action.fail(Instant.parse("2026-08-28T10:00:03Z"));

        repository.update(action);

        var saved = repository.findById(action.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ActionStatus.FAILED);
        assertThat(saved.getResult()).isNull();
    }

    @Test
    void update_whenVersionChanged_shouldRejectOldAction() {
        var repository = repository(db);
        var tenantId = UUID.randomUUID();
        var action = action(tenantId, "version-request");
        assertThat(repository.saveIfMissing(action)).isTrue();
        var first = repository.findById(tenantId, action.getId()).orElseThrow();
        var second = repository.findById(tenantId, action.getId()).orElseThrow();
        first.applyDecision(ActionDecision.CONFIRM, first.getPayloadHash(), Instant.parse("2026-08-28T10:00:01Z"));
        second.applyDecision(ActionDecision.CONFIRM, second.getPayloadHash(), Instant.parse("2026-08-28T10:00:01Z"));
        repository.update(first);

        assertThatThrownBy(() -> repository.update(second))
                .isInstanceOf(ActionChanged.class)
                .hasMessageContaining(action.getId().toString());
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

    @Test
    void outbox_whenStartIsRepeated_shouldKeepOneStartAndOneDecision() {
        var actionRepository = repository(db);
        var outboxRepository = new OutboxRepository(db);
        var action = action(UUID.randomUUID(), "outbox-events");
        var now = Instant.parse("2026-08-28T10:00:00Z");
        assertThat(actionRepository.saveIfMissing(action)).isTrue();

        outboxRepository.save(OutboxItem.start(action.getId(), now));
        outboxRepository.save(OutboxItem.start(action.getId(), now.plusSeconds(1)));
        outboxRepository.save(
                OutboxItem.decision(action.getId(), "CONFIRM", action.getPayloadHash(), now.plusSeconds(2)));

        var events = outboxRepository.findPending(100, now.plusSeconds(3)).stream()
                .filter(item -> item.actionId().equals(action.getId()))
                .toList();
        assertThat(events).hasSize(2);
        assertThat(events).extracting(OutboxItem::type).containsExactly(OutboxType.START, OutboxType.DECISION);
        assertThat(events.get(1).decision()).isEqualTo("CONFIRM");
        assertThat(events.get(1).payloadHash()).isEqualTo(action.getPayloadHash());
    }

    private boolean saveAfterStart(Action action, CountDownLatch start) throws Exception {
        start.await();
        try (var taskConnection =
                DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
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
