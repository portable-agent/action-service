package dev.portableagent.action.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class MapUtilTest {
    @Test
    void toMap_whenKeysAreUnique_shouldMakeMap() {
        var result = MapUtil.toMap(List.of("one", "three"), String::length);

        assertThat(result).containsEntry(3, "one").containsEntry(5, "three");
    }

    @Test
    void toMap_whenKeyIsRepeated_shouldRejectValues() {
        assertThatThrownBy(() -> MapUtil.toMap(List.of("one", "two"), String::length))
                .isInstanceOf(IllegalStateException.class);
    }
}
