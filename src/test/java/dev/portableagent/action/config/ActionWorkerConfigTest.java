package dev.portableagent.action.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.portableagent.action.model.ActionKind;
import dev.portableagent.action.model.ActionStatus;
import dev.portableagent.action.workflow.ActionActivityImpl;
import dev.portableagent.action.workflow.ActionRunInput;
import dev.portableagent.action.workflow.ActionRunResult;
import dev.portableagent.action.workflow.ActionWorkflow;
import dev.portableagent.action.workflow.ActionWorkflowInput;
import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.WorkerFactory;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ActionWorkerConfigTest {
    private TestWorkflowEnvironment testEnvironment;
    private ActionActivityImpl activity;
    private TemporalProperties properties;

    @BeforeEach
    void setUp() {
        testEnvironment = TestWorkflowEnvironment.newInstance();
        registerSearchAttributes();
        activity = Mockito.mock(ActionActivityImpl.class);
        properties = new TemporalProperties("unused", "default", "action-test");
    }

    private void registerSearchAttributes() {
        testEnvironment.registerSearchAttribute("ActionKind", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
        testEnvironment.registerSearchAttribute("ActionConnector", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
        testEnvironment.registerSearchAttribute("ActionTenantId", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
        testEnvironment.registerSearchAttribute("ActionActorId", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
        testEnvironment.registerSearchAttribute("ActionStatus", IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD);
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
                .withBean("actionActivity", ActionActivityImpl.class, () -> activity)
                .run(context -> {
                    assertThat(context).hasSingleBean(WorkerFactory.class);
                    var payloadHash = "a".repeat(64);
                    var input = input(payloadHash);
                    var runInput = new ActionRunInput(input, payloadHash);
                    var runResult = new ActionRunResult(
                            input.actionId(),
                            input.kind(),
                            input.connector(),
                            ActionStatus.SUCCEEDED,
                            Map.of("eventId", "event-123"));
                    when(activity.run(runInput)).thenReturn(runResult);
                    var workflow = testEnvironment
                            .getWorkflowClient()
                            .newWorkflowStub(
                                    ActionWorkflow.class,
                                    WorkflowOptions.newBuilder()
                                            .setWorkflowId("action-" + input.actionId())
                                            .setTaskQueue(properties.taskQueue())
                                            .build());
                    WorkflowClient.start(workflow::run, input);
                    workflow.decision("CONFIRM", payloadHash);

                    WorkflowStub.fromTyped(workflow).getResult(ActionRunResult.class);

                    verify(activity, timeout(2_000)).run(runInput);
                });
    }

    private ActionWorkflowInput input(String payloadHash) {
        return new ActionWorkflowInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "calendar-request-123",
                ActionKind.CALENDAR_CREATE_EVENT,
                "google-calendar",
                Map.of("title", "Demo"),
                payloadHash);
    }
}
