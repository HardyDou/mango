package io.mango.file.core.service.impl;

import io.mango.file.core.entity.FileRecord;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件预览地址构建器。
 */
final class FilePreviewUrlBuilder {

    private static final String FILE_ID = "fileId";
    private static final String FILE_URL = "fileUrl";
    private static final String FILE_NAME = "fileName";
    private static final String EXPIRE_SECONDS = "expireSeconds";

    private FilePreviewUrlBuilder() {
    }

    static String build(String providerUrl, FileRecord record, String fileUrl, long expireSeconds) {
        if (!StringUtils.hasText(providerUrl)) {
            return fileUrl;
        }
        Map<String, String> values = values(record, fileUrl, expireSeconds, true);
        String resolved = providerUrl.trim();
        boolean replaced = false;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (resolved.contains(placeholder)) {
                resolved = resolved.replace(placeholder, encode(entry.getValue()));
                replaced = true;
            }
        }
        if (replaced) {
            return resolved;
        }
        values = values(record, fileUrl, expireSeconds, false);
        StringBuilder builder = new StringBuilder(resolved);
        String separator = resolved.contains("?") ? "&" : "?";
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (StringUtils.hasText(value)) {
                builder.append(separator)
                        .append(key)
                        .append("=")
                        .append(encode(value));
                separator = "&";
            }
        }
        return builder.toString();
    }

    private static Map<String, String> values(FileRecord record, String fileUrl, long expireSeconds, boolean includeFileUrl) {
        Map<String, String> values = new LinkedHashMap<>();
        String fileId = record.getId() == null ? "" : String.valueOf(record.getId());
        values.put(FILE_ID, fileId);
        if (includeFileUrl || !StringUtils.hasText(fileId)) {
            values.put(FILE_URL, fileUrl);
        }
        values.put(FILE_NAME, record.getFileName());
        values.put(EXPIRE_SECONDS, String.valueOf(expireSeconds));
        return values;
    }

    private static String encode(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return UriUtils.encodePath(value, StandardCharsets.UTF_8)
                .replace("?", "%3F")
                .replace("=", "%3D")
                .replace("&", "%26");
    }
}
