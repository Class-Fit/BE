package com.example.classfit.config;

import com.example.classfit.security.WebSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProductionConfigurationTest {

    @Test
    void productionProfileRequiresExternalSettingsAndUsesSafeDefaults() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application-prod", new ClassPathResource("application-prod.yaml"));
        sources.forEach(environment.getPropertySources()::addLast);

        PropertySource<?> production = sources.getFirst();

        assertThat(production.getProperty("spring.datasource.url")).isEqualTo("${DB_URL}");
        assertThat(production.getProperty("spring.datasource.username")).isEqualTo("${DB_USER}");
        assertThat(production.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(environment.getProperty("spring.jpa.show-sql")).isEqualTo("false");
        assertThat(environment.getProperty("server.forward-headers-strategy")).isEqualTo("framework");
        assertThat(environment.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
        assertThat(environment.getProperty("public-data.sync.enabled")).isEqualTo("false");
        assertThat(production.getProperty("classfit.web.frontend-origin"))
                .isEqualTo("${CLASSFIT_FRONTEND_ORIGIN}");
    }

    @Test
    void blankFrontendOriginIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new WebSecurityProperties("  "))
                .withMessageContaining("frontend-origin");
    }
}
