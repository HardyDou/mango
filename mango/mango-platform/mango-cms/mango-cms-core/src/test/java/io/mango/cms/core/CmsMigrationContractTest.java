package io.mango.cms.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CmsMigrationContractTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration/mango-cms");
    private static final Path BASELINE = MIGRATION_DIR.resolve("V1__init_mango_cms.sql");
    private static final Pattern DML = Pattern.compile(
            "(?im)^\\s*(insert|update|delete|replace|merge)\\s+(into\\s+|from\\s+)?");
    private static final Set<String> TABLES = Set.of(
            "cms_site",
            "cms_site_category",
            "cms_content_category",
            "cms_content_tag",
            "cms_content",
            "cms_content_tag_rel",
            "cms_content_publish",
            "cms_navigation",
            "cms_advertisement",
            "cms_ad_delivery",
            "cms_site_setting");

    @Test
    @DisplayName("CMS should expose the V1 baseline plus the follow-up migrations")
    void cmsExposesVersionedMigrations() throws IOException {
        try (Stream<Path> migrations = Files.list(MIGRATION_DIR)) {
            assertThat(migrations
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("V[0-9]+__.+\\.sql"))
                    .map(path -> path.getFileName().toString())
                    .toList())
                    .containsExactlyInAnyOrder(
                            "V1__init_mango_cms.sql",
                            "V2__drop_cms_banner.sql",
                            "V3__cms_publish_history.sql");
        }
    }

    @Test
    @DisplayName("CMS must not create or keep the removed banner table")
    void cmsDropsRemovedBannerTable() throws IOException {
        assertThat(Files.readString(BASELINE, StandardCharsets.UTF_8)).doesNotContain("cms_banner");
        assertThat(Files.readString(MIGRATION_DIR.resolve("V2__drop_cms_banner.sql"), StandardCharsets.UTF_8))
                .contains("DROP TABLE IF EXISTS `cms_banner`");
    }

    @Test
    @DisplayName("CMS V1 should contain only module-owned DDL")
    void cmsV1ContainsOnlyModuleOwnedDdl() throws IOException {
        String sql = Files.readString(BASELINE, StandardCharsets.UTF_8);

        assertThat(DML.matcher(sql).find()).isFalse();
        TABLES.forEach(table -> assertThat(sql).contains("CREATE TABLE IF NOT EXISTS " + table));
        assertThat(sql)
                .doesNotContain("authorization_")
                .doesNotContain("resource_registry")
                .doesNotContain("file_record");
    }
}
