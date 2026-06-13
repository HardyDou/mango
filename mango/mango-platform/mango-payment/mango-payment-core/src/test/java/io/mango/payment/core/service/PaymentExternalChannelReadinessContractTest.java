package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.payment.api.PaymentCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentExternalChannelReadinessContractTest {

    private static final Path PAYMENT_MODULE_DIR = Path.of("..");
    private static final Path LEDGER = Path.of("../../../../mango-docs/plans/2026-05-25-payment-delivery-ledger.md");
    private static final Path BLOCKER_EVIDENCE =
            Path.of("../../../../mango-docs/plans/evidence/payment-delivery-evidence-summary.md");
    private static final List<String> UNFINISHED_EXTERNAL_CHANNELS = List.of(
            "ALLINPAY",
            "HUAXIA_BANK",
            "WECHAT_PAY",
            "ALIPAY",
            "LIANLIAN_PAY"
    );
    private static final List<String> UNFINISHED_LEDGER_ITEMS = List.of(
            "PAY-CHANNEL-003",
            "PAY-CHANNEL-005",
            "PAY-CHANNEL-007",
            "PAY-CHANNEL-008"
    );
    private static final List<String> EXTERNAL_SDK_TOKENS = List.of(
            "IJPay-WxPay",
            "IJPay-AliPay",
            "wechatpay-java",
            "weixin-java",
            "alipay-sdk-java",
            "alipay-sdk"
    );

    @Test
    @DisplayName("unfinished external channels should be rejected by registry")
    void unfinishedExternalChannels_shouldBeRejectedByRegistry() {
        PaymentChannelAdapterRegistry registry = new PaymentChannelAdapterRegistry(List.of());

        for (String channelCode : UNFINISHED_EXTERNAL_CHANNELS) {
            assertThatThrownBy(() -> registry.requireAdapter(channelCode))
                    .isInstanceOf(BizException.class)
                    .extracting("code")
                    .isEqualTo(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode());
        }
    }

    @Test
    @DisplayName("production payment adapters should only include internal channels until external channels are implemented")
    void productionAdapters_shouldOnlyIncludeInternalChannelsUntilExternalChannelsAreImplemented() throws IOException {
        List<Path> adapterImplementations = productionJavaFiles()
                .filter(path -> read(path).contains("implements IPaymentChannelAdapter"))
                .toList();

        assertThat(adapterImplementations)
                .extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder(
                        "PaymentMangoPayChannelAdapter.java",
                        "PaymentFuiouPayChannelAdapter.java",
                        "PaymentOfflineCollectionChannelAdapter.java");
        for (String channelCode : UNFINISHED_EXTERNAL_CHANNELS) {
            assertThat(adapterImplementations)
                    .noneMatch(path -> path.getFileName().toString()
                            .toUpperCase(Locale.ROOT)
                            .contains(channelCode.replace("_", "")));
        }
    }

    @Test
    @DisplayName("external channel SDKs should not be declared before real adapters exist")
    void externalChannelSdks_shouldNotBeDeclaredBeforeRealAdaptersExist() throws IOException {
        String pomContent = String.join("\n", productionPomFiles().stream().map(this::read).toList());

        for (String token : EXTERNAL_SDK_TOKENS) {
            assertThat(pomContent).doesNotContain(token);
        }
    }

    @Test
    @DisplayName("unfinished external channels should stay exception-scoped in delivery evidence")
    void unfinishedExternalChannels_shouldStayExceptionScopedInDeliveryEvidence() throws IOException {
        String ledger = read(LEDGER);
        String blockerEvidence = read(BLOCKER_EVIDENCE);

        for (String item : UNFINISHED_LEDGER_ITEMS) {
            assertThat(ledger).contains("| " + item + " |");
            assertThat(ledger).contains(item + " |").contains("| EXCEPTION |");
            assertThat(blockerEvidence).contains("| " + item + " |");
            assertThat(blockerEvidence).contains("| EXCEPTION |");
        }
    }

    private List<Path> productionPomFiles() throws IOException {
        try (var files = Files.walk(PAYMENT_MODULE_DIR)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .toList();
        }
    }

    private java.util.stream.Stream<Path> productionJavaFiles() throws IOException {
        return Files.walk(PAYMENT_MODULE_DIR)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().contains("/src/main/java/"))
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .filter(path -> !path.toString().contains("/target/"));
    }

    private String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + path, ex);
        }
    }
}
