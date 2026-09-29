package dev.portableagent.action.service;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CalendarActionRule implements ActionRule {
    private static final String KIND = "calendar.create_event";
    private static final Set<String> CONNECTORS = Set.of("fake-calendar", "google-calendar");

    private final CalendarInputCheck inputCheck;

    public CalendarActionRule(CalendarInputCheck inputCheck) {
        this.inputCheck = inputCheck;
    }

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public void check(String connector, Map<String, Object> payload) {
        if (!CONNECTORS.contains(connector)) {
            throw new IllegalArgumentException("Unsupported connector: " + connector);
        }
        inputCheck.check(payload);
    }
}
