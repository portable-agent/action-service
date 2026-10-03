package dev.portableagent.action.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.portableagent.action.client.McpCallFailed;
import dev.portableagent.action.config.TemporalProperties;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionDecision;
import dev.portableagent.action.model.ActionStatus;
import dev.portableagent.action.service.ActionService;
import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ActionWorkflowTest {
    private TestWorkflowEnvironment testEnvironment;
    private ActionActivityImpl activity;
    private ActionWorkflowV2 workflow;
    private TemporalSender sender;
    private ActionService actionService;

    @BeforeEach
    void startWorker() {
        testEnvironment = TestWorkflowEnvironment.newInstance();
        registerSearchAttributes();
        var worker = testEnvironment.newWorker("action-test");
        activity = Mockito.mock(ActionActivityImpl.class);
        actionService = Mockito.mock(ActionService.class);
        worker.registerWorkflowImplementationTypes(ActionWorkflowV2Impl.class);
        worker.registerActivitiesImplementations(activity);
        testEnvironment.start();

        workflow = testEnvironment
                .getWorkflowClient()
                .newWorkflowStub(
                        ActionWorkflowV2.class,
                        WorkflowOptions.newBuilder().setTaskQueue("action-test").build());
        var properties = new TemporalProperties("unused", "default", "action-test");
        sender = new TemporalSender(
                testEnvironment.getWorkflowClient(), new ActionWorkflowOptions(properties), actionService);
    }

    @AfterEach
    void stopWorker() {
        testEnvironment.close();
    }

    @Test
    void run_whenActionIsConfirmed_shouldRunActivity() {
        var actionId = UUID.randomUUID();
        var payloadHash = "a".repeat(64);
        var action = action(actionId, payloadHash);
        var input = ActionWorkflowInput.from(action);
        var expected = new ActionRunResult(
                actionId, input.kind(), input.connector(), ActionStatus.SUCCEEDED, Map.of("eventId", "event-123"));
        Mockito.when(actionService.getForWork(actionId)).thenReturn(action);
        Mockito.when(activity.run(new ActionRunInput(input, payloadHash))).thenReturn(expected);
        sender.sendDecision(actionId, "CONFIRM", payloadHash);
        workflow = testEnvironment.getWorkflowClient().newWorkflowStub(ActionWorkflowV2.class, "action-" + actionId);
        var result = WorkflowStub.fromTyped(workflow).getResult(ActionRunResult.class);

        verify(activity, timeout(2_000)).run(new ActionRunInput(input, payloadHash));
        assertThat(result).isEqualTo(expected);
        assertThat(WorkflowStub.fromTyped(workflow)
                        .describe()
                        .getTypedSearchAttributes()
                        .get(ActionWorkflowOptions.ACTION_STATUS))
                .isEqualTo("SUCCEEDED");
    }

    @Test
    void run_whenActionIsCancelled_shouldNotRunActivity() {
        var actionId = UUID.randomUUID();
        var payloadHash = "a".repeat(64);
        var action = action(actionId, payloadHash);
        var input = ActionWorkflowInput.from(action);
        Mockito.when(actionService.getForWork(actionId)).thenReturn(action);
        sender.sendDecision(actionId, "CANCEL", payloadHash);
        workflow = testEnvironment.getWorkflowClient().newWorkflowStub(ActionWorkflowV2.class, "action-" + actionId);
        var result = WorkflowStub.fromTyped(workflow).getResult(ActionRunResult.class);

        verify(activity, never()).run(new ActionRunInput(input, payloadHash));
        assertThat(result.status()).isEqualTo(ActionStatus.CANCELLED);
        assertThat(WorkflowStub.fromTyped(workflow)
                        .describe()
                        .getTypedSearchAttributes()
                        .get(ActionWorkflowOptions.ACTION_STATUS))
                .isEqualTo("CANCELLED");
    }

    @Test
    void run_whenMcpCallKeepsFailing_shouldRetryAndMarkActionFailed() {
        var actionId = UUID.randomUUID();
        var payloadHash = "a".repeat(64);
        var action = action(actionId, payloadHash);
        var input = ActionWorkflowInput.from(action);
        var failed = new ActionRunResult(actionId, input.kind(), input.connector(), ActionStatus.FAILED, Map.of());
        Mockito.when(actionService.getForWork(actionId)).thenReturn(action);
        doThrow(new McpCallFailed("Gateway call failed")).when(activity).run(new ActionRunInput(input, payloadHash));
        Mockito.when(activity.fail(input)).thenReturn(failed);

        sender.sendDecision(actionId, "CONFIRM", payloadHash);
        workflow = testEnvironment.getWorkflowClient().newWorkflowStub(ActionWorkflowV2.class, "action-" + actionId);
        var workflowStub = WorkflowStub.fromTyped(workflow);

        assertThatThrownBy(() -> workflowStub.getResult(ActionRunResult.class))
                .isInstanceOf(WorkflowFailedException.class);
        verify(activity, times(3)).run(new ActionRunInput(input, payloadHash));
        verify(activity).fail(input);
        assertThat(workflowStub.describe().getStatus())
                .isEqualTo(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_FAILED);
        assertThat(workflowStub.describe().getTypedSearchAttributes().get(ActionWorkflowOptions.ACTION_STATUS))
                .isEqualTo("FAILED");
    }

    @Test
    void sendDecision_whenWorkflowAlreadyFinished_shouldKeepRetrySafe() {
        var actionId = UUID.randomUUID();
        var payloadHash = "a".repeat(64);
        var action = action(actionId, payloadHash);
        var input = ActionWorkflowInput.from(action);
        Mockito.when(actionService.getForWork(actionId)).thenReturn(action);
        Mockito.when(activity.run(new ActionRunInput(input, payloadHash)))
                .thenReturn(new ActionRunResult(
                        actionId,
                        input.kind(),
                        input.connector(),
                        ActionStatus.SUCCEEDED,
                        Map.of("eventId", "event-123")));
        sender.sendDecision(actionId, "CONFIRM", payloadHash);
        workflow = testEnvironment.getWorkflowClient().newWorkflowStub(ActionWorkflowV2.class, "action-" + actionId);
        WorkflowStub.fromTyped(workflow).getResult(ActionRunResult.class);

        assertThatCode(() -> sender.sendDecision(actionId, "CONFIRM", payloadHash))
                .doesNotThrowAnyException();
        verify(activity, times(1)).run(new ActionRunInput(input, payloadHash));
    }

    private void registerSearchAttributes() {
        testEnvironment.registerSearchAttribute("ActionKind", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
        testEnvironment.registerSearchAttribute("ActionConnector", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
        testEnvironment.registerSearchAttribute("ActionTenantId", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
        testEnvironment.registerSearchAttribute("ActionActorId", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
        testEnvironment.registerSearchAttribute("ActionStatus", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
    }

    private Action action(UUID actionId, String payloadHash) {
        var now = Instant.parse("2026-09-11T06:00:00Z");
        var action = Action.fromData(
                actionId,
                0,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "calendar-request-123",
                "calendar.create_event",
                "google-calendar",
                Map.of("title", "Demo"),
                payloadHash,
                ActionStatus.AWAITING_APPROVAL,
                null,
                now,
                now);
        action.applyDecision(ActionDecision.CONFIRM, payloadHash, now.plusSeconds(1));
        return action;
    }
}
