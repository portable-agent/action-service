package dev.portableagent.action.workflow;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.portableagent.action.client.McpCallFailed;
import dev.portableagent.action.config.TemporalProperties;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ActionWorkflowTest {
    private TestWorkflowEnvironment testEnvironment;
    private ActionActivity activity;
    private ActionWorkflow workflow;
    private TemporalSender sender;

    @BeforeEach
    void startWorker() {
        testEnvironment = TestWorkflowEnvironment.newInstance();
        var worker = testEnvironment.newWorker("action-test");
        activity = Mockito.mock(ActionActivity.class);
        worker.registerWorkflowImplementationTypes(ActionWorkflowImpl.class);
        worker.registerActivitiesImplementations(activity);
        testEnvironment.start();

        workflow = testEnvironment
                .getWorkflowClient()
                .newWorkflowStub(
                        ActionWorkflow.class,
                        WorkflowOptions.newBuilder().setTaskQueue("action-test").build());
        sender = new TemporalSender(
                testEnvironment.getWorkflowClient(), new TemporalProperties("unused", "default", "action-test"));
    }

    @AfterEach
    void stopWorker() {
        testEnvironment.close();
    }

    @Test
    void run_whenActionIsConfirmed_shouldRunActivity() {
        var actionId = UUID.randomUUID();
        var payloadHash = "a".repeat(64);
        sender.sendDecision(actionId, "CONFIRM", payloadHash);
        workflow = testEnvironment.getWorkflowClient().newWorkflowStub(ActionWorkflow.class, "action-" + actionId);
        WorkflowStub.fromTyped(workflow).getResult(Void.class);

        verify(activity, timeout(2_000)).run(actionId, payloadHash);
    }

    @Test
    void run_whenActionIsCancelled_shouldNotRunActivity() {
        var actionId = UUID.randomUUID();
        sender.sendDecision(actionId, "CANCEL", "a".repeat(64));
        workflow = testEnvironment.getWorkflowClient().newWorkflowStub(ActionWorkflow.class, "action-" + actionId);
        WorkflowStub.fromTyped(workflow).getResult(Void.class);

        verify(activity, never()).run(actionId, "a".repeat(64));
    }

    @Test
    void run_whenMcpCallKeepsFailing_shouldRetryAndMarkActionFailed() {
        var actionId = UUID.randomUUID();
        var payloadHash = "a".repeat(64);
        doThrow(new McpCallFailed("Gateway call failed")).when(activity).run(actionId, payloadHash);

        sender.sendDecision(actionId, "CONFIRM", payloadHash);
        workflow = testEnvironment.getWorkflowClient().newWorkflowStub(ActionWorkflow.class, "action-" + actionId);
        WorkflowStub.fromTyped(workflow).getResult(Void.class);

        verify(activity, times(3)).run(actionId, payloadHash);
        verify(activity).fail(actionId);
    }

    @Test
    void sendDecision_whenWorkflowAlreadyFinished_shouldKeepRetrySafe() {
        var actionId = UUID.randomUUID();
        var payloadHash = "a".repeat(64);
        sender.sendDecision(actionId, "CONFIRM", payloadHash);
        workflow = testEnvironment.getWorkflowClient().newWorkflowStub(ActionWorkflow.class, "action-" + actionId);
        WorkflowStub.fromTyped(workflow).getResult(Void.class);

        assertThatCode(() -> sender.sendDecision(actionId, "CONFIRM", payloadHash))
                .doesNotThrowAnyException();
        verify(activity, times(1)).run(actionId, payloadHash);
    }
}
