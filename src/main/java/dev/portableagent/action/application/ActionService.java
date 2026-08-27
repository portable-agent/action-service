package dev.portableagent.action.application;

import dev.portableagent.action.api.ActionDecisionRequest;
import dev.portableagent.action.api.ProposeActionRequest;
import dev.portableagent.action.domain.ActionDispatchOutbox;
import dev.portableagent.action.domain.ActionProposal;
import dev.portableagent.action.repository.ActionDispatchOutboxRepository;
import dev.portableagent.action.repository.ActionProposalRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionService {
    private final ActionProposalRepository actionRepository;
    private final ActionDispatchOutboxRepository outboxRepository;
    private final PayloadHasher payloadHasher;
    private final Clock clock;

    public ActionService(
            ActionProposalRepository actionRepository,
            ActionDispatchOutboxRepository outboxRepository,
            PayloadHasher payloadHasher,
            Clock clock) {
        this.actionRepository = actionRepository;
        this.outboxRepository = outboxRepository;
        this.payloadHasher = payloadHasher;
        this.clock = clock;
    }

    @Transactional
    public ActionProposal propose(UUID tenantId, UUID actorId, ProposeActionRequest request) {
        var existing = actionRepository.findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant now = clock.instant();
        var action = ActionProposal.propose(
                tenantId,
                actorId,
                request.idempotencyKey(),
                request.kind(),
                request.connector(),
                payloadHasher.sha256(request.payload()),
                now);
        actionRepository.save(action);
        outboxRepository.save(ActionDispatchOutbox.pending(action.getId(), now));
        return action;
    }

    @Transactional(readOnly = true)
    public ActionProposal get(UUID tenantId, UUID actionId) {
        return actionRepository.findByIdAndTenantId(actionId, tenantId)
                .orElseThrow(() -> new ActionNotFoundException(actionId));
    }

    @Transactional
    public ActionProposal decide(UUID tenantId, UUID actionId, ActionDecisionRequest request) {
        var action = actionRepository.findByIdAndTenantId(actionId, tenantId)
                .orElseThrow(() -> new ActionNotFoundException(actionId));
        action.decide(request.decision(), request.payloadHash(), clock.instant());
        return action;
    }
}
