package dev.portableagent.action.workflow;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import io.temporal.client.WorkflowClient;
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
    }

    @AfterEach
    void stopWorker() {
        testEnvironment.close();
    }

    @Test
    void run_whenActionIsConfirmed_shouldRunActivity() {
        var actionId = UUID.randomUUID();
        var payloadHash = "a".repeat(64);
        WorkflowClient.start(workflow::run, actionId);

        workflow.decision("CONFIRM", payloadHash);
        WorkflowStub.fromTyped(workflow).getResult(Void.class);

        verify(activity, timeout(2_000)).run(actionId, payloadHash);
    }

    @Test
    void run_whenActionIsCancelled_shouldNotRunActivity() {
        var actionId = UUID.randomUUID();
        WorkflowClient.start(workflow::run, actionId);

        workflow.decision("CANCEL", "a".repeat(64));
        WorkflowStub.fromTyped(workflow).getResult(Void.class);

        verify(activity, never()).run(actionId, "a".repeat(64));
    }
}
