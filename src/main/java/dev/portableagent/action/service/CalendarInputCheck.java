package dev.portableagent.action.service;

import dev.portableagent.action.exception.InvalidActionInput;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CalendarInputCheck {
    public void check(Map<String, Object> input) {
        var startAt = readDate(input, "startAt");
        var endAt = readDate(input, "endAt");
        if (!endAt.isAfter(startAt)) {
            throw new InvalidActionInput("endAt must be after startAt");
        }

        var timeZone = readText(input, "timeZone");
        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException error) {
            throw new InvalidActionInput("timeZone must be a valid zone");
        }
    }

    private OffsetDateTime readDate(Map<String, Object> input, String name) {
        var value = readText(input, name);
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeException error) {
            throw new InvalidActionInput(name + " must be a valid date with offset");
        }
    }

    private String readText(Map<String, Object> input, String name) {
        if (input.get(name) instanceof String value && !value.isBlank()) {
            return value;
        }
        throw new InvalidActionInput(name + " must be text");
    }
}
