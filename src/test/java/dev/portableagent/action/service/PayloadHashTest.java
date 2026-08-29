package dev.portableagent.action.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PayloadHashTest {
  private final PayloadHash payloadHash = new PayloadHash();

  @Test
  void make_whenMapOrderDiffers_shouldReturnSameHash() {
    var first = new LinkedHashMap<String, Object>();
    first.put("title", "Встреча");
    first.put("people", List.of("Коля"));
    var second = new LinkedHashMap<String, Object>();
    second.put("people", List.of("Коля"));
    second.put("title", "Встреча");

    assertThat(payloadHash.make(first)).isEqualTo(payloadHash.make(second));
  }

  @Test
  void make_whenPayloadChanges_shouldReturnDifferentHash() {
    assertThat(payloadHash.make(Map.of("amount", 10)))
        .isNotEqualTo(payloadHash.make(Map.of("amount", 11)));
  }
}
