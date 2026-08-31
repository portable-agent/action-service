package dev.portableagent.action.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ActionRepositoryWiringTest {
  @Test
  void context_whenBootJacksonIsUsed_shouldCreateRepository() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(DSLContext.class, () -> mock(DSLContext.class));
      context.register(JacksonAutoConfiguration.class, ActionRepository.class);
      context.refresh();

      assertThat(context.getBean(ActionRepository.class)).isNotNull();
    }
  }
}
