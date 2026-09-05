package dev.portableagent.action.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/not-used")
@Testcontainers(disabledWithoutDocker = true)
class FlywayStartupTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

  @Autowired private DSLContext db;

  @Test
  void application_whenStarted_shouldRunFlywayMigrations() {
    assertThat(table("action_proposals")).isEqualTo("action_proposals");
    assertThat(table("action_dispatch_outbox")).isEqualTo("action_dispatch_outbox");
  }

  private String table(String name) {
    return db.fetchOne("select to_regclass(?)", "public." + name).get(0, String.class);
  }
}
