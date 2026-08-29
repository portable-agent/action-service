package dev.portableagent.action.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CreateActionRequest(
    @NotBlank @Size(max = 80) String kind,
    @NotBlank @Size(max = 80) String connector,
    @NotEmpty Map<String, Object> payload,
    @NotBlank @Size(min = 8, max = 128) String requestKey) {}
