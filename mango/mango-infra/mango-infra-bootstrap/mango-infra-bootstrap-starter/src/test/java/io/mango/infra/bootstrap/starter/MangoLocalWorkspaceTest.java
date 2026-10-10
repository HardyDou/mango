package io.mango.infra.bootstrap.starter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MangoLocalWorkspaceTest {

    @Test
    void acceptsOnlyTheWorkspaceDatabaseForLocalStartup() {
        MangoLocalWorkspace.validateDatabase(
                Map.of("MANGO_DB_NAME", "mango_dev_test",
                        "MANGO_DB_URL", "jdbc:mysql://127.0.0.1:3306/mango_dev_test?useSSL=false"),
                new String[]{"--spring.main.banner-mode=off"});
    }

    @Test
    void rejectsDatasourceDriftFromTheWorkspaceDatabase() {
        assertThatThrownBy(() -> MangoLocalWorkspace.validateDatabase(
                Map.of("MANGO_DB_NAME", "mango_dev_test",
                        "MANGO_DB_URL", "jdbc:mysql://127.0.0.1:3306/production"),
                new String[0]))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MANGO_LOCAL_DATABASE_REJECTED");
    }
}
