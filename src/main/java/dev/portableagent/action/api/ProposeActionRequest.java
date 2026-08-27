package dev.portableagent.action.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record ProposeActionRequest(
        @NotBlank @Size(max = 80) String kind,
        @NotBlank @Size(max = 80) String connector,
        @NotEmpty Map<String, Object> payload,
        @NotBlank @Size(min = 8, max = 128) String idempotencyKey) {}
