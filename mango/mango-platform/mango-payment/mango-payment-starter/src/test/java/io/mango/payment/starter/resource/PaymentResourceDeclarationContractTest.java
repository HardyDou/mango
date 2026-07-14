package io.mango.payment.starter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentResourceDeclarationContractTest {

    private static final Path FORMAL_ROOT = Path.of("src/main/resources/META-INF/mango/resources");
    private static final Path DEMO_ROOT = Path.of("src/main/resources/META-INF/mango/demo");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern SECRET_KEY = Pattern.compile(
            "(?i)appSecret|apiSecret|privateKey|merchantKey|gatewayMerchantKey|mchntKey");
    private static final Set<String> FORMAL_TYPES = Set.of(
            PaymentResourceTypes.METHOD_CATEGORY,
            PaymentResourceTypes.METHOD,
            PaymentResourceTypes.CHANNEL,
            PaymentResourceTypes.CHANNEL_FIELD_TEMPLATE,
            PaymentResourceTypes.CHANNEL_CAPABILITY,
            PaymentResourceTypes.RISK_RULE);
    private static final Set<String> DEMO_TYPES = Set.of(
            PaymentResourceTypes.TENANT,
            PaymentResourceTypes.APPLICATION,
            PaymentResourceTypes.ENTERPRISE_SUBJECT,
            PaymentResourceTypes.SUBJECT_BANK_ACCOUNT,
            PaymentResourceTypes.CASHIER_CONFIG,
            PaymentResourceTypes.CHANNEL_CONTRACT,
            PaymentResourceTypes.CHANNEL_CONTRACT_VALUE,
            PaymentResourceTypes.CHANNEL_CONTRACT_CAPABILITY,
            PaymentResourceTypes.METHOD_ROUTE_RULE,
            PaymentResourceTypes.METHOD_ROUTE_RULE_ITEM);

    @Test
    @DisplayName("payment should own flat, typed formal and demo resource declarations")
    void paymentOwnsTypedFormalAndDemoDeclarations() throws IOException {
        List<DeclaredResource> formal = declarations(FORMAL_ROOT, "payment-common-");
        List<DeclaredResource> demo = declarations(DEMO_ROOT, "payment-demo-");

        assertThat(formal).hasSize(65);
        assertThat(demo).hasSize(73);
        assertThat(types(formal)).containsExactlyInAnyOrderElementsOf(FORMAL_TYPES);
        assertThat(types(demo)).containsExactlyInAnyOrderElementsOf(DEMO_TYPES);
        assertThat(FORMAL_TYPES).doesNotContainAnyElementsOf(DEMO_TYPES);

        List<DeclaredResource> all = Stream.concat(formal.stream(), demo.stream()).toList();
        assertThat(all).allSatisfy(resource -> {
            assertThat(resource.moduleCode()).isEqualTo("payment");
            assertThat(resource.targetModule()).isEqualTo("payment");
            assertThat(resource.syncMode()).isEqualTo("INIT_ONLY");
        });
        assertThat(all.stream().map(DeclaredResource::id)).doesNotHaveDuplicates();
        assertThat(all.stream().map(DeclaredResource::bizKey)).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("payment resource declarations should preserve channel capabilities without merchant secrets")
    void declarationsPreserveCapabilitiesWithoutMerchantSecrets() throws IOException {
        List<DeclaredResource> all = Stream.concat(
                declarations(FORMAL_ROOT, "payment-common-").stream(),
                declarations(DEMO_ROOT, "payment-demo-").stream()).toList();
        String declarations = all.stream().map(resource -> resource.node().toString()).reduce("", String::concat);

        assertThat(declarations)
                .contains("FUIOU_PAY")
                .contains("FUIOU_PAY_MANGO_TECH")
                .contains("PERSONAL_WECHAT_QR")
                .contains("PERSONAL_ALIPAY_QR")
                .contains("PERSONAL_EBANK_REDIRECT")
                .contains("CORPORATE_EBANK_REDIRECT")
                .contains("mangoPayScenario")
                .contains("mangoPayRefundScenario")
                .doesNotContain("MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJc")
                .doesNotContain("vau6p7ldawpezyaugc0kopdrrwm4gkpu")
                .doesNotContain("27.185.20.146")
                .doesNotContain("douxy.inner.yunxinbaokeji.com")
                .doesNotContain("gatewayUrl");

        all.forEach(resource -> {
            JsonNode config = resource.node().path("fields").path("configValuesJson").path("value");
            if (!config.isMissingNode() && !config.isNull()) {
                assertThat(SECRET_KEY.matcher(config.asText()).find())
                        .as(resource.bizKey())
                        .isFalse();
            }
        });
    }

    @Test
    @DisplayName("payment resource declarations should never provision runtime business data")
    void declarationsNeverProvisionRuntimeBusinessData() throws IOException {
        Set<String> runtimeTokens = Set.of(
                "BUSINESS_ORDER", "PAYMENT_ORDER", "REFUND_ORDER", "TRANSACTION_FLOW", "EXCEPTION_ORDER",
                "NOTIFICATION_RECORD", "RECONCILIATION", "DIFFERENCE", "SETTLEMENT_SUMMARY", "OPERATION_AUDIT",
                "CHANNEL_BILL_BATCH", "ORDER_STATUS_FLOW", "OFFLINE_REFUND_PROCESS");
        Set<String> declaredTypes = new LinkedHashSet<>();
        declaredTypes.addAll(types(declarations(FORMAL_ROOT, "payment-common-")));
        declaredTypes.addAll(types(declarations(DEMO_ROOT, "payment-demo-")));

        runtimeTokens.forEach(token -> assertThat(declaredTypes)
                .as(token)
                .noneMatch(type -> type.contains(token)));
    }

    private List<DeclaredResource> declarations(Path root, String filePrefix) throws IOException {
        assertThat(root).isDirectory();
        List<Path> files;
        try (Stream<Path> paths = Files.walk(root)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(filePrefix))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .toList();
        }
        assertThat(files).allSatisfy(path -> assertThat(path.getParent()).isEqualTo(root));

        List<DeclaredResource> result = new ArrayList<>();
        for (Path path : files) {
            JsonNode resource = MAPPER.readTree(path.toFile()).path("mango").path("resource");
            String moduleCode = resource.path("moduleCode").asText();
            Iterator<Map.Entry<String, JsonNode>> types = resource.path("declarations").fields();
            while (types.hasNext()) {
                Map.Entry<String, JsonNode> type = types.next();
                if (!type.getKey().startsWith("PAYMENT_")) {
                    continue;
                }
                type.getValue().forEach(node -> result.add(new DeclaredResource(
                        type.getKey(),
                        moduleCode,
                        node.path("id").asText(),
                        node.path("bizKey").asText(),
                        node.path("targetModule").asText(),
                        node.path("syncMode").asText(),
                        node)));
            }
        }
        return result;
    }

    private Set<String> types(List<DeclaredResource> declarations) {
        Set<String> result = new HashSet<>();
        declarations.forEach(resource -> result.add(resource.type()));
        return result;
    }

    private record DeclaredResource(
            String type,
            String moduleCode,
            String id,
            String bizKey,
            String targetModule,
            String syncMode,
            JsonNode node) {
    }
}
