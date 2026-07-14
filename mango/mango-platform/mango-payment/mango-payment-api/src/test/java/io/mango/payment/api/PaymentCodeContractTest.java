package io.mango.payment.api;

import io.mango.common.result.BizCode;
import io.mango.payment.api.enums.PaymentCode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCodeContractTest {

    private static final String CONTRACT_SHA256 =
            "9e8f7c3c3a79b8d9872e5bba89c2a8367b16bea23eabd594ce581d05dc7f6cd2";

    @Test
    void paymentCodesKeepUniqueStableExternalContract() throws NoSuchAlgorithmException {
        PaymentCode[] codes = PaymentCode.values();

        assertThat(codes).hasSize(80);
        assertThat(codes)
                .allSatisfy(code -> {
                    assertThat(code).isInstanceOf(BizCode.class);
                    assertThat(code.getCode()).isBetween(3700, 3899);
                    assertThat(code.getMessage()).isNotBlank();
                });
        assertThat(Arrays.stream(codes).map(PaymentCode::getCode)).doesNotHaveDuplicates();

        String contract = Arrays.stream(codes)
                .map(code -> code.name() + "=" + code.getCode() + "=" + code.getMessage())
                .reduce("", (snapshot, line) -> snapshot + line + "\n");
        String digest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(contract.getBytes(StandardCharsets.UTF_8)));

        assertThat(digest).isEqualTo(CONTRACT_SHA256);
    }
}
