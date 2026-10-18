package io.mango.information.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class InformationMigrationContractTest {

    @Test
    void declaresFormSubmissionAndSnapshotTables() throws IOException {
        String migration = new String(
                getClass().getResourceAsStream("/db/migration/mango-information/V1__init_mango_information.sql").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(migration)
                .contains("CREATE TABLE IF NOT EXISTS `information_form`")
                .contains("CREATE TABLE IF NOT EXISTS `information_submission`")
                .contains("CREATE TABLE IF NOT EXISTS `information_submission_snapshot`")
                .contains("uk_information_form_tenant_code")
                .contains("uk_information_submission_idempotency");
    }
}
