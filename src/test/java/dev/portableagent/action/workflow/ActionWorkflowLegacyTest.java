package dev.portableagent.action.workflow;

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

class ActionWorkflowLegacyTest {
    private TestWorkflowEnvironment testEnvironment;
    private ActionActivityImpl activity;

    @BeforeEach
    void startWorker() {
        testEnvironment = TestWorkflowEnvironment.newInstance();
        var worker = testEnvironment.newWorker("action-legacy-test");
        activity = Mockito.mock(ActionActivityImpl.class);
        worker.registerWorkflowImplementationTypes(ActionWorkflowImpl.class);
        worker.registerActivitiesImplementations(activity);
        testEnvironment.start();
    }

    @AfterEach
    void stopWorker() {
        testEnvironment.close();
    }

    @Test
    void run_whenOldWorkflowIsConfirmed_shouldKeepOldActivityContract() {
        var actionId = UUID.randomUUID();
        var payloadHash = "a".repeat(64);
        var workflow = testEnvironment
                .getWorkflowClient()
                .newWorkflowStub(
                        ActionWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId("legacy-action-" + actionId)
                                .setTaskQueue("action-legacy-test")
                                .build());

        WorkflowClient.start(workflow::run, actionId);
        workflow.decision("CONFIRM", payloadHash);
        WorkflowStub.fromTyped(workflow).getResult(Void.class);

        verify(activity, timeout(2_000)).run(actionId, payloadHash);
    }
}
