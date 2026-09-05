package dev.portableagent.action.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

class TomcatVersionTest {

  private static final int SAFE_PATCH_VERSION = 25;

  @Test
  void application_shouldUseSafeTomcatVersion() {
    String version = ServerInfo.getServerNumber();
    String[] parts = version.split("\\.");

    assertThat(parts).hasSizeGreaterThanOrEqualTo(3);
    assertThat(Integer.parseInt(parts[0])).isEqualTo(11);
    assertThat(Integer.parseInt(parts[1])).isZero();
    assertThat(Integer.parseInt(parts[2]))
        .as("Tomcat 11.0.24 and older contain known critical vulnerabilities")
        .isGreaterThanOrEqualTo(SAFE_PATCH_VERSION);
  }
}
