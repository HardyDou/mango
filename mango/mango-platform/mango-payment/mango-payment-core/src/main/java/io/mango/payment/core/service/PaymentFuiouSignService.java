package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

@Service
public class PaymentFuiouSignService {

    static final Charset FUIOU_CHARSET = Charset.forName("GBK");
    private static final String SIGN_ALGORITHM = "MD5WithRSA";
    private static final String KEY_ALGORITHM = "RSA";

    public String canonicalText(Map<String, String> fields) {
        Require.notNull(fields, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友签名字段不能为空");
        Map<String, String> sortedFields = new TreeMap<>();
        fields.forEach((key, value) -> {
            if (shouldSign(key)) {
                sortedFields.put(key, valueOrEmpty(value));
            }
        });
        StringBuilder builder = new StringBuilder();
        sortedFields.forEach((key, value) -> {
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(key).append('=').append(value);
        });
        return builder.toString();
    }

    public String sign(Map<String, String> fields, String privateKey) {
        Require.notBlank(privateKey, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友商户私钥不能为空");
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(privateKey(privateKey));
            signature.update(canonicalText(fields).getBytes(FUIOU_CHARSET));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友报文签名失败", ex);
        }
    }

    public boolean verify(Map<String, String> fields, String publicKey) {
        String sign = signValue(fields);
        if (sign == null || sign.isBlank()) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(publicKey(publicKey));
            signature.update(canonicalText(fields).getBytes(FUIOU_CHARSET));
            return signature.verify(Base64.getDecoder().decode(normalizeKey(sign)));
        } catch (Exception ex) {
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友报文验签失败", ex);
        }
    }

    private boolean shouldSign(String key) {
        return key != null && !"sign".equals(key) && !key.startsWith("reserved");
    }

    private String valueOrEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private String signValue(Map<String, String> fields) {
        if (fields == null) {
            return null;
        }
        return fields.get("sign");
    }

    private PrivateKey privateKey(String privateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(normalizeKey(privateKey));
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private PublicKey publicKey(String publicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(normalizeKey(publicKey));
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private String normalizeKey(String key) {
        return key.replace("-----BEGIN PRIVATE KEY-----", "").
                replace("-----END PRIVATE KEY-----", "").
                replace("-----BEGIN PUBLIC KEY-----", "").
                replace("-----END PUBLIC KEY-----", "").
                replaceAll("\\s+", "");
    }
}
