package dev.portableagent.action.dto;

import dev.portableagent.action.model.ActionDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DecideActionRequest(
    @NotNull ActionDecision decision,
    @NotBlank @Pattern(regexp = "^[a-f0-9]{64}$") String payloadHash) {}
