package dev.portableagent.action.repository;

import static dev.portableagent.action.db.tables.ActionProposals.ACTION_PROPOSALS;

import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionResult;
import dev.portableagent.action.model.ActionStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class ActionRepository {
  private final DSLContext db;
  private final JsonMapper jsonMapper;

  public ActionRepository(DSLContext db, JsonMapper jsonMapper) {
    this.db = db;
    this.jsonMapper = jsonMapper;
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

  public boolean saveIfMissing(Action action) {
    int changed =
        db.insertInto(ACTION_PROPOSALS)
            .set(ACTION_PROPOSALS.ID, action.getId())
            .set(ACTION_PROPOSALS.VERSION, action.getVersion())
            .set(ACTION_PROPOSALS.TENANT_ID, action.getTenantId())
            .set(ACTION_PROPOSALS.ACTOR_ID, action.getActorId())
            .set(ACTION_PROPOSALS.IDEMPOTENCY_KEY, action.getRequestKey())
            .set(ACTION_PROPOSALS.KIND, action.getKind())
            .set(ACTION_PROPOSALS.CONNECTOR, action.getConnector())
            .set(ACTION_PROPOSALS.PAYLOAD, toJson(action.getPayload()))
            .set(ACTION_PROPOSALS.PAYLOAD_HASH, action.getPayloadHash())
            .set(ACTION_PROPOSALS.STATUS, action.getStatus().name())
            .set(ACTION_PROPOSALS.CREATED_AT, utc(action.getCreatedAt()))
            .set(ACTION_PROPOSALS.UPDATED_AT, utc(action.getUpdatedAt()))
            .onConflict(ACTION_PROPOSALS.TENANT_ID, ACTION_PROPOSALS.IDEMPOTENCY_KEY)
            .doNothing()
            .execute();
    return changed == 1;
  }

  public void update(Action action) {
    int changed =
        db.update(ACTION_PROPOSALS)
            .set(ACTION_PROPOSALS.STATUS, action.getStatus().name())
            .set(ACTION_PROPOSALS.RESULT, toJson(action.getResult()))
            .set(ACTION_PROPOSALS.UPDATED_AT, utc(action.getUpdatedAt()))
            .set(ACTION_PROPOSALS.VERSION, action.getVersion() + 1)
            .where(ACTION_PROPOSALS.ID.eq(action.getId()))
            .and(ACTION_PROPOSALS.TENANT_ID.eq(action.getTenantId()))
            .and(ACTION_PROPOSALS.VERSION.eq(action.getVersion()))
            .execute();
    if (changed != 1) {
      throw new IllegalStateException("Action was changed by another request");
    }
    action.markSaved();
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
        fromJson(row.get(ACTION_PROPOSALS.PAYLOAD)),
        row.get(ACTION_PROPOSALS.PAYLOAD_HASH),
        ActionStatus.valueOf(row.get(ACTION_PROPOSALS.STATUS)),
        fromJsonResult(row.get(ACTION_PROPOSALS.RESULT)),
        row.get(ACTION_PROPOSALS.CREATED_AT).toInstant(),
        row.get(ACTION_PROPOSALS.UPDATED_AT).toInstant());
  }

  private JSON toJson(Map<String, Object> payload) {
    try {
      return JSON.valueOf(jsonMapper.writeValueAsString(payload));
    } catch (JacksonException error) {
      throw new IllegalArgumentException("Payload is not valid JSON", error);
    }
  }

  private JSON toJson(ActionResult result) {
    return result == null ? null : toJson(Map.of("eventId", result.eventId()));
  }

  private ActionResult fromJsonResult(JSON result) {
    if (result == null) {
      return null;
    }
    var data = fromJson(result);
    var eventId = data.get("eventId");
    if (!(eventId instanceof String text)) {
      throw new IllegalStateException("Saved result does not contain eventId");
    }
    return new ActionResult(text);
  }

  private Map<String, Object> fromJson(JSON payload) {
    try {
      return jsonMapper.readValue(payload.data(), new TypeReference<>() {});
    } catch (JacksonException error) {
      throw new IllegalStateException("Saved payload is not valid JSON", error);
    }
  }

  private OffsetDateTime utc(java.time.Instant value) {
    return value.atOffset(ZoneOffset.UTC);
  }
}
