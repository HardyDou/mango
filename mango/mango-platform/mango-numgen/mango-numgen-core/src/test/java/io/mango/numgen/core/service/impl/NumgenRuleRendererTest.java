package io.mango.numgen.core.service.impl;

import io.mango.numgen.core.entity.NumgenRuleEntity;
import io.mango.numgen.core.entity.NumgenRuleSegmentEntity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
class NumgenRuleRendererTest {

    private final NumgenRuleRenderer renderer = new NumgenRuleRenderer(
            Clock.fixed(Instant.parse("2026-05-23T14:30:59Z"), ZoneId.of("UTC")));

    @Test
    void validate_supportsFixedDateParamSeqRule() {
        NumgenRuleEntity rule = rule();

        List<NumgenRuleSegmentEntity> segments = List.of(
                segment(1, "TEXT", "SO", null, null, null, null),
                segment(2, "DATE", null, null, "yyyyMMdd", null, null),
                segment(3, "PARAM", null, "orgCode", null, null, null),
                segment(4, "SEQ", null, null, null, 6, "0")
        );

        assertThat(renderer.validate(rule, segments).isValid()).isTrue();
    }

    @Test
    void validate_supportsMMddAndCustomDateFormats() {
        List<NumgenRuleSegmentEntity> segments = List.of(
                segment(1, "DATE", null, null, "MMdd", null, null),
                segment(2, "DATE", null, null, "yyyy年MMdd日", null, null));

        assertThat(renderer.validate(rule(), segments).isValid()).isTrue();
        assertThat(renderer.render(segments, Map.of(), 1L)).isEqualTo("05232026年0523日");
    }

    @Test
    void validate_rejectsInvalidDateFormatBeforeRendering() {
        List<NumgenRuleSegmentEntity> segments = List.of(segment(1, "DATE", null, null, "yyyy'", null, null));

        assertThat(renderer.validate(rule(), segments).isValid()).isFalse();
        assertThat(renderer.validate(rule(), segments).getErrors())
                .anyMatch(error -> error.contains("日期格式非法") && error.contains("yyyy'"));
    }

    @Test
    void validate_allowsRuleWithoutSequence() {
        NumgenRuleEntity rule = rule();

        List<NumgenRuleSegmentEntity> segments = List.of(segment(1, "TEXT", "SO", null, null, null, null));

        assertThat(renderer.validate(rule, segments).isValid()).isTrue();
    }

    @Test
    void render_allowsMultipleSequenceSegments() {
        List<NumgenRuleSegmentEntity> segments = List.of(
                segment(1, "SEQ", null, null, null, 2, "0"),
                segment(2, "TEXT", "-", null, null, null, null),
                segment(3, "SEQ", null, null, null, 4, "0")
        );

        String value = renderer.render(segments, Map.of(), 7L);

        assertThat(value).isEqualTo("07-0007");
    }

    @Test
    void render_buildsNumberFromOrderedSegments() {
        List<NumgenRuleSegmentEntity> segments = List.of(
                segment(2, "PARAM", null, "orgCode", null, null, null),
                segment(1, "TEXT", "SO", null, null, null, null),
                segment(3, "SEQ", null, null, null, 4, "0")
        );

        String value = renderer.render(segments, Map.of("orgCode", "A1"), 7L);

        assertThat(value).isEqualTo("SOA10007");
    }

    @Test
    void render_supportsPlaceholdersInTextSegment() {
        List<NumgenRuleSegmentEntity> segments = List.of(
                segment(1, "TEXT", "SO", null, null, null, null),
                segment(2, "TEXT", "${orgCode}-${bizType}", null, null, null, null)
        );

        String value = renderer.render(segments, Map.of("orgCode", "A1", "bizType", "SALE"), 1L);

        assertThat(value).isEqualTo("SOA1-SALE");
    }

    @Test
    void render_supportsExpressionSegment() {
        List<NumgenRuleSegmentEntity> segments = List.of(
                segment(1, "TEXT", "SO", null, null, null, null),
                segment(2, "EXPR", "${orgCode}-${bizType}", null, null, null, null)
        );

        String value = renderer.render(segments, Map.of("orgCode", "A1", "bizType", "SALE"), 1L);

        assertThat(value).isEqualTo("SOA1-SALE");
    }

    @Test
    void sequenceScopeKey_usesMarkedNonSequenceSegments() {
        List<NumgenRuleSegmentEntity> segments = List.of(
                scopedSegment(1, "DATE", null, null, "yyyyMMdd", null, null, 1),
                scopedSegment(2, "PARAM", null, "orgCode", null, null, null, 1),
                scopedSegment(3, "SEQ", null, null, null, 4, "0", 1)
        );

        String scopeKey = renderer.sequenceScopeKey(segments, Map.of("orgCode", "A1"));

        assertThat(scopeKey).endsWith("|2:A1");
        assertThat(scopeKey).doesNotContain("0000");
    }

    @Test
    void sequenceScopeKey_supportsYearAndMonthDayBoundaries() {
        List<NumgenRuleSegmentEntity> segments = List.of(
                scopedSegment(1, "DATE", null, null, "yyyy", null, null, 1),
                scopedSegment(2, "DATE", null, null, "MMdd", null, null, 1));

        assertThat(renderer.sequenceScopeKey(segments, Map.of())).isEqualTo("1:2026|2:0523");
    }

    @Test
    void sequenceScopeKey_defaultsToGlobalWhenNoSegmentIsMarked() {
        List<NumgenRuleSegmentEntity> segments = List.of(
                segment(1, "TEXT", "SO", null, null, null, null),
                segment(2, "SEQ", null, null, null, 4, "0")
        );

        assertThat(renderer.sequenceScopeKey(segments, Map.of())).isEqualTo("GLOBAL");
    }

    private NumgenRuleEntity rule() {
        NumgenRuleEntity rule = new NumgenRuleEntity();
        rule.setGenKey("ORDER_NO");
        rule.setRuleName("订单号");
        rule.setVersion(1);
        return rule;
    }

    private NumgenRuleSegmentEntity segment(int sortOrder, String type, String literalValue, String variableKey, String dateFormat, Integer seqWidth, String padChar) {
        return scopedSegment(sortOrder, type, literalValue, variableKey, dateFormat, seqWidth, padChar, 0);
    }

    private NumgenRuleSegmentEntity scopedSegment(int sortOrder, String type, String literalValue, String variableKey, String dateFormat, Integer seqWidth, String padChar, Integer sequenceScope) {
        NumgenRuleSegmentEntity segment = new NumgenRuleSegmentEntity();
        segment.setSortOrder(sortOrder);
        segment.setSegmentType(type);
        segment.setSegmentName(type + "-" + sortOrder);
        segment.setLiteralValue(literalValue);
        segment.setVariableKey(variableKey);
        segment.setDateFormat(dateFormat);
        segment.setSeqWidth(seqWidth);
        segment.setPadChar(padChar);
        segment.setSequenceScope(sequenceScope);
        return segment;
    }
}
