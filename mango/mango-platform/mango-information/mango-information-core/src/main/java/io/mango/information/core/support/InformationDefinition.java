package io.mango.information.core.support;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 表单定义解析。
 * <p>
 * 表单定义由前端表单设计器产出，存储为 form-create 规则 JSON：{type, field, title, validate, options, children}。
 * 同时兼容历史版本使用的 {fieldCode, fieldName, fieldType, required} 形状，避免存量数据失效。
 */
public final class InformationDefinition {

    private InformationDefinition() {
    }

    /** 单个可提交采集项。 */
    public record Field(String code, String name, String type, boolean required) {
    }

    /** 提取所有可提交采集项；布局与展示组件（没有 field 的规则）不参与校验与动态列。 */
    public static List<Field> fields(JsonNode definition) {
        List<Field> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        collect(definition, result, seen);
        return result;
    }

    private static void collect(JsonNode node, List<Field> result, Set<String> seen) {
        if (node == null || !node.isArray()) {
            return;
        }
        for (JsonNode rule : node) {
            String code = text(rule, "field");
            if (!hasText(code)) {
                code = text(rule, "fieldCode");
            }
            if (hasText(code) && seen.add(code)) {
                String name = text(rule, "title");
                if (!hasText(name)) {
                    name = text(rule, "fieldName");
                }
                String type = text(rule, "type");
                if (!hasText(type)) {
                    type = text(rule, "fieldType");
                }
                result.add(new Field(code, hasText(name) ? name : code, hasText(type) ? type : "input", required(rule)));
            }
            collect(rule.get("children"), result, seen);
        }
    }

    /** 必填判定同时支持 form-create 的 validate 数组与历史 required 标记。 */
    public static boolean required(JsonNode rule) {
        if (rule == null) {
            return false;
        }
        if (rule.path("$required").asBoolean(false) || rule.path("required").asBoolean(false)) {
            return true;
        }
        JsonNode validate = rule.get("validate");
        if (validate != null && validate.isArray()) {
            for (JsonNode item : validate) {
                if (item.path("required").asBoolean(false)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 展示型与布局型规则不参与提交值校验。 */
    public static boolean displayOnly(Field field) {
        String type = field.type() == null ? "" : field.type().toLowerCase(java.util.Locale.ROOT);
        return Set.of("divider", "eldivider", "static", "elalert", "text", "html", "eltag", "elimage", "elbutton")
                .contains(type);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
