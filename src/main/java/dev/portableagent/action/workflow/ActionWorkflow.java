package dev.portableagent.action.workflow;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.UUID;

@WorkflowInterface
public interface ActionWorkflow {
  @WorkflowMethod
  void run(UUID actionId);

  @SignalMethod
  void decision(String decision, String payloadHash);
}
