package dev.portableagent.action.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.portableagent.action.exception.InvalidActionInput;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalendarInputCheckTest {
    private final CalendarInputCheck check = new CalendarInputCheck();

    @Test
    void check_whenInputIsValid_shouldAcceptIt() {
        assertThatCode(() -> check.check(validInput())).doesNotThrowAnyException();
    }

    @Test
    void check_whenEndIsNotAfterStart_shouldRejectIt() {
        var input = Map.<String, Object>of(
                "startAt", "2026-09-01T12:30:00+03:00",
                "endAt", "2026-09-01T12:00:00+03:00",
                "timeZone", "Europe/Moscow");

        assertThatThrownBy(() -> check.check(input))
                .isInstanceOf(InvalidActionInput.class)
                .hasMessage("endAt must be after startAt");
    }

    @Test
    void check_whenTimeZoneDoesNotExist_shouldRejectIt() {
        var input = Map.<String, Object>of(
                "startAt", "2026-09-01T12:00:00+03:00",
                "endAt", "2026-09-01T12:30:00+03:00",
                "timeZone", "Moon/Base");

        assertThatThrownBy(() -> check.check(input))
                .isInstanceOf(InvalidActionInput.class)
                .hasMessage("timeZone must be a valid zone");
    }

    private Map<String, Object> validInput() {
        return Map.of(
                "startAt", "2026-09-01T12:00:00+03:00",
                "endAt", "2026-09-01T12:30:00+03:00",
                "timeZone", "Europe/Moscow");
    }
}
