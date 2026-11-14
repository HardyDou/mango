package io.mango.workflow.core.service.impl;

import io.mango.common.result.Require;
import io.mango.workflow.api.enums.WorkflowCode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 业务申请可筛属性的键值约束。
 *
 * <p>属性由业务自定义，平台只保证写入和查询使用同一套键值边界：属性名可由索引承载、
 * 属性值长度可控、同一个申请内不出现重复属性。查询侧和写入侧共用本类，避免两套校验规则漂移。
 */
final class WorkflowApplyAttributeRules {

    static final int MAX_ATTRIBUTES_PER_APPLY = 20;
    static final int MAX_FILTER_CONDITIONS = 10;
    static final int MAX_VALUES_PER_CONDITION = 50;
    static final int MAX_VALUE_LENGTH = 255;

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_.-]{0,63}");

    private WorkflowApplyAttributeRules() {
    }

    /** 校验并归一化单个属性名，返回去掉首尾空白后的值。 */
    static String requireKey(String key, WorkflowCode code) {
        Require.isTrue(StringUtils.hasText(key), code, "可筛属性名不能为空");
        String normalized = key.trim();
        Require.isTrue(KEY_PATTERN.matcher(normalized).matches(), code,
                "可筛属性名只支持字母开头的字母、数字、下划线、点和短横线：" + normalized);
        return normalized;
    }

    /** 校验并归一化单个属性值，返回去掉首尾空白后的值。 */
    static String requireValue(String key, String value, WorkflowCode code) {
        Require.isTrue(StringUtils.hasText(value), code, "可筛属性值不能为空：" + key);
        String normalized = value.trim();
        Require.isTrue(normalized.length() <= MAX_VALUE_LENGTH, code,
                "可筛属性值长度不能超过" + MAX_VALUE_LENGTH + "：" + key);
        return normalized;
    }

    /** 校验并归一化一次写入的完整属性集合；同一属性名只允许出现一次，值按去重后的顺序保存。 */
    static Map<String, List<String>> requireAttributes(Map<String, List<String>> attributes) {
        Require.notNull(attributes, WorkflowCode.APPLY_INVALID, "可筛属性不能为空集合，清空请传空 Map");
        Require.isTrue(attributes.size() <= MAX_ATTRIBUTES_PER_APPLY, WorkflowCode.APPLY_INVALID,
                "可筛属性数量不能超过" + MAX_ATTRIBUTES_PER_APPLY);
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
            String key = requireKey(entry.getKey(), WorkflowCode.APPLY_INVALID);
            Require.isTrue(!normalized.containsKey(key), WorkflowCode.APPLY_INVALID, "可筛属性名重复：" + key);
            List<String> values = entry.getValue();
            Require.isTrue(values != null && !values.isEmpty(), WorkflowCode.APPLY_INVALID,
                    "可筛属性值不能为空：" + key);
            Require.isTrue(values.size() <= MAX_VALUES_PER_CONDITION, WorkflowCode.APPLY_INVALID,
                    "单个可筛属性的值数量不能超过" + MAX_VALUES_PER_CONDITION + "：" + key);
            LinkedHashSet<String> distinct = new LinkedHashSet<>();
            for (String value : values) {
                distinct.add(requireValue(key, value, WorkflowCode.APPLY_INVALID));
            }
            normalized.put(key, new ArrayList<>(distinct));
        }
        return normalized;
    }

    /** 校验查询侧的条件属性名，同一属性名只允许出现一次，避免「且」条件永远不成立。 */
    static void requireDistinctKeys(Iterable<String> keys) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String key : keys) {
            Require.isTrue(seen.add(key), WorkflowCode.TASK_QUERY_INVALID, "可筛属性名重复：" + key);
        }
    }
}
