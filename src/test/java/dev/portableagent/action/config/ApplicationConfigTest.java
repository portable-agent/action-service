package dev.portableagent.action.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.portableagent.action.model.ActionKind;
import dev.portableagent.action.service.ActionRule;
import dev.portableagent.action.service.ActionRules;
import dev.portableagent.action.workflow.ActionCall;
import dev.portableagent.action.workflow.ActionCalls;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ApplicationConfigTest {
    @Test
    void config_whenStrategiesExist_shouldInjectMapsByActionKind() {
        var rule = Mockito.mock(ActionRule.class);
        var call = Mockito.mock(ActionCall.class);
        when(rule.kind()).thenReturn(ActionKind.CALENDAR_CREATE_EVENT);
        when(call.kind()).thenReturn(ActionKind.CALENDAR_CREATE_EVENT);

        new ApplicationContextRunner()
                .withUserConfiguration(ApplicationConfig.class, ActionRules.class, ActionCalls.class)
                .withBean("calendarRule", ActionRule.class, () -> rule)
                .withBean("calendarCall", ActionCall.class, () -> call)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ActionRules.class);
                    assertThat(context).hasSingleBean(ActionCalls.class);
                    assertThat(context.getBean("actionRulesByKind", java.util.Map.class))
                            .containsEntry(ActionKind.CALENDAR_CREATE_EVENT, rule);
                    assertThat(context.getBean("actionCallsByKind", java.util.Map.class))
                            .containsEntry(ActionKind.CALENDAR_CREATE_EVENT, call);
                });
    }
}
