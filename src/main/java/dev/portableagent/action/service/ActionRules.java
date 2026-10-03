package dev.portableagent.action.service;

import dev.portableagent.action.model.ActionKind;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActionRules {
    private final Map<ActionKind, ActionRule> byKind;

    public void check(CreateActionCommand request) {
        var rule = byKind.get(ActionKind.from(request.kind()));
        if (rule == null) {
            throw new IllegalArgumentException("Unsupported action kind: " + request.kind());
        }
        rule.check(request.connector(), request.payload());
    }
}
