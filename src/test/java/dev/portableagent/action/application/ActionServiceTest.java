package dev.portableagent.action.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.portableagent.action.api.ProposeActionRequest;
import dev.portableagent.action.domain.ActionProposal;
import dev.portableagent.action.repository.ActionDispatchOutboxRepository;
import dev.portableagent.action.repository.ActionProposalRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {
    @Mock ActionProposalRepository actionRepository;
    @Mock ActionDispatchOutboxRepository outboxRepository;
    @Mock PayloadHasher payloadHasher;
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void propose_whenRequestIsNew_shouldSaveActionAndOutbox() {
        var service = new ActionService(actionRepository, outboxRepository, payloadHasher, clock);
        var tenantId = UUID.randomUUID();
        var actorId = UUID.randomUUID();
        var request = new ProposeActionRequest("calendar.create_event", "google-calendar", Map.of("title", "Demo"), "request-123");
        when(actionRepository.findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey()))
                .thenReturn(Optional.empty());
        when(payloadHasher.sha256(request.payload())).thenReturn("a".repeat(64));
        when(actionRepository.save(any(ActionProposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.propose(tenantId, actorId, request);

        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getPayloadHash()).isEqualTo("a".repeat(64));
        verify(actionRepository).save(result);
        verify(outboxRepository).save(any());
    }

    @Test
    void propose_whenIdempotencyKeyExists_shouldReturnExistingWithoutNewOutbox() {
        var service = new ActionService(actionRepository, outboxRepository, payloadHasher, clock);
        var tenantId = UUID.randomUUID();
        var existing = ActionProposal.propose(tenantId, UUID.randomUUID(), "request-123", "calendar.create_event", "calendar", "a".repeat(64), clock.instant());
        var request = new ProposeActionRequest("ignored", "ignored", Map.of("x", "y"), "request-123");
        when(actionRepository.findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey()))
                .thenReturn(Optional.of(existing));

        assertThat(service.propose(tenantId, UUID.randomUUID(), request)).isSameAs(existing);
    }
}
