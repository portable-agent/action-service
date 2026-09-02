package dev.portableagent.action.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class FlywayStartupTest {
  @Test
  void application_whenStarted_shouldHaveFlywayAutoConfiguration() {
    assertThatCode(
            () ->
                Class.forName(
                    "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"))
        .doesNotThrowAnyException();
  }
}
