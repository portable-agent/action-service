package dev.portableagent.action.scheduler;

import dev.portableagent.action.repository.OutboxRepository;
import dev.portableagent.action.workflow.TemporalSender;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxJob {
  private static final int BATCH_SIZE = 20;

  private final OutboxRepository outboxRepository;
  private final TemporalSender temporalSender;
  private final Clock clock;

  public OutboxJob(OutboxRepository outboxRepository, TemporalSender temporalSender, Clock clock) {
    this.outboxRepository = outboxRepository;
    this.temporalSender = temporalSender;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${action.outbox.delay:PT1S}")
  @Transactional
  public void sendPending() {
    for (var item : outboxRepository.findPending(BATCH_SIZE)) {
      try {
        temporalSender.send(item.actionId());
        outboxRepository.markSent(item, clock.instant());
      } catch (RuntimeException error) {
        outboxRepository.markFailed(item, error.getMessage());
      }
    }
  }
}
