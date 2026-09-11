package dev.portableagent.action.workflow;

import io.temporal.activity.ActivityInterface;
import java.util.UUID;

@ActivityInterface
public interface ActionActivity {
    void run(UUID actionId, String payloadHash);
}
