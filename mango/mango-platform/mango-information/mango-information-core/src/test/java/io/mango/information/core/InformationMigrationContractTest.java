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

    /**
     * 平台标准业务库是 utf8mb4 + utf8mb4_unicode_ci（mango-cli 建库、Bootstrap、baseline 生成器一致）。
     * 迁移里硬编码 utf8mb4_0900_ai_ci 会让本模块表与业务表排序规则不一致，
     * 字符串 JOIN/比较时出现 Illegal mix of collations，因此这里锁住契约。
     */
    @Test
    void usesPlatformStandardCollation() throws IOException {
        String migration = new String(
                getClass().getResourceAsStream("/db/migration/mango-information/V1__init_mango_information.sql").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(migration).doesNotContain("0900_ai_ci");
        assertThat(migration.split("utf8mb4_unicode_ci", -1)).hasSize(4);
    }
}
