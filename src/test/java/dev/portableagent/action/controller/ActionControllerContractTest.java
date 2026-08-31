package dev.portableagent.action.controller;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.action.api.ActionsApi;
import org.junit.jupiter.api.Test;

class ActionControllerContractTest {
  @Test
  void controller_shouldImplementGeneratedContract() {
    assertThat(ActionsApi.class).isAssignableFrom(ActionController.class);
  }
}
