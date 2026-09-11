package dev.portableagent.action.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.portableagent.action.model.OutboxItem;
import dev.portableagent.action.repository.OutboxRepository;
import dev.portableagent.action.workflow.TemporalSender;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxJobTest {
    @Mock
    OutboxRepository repository;

    @Mock
    TemporalSender sender;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-11T06:00:00Z"), ZoneOffset.UTC);
    private OutboxJob job;

    @BeforeEach
    void setUp() {
        job = new OutboxJob(repository, sender, clock);
    }

    @Test
    void sendPending_whenEventStartsWorkflow_shouldMarkSent() {
        var item = OutboxItem.start(UUID.randomUUID(), clock.instant());
        when(repository.findPending(20, clock.instant())).thenReturn(List.of(item));

        job.sendPending();

        verify(sender).send(item.actionId());
        verify(repository).markSent(item, clock.instant());
    }

    @Test
    void sendPending_whenEventHasDecision_shouldSendSignal() {
        var item = OutboxItem.decision(UUID.randomUUID(), "CONFIRM", "a".repeat(64), clock.instant());
        when(repository.findPending(20, clock.instant())).thenReturn(List.of(item));

        job.sendPending();

        verify(sender).sendDecision(item.actionId(), item.decision(), item.payloadHash());
        verify(repository).markSent(item, clock.instant());
    }

    @Test
    void sendPending_whenTemporalFails_shouldKeepEventPending() {
        var item = OutboxItem.start(UUID.randomUUID(), clock.instant());
        when(repository.findPending(20, clock.instant())).thenReturn(List.of(item));
        org.mockito.Mockito.doThrow(new IllegalStateException("Temporal is down"))
                .when(sender)
                .send(item.actionId());

        job.sendPending();

        verify(repository).markFailed(item, "Temporal is down", clock.instant());
        verify(repository, never()).markSent(item, clock.instant());
    }
}
