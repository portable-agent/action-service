package dev.portableagent.action.repository;

import static dev.portableagent.action.db.tables.ActionDispatchOutbox.ACTION_DISPATCH_OUTBOX;

import dev.portableagent.action.model.OutboxItem;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxRepository {
    private static final int MAX_ERROR_LENGTH = 1000;

    private final DSLContext db;

    public OutboxRepository(DSLContext db) {
        this.db = db;
    }

    public void save(OutboxItem item) {
        db.insertInto(ACTION_DISPATCH_OUTBOX)
                .set(ACTION_DISPATCH_OUTBOX.ID, item.id())
                .set(ACTION_DISPATCH_OUTBOX.ACTION_ID, item.actionId())
                .set(ACTION_DISPATCH_OUTBOX.EVENT_TYPE, item.type().name())
                .set(ACTION_DISPATCH_OUTBOX.DECISION, item.decision())
                .set(ACTION_DISPATCH_OUTBOX.PAYLOAD_HASH, item.payloadHash())
                .set(ACTION_DISPATCH_OUTBOX.CREATED_AT, utc(item.createdAt()))
                .set(ACTION_DISPATCH_OUTBOX.NEXT_ATTEMPT_AT, utc(item.createdAt()))
                .set(ACTION_DISPATCH_OUTBOX.ATTEMPTS, item.attempts())
                .onConflict(ACTION_DISPATCH_OUTBOX.ACTION_ID, ACTION_DISPATCH_OUTBOX.EVENT_TYPE)
                .doNothing()
                .execute();
    }

    public List<OutboxItem> findPending(int limit, Instant now) {
        return db.selectFrom(ACTION_DISPATCH_OUTBOX)
                .where(ACTION_DISPATCH_OUTBOX.DISPATCHED_AT.isNull())
                .and(ACTION_DISPATCH_OUTBOX.NEXT_ATTEMPT_AT.le(utc(now)))
                .orderBy(ACTION_DISPATCH_OUTBOX.NEXT_ATTEMPT_AT.asc(), ACTION_DISPATCH_OUTBOX.CREATED_AT.asc())
                .limit(limit)
                .forUpdate()
                .skipLocked()
                .fetch(row -> new OutboxItem(
                        row.getId(),
                        row.getActionId(),
                        dev.portableagent.action.model.OutboxType.valueOf(row.getEventType()),
                        row.getDecision(),
                        row.getPayloadHash(),
                        row.getCreatedAt().toInstant(),
                        row.getAttempts()));
    }

    public void markSent(OutboxItem item, Instant now) {
        db.update(ACTION_DISPATCH_OUTBOX)
                .set(ACTION_DISPATCH_OUTBOX.DISPATCHED_AT, utc(now))
                .set(ACTION_DISPATCH_OUTBOX.ATTEMPTS, item.attempts() + 1)
                .setNull(ACTION_DISPATCH_OUTBOX.LAST_ERROR)
                .where(ACTION_DISPATCH_OUTBOX.ID.eq(item.id()))
                .execute();
    }

    public void markFailed(OutboxItem item, String error, Instant now) {
        String message = error == null ? "Unknown send error" : error;
        long delaySeconds = Math.min(300, 1L << Math.min(item.attempts(), 8));
        db.update(ACTION_DISPATCH_OUTBOX)
                .set(ACTION_DISPATCH_OUTBOX.ATTEMPTS, item.attempts() + 1)
                .set(ACTION_DISPATCH_OUTBOX.LAST_ERROR, shortText(message))
                .set(ACTION_DISPATCH_OUTBOX.NEXT_ATTEMPT_AT, utc(now.plus(Duration.ofSeconds(delaySeconds))))
                .where(ACTION_DISPATCH_OUTBOX.ID.eq(item.id()))
                .execute();
    }

    private String shortText(String value) {
        return value.substring(0, Math.min(value.length(), MAX_ERROR_LENGTH));
    }

    private OffsetDateTime utc(Instant value) {
        return value.atOffset(ZoneOffset.UTC);
    }
}
