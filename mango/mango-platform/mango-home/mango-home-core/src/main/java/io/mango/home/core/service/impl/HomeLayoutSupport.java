package io.mango.home.core.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;

final class HomeLayoutSupport {

    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_ITEMS = 100;
    private static final int MAX_COLUMNS = 12;
    private static final int MAX_ROWS = 1000;
    private static final int MAX_COORDINATE = 999;
    private static final int BAD_REQUEST_CODE = 400;

    private HomeLayoutSupport() {
    }

    static String normalize(ObjectMapper objectMapper, String layoutJson) {
        if (layoutJson == null || layoutJson.isBlank()) {
            return defaultLayoutJson();
        }
        validate(objectMapper, layoutJson);
        return layoutJson;
    }

    static String defaultLayoutJson() {
        return "{\"schemaVersion\":1,\"items\":[]}";
    }

    static void validate(ObjectMapper objectMapper, String layoutJson) {
        try {
            JsonNode root = objectMapper.readTree(layoutJson);
            Require.isTrue(root.path("schemaVersion").asInt() == SCHEMA_VERSION, "布局结构版本不支持");
            JsonNode items = root.path("items");
            Require.isTrue(items.isArray(), "布局 items 必须是数组");
            Require.isTrue(items.size() <= MAX_ITEMS, "布局组件数量不能超过100个");
            for (JsonNode item : items) {
                validateItem(item);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            Require.fail(BAD_REQUEST_CODE, "布局 JSON 格式不正确");
        }
    }

    private static void validateItem(JsonNode item) {
        Require.notBlank(item.path("id").asText(null), "布局项 id 不能为空");
        Require.notBlank(item.path("widgetType").asText(null), "布局项 widgetType 不能为空");
        JsonNode layout = item.path("layout");
        Require.isTrue(layout.isObject(), "布局项 layout 不能为空");
        int x = layout.path("x").asInt(-1);
        int y = layout.path("y").asInt(-1);
        int w = layout.path("w").asInt(-1);
        int h = layout.path("h").asInt(-1);
        Require.inRange(x, 0, MAX_COORDINATE, "布局项 x 超出范围");
        Require.inRange(y, 0, MAX_COORDINATE, "布局项 y 超出范围");
        Require.inRange(w, 1, MAX_COLUMNS, "布局项 w 超出范围");
        Require.inRange(h, 1, MAX_ROWS, "布局项 h 超出范围");
        Require.isTrue(x + w <= MAX_COLUMNS, "布局项宽度超出12栅格");
        validateOptionalSize(layout, "minW", MAX_COLUMNS);
        validateOptionalSize(layout, "minH", MAX_ROWS);
        validateOptionalSize(layout, "maxW", MAX_COLUMNS);
        validateOptionalSize(layout, "maxH", MAX_ROWS);
    }

    private static void validateOptionalSize(JsonNode layout, String fieldName, int maxValue) {
        JsonNode node = layout.get(fieldName);
        if (node != null && !node.isNull()) {
            Require.inRange(node.asInt(-1), 1, maxValue, "布局项 " + fieldName + " 超出范围");
        }
    }
}
