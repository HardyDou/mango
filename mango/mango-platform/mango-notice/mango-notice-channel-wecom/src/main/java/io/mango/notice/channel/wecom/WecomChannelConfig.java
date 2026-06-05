package io.mango.notice.channel.wecom;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record WecomChannelConfig(String corpId, String agentId, String secret) {

    public static WecomChannelConfig fromJson(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return new WecomChannelConfig(null, null, null);
        }
        return new WecomChannelConfig(
                readValue(configJson, "corpId"),
                readValue(configJson, "agentId"),
                firstText(readValue(configJson, "secret"), readValue(configJson, "corpSecret")));
    }

    private static String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private static String readValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(?:\"([^\"]*)\"|(\\d+))");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }
}
