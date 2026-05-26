package io.mango.infra.sensitive.core;

import cn.hutool.core.util.DesensitizedUtil;
import io.mango.infra.sensitive.api.annotation.Sensitive;
import io.mango.infra.sensitive.api.enums.SensitiveType;

import java.util.Locale;

/**
 * Built-in sensitive value masking algorithms migrated from Pigx.
 */
public final class SensitiveMasker {

    private static final String STAR = "*";
    private static final String PASSWORD_MASK = "******";
    private static final String QUERY_MASK = "***";

    private SensitiveMasker() {
    }

    /**
     * Masks a string value according to the annotation strategy.
     *
     * @param sensitive annotation metadata
     * @param input     raw input
     * @return masked value
     */
    public static String mask(Sensitive sensitive, String input) {
        if (input == null) {
            return null;
        }
        SensitiveType type = sensitive.type();
        return switch (type) {
            case CUSTOM, CUSTOMER -> custom(input, sensitive.prefixNoMaskLen(), sensitive.suffixNoMaskLen(),
                    sensitive.maskStr());
            case CHINESE_NAME -> chineseName(input);
            case ID_CARD -> idCard(input);
            case FIXED_PHONE -> fixedPhone(input);
            case MOBILE_PHONE -> mobilePhone(input);
            case ADDRESS -> address(input);
            case EMAIL -> email(input);
            case BANK_CARD -> bankCard(input);
            case PASSWORD -> password(input);
            case KEY -> key(input);
            case IPV4 -> ipv4(input);
            case CAR_LICENSE -> carLicense(input);
            case QUERY_PARAM -> queryParam(input);
            case JSON -> SensitiveJsonMasker.mask(input, sensitive.fuzzy(), sensitive.keys());
        };
    }

    /**
     * Masks a value by keeping configured prefix and suffix lengths.
     *
     * @param origin          raw value
     * @param prefixNoMaskLen visible prefix length
     * @param suffixNoMaskLen visible suffix length
     * @param maskStr         mask token
     * @return masked value
     */
    public static String custom(String origin, int prefixNoMaskLen, int suffixNoMaskLen, String maskStr) {
        if (origin == null) {
            return null;
        }
        String replacement = maskStr == null ? STAR : maskStr;
        StringBuilder builder = new StringBuilder();
        for (int i = 0, n = origin.length(); i < n; i++) {
            if (i < prefixNoMaskLen || i > n - suffixNoMaskLen - 1) {
                builder.append(origin.charAt(i));
                continue;
            }
            builder.append(replacement);
        }
        return builder.toString();
    }

    public static String chineseName(String fullName) {
        return custom(fullName, 0, 1, STAR);
    }

    public static String idCard(String id) {
        return custom(id, 6, 4, STAR);
    }

    public static String fixedPhone(String num) {
        return custom(num, 0, 4, STAR);
    }

    public static String mobilePhone(String num) {
        return custom(num, 3, 4, STAR);
    }

    public static String address(String address) {
        return custom(address, 6, 0, STAR);
    }

    public static String email(String email) {
        if (email == null) {
            return null;
        }
        int index = email.indexOf('@');
        if (index <= 1) {
            return email;
        }
        return custom(email.substring(0, index), 1, 0, STAR) + email.substring(index);
    }

    public static String bankCard(String cardNum) {
        return custom(cardNum, 6, 4, STAR);
    }

    public static String password(String password) {
        if (password == null) {
            return null;
        }
        return PASSWORD_MASK;
    }

    public static String key(String key) {
        if (key == null) {
            return null;
        }
        int viewLength = 6;
        StringBuilder masked = new StringBuilder(custom(key, 0, 3, STAR));
        if (masked.length() > viewLength) {
            return masked.substring(masked.length() - viewLength);
        }
        while (masked.length() < viewLength) {
            masked.insert(0, STAR);
        }
        return masked.toString();
    }

    public static String ipv4(String origin) {
        if (origin == null) {
            return null;
        }
        int index = origin.lastIndexOf('.');
        if (index < 0) {
            return origin + ".*";
        }
        return origin.substring(0, index) + ".*";
    }

    public static String carLicense(String license) {
        return DesensitizedUtil.carLicense(license);
    }

    public static String queryParam(String url) {
        if (url == null) {
            return null;
        }
        int queryStart = url.indexOf('?');
        if (queryStart < 0 || queryStart == url.length() - 1) {
            return url;
        }
        int fragmentStart = url.indexOf('#', queryStart);
        String prefix = url.substring(0, queryStart + 1);
        String query = fragmentStart >= 0 ? url.substring(queryStart + 1, fragmentStart) : url.substring(queryStart + 1);
        String suffix = fragmentStart >= 0 ? url.substring(fragmentStart) : "";
        if (query.isEmpty()) {
            return url;
        }
        String[] pairs = query.split("&", -1);
        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                masked.append('&');
            }
            String pair = pairs[i];
            if (pair.isEmpty()) {
                continue;
            }
            int equalIndex = pair.indexOf('=');
            String name = equalIndex >= 0 ? pair.substring(0, equalIndex) : pair;
            masked.append(name).append('=').append(QUERY_MASK);
        }
        return masked.append(suffix).toString();
    }

    static boolean matchesKey(String actualKey, boolean fuzzy, String expectedKey) {
        if (actualKey == null || expectedKey == null || expectedKey.isBlank()) {
            return false;
        }
        if (!fuzzy) {
            return actualKey.equals(expectedKey);
        }
        return actualKey.toLowerCase(Locale.ROOT).contains(expectedKey.toLowerCase(Locale.ROOT));
    }
}
