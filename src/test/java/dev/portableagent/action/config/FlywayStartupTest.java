package dev.portableagent.action.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

import dev.portableagent.action.model.ActionDecision;
import dev.portableagent.action.model.ActionStatus;
import dev.portableagent.action.model.OutboxItem;
import dev.portableagent.action.model.OutboxType;
import dev.portableagent.action.repository.ActionRepository;
import dev.portableagent.action.repository.OutboxRepository;
import dev.portableagent.action.service.ActionService;
import dev.portableagent.action.service.CreateActionCommand;
import dev.portableagent.action.service.DecideActionCommand;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        properties = {
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/not-used",
            "action.outbox.enabled=false"
        })
@Testcontainers(disabledWithoutDocker = true)
class FlywayStartupTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private DSLContext db;

    @Autowired
    private ActionService actionService;

    @Autowired
    private ActionRepository actionRepository;

    @MockitoSpyBean
    private OutboxRepository outboxRepository;

    @Test
    void application_whenStarted_shouldRunFlywayMigrations() {
        assertThat(table("action_proposals")).isEqualTo("action_proposals");
        assertThat(table("action_dispatch_outbox")).isEqualTo("action_dispatch_outbox");
        assertThat(column("action_proposals", "result")).isEqualTo("result");
        assertThat(column("action_dispatch_outbox", "event_type")).isEqualTo("event_type");
        assertThat(column("action_dispatch_outbox", "decision")).isEqualTo("decision");
        assertThat(column("action_dispatch_outbox", "payload_hash")).isEqualTo("payload_hash");
        assertThat(column("action_dispatch_outbox", "next_attempt_at")).isEqualTo("next_attempt_at");
    }

    @Test
    void decide_whenOutboxSaveFails_shouldRollbackStatusAndThenSaveBoth() {
        var tenantId = UUID.randomUUID();
        var action = actionService.create(
                tenantId,
                UUID.randomUUID(),
                new CreateActionCommand(
                        "calendar.create_event",
                        "fake-calendar",
                        Map.of(
                                "title", "Atomic outbox",
                                "startAt", "2026-09-01T12:00:00+03:00",
                                "endAt", "2026-09-01T12:30:00+03:00",
                                "timeZone", "Europe/Moscow"),
                        "atomic-outbox-request"));
        var command = new DecideActionCommand(ActionDecision.CONFIRM, action.getPayloadHash());
        doThrow(new IllegalStateException("Outbox is not available"))
                .when(outboxRepository)
                .save(argThat(item -> item.type() == OutboxType.DECISION));

        assertThatThrownBy(() -> actionService.decide(tenantId, action.getId(), command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Outbox is not available");
        assertThat(actionRepository
                        .findById(tenantId, action.getId())
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(ActionStatus.AWAITING_APPROVAL);

        doCallRealMethod().when(outboxRepository).save(any(OutboxItem.class));
        actionService.decide(tenantId, action.getId(), command);

        assertThat(actionRepository
                        .findById(tenantId, action.getId())
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(ActionStatus.APPROVED);
        assertThat(outboxRepository.findPending(100, java.time.Instant.now()).stream()
                        .filter(item -> item.actionId().equals(action.getId()))
                        .map(OutboxItem::type))
                .containsExactly(OutboxType.START, OutboxType.DECISION);
    }

    private String table(String name) {
        return db.fetchOne("select to_regclass(?)", "public." + name).get(0, String.class);
    }

    private String column(String table, String column) {
        return db.fetchOne(
                        "select column_name from information_schema.columns where table_schema = 'public' and table_name = ? and column_name = ?",
                        table,
                        column)
                .get(0, String.class);
    }
}
