package dev.portableagent.action.workflow;

import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionKind;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActionCalls {
    private final Map<ActionKind, ActionCall> byKind;

    public String run(Action action) {
        var call = byKind.get(ActionKind.from(action.getKind()));
        if (call == null) {
            throw new IllegalArgumentException("Unsupported action kind: " + action.getKind());
        }
        return call.run(action);
    }
}
