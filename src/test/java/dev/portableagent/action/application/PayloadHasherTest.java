package dev.portableagent.action.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PayloadHasherTest {
    private final PayloadHasher payloadHasher = new PayloadHasher();

    @Test
    void sha256_whenMapOrderDiffers_shouldReturnSameHash() {
        var first = new LinkedHashMap<String, Object>();
        first.put("title", "Встреча");
        first.put("attendees", List.of("Коля"));
        var second = new LinkedHashMap<String, Object>();
        second.put("attendees", List.of("Коля"));
        second.put("title", "Встреча");

        assertThat(payloadHasher.sha256(first)).isEqualTo(payloadHasher.sha256(second));
    }

    @Test
    void sha256_whenPayloadChanges_shouldReturnDifferentHash() {
        assertThat(payloadHasher.sha256(Map.of("amount", 10)))
                .isNotEqualTo(payloadHasher.sha256(Map.of("amount", 11)));
    }
}
