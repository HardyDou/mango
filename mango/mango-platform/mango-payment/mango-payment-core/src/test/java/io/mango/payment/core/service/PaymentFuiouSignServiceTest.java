package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFuiouSignServiceTest {

    private final PaymentFuiouSignService signService = new PaymentFuiouSignService();

    @Test
    @DisplayName("canonicalText should sort fields and skip sign and reserved fields")
    void canonicalText_skipsSignAndReservedFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("mchnt_cd", "0002900F0370542");
        fields.put("reserved_fy_order_no", "FY123");
        fields.put("ins_cd", "08A9999999");
        fields.put("sign", "SIGN");
        fields.put("order_amt", "1");
        fields.put("goods_detail", null);

        String canonicalText = signService.canonicalText(fields);

        assertThat(canonicalText)
                .isEqualTo("goods_detail=&ins_cd=08A9999999&mchnt_cd=0002900F0370542&order_amt=1");
    }

    @Test
    @DisplayName("sign and verify should use MD5WithRSA and GBK canonical text")
    void signAndVerify_usesFuiouAlgorithm() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("ins_cd", "08A9999999");
        fields.put("mchnt_cd", "0002900F0370542");
        fields.put("goods_des", "测试订单");
        fields.put("order_amt", "1");
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        fields.put("sign", signService.sign(fields, privateKey));

        assertThat(signService.verify(fields, publicKey)).isTrue();
    }
}
