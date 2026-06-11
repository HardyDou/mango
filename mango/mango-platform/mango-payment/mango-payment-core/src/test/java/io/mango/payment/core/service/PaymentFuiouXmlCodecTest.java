package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFuiouXmlCodecTest {

    private final PaymentFuiouXmlCodec codec = new PaymentFuiouXmlCodec();

    @Test
    @DisplayName("encode and decode should preserve fuiou xml fields")
    void encodeAndDecode_preservesFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("result_code", "000000");
        fields.put("result_msg", "SUCCESS");
        fields.put("goods_des", "订单<A&B>");

        Map<String, String> decoded = codec.decode(codec.encode(fields));

        assertThat(decoded)
                .containsEntry("result_code", "000000")
                .containsEntry("result_msg", "SUCCESS")
                .containsEntry("goods_des", "订单<A&B>");
    }
}
