package io.mango.infra.bootstrap.starter;

import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BootstrapReceiptWriter {

    private static final Pattern ENVIRONMENT_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final Pattern JDBC_DATABASE = Pattern.compile("^jdbc:[^:]+://[^/]+/([^?;]+).*$");

    private final Path receiptDirectory;
    private final Environment environment;

    BootstrapReceiptWriter(BootstrapProperties properties, Environment environment) {
        Objects.requireNonNull(properties, "properties");
        this.environment = Objects.requireNonNull(environment, "environment");
        String configuredDirectory = requireText(
                properties.getReceiptDirectory(), "Mango bootstrap receipt directory is required");
        receiptDirectory = Path.of(configuredDirectory).toAbsolutePath().normalize();
    }

    String databaseName() {
        String explicit = environment.getProperty("MANGO_DB_NAME");
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        String jdbcUrl = requireText(
                environment.getProperty("spring.datasource.url"), "Spring datasource URL is required for receipt");
        Matcher matcher = JDBC_DATABASE.matcher(jdbcUrl.trim());
        if (!matcher.matches() || matcher.group(1).isBlank()) {
            throw new IllegalStateException("BOOTSTRAP_RECEIPT_DATABASE_UNRESOLVED");
        }
        return matcher.group(1);
    }

    Path write(BootstrapStableReceipt receipt) {
        validate(receipt);
        Path temporary = null;
        try {
            Files.createDirectories(receiptDirectory);
            Path target = receiptDirectory.resolve(receipt.environmentKey() + ".json");
            temporary = Files.createTempFile(receiptDirectory, ".receipt-", ".tmp");
            byte[] content = render(receipt).getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(
                    temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(content));
                channel.force(true);
            }
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IllegalStateException("BOOTSTRAP_RECEIPT_ATOMIC_MOVE_UNSUPPORTED", exception);
            }
            return target;
        } catch (IOException exception) {
            throw new IllegalStateException("BOOTSTRAP_RECEIPT_WRITE_FAILED", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The primary write failure is more useful than best-effort temp cleanup failure.
                }
            }
        }
    }

    private static void validate(BootstrapStableReceipt receipt) {
        Objects.requireNonNull(receipt, "receipt");
        if (!ENVIRONMENT_KEY.matcher(requireText(receipt.environmentKey(),
                "Bootstrap receipt environment key is required")).matches()) {
            throw new IllegalStateException("BOOTSTRAP_RECEIPT_ENVIRONMENT_INVALID");
        }
        requireText(receipt.databaseName(), "Bootstrap receipt database is required");
        requireText(receipt.releaseId(), "Bootstrap receipt release id is required");
        requireText(receipt.buildRevision(), "Bootstrap receipt build revision is required");
        if (receipt.stableGeneration() <= 0) {
            throw new IllegalStateException("BOOTSTRAP_RECEIPT_GENERATION_INVALID");
        }
        if (!requireText(receipt.stableFingerprint(), "Bootstrap receipt fingerprint is required")
                .matches("[0-9a-f]{64}")) {
            throw new IllegalStateException("BOOTSTRAP_RECEIPT_FINGERPRINT_INVALID");
        }
        if (!"FINALIZED".equals(receipt.state())) {
            throw new IllegalStateException("BOOTSTRAP_RECEIPT_STATE_INVALID");
        }
    }

    private static String render(BootstrapStableReceipt receipt) {
        return "{\n"
                + "  \"schemaVersion\": 1,\n"
                + "  \"environmentKey\": \"" + escape(receipt.environmentKey()) + "\",\n"
                + "  \"databaseName\": \"" + escape(receipt.databaseName()) + "\",\n"
                + "  \"releaseId\": \"" + escape(receipt.releaseId()) + "\",\n"
                + "  \"buildRevision\": \"" + escape(receipt.buildRevision()) + "\",\n"
                + "  \"stableGeneration\": " + receipt.stableGeneration() + ",\n"
                + "  \"stableFingerprint\": \"" + escape(receipt.stableFingerprint()) + "\",\n"
                + "  \"state\": \"" + escape(receipt.state()) + "\"\n"
                + "}\n";
    }

    private static String escape(String value) {
        StringBuilder result = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) {
                        result.append(String.format("\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.toString();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }
}
