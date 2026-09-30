package dev.portableagent.action.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ActionRules {
    private final Map<String, ActionRule> byKind;

    public ActionRules(List<ActionRule> rules) {
        this.byKind = rules.stream().collect(Collectors.toUnmodifiableMap(ActionRule::kind, Function.identity()));
    }

    public void check(CreateActionCommand request) {
        var rule = byKind.get(request.kind());
        if (rule == null) {
            throw new IllegalArgumentException("Unsupported action kind: " + request.kind());
        }
        rule.check(request.connector(), request.payload());
    }
}
