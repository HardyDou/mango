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

class PaymentSensitiveLogContractTest {

    private static final Path PAYMENT_MODULE_DIR = Path.of("..");
    private static final Pattern LOG_OR_CONSOLE_LINE = Pattern.compile(
            "\\b(log|LOGGER)\\s*\\.|System\\.(out|err)\\.|printStackTrace\\s*\\(");
    private static final List<String> SENSITIVE_TOKENS = List.of(
            "appsecret",
            "app_secret",
            "secret",
            "privatekey",
            "private_key",
            "apikey",
            "api_key",
            "bankaccountno",
            "bank_account_no",
            "accountno",
            "account_no",
            "creditcode",
            "credit_code",
            "certificate",
            "encryptedvalue",
            "encrypted_value",
            "configvaluesjson",
            "config_values_json",
            "notifypayload",
            "notify_payload",
            "requestpayload",
            "request_payload",
            "responsepayload",
            "response_payload",
            "extendinfo",
            "extend_info"
    );

    @Test
    @DisplayName("payment logs should not print complete sensitive fields")
    void paymentLogs_shouldNotPrintSensitiveFields() throws IOException {
        List<String> violations = mainJavaFiles()
                .flatMap(path -> sensitiveLogLines(path).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("payment source should not use console output or stack trace printing")
    void paymentSource_shouldNotUseConsoleOutputOrStackTracePrinting() throws IOException {
        List<String> violations = mainJavaFiles()
                .flatMap(path -> consoleOrStackTraceLines(path).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    private Stream<Path> mainJavaFiles() throws IOException {
        return Files.walk(PAYMENT_MODULE_DIR)
                .filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("/target/"))
                .filter(path -> path.toString().contains("/src/main/java/"))
                .filter(path -> path.getFileName().toString().endsWith(".java"));
    }

    private List<String> sensitiveLogLines(Path path) {
        return lines(path)
                .filter(line -> LOG_OR_CONSOLE_LINE.matcher(line.content()).find())
                .filter(line -> containsSensitiveToken(line.content()))
                .map(line -> path + ":" + line.number() + " -> " + line.content().trim())
                .toList();
    }

    private List<String> consoleOrStackTraceLines(Path path) {
        return lines(path)
                .filter(line -> line.content().contains("System.out.")
                        || line.content().contains("System.err.")
                        || line.content().contains("printStackTrace("))
                .map(line -> path + ":" + line.number() + " -> " + line.content().trim())
                .toList();
    }

    private boolean containsSensitiveToken(String line) {
        String normalized = line.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        return SENSITIVE_TOKENS.stream().anyMatch(normalized::contains);
    }

    private Stream<SourceLine> lines(Path path) {
        String content = read(path);
        String[] split = content.split("\\R");
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
