package io.mango.infra.bootstrap.starter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MangoApplicationTest {

    private static final AtomicReference<LifecycleSnapshot> SNAPSHOT = new AtomicReference<>();

    @AfterEach
    void resetSnapshot() {
        SNAPSHOT.set(null);
    }

    @Test
    void runtimeModeKeepsContextOpenAndDisablesImplicitFlyway() {
        ConfigurableApplicationContext context = MangoApplication.run(TestApplication.class,
                "runtime", "--spring.main.banner-mode=off");
        try {
            assertThat(context.isActive()).isTrue();
            assertThat(SNAPSHOT.get()).isEqualTo(new LifecycleSnapshot("runtime", null, "false"));
        } finally {
            context.close();
        }
    }

    @Test
    void bootstrapModeConsumesActionRunsWithoutWebServerAndClosesContext() {
        ConfigurableApplicationContext context = MangoApplication.run(TestApplication.class,
                "bootstrap", "apply", "--spring.main.banner-mode=off");

        assertThat(context.isActive()).isFalse();
        assertThat(SNAPSHOT.get()).isEqualTo(new LifecycleSnapshot("bootstrap", "apply", "false"));
    }

    @Test
    void bootstrapModeAcceptsAbortAction() {
        ConfigurableApplicationContext context = MangoApplication.run(TestApplication.class,
                "bootstrap", "abort", "--spring.main.banner-mode=off");

        assertThat(context.isActive()).isFalse();
        assertThat(SNAPSHOT.get()).isEqualTo(new LifecycleSnapshot("bootstrap", "abort", "false"));
    }

    @Test
    void rejectsMissingOrUnsupportedLifecycleCommandsBeforeStartingSpring() {
        assertThatThrownBy(() -> MangoApplication.run(TestApplication.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("process mode is required");
        assertThatThrownBy(() -> MangoApplication.run(TestApplication.class, "serve"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported Mango process mode");
        assertThatThrownBy(() -> MangoApplication.run(TestApplication.class, "bootstrap", "--debug"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bootstrap action is required");
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    static class TestApplication {

        @Bean
        ApplicationRunner lifecycleSnapshot(Environment environment) {
            return args -> SNAPSHOT.set(new LifecycleSnapshot(
                    environment.getProperty("mango.bootstrap.mode"),
                    environment.getProperty("mango.bootstrap.action"),
                    environment.getProperty("spring.flyway.enabled")));
        }
    }

    private record LifecycleSnapshot(String mode, String action, String flywayEnabled) {
    }
}
