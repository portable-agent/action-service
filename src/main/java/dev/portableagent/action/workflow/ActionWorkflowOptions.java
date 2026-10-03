package dev.portableagent.action.workflow;

import dev.portableagent.action.config.TemporalProperties;
import dev.portableagent.action.model.ActionStatus;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributes;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActionWorkflowOptions {
    static final SearchAttributeKey<String> ACTION_KIND = SearchAttributeKey.forKeyword("ActionKind");
    static final SearchAttributeKey<String> ACTION_CONNECTOR = SearchAttributeKey.forKeyword("ActionConnector");
    static final SearchAttributeKey<String> ACTION_TENANT_ID = SearchAttributeKey.forKeyword("ActionTenantId");
    static final SearchAttributeKey<String> ACTION_ACTOR_ID = SearchAttributeKey.forKeyword("ActionActorId");
    static final SearchAttributeKey<String> ACTION_STATUS = SearchAttributeKey.forKeyword("ActionStatus");

    private final TemporalProperties properties;

    public WorkflowOptions make(ActionWorkflowInput input) {
        return WorkflowOptions.newBuilder()
                .setWorkflowId("action-" + input.actionId())
                .setTaskQueue(properties.taskQueue())
                .setStaticSummary(input.summary())
                .setStaticDetails(details(input))
                .setMemo(Map.of(
                        "actionId", input.actionId().toString(),
                        "kind", input.kind().value(),
                        "connector", input.connector()))
                .setTypedSearchAttributes(searchAttributes(input))
                .build();
    }

    private SearchAttributes searchAttributes(ActionWorkflowInput input) {
        return SearchAttributes.newBuilder()
                .set(ACTION_KIND, input.kind().value())
                .set(ACTION_CONNECTOR, input.connector())
                .set(ACTION_TENANT_ID, input.tenantId().toString())
                .set(ACTION_ACTOR_ID, input.actorId().toString())
                .set(ACTION_STATUS, ActionStatus.AWAITING_APPROVAL.name())
                .build();
    }

    private String details(ActionWorkflowInput input) {
        return """
                Action `%s` with connector `%s`.

                - Action ID: `%s`
                - Request key: `%s`
                """.formatted(input.kind().value(), input.connector(), input.actionId(), input.requestKey());
    }
}
