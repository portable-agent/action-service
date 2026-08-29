package dev.portableagent.action.repository;

import static dev.portableagent.action.db.tables.ActionProposals.ACTION_PROPOSALS;

import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
public class ActionRepository {
  private final DSLContext db;

  public ActionRepository(DSLContext db) {
    this.db = db;
  }

  public Optional<Action> findByRequestKey(UUID tenantId, String requestKey) {
    return db.selectFrom(ACTION_PROPOSALS)
        .where(ACTION_PROPOSALS.TENANT_ID.eq(tenantId))
        .and(ACTION_PROPOSALS.IDEMPOTENCY_KEY.eq(requestKey))
        .fetchOptional(this::toAction);
  }

  public Optional<Action> findById(UUID tenantId, UUID actionId) {
    return db.selectFrom(ACTION_PROPOSALS)
        .where(ACTION_PROPOSALS.ID.eq(actionId))
        .and(ACTION_PROPOSALS.TENANT_ID.eq(tenantId))
        .fetchOptional(this::toAction);
  }

  public void save(Action action) {
    db.insertInto(ACTION_PROPOSALS)
        .set(ACTION_PROPOSALS.ID, action.getId())
        .set(ACTION_PROPOSALS.VERSION, action.getVersion())
        .set(ACTION_PROPOSALS.TENANT_ID, action.getTenantId())
        .set(ACTION_PROPOSALS.ACTOR_ID, action.getActorId())
        .set(ACTION_PROPOSALS.IDEMPOTENCY_KEY, action.getRequestKey())
        .set(ACTION_PROPOSALS.KIND, action.getKind())
        .set(ACTION_PROPOSALS.CONNECTOR, action.getConnector())
        .set(ACTION_PROPOSALS.PAYLOAD_HASH, action.getPayloadHash())
        .set(ACTION_PROPOSALS.STATUS, action.getStatus().name())
        .set(ACTION_PROPOSALS.CREATED_AT, utc(action.getCreatedAt()))
        .set(ACTION_PROPOSALS.UPDATED_AT, utc(action.getUpdatedAt()))
        .execute();
  }

  public void update(Action action) {
    int changed =
        db.update(ACTION_PROPOSALS)
            .set(ACTION_PROPOSALS.STATUS, action.getStatus().name())
            .set(ACTION_PROPOSALS.UPDATED_AT, utc(action.getUpdatedAt()))
            .set(ACTION_PROPOSALS.VERSION, action.getVersion() + 1)
            .where(ACTION_PROPOSALS.ID.eq(action.getId()))
            .and(ACTION_PROPOSALS.TENANT_ID.eq(action.getTenantId()))
            .and(ACTION_PROPOSALS.VERSION.eq(action.getVersion()))
            .execute();
    if (changed != 1) {
      throw new IllegalStateException("Action was changed by another request");
    }
  }

  private Action toAction(Record row) {
    return Action.fromData(
        row.get(ACTION_PROPOSALS.ID),
        row.get(ACTION_PROPOSALS.VERSION),
        row.get(ACTION_PROPOSALS.TENANT_ID),
        row.get(ACTION_PROPOSALS.ACTOR_ID),
        row.get(ACTION_PROPOSALS.IDEMPOTENCY_KEY),
        row.get(ACTION_PROPOSALS.KIND),
        row.get(ACTION_PROPOSALS.CONNECTOR),
        row.get(ACTION_PROPOSALS.PAYLOAD_HASH),
        ActionStatus.valueOf(row.get(ACTION_PROPOSALS.STATUS)),
        row.get(ACTION_PROPOSALS.CREATED_AT).toInstant(),
        row.get(ACTION_PROPOSALS.UPDATED_AT).toInstant());
  }

  private OffsetDateTime utc(java.time.Instant value) {
    return value.atOffset(ZoneOffset.UTC);
  }
}
