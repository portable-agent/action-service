package dev.portableagent.action.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import dev.portableagent.action.workflow.ActionActivity;
import dev.portableagent.action.workflow.ActionWorkflow;
import dev.portableagent.action.workflow.TemporalSender;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.WorkerFactory;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ActionWorkerConfigTest {
    private TestWorkflowEnvironment testEnvironment;
    private ActionActivity activity;
    private TemporalProperties properties;

    @BeforeEach
    void setUp() {
        testEnvironment = TestWorkflowEnvironment.newInstance();
        activity = Mockito.mock(ActionActivity.class);
        properties = new TemporalProperties("unused", "default", "action-test");
    }

    @AfterEach
    void close() {
        testEnvironment.close();
    }

    @Test
    void config_whenActivityExists_shouldStartWorker() {
        new ApplicationContextRunner()
                .withUserConfiguration(ActionWorkerConfig.class)
                .withPropertyValues("mcp.gateway.enabled=true")
                .withBean("workflowClient", WorkflowClient.class, () -> testEnvironment.getWorkflowClient())
                .withBean("temporalProperties", TemporalProperties.class, () -> properties)
                .withBean("actionActivity", ActionActivity.class, () -> activity)
                .run(context -> {
                    assertThat(context).hasSingleBean(WorkerFactory.class);
                    var actionId = UUID.randomUUID();
                    var payloadHash = "a".repeat(64);
                    new TemporalSender(testEnvironment.getWorkflowClient(), properties)
                            .sendDecision(actionId, "CONFIRM", payloadHash);
                    var workflow = testEnvironment
                            .getWorkflowClient()
                            .newWorkflowStub(ActionWorkflow.class, "action-" + actionId);

                    WorkflowStub.fromTyped(workflow).getResult(Void.class);

                    verify(activity, timeout(2_000)).run(actionId, payloadHash);
                });
    }
}
