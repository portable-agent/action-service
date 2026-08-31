package dev.portableagent.action.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.portableagent.action.dto.CreateActionRequest;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.OutboxItem;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {
  @Mock ActionRepository actionRepository;
  @Mock OutboxRepository outboxRepository;
  @Mock PayloadHash payloadHash;

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
    var request =
        new CreateActionRequest(
            "calendar.create_event", "fake-calendar", Map.of("title", "Demo"), "request-123");
    when(actionRepository.findByRequestKey(tenantId, request.requestKey()))
        .thenReturn(Optional.empty());
    when(payloadHash.make(request.payload())).thenReturn("a".repeat(64));
    when(actionRepository.saveIfMissing(any(Action.class))).thenReturn(true);

    var result = service.create(tenantId, userId, request);

    assertThat(result.getTenantId()).isEqualTo(tenantId);
    assertThat(result.getPayloadHash()).isEqualTo("a".repeat(64));
    verify(actionRepository).saveIfMissing(result);
    verify(outboxRepository).save(any(OutboxItem.class));
  }

  @Test
  void create_whenAnotherRequestSavesFirst_shouldReturnSavedAction() {
    var tenantId = UUID.randomUUID();
    var oldAction =
        Action.create(
            tenantId,
            UUID.randomUUID(),
            "request-123",
            "calendar.create_event",
            "fake-calendar",
            Map.of("title", "Demo"),
            "a".repeat(64),
            clock.instant());
    var request =
        new CreateActionRequest(
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
    var oldAction =
        Action.create(
            tenantId,
            UUID.randomUUID(),
            "request-123",
            "calendar.create_event",
            "calendar",
            Map.of("title", "Demo"),
            "a".repeat(64),
            clock.instant());
    var request = new CreateActionRequest("ignored", "ignored", Map.of("x", "y"), "request-123");
    when(actionRepository.findByRequestKey(tenantId, request.requestKey()))
        .thenReturn(Optional.of(oldAction));

    assertThat(service.create(tenantId, UUID.randomUUID(), request)).isSameAs(oldAction);
    verify(outboxRepository, never()).save(any(OutboxItem.class));
  }

  @Test
  void create_whenKindIsNotCalendar_shouldRejectRequest() {
    var tenantId = UUID.randomUUID();
    var request =
        new CreateActionRequest(
            "task.create", "fake-calendar", Map.of("title", "Demo"), "request-123");
    when(actionRepository.findByRequestKey(tenantId, request.requestKey()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(tenantId, UUID.randomUUID(), request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Only calendar.create_event is supported");

    verify(actionRepository, never()).saveIfMissing(any(Action.class));
    verifyNoInteractions(outboxRepository, payloadHash);
  }

  @Test
  void create_whenConnectorIsNotFakeCalendar_shouldRejectRequest() {
    var tenantId = UUID.randomUUID();
    var request =
        new CreateActionRequest(
            "calendar.create_event", "google-calendar", Map.of("title", "Demo"), "request-123");
    when(actionRepository.findByRequestKey(tenantId, request.requestKey()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(tenantId, UUID.randomUUID(), request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Only fake-calendar is supported");

    verify(actionRepository, never()).saveIfMissing(any(Action.class));
    verifyNoInteractions(outboxRepository, payloadHash);
  }
}
