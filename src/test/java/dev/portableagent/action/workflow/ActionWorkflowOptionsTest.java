package dev.portableagent.action.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.action.config.TemporalProperties;
import dev.portableagent.action.model.ActionKind;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionWorkflowOptionsTest {
    private final ActionWorkflowOptions workflowOptions =
            new ActionWorkflowOptions(new TemporalProperties("localhost:7233", "default", "action-task-queue"));

    @Test
    void make_whenInputIsValid_shouldAddReadableUiData() {
        var input = input();

        var options = workflowOptions.make(input);

        assertThat(options.getWorkflowId()).isEqualTo("action-" + input.actionId());
        assertThat(options.getTaskQueue()).isEqualTo("action-task-queue");
        assertThat(options.getStaticSummary()).isEqualTo("calendar.create_event · google-calendar");
        assertThat(options.getStaticDetails())
                .contains(input.actionId().toString())
                .contains("calendar.create_event")
                .contains("google-calendar");
        assertThat(options.getMemo())
                .containsEntry("actionId", input.actionId().toString())
                .containsEntry("kind", "calendar.create_event")
                .containsEntry("connector", "google-calendar");
        assertThat(options.getTypedSearchAttributes().get(ActionWorkflowOptions.ACTION_KIND))
                .isEqualTo("calendar.create_event");
        assertThat(options.getTypedSearchAttributes().get(ActionWorkflowOptions.ACTION_CONNECTOR))
                .isEqualTo("google-calendar");
        assertThat(options.getTypedSearchAttributes().get(ActionWorkflowOptions.ACTION_TENANT_ID))
                .isEqualTo(input.tenantId().toString());
        assertThat(options.getTypedSearchAttributes().get(ActionWorkflowOptions.ACTION_ACTOR_ID))
                .isEqualTo(input.actorId().toString());
        assertThat(options.getTypedSearchAttributes().get(ActionWorkflowOptions.ACTION_STATUS))
                .isEqualTo("AWAITING_APPROVAL");
    }

    private ActionWorkflowInput input() {
        return new ActionWorkflowInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "calendar-request-123",
                ActionKind.CALENDAR_CREATE_EVENT,
                "google-calendar",
                Map.of("title", "Demo"),
                "a".repeat(64));
    }
}
