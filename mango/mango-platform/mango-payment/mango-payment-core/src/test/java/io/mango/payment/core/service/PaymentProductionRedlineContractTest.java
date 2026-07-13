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
    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration/payment");
    private static final Path PAYMENT_BASELINE = MIGRATION_DIR.resolve("V1__payment_platform.sql");
    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            "(?i)\\b(mock|fake|dummy|hardcode|hard-coded|todo|fixme)\\b|UnsupportedOperationException|固定成功|模拟|伪代码|沙箱|sandbox");
    private static final List<String> RETIRED_RUNTIME_VALUES = List.of(
            "BO202605250001", "PO202605250001", "RO202605250001", "FLOW202605250001",
            "EX202605250001", "NT202605250001", "RC202605250001", "DF202605250001",
            "MANGO_PAY-T202605250001");

    @Test
    @DisplayName("payment production source should not contain redline delivery tokens")
    void paymentProductionSource_shouldNotContainRedlineDeliveryTokens() throws IOException {
        List<String> violations = productionSourceFiles()
                .flatMap(path -> forbiddenLines(path).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("payment should expose one clean V1 without retired runtime values")
    void paymentBaseline_shouldBeSingleAndRuntimeClean() throws IOException {
        try (Stream<Path> migrations = Files.list(MIGRATION_DIR)) {
            assertThat(migrations
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("V[0-9]+__.+\\.sql"))
                    .toList())
                    .containsExactly(PAYMENT_BASELINE);
        }
        String baseline = read(PAYMENT_BASELINE);
        RETIRED_RUNTIME_VALUES.forEach(value -> assertThat(baseline).doesNotContain(value));
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
