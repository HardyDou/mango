package io.mango.infra.bootstrap.starter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BootstrapReceiptWriterTest {

    private static final String FINGERPRINT = "a".repeat(64);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void atomicallyWritesAStableReceiptResolvedFromTheDatasource() throws Exception {
        BootstrapProperties properties = new BootstrapProperties();
        properties.setReceiptDirectory(temporaryDirectory.toString());
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306/mango_dev_test?useSSL=false");
        BootstrapReceiptWriter writer = new BootstrapReceiptWriter(properties, environment);

        Path path = writer.write(new BootstrapStableReceipt(
                "mango_023", writer.databaseName(), "release-1", "revision-1", 1, FINGERPRINT, "FINALIZED"));

        assertThat(path).isEqualTo(temporaryDirectory.resolve("mango_023.json"));
        assertThat(Files.readString(path))
                .contains("\"schemaVersion\": 1")
                .contains("\"databaseName\": \"mango_dev_test\"")
                .contains("\"stableGeneration\": 1")
                .contains("\"state\": \"FINALIZED\"");
        assertThat(Files.list(temporaryDirectory).toList())
                .extracting(item -> item.getFileName().toString())
                .containsExactly("mango_023.json");
    }

    @Test
    void rejectsAnyReceiptThatIsNotFinalizedAndPositive() {
        BootstrapProperties properties = new BootstrapProperties();
        properties.setReceiptDirectory(temporaryDirectory.toString());
        BootstrapReceiptWriter writer = new BootstrapReceiptWriter(
                properties, new MockEnvironment().withProperty("MANGO_DB_NAME", "mango_dev_test"));

        assertThatThrownBy(() -> writer.write(new BootstrapStableReceipt(
                "mango_023", "mango_dev_test", "release-1", "revision-1", 0, FINGERPRINT, "FINALIZED")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_RECEIPT_GENERATION_INVALID");
        assertThatThrownBy(() -> writer.write(new BootstrapStableReceipt(
                "mango_023", "mango_dev_test", "release-1", "revision-1", 1, FINGERPRINT, "EXPANDED")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_RECEIPT_STATE_INVALID");
    }
}
