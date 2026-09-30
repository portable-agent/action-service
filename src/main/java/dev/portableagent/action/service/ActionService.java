package dev.portableagent.action.service;

import dev.portableagent.action.exception.ActionChanged;
import dev.portableagent.action.exception.ActionNotFound;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionStatus;
import dev.portableagent.action.model.OutboxItem;
import dev.portableagent.action.repository.ActionRepository;
import dev.portableagent.action.repository.OutboxRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActionService {
    private static final int CHANGE_TRIES = 3;

    private final ActionRepository actionRepository;
    private final OutboxRepository outboxRepository;
    private final PayloadHash payloadHash;
    private final ActionRules actionRules;
    private final Clock clock;

    @Transactional
    public Action create(UUID tenantId, UUID userId, CreateActionCommand request) {
        var oldAction = actionRepository.findByRequestKey(tenantId, request.requestKey());
        if (oldAction.isPresent()) {
            return oldAction.get();
        }

        actionRules.check(request);

        var now = clock.instant();
        var action = Action.create(
                tenantId,
                userId,
                request.requestKey(),
                request.kind(),
                request.connector(),
                request.payload(),
                payloadHash.make(request.payload()),
                now);
        if (!actionRepository.saveIfMissing(action)) {
            return actionRepository
                    .findByRequestKey(tenantId, request.requestKey())
                    .orElseThrow(() -> new IllegalStateException("Saved action was not found"));
        }
        outboxRepository.save(OutboxItem.start(action.getId(), now));
        return action;
    }

    @Transactional(readOnly = true)
    public Action get(UUID tenantId, UUID actionId) {
        return actionRepository.findById(tenantId, actionId).orElseThrow(() -> new ActionNotFound(actionId));
    }

    @Transactional
    public Action decide(UUID tenantId, UUID actionId, DecideActionCommand request) {
        var action = change(
                () -> get(tenantId, actionId),
                current -> current.applyDecision(request.decision(), request.payloadHash(), clock.instant()));
        outboxRepository.save(
                OutboxItem.decision(actionId, request.decision().name(), request.payloadHash(), clock.instant()));
        return action;
    }

    @Transactional
    public Action start(UUID actionId, String checkedHash) {
        return change(() -> getForWork(actionId), action -> {
            if (!action.getPayloadHash().equals(checkedHash)) {
                throw new IllegalArgumentException("Payload hash does not match");
            }
            if (action.getStatus() == ActionStatus.SUCCEEDED || action.getStatus() == ActionStatus.FAILED) {
                return false;
            }
            return action.startExecution(clock.instant());
        });
    }

    @Transactional
    public Action succeed(UUID actionId, String eventId) {
        return change(() -> getForWork(actionId), action -> action.succeed(eventId, clock.instant()));
    }

    @Transactional
    public Action fail(UUID actionId) {
        return change(() -> getForWork(actionId), action -> action.fail(clock.instant()));
    }

    private Action getForWork(UUID actionId) {
        return actionRepository.findById(actionId).orElseThrow(() -> new ActionNotFound(actionId));
    }

    private Action change(Supplier<Action> load, Function<Action, Boolean> apply) {
        ActionChanged lastError = null;
        for (int attempt = 0; attempt < CHANGE_TRIES; attempt++) {
            var action = load.get();
            if (!apply.apply(action)) {
                return action;
            }
            try {
                actionRepository.update(action);
                return action;
            } catch (ActionChanged error) {
                lastError = error;
            }
        }
        throw lastError;
    }
}
