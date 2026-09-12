package dev.portableagent.action.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SecuritySettingsTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(Settings.class);

    @Test
    void settings_shouldRequireActionServiceAudienceByDefault() {
        contextRunner.run(context -> assertThat(context.getBean(OAuth2ResourceServerProperties.class)
                        .getJwt()
                        .getAudiences())
                .containsExactly("action-service"));
    }

    @Test
    void settings_whenAudienceIsSet_shouldUseIt() {
        contextRunner.withPropertyValues("OIDC_AUDIENCE=custom-action").run(context -> assertThat(
                        context.getBean(OAuth2ResourceServerProperties.class)
                                .getJwt()
                                .getAudiences())
                .containsExactly("custom-action"));
    }

    @EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
    static class Settings {}
}
