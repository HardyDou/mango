package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProductionRedlineContractTest {

    private static final Path PAYMENT_MODULE_DIR = Path.of("..");
    private static final Path PAYMENT_UI_DIR = Path.of("../../../../mango-ui/packages/payment/src");
    private static final Path LEGACY_SEED_MIGRATION =
            Path.of("src/main/resources/db/migration/payment/V4__payment_mango_pay_seed.sql");
    private static final Path SEED_CLEANUP_MIGRATION =
            Path.of("src/main/resources/db/migration/payment/V64__payment_remove_seeded_business_runtime_data.sql");
    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            "(?i)\\b(mock|fake|dummy|hardcode|hard-coded|todo|fixme)\\b|UnsupportedOperationException|固定成功|模拟|伪代码|沙箱|sandbox");
    private static final List<String> SEEDED_RUNTIME_VALUES = List.of(
            "BO202605250001",
            "BO202605250002",
            "BO202605250003",
            "PO202605250001",
            "PO202605250002",
            "PO202605250003",
            "RO202605250001",
            "RO202605250002",
            "BR202605250001",
            "BR202605250002",
            "FLOW202605250001",
            "FLOW202605250002",
            "FLOW202605250003",
            "EX202605250001",
            "EX202605250002",
            "NT202605250001",
            "NT202605250002",
            "RC202605250001",
            "RC202605250002",
            "DF202605250001",
            "DF202605250002",
            "MANGO_PAY-T202605250001",
            "MANGO_PAY-T202605250002",
            "ALLINPAY-T202605250003"
    );

    @Test
    @DisplayName("payment production source should not contain redline delivery tokens")
    void paymentProductionSource_shouldNotContainRedlineDeliveryTokens() throws IOException {
        List<String> violations = productionSourceFiles()
                .flatMap(path -> forbiddenLines(path).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("seeded runtime rows should remain covered by cleanup migration")
    void seededRuntimeRows_shouldRemainCoveredByCleanupMigration() throws IOException {
        String legacySeed = read(LEGACY_SEED_MIGRATION);
        String cleanupMigration = read(SEED_CLEANUP_MIGRATION);

        assertThat(legacySeed).containsSubsequence(
                "INSERT INTO `payment_business_order`",
                "INSERT INTO `payment_order`",
                "INSERT INTO `payment_refund_order`",
                "INSERT INTO `payment_transaction_flow`",
                "INSERT INTO `payment_exception_order`",
                "INSERT INTO `payment_notification_record`",
                "INSERT INTO `payment_reconciliation`",
                "INSERT INTO `payment_difference`",
                "INSERT INTO `payment_settlement_summary`",
                "INSERT INTO `payment_operation_audit`"
        );
        for (String value : SEEDED_RUNTIME_VALUES) {
            assertThat(legacySeed).contains(value);
            assertThat(cleanupMigration).contains(value);
        }
    }

    private Stream<Path> productionSourceFiles() throws IOException {
        Stream<Path> backendFiles = existingFiles(PAYMENT_MODULE_DIR)
                .filter(path -> path.toString().contains("/src/main/"))
                .filter(path -> hasExtension(path, ".java", ".xml", ".yml", ".yaml", ".properties", ".sql"));
        Stream<Path> uiFiles = existingFiles(PAYMENT_UI_DIR)
                .filter(path -> hasExtension(path, ".ts", ".vue", ".js"));
        return Stream.concat(backendFiles, uiFiles)
                .filter(path -> !path.toString().contains("/target/"))
                .filter(path -> !path.toString().contains("/node_modules/"));
    }

    private Stream<Path> existingFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return Stream.empty();
        }
        return Files.walk(root).filter(Files::isRegularFile);
    }

    private boolean hasExtension(Path path, String... extensions) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private List<String> forbiddenLines(Path path) {
        return lines(path)
                .filter(line -> FORBIDDEN_PATTERN.matcher(line.content()).find())
                .map(line -> path + ":" + line.number() + " -> " + line.content().trim())
                .toList();
    }

    private Stream<SourceLine> lines(Path path) {
        String[] split = read(path).split("\\R");
        return java.util.stream.IntStream.range(0, split.length)
                .mapToObj(index -> new SourceLine(index + 1, split[index]));
    }

    private String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + path, ex);
        }
    }

    private record SourceLine(int number, String content) {
    }
}
