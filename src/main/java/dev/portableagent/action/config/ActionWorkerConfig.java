package dev.portableagent.action.config;

import dev.portableagent.action.workflow.ActionActivity;
import dev.portableagent.action.workflow.ActionWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.WorkerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ActionWorkerConfig {
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "mcp.gateway.enabled", havingValue = "true")
    WorkerFactory actionWorkerFactory(
            WorkflowClient workflowClient, TemporalProperties properties, ActionActivity activity) {
        var factory = WorkerFactory.newInstance(workflowClient);
        var worker = factory.newWorker(properties.taskQueue());
        worker.registerWorkflowImplementationTypes(ActionWorkflowImpl.class);
        worker.registerActivitiesImplementations(activity);
        return factory;
    }
}
