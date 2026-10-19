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

    /**
     * 布局容器没有自己的值，需要继续下钻；
     * 子表单/表格表单本身就是一个字段，children 是行模板，不能再当成顶层字段。
     */
    private static final Set<String> SUBFORM_TYPES = Set.of("group", "subform", "tableform");

    private static void collect(JsonNode node, List<Field> result, Set<String> seen) {
        if (node == null || !node.isArray()) {
            return;
        }
        for (JsonNode rule : node) {
            String type = text(rule, "type");
            String normalizedType = type == null ? "" : type.toLowerCase(java.util.Locale.ROOT);
            String code = text(rule, "field");
            if (!hasText(code)) {
                code = text(rule, "fieldCode");
            }
            if (hasText(code) && seen.add(code)) {
                String name = text(rule, "title");
                if (!hasText(name)) {
                    name = text(rule, "fieldName");
                }
                String fieldType = type;
                if (!hasText(fieldType)) {
                    fieldType = text(rule, "fieldType");
                }
                result.add(new Field(code, hasText(name) ? name : code, hasText(fieldType) ? fieldType : "input", required(rule)));
            }
            if (SUBFORM_TYPES.contains(normalizedType) && hasText(code)) {
                continue;
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

    /**
     * 展示型与布局型规则不参与提交值校验。
     * <p>
     * 注意：设计器的「文字」组件类型同为 `text`，但它没有 field，不会进入采集项；
     * 旧定义的 `text` 表示单行文本，是可提交字段，不能当作展示组件过滤。
     */
    public static boolean displayOnly(Field field) {
        String type = field.type() == null ? "" : field.type().toLowerCase(java.util.Locale.ROOT);
        return Set.of("divider", "eldivider", "static", "elalert", "html", "eltag", "elimage", "elbutton")
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
