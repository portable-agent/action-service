package dev.portableagent.action.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PayloadHasher {
    public String sha256(Map<String, Object> payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical(payload).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String canonical(Object value) {
        return switch (value) {
            case null -> "null";
            case Map<?, ?> map -> map.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .map(entry -> token(String.valueOf(entry.getKey())) + canonical(entry.getValue()))
                    .collect(java.util.stream.Collectors.joining("", "map[", "]"));
            case List<?> list -> list.stream()
                    .map(this::canonical)
                    .collect(java.util.stream.Collectors.joining("", "list[", "]"));
            case Number number -> "number:" + number;
            case Boolean bool -> "boolean:" + bool;
            default -> token(String.valueOf(value));
        };
    }

    private String token(String value) {
        return "string:" + value.length() + ":" + value;
    }
}
