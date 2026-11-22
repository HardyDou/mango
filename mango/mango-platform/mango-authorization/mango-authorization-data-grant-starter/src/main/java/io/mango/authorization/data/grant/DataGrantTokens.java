package io.mango.authorization.data.grant;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link DataGrantHeaders#GRANT} 请求头的多值解析。
 * <p>
 * 前端会把本地缓存的所有数据域 Grant 一次性回传（{@code X-Mango-Data-Grant: g1, g2}），
 * 因此同一个头既可能是多行，也可能是单行逗号分隔，这里统一按「逗号切分 → 逐个 trim → 去重」展开。
 * <p>
 * 解析是防御边界：任何不可信取值只丢弃、不抛异常，坏值不能让一次详情请求变成 500。
 * 超过 {@link DataGrantHeaders#MAX_TOKENS} 个取值或 {@link DataGrantHeaders#MAX_HEADER_LENGTH} 字符时
 * 整体按「未携带」处理（fail-closed），既不做部分采纳，也不记录完整头内容。
 */
@Slf4j
final class DataGrantTokens {

    private static final String SEPARATOR = ",";

    private DataGrantTokens() {
    }

    /**
     * 展开并清洗请求头取值。
     *
     * @param rawValues 容器给出的原始头取值，允许为 null。
     * @return 去重后的可用令牌列表；超限或全部不可用时返回空列表。
     */
    static List<String> parse(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }
        List<String> pieces = new ArrayList<>();
        int totalLength = 0;
        for (String rawValue : rawValues) {
            if (rawValue == null || rawValue.isEmpty()) {
                continue;
            }
            totalLength += rawValue.length();
            if (totalLength > DataGrantHeaders.MAX_HEADER_LENGTH) {
                log.warn("数据权限 Grant 头超过 {} 字符上限，按未携带处理, headerLines={}",
                        DataGrantHeaders.MAX_HEADER_LENGTH, rawValues.size());
                return List.of();
            }
            for (String piece : rawValue.split(SEPARATOR, -1)) {
                pieces.add(piece);
            }
        }
        Set<String> tokens = new LinkedHashSet<>();
        int dropped = 0;
        for (String piece : pieces) {
            String token = piece.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (!isUsable(token)) {
                dropped++;
                continue;
            }
            tokens.add(token);
        }
        if (dropped > 0) {
            log.warn("数据权限 Grant 头存在不可用取值，已丢弃, dropped={}", dropped);
        }
        if (tokens.size() > DataGrantHeaders.MAX_TOKENS) {
            log.warn("数据权限 Grant 头取值个数 {} 超过上限 {}，按未携带处理",
                    tokens.size(), DataGrantHeaders.MAX_TOKENS);
            return List.of();
        }
        return List.copyOf(tokens);
    }

    /**
     * 判断单个取值是否可用：长度在合理区间内且只含可见 ASCII。
     * <p>
     * 令牌是 base64url 加分隔符，出现空白、控制字符或非 ASCII 只可能是拼接错误或攻击载荷，
     * 直接丢弃而不是交给解码器。
     */
    private static boolean isUsable(String token) {
        if (token.length() > DataGrantHeaders.MAX_TOKEN_LENGTH) {
            return false;
        }
        for (int index = 0; index < token.length(); index++) {
            char ch = token.charAt(index);
            if (ch <= ' ' || ch >= 0x7F) {
                return false;
            }
        }
        return true;
    }
}
