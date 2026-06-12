package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class PaymentFuiouGatewaySignService {

    private static final String SEPARATOR = "|";
    private static final List<String> PAY_FIELDS = List.of(
            "mchnt_cd",
            "order_id",
            "order_amt",
            "order_pay_type",
            "page_notify_url",
            "back_notify_url",
            "order_valid_time",
            "iss_ins_cd",
            "goods_name",
            "goods_display_url",
            "rem",
            "ver");
    private static final List<String> QUERY_FIELDS = List.of(
            "mchnt_cd",
            "order_id");
    private static final List<String> CALLBACK_FIELDS = List.of(
            "mchnt_cd",
            "order_id",
            "order_date",
            "order_amt",
            "order_st",
            "order_pay_code",
            "order_pay_error",
            "resv1",
            "fy_ssn");

    public String signPay(Map<String, String> fields, String merchantKey) {
        return digest(fields, PAY_FIELDS, merchantKey);
    }

    public String signQuery(Map<String, String> fields, String merchantKey) {
        return digest(fields, QUERY_FIELDS, merchantKey);
    }

    public boolean verifyCallback(Map<String, String> fields, String merchantKey) {
        String sign = PaymentContextSupport.trimToNull(fields.get("md5"));
        if (sign == null) {
            return false;
        }
        return sign.equalsIgnoreCase(digest(fields, CALLBACK_FIELDS, merchantKey));
    }

    public String signPlain(String plain, String merchantKey) {
        Require.notNull(plain, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友网关 plain 报文不能为空");
        Require.notBlank(merchantKey, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友网关商户密钥不能为空");
        return HexFormat.of().formatHex(md5(plain + merchantKey));
    }

    private String digest(Map<String, String> fields, List<String> orderedFields, String merchantKey) {
        Require.notNull(fields, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友网关签名字段不能为空");
        Require.notBlank(merchantKey, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友网关商户密钥不能为空");
        StringBuilder builder = new StringBuilder();
        for (String field : orderedFields) {
            if (!builder.isEmpty()) {
                builder.append(SEPARATOR);
            }
            builder.append(value(fields.get(field)));
        }
        builder.append(SEPARATOR).append(merchantKey);
        return HexFormat.of().formatHex(md5(builder.toString()));
    }

    private byte[] md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return Require.fail(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友网关签名失败");
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
