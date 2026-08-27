package dev.portableagent.action.application;

import dev.portableagent.action.repository.ActionDispatchOutboxRepository;
import dev.portableagent.action.workflow.TemporalActionDispatcher;
import java.time.Clock;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionOutboxDispatcher {
    private final ActionDispatchOutboxRepository outboxRepository;
    private final TemporalActionDispatcher temporalDispatcher;
    private final Clock clock;

    public ActionOutboxDispatcher(
            ActionDispatchOutboxRepository outboxRepository,
            TemporalActionDispatcher temporalDispatcher,
            Clock clock) {
        this.outboxRepository = outboxRepository;
        this.temporalDispatcher = temporalDispatcher;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${action.outbox.delay:PT1S}")
    @Transactional
    public void dispatchPending() {
        for (var message : outboxRepository.findByDispatchedAtIsNullOrderByCreatedAtAsc(Limit.of(20))) {
            try {
                temporalDispatcher.dispatch(message.getActionId());
                message.markDispatched(clock.instant());
            } catch (RuntimeException exception) {
                message.markFailed(exception.getMessage());
            }
        }
    }
}
