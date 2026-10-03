package dev.portableagent.action.config;

import dev.portableagent.action.model.ActionKind;
import dev.portableagent.action.service.ActionRule;
import dev.portableagent.action.util.MapUtil;
import dev.portableagent.action.workflow.ActionCall;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    Map<ActionKind, ActionRule> actionRulesByKind(List<ActionRule> rules) {
        return MapUtil.toMap(rules, ActionRule::kind);
    }

    @Bean
    Map<ActionKind, ActionCall> actionCallsByKind(List<ActionCall> calls) {
        return MapUtil.toMap(calls, ActionCall::kind);
    }
}
