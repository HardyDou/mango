package io.mango.infra.sensitive.core;

import cn.hutool.core.util.DesensitizedUtil;
import io.mango.infra.sensitive.api.annotation.Sensitive;
import io.mango.infra.sensitive.api.enums.SensitiveType;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Built-in sensitive value masking algorithms migrated from Pigx.
 */
public final class SensitiveMasker {

    private static final String STAR = "*";
    private static final String PASSWORD_MASK = "******";
    private static final String QUERY_MASK = "***";
    private static final int ID_PREFIX_LENGTH = 6;
    private static final int ID_SUFFIX_LENGTH = 4;
    private static final int PHONE_PREFIX_LENGTH = 3;
    private static final int PHONE_SUFFIX_LENGTH = 4;
    private static final int ADDRESS_PREFIX_LENGTH = 6;
    private static final int EMAIL_PREFIX_LENGTH = 1;
    private static final int KEY_VIEW_LENGTH = 6;
    private static final int KEY_SUFFIX_LENGTH = 3;
    private static final int IPV4_PART_COUNT = 4;
    private static final int IPV4_PART_MAX_LENGTH = 3;
    private static final int IPV4_MAX_OCTET = 255;
    private static final Map<SensitiveType, Function<String, String>> SIMPLE_MASKERS = Map.ofEntries(
            Map.entry(SensitiveType.CHINESE_NAME, SensitiveMasker::chineseName),
            Map.entry(SensitiveType.ID_CARD, SensitiveMasker::idCard),
            Map.entry(SensitiveType.FIXED_PHONE, SensitiveMasker::fixedPhone),
            Map.entry(SensitiveType.MOBILE_PHONE, SensitiveMasker::mobilePhone),
            Map.entry(SensitiveType.ADDRESS, SensitiveMasker::address),
            Map.entry(SensitiveType.EMAIL, SensitiveMasker::email),
            Map.entry(SensitiveType.BANK_CARD, SensitiveMasker::bankCard),
            Map.entry(SensitiveType.PASSWORD, SensitiveMasker::password),
            Map.entry(SensitiveType.KEY, SensitiveMasker::key),
            Map.entry(SensitiveType.IPV4, SensitiveMasker::ipv4),
            Map.entry(SensitiveType.CAR_LICENSE, SensitiveMasker::carLicense),
            Map.entry(SensitiveType.QUERY_PARAM, SensitiveMasker::queryParam));

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
        if (type == SensitiveType.CUSTOM || type == SensitiveType.CUSTOMER) {
            return custom(input, sensitive.prefixNoMaskLen(), sensitive.suffixNoMaskLen(), sensitive.maskStr());
        }
        if (type == SensitiveType.JSON) {
            return SensitiveJsonMasker.mask(input, sensitive.fuzzy(), sensitive.keys());
        }
        return SIMPLE_MASKERS.get(type).apply(input);
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
        int[] codePoints = origin.codePoints().toArray();
        if (codePoints.length == 0) {
            return origin;
        }
        int prefixLength = Math.min(Math.max(prefixNoMaskLen, 0), codePoints.length);
        int suffixLength = Math.min(Math.max(suffixNoMaskLen, 0), codePoints.length - prefixLength);
        if (prefixLength + suffixLength == codePoints.length) {
            if (suffixLength > 0) {
                suffixLength--;
            } else {
                prefixLength--;
            }
        }
        String replacement = maskStr;
        if (replacement == null) {
            replacement = STAR;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < codePoints.length; i++) {
            if (i < prefixLength || i >= codePoints.length - suffixLength) {
                builder.appendCodePoint(codePoints[i]);
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
        return custom(id, ID_PREFIX_LENGTH, ID_SUFFIX_LENGTH, STAR);
    }

    public static String fixedPhone(String num) {
        return custom(num, 0, PHONE_SUFFIX_LENGTH, STAR);
    }

    public static String mobilePhone(String num) {
        return custom(num, PHONE_PREFIX_LENGTH, PHONE_SUFFIX_LENGTH, STAR);
    }

    public static String address(String address) {
        return custom(address, ADDRESS_PREFIX_LENGTH, 0, STAR);
    }

    public static String email(String email) {
        if (email == null) {
            return null;
        }
        int index = email.indexOf('@');
        if (index < 0) {
            return custom(email, 0, 0, STAR);
        }
        if (index == 0) {
            return STAR + email;
        }
        return custom(email.substring(0, index), EMAIL_PREFIX_LENGTH, 0, STAR) + email.substring(index);
    }

    public static String bankCard(String cardNum) {
        return custom(cardNum, ID_PREFIX_LENGTH, ID_SUFFIX_LENGTH, STAR);
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
        if (key.codePointCount(0, key.length()) <= KEY_SUFFIX_LENGTH) {
            return STAR.repeat(KEY_VIEW_LENGTH);
        }
        StringBuilder masked = new StringBuilder(custom(key, 0, KEY_SUFFIX_LENGTH, STAR));
        if (masked.length() > KEY_VIEW_LENGTH) {
            return masked.substring(masked.length() - KEY_VIEW_LENGTH);
        }
        while (masked.length() < KEY_VIEW_LENGTH) {
            masked.insert(0, STAR);
        }
        return masked.toString();
    }

    public static String ipv4(String origin) {
        if (origin == null) {
            return null;
        }
        if (!isIpv4(origin)) {
            return custom(origin, 0, 0, STAR);
        }
        int index = origin.lastIndexOf('.');
        return origin.substring(0, index) + ".*";
    }

    public static String carLicense(String license) {
        String masked = DesensitizedUtil.carLicense(license);
        if (Objects.equals(masked, license)) {
            return custom(license, 0, 0, STAR);
        }
        return masked;
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
        String query = url.substring(queryStart + 1);
        String suffix = "";
        if (fragmentStart >= 0) {
            query = url.substring(queryStart + 1, fragmentStart);
            suffix = url.substring(fragmentStart);
        }
        if (query.isEmpty()) {
            return url;
        }
        String[] pairs = query.split("&", -1);
        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                masked.append('&');
            }
            appendMaskedQueryPair(masked, pairs[i]);
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

    private static boolean isIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != IPV4_PART_COUNT) {
            return false;
        }
        for (String part : parts) {
            if (!isIpv4Part(part)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIpv4Part(String part) {
        if (part.isEmpty() || part.length() > IPV4_PART_MAX_LENGTH) {
            return false;
        }
        if (!part.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return Integer.parseInt(part) <= IPV4_MAX_OCTET;
    }

    private static void appendMaskedQueryPair(StringBuilder target, String pair) {
        if (pair.isEmpty()) {
            return;
        }
        int equalIndex = pair.indexOf('=');
        String name = pair;
        if (equalIndex >= 0) {
            name = pair.substring(0, equalIndex);
        }
        target.append(name).append('=').append(QUERY_MASK);
    }
}
