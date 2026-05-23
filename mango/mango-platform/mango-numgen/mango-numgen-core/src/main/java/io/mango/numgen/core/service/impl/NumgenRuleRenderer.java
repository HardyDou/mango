package io.mango.numgen.core.service.impl;

import io.mango.common.result.Require;
import io.mango.numgen.api.vo.NumgenPreviewSegmentVO;
import io.mango.numgen.api.vo.NumgenPreviewVO;
import io.mango.numgen.api.vo.NumgenRuleValidationVO;
import io.mango.numgen.core.entity.NumgenRule;
import io.mango.numgen.core.entity.NumgenRuleSegment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class NumgenRuleRenderer {

    private static final Pattern TEXT_PARAM_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    public NumgenRuleValidationVO validate(NumgenRule rule, List<NumgenRuleSegment> segments) {
        NumgenRuleValidationVO vo = new NumgenRuleValidationVO();
        List<String> errors = new ArrayList<>();
        if (rule == null) {
            errors.add("编号规则不能为空");
        }
        if (segments == null || segments.isEmpty()) {
            errors.add("编号规则至少需要一个片段");
        } else {
            for (NumgenRuleSegment segment : segments) {
                validateSegment(segment, errors);
            }
        }
        vo.setValid(errors.isEmpty());
        vo.setErrors(errors);
        return vo;
    }

    public NumgenPreviewVO preview(NumgenRule rule, List<NumgenRuleSegment> segments, Map<String, Object> params, int count) {
        Require.notNull(rule, "编号规则不能为空");
        NumgenRuleValidationVO validation = validate(rule, segments);
        Require.isTrue(validation.isValid(), String.join("；", validation.getErrors()));
        int resolvedCount = Math.max(count, 1);
        List<NumgenRuleSegment> sortedSegments = sorted(segments);
        NumgenPreviewVO vo = new NumgenPreviewVO();
        vo.setGenKey(rule.getGenKey());
        vo.setRuleVersion(rule.getVersion());
        for (NumgenRuleSegment segment : sortedSegments) {
            NumgenPreviewSegmentVO segmentVO = new NumgenPreviewSegmentVO();
            segmentVO.setSortOrder(segment.getSortOrder());
            segmentVO.setSegmentType(segment.getSegmentType());
            segmentVO.setSegmentName(segment.getSegmentName());
            segmentVO.setValue(renderSegment(segment, params, 1));
            vo.getSegments().add(segmentVO);
        }
        for (long value = 1; value <= resolvedCount; value++) {
            vo.getValues().add(render(sortedSegments, params, value));
        }
        return vo;
    }

    public String render(List<NumgenRuleSegment> segments, Map<String, Object> params, long sequenceValue) {
        StringBuilder builder = new StringBuilder();
        for (NumgenRuleSegment segment : sorted(segments)) {
            builder.append(renderSegment(segment, params, sequenceValue));
        }
        return builder.toString();
    }

    public String sequenceScopeKey(List<NumgenRuleSegment> segments, Map<String, Object> params) {
        List<NumgenRuleSegment> scopeSegments = sorted(segments).stream()
                .filter(segment -> Integer.valueOf(1).equals(segment.getSequenceScope()))
                .filter(segment -> !"SEQ".equals(segment.getSegmentType()))
                .collect(Collectors.toList());
        if (scopeSegments.isEmpty()) {
            return "GLOBAL";
        }
        return scopeSegments.stream()
                .map(segment -> segment.getSortOrder() + ":" + renderSegment(segment, params, 0L))
                .collect(Collectors.joining("|"));
    }

    private void validateSegment(NumgenRuleSegment segment, List<String> errors) {
        if (segment == null) {
            errors.add("编号片段不能为空");
            return;
        }
        if (!StringUtils.hasText(segment.getSegmentType())) {
            errors.add("片段类型不能为空");
            return;
        }
        switch (segment.getSegmentType()) {
            case "TEXT" -> {
                if (!StringUtils.hasText(segment.getLiteralValue())) {
                    errors.add(segmentName(segment) + " 字符串不能为空");
                }
            }
            case "EXPR" -> {
                if (!StringUtils.hasText(segment.getLiteralValue())) {
                    errors.add(segmentName(segment) + " 表达式不能为空");
                }
            }
            case "DATE" -> {
                if (!StringUtils.hasText(segment.getDateFormat())) {
                    errors.add(segmentName(segment) + " 日期格式不能为空");
                }
            }
            case "PARAM" -> {
                if (!StringUtils.hasText(segment.getVariableKey())) {
                    errors.add(segmentName(segment) + " 参数键不能为空");
                }
            }
            case "SEQ" -> {
                if (segment.getSeqWidth() == null || segment.getSeqWidth() <= 0) {
                    errors.add(segmentName(segment) + " 流水位数必须大于0");
                }
                if (Integer.valueOf(1).equals(segment.getSequenceScope())) {
                    errors.add(segmentName(segment) + " 流水片段不能参与流水分组");
                }
            }
            default -> errors.add("不支持的片段类型：" + segment.getSegmentType());
        }
    }

    private String renderSegment(NumgenRuleSegment segment, Map<String, Object> params, long sequenceValue) {
        return switch (segment.getSegmentType()) {
            case "TEXT", "EXPR" -> renderText(segment.getLiteralValue(), params);
            case "DATE" -> LocalDateTime.now().format(DateTimeFormatter.ofPattern(segment.getDateFormat()));
            case "PARAM" -> resolveParam(segment, params);
            case "SEQ" -> padSequence(sequenceValue, segment);
            default -> Require.fail(400, "不支持的片段类型：" + segment.getSegmentType());
        };
    }

    private String renderText(String text, Map<String, Object> params) {
        Matcher matcher = TEXT_PARAM_PATTERN.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = resolveParamValue(key, params);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private String resolveParam(NumgenRuleSegment segment, Map<String, Object> params) {
        Object value = resolveParamValue(segment.getVariableKey(), params);
        Require.notNull(value, "缺少编号变量参数：" + segment.getVariableKey());
        return String.valueOf(value);
    }

    private Object resolveParamValue(String key, Map<String, Object> params) {
        return params == null || !StringUtils.hasText(key) ? null : params.get(key);
    }

    private String padSequence(long sequenceValue, NumgenRuleSegment segment) {
        String value = String.valueOf(sequenceValue);
        int width = segment.getSeqWidth() == null ? 1 : segment.getSeqWidth();
        String pad = StringUtils.hasText(segment.getPadChar()) ? segment.getPadChar() : "0";
        if (value.length() >= width) {
            return value;
        }
        return pad.repeat(width - value.length()) + value;
    }

    private List<NumgenRuleSegment> sorted(List<NumgenRuleSegment> segments) {
        return segments.stream()
                .sorted(Comparator.comparing(NumgenRuleSegment::getSortOrder).thenComparing(NumgenRuleSegment::getId))
                .collect(java.util.stream.Collectors.toList());
    }

    private String segmentName(NumgenRuleSegment segment) {
        return StringUtils.hasText(segment.getSegmentName()) ? segment.getSegmentName() : "片段" + segment.getSortOrder();
    }
}
