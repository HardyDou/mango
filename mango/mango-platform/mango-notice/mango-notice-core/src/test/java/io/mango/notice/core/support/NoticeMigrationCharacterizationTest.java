package io.mango.notice.core.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeMigrationCharacterizationTest {

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+`([^`]+)`");
    private static final Pattern DML = Pattern.compile(
            "(?im)^\\s*(INSERT|UPDATE|DELETE|REPLACE)\\s+");

    private static final Set<String> EXPECTED_TABLES = Set.of(
            "notice_announcement",
            "notice_announcement_recipient",
            "notice_announcement_target",
            "notice_audit_log",
            "notice_business_channel_template",
            "notice_business_config_version",
            "notice_business_type",
            "notice_callback_log",
            "notice_channel_config",
            "notice_channel_config_route_tag",
            "notice_channel_route_tag",
            "notice_receive_preference",
            "notice_recipient",
            "notice_recipient_account",
            "notice_retry_log",
            "notice_send_record",
            "notice_setting",
            "notice_site_message",
            "notice_site_message_action",
            "notice_site_message_action_request",
            "notice_task",
            "notice_wecom_sync_mapping");

    @Test
    void migrationsDescribeTheCompleteCurrentNoticeSchema() throws IOException, URISyntaxException {
        List<Path> migrations = migrations();
        assertThat(migrations)
                .extracting(path -> path.getFileName().toString())
                .containsExactly("V1__init_notice.sql", "V2__notice_channel_resource_route_and_secret.sql");

        String sql = read(migrations.getFirst());
        String upgradeSql = read(migrations.get(1));

        Matcher matcher = CREATE_TABLE.matcher(sql);
        Set<String> tables = new TreeSet<>();
        while (matcher.find()) {
            tables.add(matcher.group(1));
        }

        assertThat(tables).containsExactlyElementsOf(new TreeSet<>(EXPECTED_TABLES));
        assertThat(sql)
                .contains("`domain_code`")
                .contains("`recipient_targets_snapshot`")
                .contains("`channel_config_id`")
                .contains("`config_code`")
                .contains("`secret_refs_json`")
                .contains("`route_mode`")
                .contains("`notice_channel_route_tag`")
                .contains("`biz_type`")
                .contains("`biz_id`")
                .contains("`message_actions_json`")
                .contains("`notice_site_message_action_request`")
                .contains("`notice_announcement_recipient`");
        assertThat(DML.matcher(sql).find()).isFalse();
        assertThat(upgradeSql)
                .contains("information_schema.columns")
                .contains("information_schema.statistics")
                .contains("CONCAT('LEGACY_', `id`)")
                .contains("WHEN `channel_config_id` IS NULL THEN 'AUTO' ELSE 'EXACT'");
    }

    private List<Path> migrations() throws IOException, URISyntaxException {
        Path root = Path.of(getClass().getClassLoader().getResource("db/migration/notice").toURI());
        try (var paths = Files.list(root)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read " + path, ex);
        }
    }
}
