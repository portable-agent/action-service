package dev.portableagent.action.api;

import dev.portableagent.action.domain.ActionDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ActionDecisionRequest(
        @NotNull ActionDecision decision,
        @Pattern(regexp = "^[a-f0-9]{64}$") String payloadHash) {}
