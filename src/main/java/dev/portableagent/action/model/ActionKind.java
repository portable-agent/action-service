package dev.portableagent.action.model;

public enum ActionKind {
    CALENDAR_CREATE_EVENT("calendar.create_event");

    private final String value;

    ActionKind(String value) {
        this.value = value;
    }

    public static ActionKind from(String value) {
        for (var kind : values()) {
            if (kind.value.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unsupported action kind: " + value);
    }

    public String value() {
        return value;
    }
}
