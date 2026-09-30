package dev.portableagent.action.workflow;

import dev.portableagent.action.model.Action;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ActionCalls {
    private final Map<String, ActionCall> byKind;

    public ActionCalls(List<ActionCall> calls) {
        byKind = calls.stream().collect(Collectors.toUnmodifiableMap(ActionCall::kind, Function.identity()));
    }

    public String run(Action action) {
        var call = byKind.get(action.getKind());
        if (call == null) {
            throw new IllegalArgumentException("Unsupported action kind: " + action.getKind());
        }
        return call.run(action);
    }
}
