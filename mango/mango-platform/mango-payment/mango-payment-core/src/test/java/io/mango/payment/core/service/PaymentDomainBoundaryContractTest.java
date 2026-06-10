package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDomainBoundaryContractTest {

    private static final Path PAYMENT_MODULE_DIR = Path.of("..");
    private static final List<String> BANNED_MODULE_ARTIFACTS = List.of(
            "mango-guarantee",
            "mango-guaranty",
            "mango-bond",
            "mango-deposit",
            "mango-margin",
            "mango-wallet",
            "mango-ledger",
            "mango-accounting",
            "mango-escrow"
    );
    private static final Pattern BANNED_IMPORT = Pattern.compile(
            "^import\\s+io\\.mango(?:\\.platform)?\\.(guarantee|guaranty|bond|deposit|margin|wallet|ledger|accounting|escrow)\\.",
            Pattern.MULTILINE);
    private static final Pattern BANNED_TYPE_DECLARATION = Pattern.compile(
            "\\b(class|interface|record|enum)\\s+\\w*(Guarantee|Guaranty|Bond|Deposit|Margin|Escrow|WalletAccount|WalletBalance|GeneralLedger|AccountingEntry|AccountingVoucher|VirtualAccount|CustodialAccount)\\w*");
    private static final Pattern BANNED_FIELD_DECLARATION = Pattern.compile(
            "\\b(private|protected|public)\\s+[^;=]+\\s+\\w*(guarantee|guaranty|bond|deposit|margin|escrow|walletAccount|walletBalance|generalLedger|accountingEntry|accountingVoucher|virtualAccount|custodialAccount)\\w*\\s*(?:[;=])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_TABLE = Pattern.compile("CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+`([^`]+)`", Pattern.CASE_INSENSITIVE);
    private static final Pattern BANNED_SQL_IDENTIFIER = Pattern.compile(
            "(guarantee|guaranty|bond|deposit|margin|escrow|wallet_account|wallet_balance|general_ledger|accounting_entry|accounting_voucher|virtual_account|custodial_account)",
            Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("payment module should not depend on non-payment business modules")
    void paymentModule_shouldNotDependOnNonPaymentBusinessModules() throws IOException {
        List<String> violations = paymentFiles("pom.xml")
                .flatMap(path -> bannedModuleArtifacts(path).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("payment source should not import non-payment business packages")
    void paymentSource_shouldNotImportNonPaymentBusinessPackages() throws IOException {
        List<String> violations = paymentFiles(".java")
                .flatMap(path -> bannedImports(path).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("payment source should not declare non-payment business models")
    void paymentSource_shouldNotDeclareNonPaymentBusinessModels() throws IOException {
        List<String> violations = paymentFiles(".java")
                .flatMap(path -> bannedJavaDeclarations(path).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("payment migrations should not create non-payment business tables")
    void paymentMigrations_shouldNotCreateNonPaymentBusinessTables() throws IOException {
        List<String> violations = paymentFiles(".sql")
                .flatMap(path -> bannedSqlTables(path).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    private Stream<Path> paymentFiles(String suffix) throws IOException {
        return Files.walk(PAYMENT_MODULE_DIR)
                .filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("/target/"))
                .filter(path -> path.getFileName().toString().endsWith(suffix));
    }

    private List<String> bannedModuleArtifacts(Path path) {
        String content = read(path).toLowerCase(Locale.ROOT);
        return BANNED_MODULE_ARTIFACTS.stream()
                .filter(content::contains)
                .map(artifactId -> path + " -> " + artifactId)
                .toList();
    }

    private List<String> bannedImports(Path path) {
        String content = read(path);
        Matcher matcher = BANNED_IMPORT.matcher(content);
        return matcher.results()
                .map(result -> path + " -> " + result.group())
                .toList();
    }

    private List<String> bannedJavaDeclarations(Path path) {
        String content = read(path);
        Stream<String> typeViolations = BANNED_TYPE_DECLARATION.matcher(content)
                .results()
                .map(result -> path + " -> " + result.group());
        Stream<String> fieldViolations = BANNED_FIELD_DECLARATION.matcher(content)
                .results()
                .map(result -> path + " -> " + result.group());
        return Stream.concat(typeViolations, fieldViolations).toList();
    }

    private List<String> bannedSqlTables(Path path) {
        String content = read(path);
        Matcher matcher = CREATE_TABLE.matcher(content);
        return matcher.results()
                .map(result -> result.group(1))
                .filter(tableName -> BANNED_SQL_IDENTIFIER.matcher(tableName).find())
                .map(tableName -> path + " -> " + tableName)
                .toList();
    }

    private String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + path, ex);
        }
    }
}
