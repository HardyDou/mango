package io.mango.plugin.check;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checkstyle 描述到稳定指纹 token 的转换。
 *
 * <p>描述文本随 locale 变化（英文 {@code Cyclomatic complexity is 24 (max allowed is 20).}、
 * 中文 {@code 圈复杂度： 24 （最多： 20 ）。}），直接入指纹会让同一问题在不同机器上指纹不同，
 * 因此只保留「字符字面量」和「数字」，其余措辞忽略。</p>
 *
 * <p>描述里同时出现<b>实际值</b>和<b>配置阈值</b>（{@code max allowed is N} / {@code 最多： N}），
 * 后者属于规则文件配置而非代码事实。阈值一旦调整，所有命中该规则的存量指纹都会变，
 * 未改动的代码会被判成新增问题。所以取 token 前先剥掉配置阈值段，只留实际值。</p>
 */
final class CheckstyleFingerprintTokens {

    private static final Pattern IDENTITY_TOKEN_PATTERN =
            Pattern.compile("'([^']*)'|\\b\\d[\\d,_]*(?:\\.\\d+)?[a-zA-Z]?\\b");
    /** 配置阈值段：半角/全角括号皆可，英文 {@code max} 与中文 {@code 最多} 皆可。 */
    private static final Pattern CONFIGURED_THRESHOLD_SEGMENT_PATTERN =
            Pattern.compile("[(（][^()（）]*?(?:max|最多)[^()（）]*?[)）]", Pattern.CASE_INSENSITIVE);

    private CheckstyleFingerprintTokens() {
    }

    /**
     * 生成用于指纹的稳定 token 序列。
     *
     * @param description 原始描述
     * @return 以英文逗号连接、按字典序排列的 token；描述为空时返回空串
     */
    static String of(String description) {
        String text = stripConfiguredThreshold(description == null ? "" : description);
        Matcher matcher = IDENTITY_TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        Set<String> tokens = new TreeSet<>();
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add("literal:" + matcher.group(1));
            } else {
                tokens.add("number:" + matcher.group().replace(",", "").replace("_", ""));
            }
        }
        return String.join(",", tokens);
    }

    /**
     * 去掉描述中的配置阈值段，只保留实际值。
     *
     * @param description 原始描述
     * @return 剥掉 {@code (max …)} / {@code （最多…）} 之后的描述
     */
    static String stripConfiguredThreshold(String description) {
        if (description == null) {
            return "";
        }
        return CONFIGURED_THRESHOLD_SEGMENT_PATTERN.matcher(description).replaceAll(" ");
    }
}
