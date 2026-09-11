package dev.portableagent.action.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.portableagent.action.exception.ActionChanged;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionDecision;
import dev.portableagent.action.model.ActionStatus;
import dev.portableagent.action.model.OutboxItem;
import dev.portableagent.action.model.OutboxType;
import dev.portableagent.action.repository.ActionRepository;
import dev.portableagent.action.repository.OutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {
    @Mock
    ActionRepository actionRepository;

    @Mock
    OutboxRepository outboxRepository;

    @Mock
    PayloadHash payloadHash;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);
    private ActionService service;

    @BeforeEach
    void setUp() {
        service = new ActionService(actionRepository, outboxRepository, payloadHash, clock);
    }

    @Test
    void create_whenRequestIsNew_shouldSaveActionAndOutbox() {
        var tenantId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var request = new CreateActionCommand(
                "calendar.create_event", "fake-calendar", Map.of("title", "Demo"), "request-123");
        when(actionRepository.findByRequestKey(tenantId, request.requestKey())).thenReturn(Optional.empty());
        when(payloadHash.make(request.payload())).thenReturn("a".repeat(64));
        when(actionRepository.saveIfMissing(any(Action.class))).thenReturn(true);

        var result = service.create(tenantId, userId, request);

        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getPayloadHash()).isEqualTo("a".repeat(64));
        verify(actionRepository).saveIfMissing(result);
        var outbox = ArgumentCaptor.forClass(OutboxItem.class);
        verify(outboxRepository).save(outbox.capture());
        assertThat(outbox.getValue().type()).isEqualTo(OutboxType.START);
    }

    @Test
    void create_whenAnotherRequestSavesFirst_shouldReturnSavedAction() {
        var tenantId = UUID.randomUUID();
        var oldAction = Action.create(
                tenantId,
                UUID.randomUUID(),
                "request-123",
                "calendar.create_event",
                "fake-calendar",
                Map.of("title", "Demo"),
                "a".repeat(64),
                clock.instant());
        var request = new CreateActionCommand(
                "calendar.create_event", "fake-calendar", Map.of("title", "Demo"), "request-123");
        when(actionRepository.findByRequestKey(tenantId, request.requestKey()))
                .thenReturn(Optional.empty(), Optional.of(oldAction));
        when(payloadHash.make(request.payload())).thenReturn("a".repeat(64));
        when(actionRepository.saveIfMissing(any(Action.class))).thenReturn(false);

        assertThat(service.create(tenantId, UUID.randomUUID(), request)).isSameAs(oldAction);
        verifyNoInteractions(outboxRepository);
    }

    @Test
    void create_whenRequestKeyExists_shouldReturnOldAction() {
        var tenantId = UUID.randomUUID();
        var oldAction = Action.create(
                tenantId,
                UUID.randomUUID(),
                "request-123",
                "calendar.create_event",
                "calendar",
                Map.of("title", "Demo"),
                "a".repeat(64),
                clock.instant());
        var request = new CreateActionCommand("ignored", "ignored", Map.of("x", "y"), "request-123");
        when(actionRepository.findByRequestKey(tenantId, request.requestKey())).thenReturn(Optional.of(oldAction));

        assertThat(service.create(tenantId, UUID.randomUUID(), request)).isSameAs(oldAction);
        verify(outboxRepository, never()).save(any(OutboxItem.class));
    }

    @Test
    void create_whenKindIsNotCalendar_shouldRejectRequest() {
        var tenantId = UUID.randomUUID();
        var request = new CreateActionCommand("task.create", "fake-calendar", Map.of("title", "Demo"), "request-123");
        when(actionRepository.findByRequestKey(tenantId, request.requestKey())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(tenantId, UUID.randomUUID(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only calendar.create_event is supported");

        verify(actionRepository, never()).saveIfMissing(any(Action.class));
        verifyNoInteractions(outboxRepository, payloadHash);
    }

    @Test
    void create_whenConnectorIsNotFakeCalendar_shouldRejectRequest() {
        var tenantId = UUID.randomUUID();
        var request = new CreateActionCommand(
                "calendar.create_event", "google-calendar", Map.of("title", "Demo"), "request-123");
        when(actionRepository.findByRequestKey(tenantId, request.requestKey())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(tenantId, UUID.randomUUID(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only fake-calendar is supported");

        verify(actionRepository, never()).saveIfMissing(any(Action.class));
        verifyNoInteractions(outboxRepository, payloadHash);
    }

    @Test
    void decide_whenConfirmIsNew_shouldSaveApprovedAction() {
        var action = action();
        when(actionRepository.findById(action.getTenantId(), action.getId())).thenReturn(Optional.of(action));

        var result = service.decide(
                action.getTenantId(),
                action.getId(),
                new DecideActionCommand(ActionDecision.CONFIRM, action.getPayloadHash()));

        assertThat(result.getStatus()).isEqualTo(ActionStatus.APPROVED);
        verify(actionRepository).update(action);
        var outbox = ArgumentCaptor.forClass(OutboxItem.class);
        verify(outboxRepository).save(outbox.capture());
        assertThat(outbox.getValue().type()).isEqualTo(OutboxType.DECISION);
        assertThat(outbox.getValue().decision()).isEqualTo("CONFIRM");
        assertThat(outbox.getValue().payloadHash()).isEqualTo(action.getPayloadHash());
    }

    @Test
    void decide_whenConfirmIsRepeated_shouldNotSaveAgain() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, action.getPayloadHash(), clock.instant());
        when(actionRepository.findById(action.getTenantId(), action.getId())).thenReturn(Optional.of(action));

        var result = service.decide(
                action.getTenantId(),
                action.getId(),
                new DecideActionCommand(ActionDecision.CONFIRM, action.getPayloadHash()));

        assertThat(result.getStatus()).isEqualTo(ActionStatus.APPROVED);
        verify(actionRepository, never()).update(action);
        verify(outboxRepository).save(any(OutboxItem.class));
    }

    @Test
    void decide_whenSameConfirmWinsRace_shouldReturnSavedState() {
        var first = action();
        var saved = sameAction(first);
        saved.applyDecision(ActionDecision.CONFIRM, saved.getPayloadHash(), clock.instant());
        when(actionRepository.findById(first.getTenantId(), first.getId()))
                .thenReturn(Optional.of(first), Optional.of(saved));
        org.mockito.Mockito.doThrow(new ActionChanged(first.getId()))
                .when(actionRepository)
                .update(first);

        var result = service.decide(
                first.getTenantId(),
                first.getId(),
                new DecideActionCommand(ActionDecision.CONFIRM, first.getPayloadHash()));

        assertThat(result).isSameAs(saved);
        assertThat(result.getStatus()).isEqualTo(ActionStatus.APPROVED);
    }

    @Test
    void start_whenActionIsApproved_shouldSaveExecutingAction() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, action.getPayloadHash(), clock.instant());
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        var result = service.start(action.getId(), action.getPayloadHash());

        assertThat(result.getStatus()).isEqualTo(ActionStatus.EXECUTING);
        verify(actionRepository).update(action);
    }

    @Test
    void start_whenActionIsAlreadyExecuting_shouldNotSaveAgain() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, action.getPayloadHash(), clock.instant());
        action.startExecution(clock.instant());
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        var result = service.start(action.getId(), action.getPayloadHash());

        assertThat(result.getStatus()).isEqualTo(ActionStatus.EXECUTING);
        verify(actionRepository, never()).update(action);
    }

    @Test
    void start_whenPayloadHashChanged_shouldRejectAction() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, action.getPayloadHash(), clock.instant());
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        assertThatThrownBy(() -> service.start(action.getId(), "b".repeat(64)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payload hash does not match");

        verify(actionRepository, never()).update(action);
    }

    @Test
    void start_whenActionAlreadyFinished_shouldReturnWithoutChange() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, action.getPayloadHash(), clock.instant());
        action.startExecution(clock.instant());
        action.succeed("event-123", clock.instant());
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        var result = service.start(action.getId(), action.getPayloadHash());

        assertThat(result.getStatus()).isEqualTo(ActionStatus.SUCCEEDED);
        verify(actionRepository, never()).update(action);
    }

    @Test
    void succeed_whenActionIsExecuting_shouldSaveResult() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, action.getPayloadHash(), clock.instant());
        action.startExecution(clock.instant());
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        var result = service.succeed(action.getId(), "event-123");

        assertThat(result.getStatus()).isEqualTo(ActionStatus.SUCCEEDED);
        assertThat(result.getResult().eventId()).isEqualTo("event-123");
        verify(actionRepository).update(action);
    }

    @Test
    void succeed_whenSameResultIsRepeated_shouldNotSaveAgain() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, action.getPayloadHash(), clock.instant());
        action.startExecution(clock.instant());
        action.succeed("event-123", clock.instant());
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        var result = service.succeed(action.getId(), "event-123");

        assertThat(result.getStatus()).isEqualTo(ActionStatus.SUCCEEDED);
        verify(actionRepository, never()).update(action);
    }

    @Test
    void fail_whenActionIsExecuting_shouldSaveFailedAction() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, action.getPayloadHash(), clock.instant());
        action.startExecution(clock.instant());
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        var result = service.fail(action.getId());

        assertThat(result.getStatus()).isEqualTo(ActionStatus.FAILED);
        verify(actionRepository).update(action);
    }

    @Test
    void fail_whenActionIsAlreadyFailed_shouldNotSaveAgain() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, action.getPayloadHash(), clock.instant());
        action.startExecution(clock.instant());
        action.fail(clock.instant());
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        var result = service.fail(action.getId());

        assertThat(result.getStatus()).isEqualTo(ActionStatus.FAILED);
        verify(actionRepository, never()).update(action);
    }

    private Action action() {
        return Action.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "request-123",
                "calendar.create_event",
                "fake-calendar",
                Map.of("title", "Demo"),
                "a".repeat(64),
                clock.instant());
    }

    private Action sameAction(Action source) {
        return Action.fromData(
                source.getId(),
                source.getVersion() + 1,
                source.getTenantId(),
                source.getActorId(),
                source.getRequestKey(),
                source.getKind(),
                source.getConnector(),
                source.getPayload(),
                source.getPayloadHash(),
                source.getStatus(),
                source.getResult(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }
}
