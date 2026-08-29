package dev.portableagent.action.service;

import dev.portableagent.action.dto.CreateActionRequest;
import dev.portableagent.action.dto.DecideActionRequest;
import dev.portableagent.action.exception.ActionNotFound;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.OutboxItem;
import dev.portableagent.action.repository.ActionRepository;
import dev.portableagent.action.repository.OutboxRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionService {
  private final ActionRepository actionRepository;
  private final OutboxRepository outboxRepository;
  private final PayloadHash payloadHash;
  private final Clock clock;

  public ActionService(
      ActionRepository actionRepository,
      OutboxRepository outboxRepository,
      PayloadHash payloadHash,
      Clock clock) {
    this.actionRepository = actionRepository;
    this.outboxRepository = outboxRepository;
    this.payloadHash = payloadHash;
    this.clock = clock;
  }

  @Transactional
  public Action create(UUID tenantId, UUID userId, CreateActionRequest request) {
    var oldAction = actionRepository.findByRequestKey(tenantId, request.requestKey());
    if (oldAction.isPresent()) {
      return oldAction.get();
    }

    var now = clock.instant();
    var action =
        Action.create(
            tenantId,
            userId,
            request.requestKey(),
            request.kind(),
            request.connector(),
            payloadHash.make(request.payload()),
            now);
    actionRepository.save(action);
    outboxRepository.save(OutboxItem.create(action.getId(), now));
    return action;
  }

  @Transactional(readOnly = true)
  public Action get(UUID tenantId, UUID actionId) {
    return actionRepository
        .findById(tenantId, actionId)
        .orElseThrow(() -> new ActionNotFound(actionId));
  }

  @Transactional
  public Action decide(UUID tenantId, UUID actionId, DecideActionRequest request) {
    var action = get(tenantId, actionId);
    action.applyDecision(request.decision(), request.payloadHash(), clock.instant());
    actionRepository.update(action);
    return action;
  }
}
