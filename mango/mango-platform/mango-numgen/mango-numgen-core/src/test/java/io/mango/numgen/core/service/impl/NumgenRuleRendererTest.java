package io.mango.numgen.core.service.impl;

import io.mango.numgen.core.entity.NumgenRule;
import io.mango.numgen.core.entity.NumgenRuleSegment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
class NumgenRuleRendererTest {

    private final NumgenRuleRenderer renderer = new NumgenRuleRenderer();

    @Test
    void validate_supportsFixedDateParamSeqRule() {
        NumgenRule rule = rule();

        List<NumgenRuleSegment> segments = List.of(
                segment(1, "TEXT", "SO", null, null, null, null),
                segment(2, "DATE", null, null, "yyyyMMdd", null, null),
                segment(3, "PARAM", null, "orgCode", null, null, null),
                segment(4, "SEQ", null, null, null, 6, "0")
        );

        assertThat(renderer.validate(rule, segments).isValid()).isTrue();
    }

    @Test
    void validate_allowsRuleWithoutSequence() {
        NumgenRule rule = rule();

        List<NumgenRuleSegment> segments = List.of(segment(1, "TEXT", "SO", null, null, null, null));

        assertThat(renderer.validate(rule, segments).isValid()).isTrue();
    }

    @Test
    void render_allowsMultipleSequenceSegments() {
        List<NumgenRuleSegment> segments = List.of(
                segment(1, "SEQ", null, null, null, 2, "0"),
                segment(2, "TEXT", "-", null, null, null, null),
                segment(3, "SEQ", null, null, null, 4, "0")
        );

        String value = renderer.render(segments, Map.of(), 7L);

        assertThat(value).isEqualTo("07-0007");
    }

    @Test
    void render_buildsNumberFromOrderedSegments() {
        List<NumgenRuleSegment> segments = List.of(
                segment(2, "PARAM", null, "orgCode", null, null, null),
                segment(1, "TEXT", "SO", null, null, null, null),
                segment(3, "SEQ", null, null, null, 4, "0")
        );

        String value = renderer.render(segments, Map.of("orgCode", "A1"), 7L);

        assertThat(value).isEqualTo("SOA10007");
    }

    @Test
    void render_supportsPlaceholdersInTextSegment() {
        List<NumgenRuleSegment> segments = List.of(
                segment(1, "TEXT", "SO", null, null, null, null),
                segment(2, "TEXT", "${orgCode}-${bizType}", null, null, null, null)
        );

        String value = renderer.render(segments, Map.of("orgCode", "A1", "bizType", "SALE"), 1L);

        assertThat(value).isEqualTo("SOA1-SALE");
    }

    @Test
    void render_supportsExpressionSegment() {
        List<NumgenRuleSegment> segments = List.of(
                segment(1, "TEXT", "SO", null, null, null, null),
                segment(2, "EXPR", "${orgCode}-${bizType}", null, null, null, null)
        );

        String value = renderer.render(segments, Map.of("orgCode", "A1", "bizType", "SALE"), 1L);

        assertThat(value).isEqualTo("SOA1-SALE");
    }

    private NumgenRule rule() {
        NumgenRule rule = new NumgenRule();
        rule.setGenKey("ORDER_NO");
        rule.setRuleName("订单号");
        rule.setVersion(1);
        return rule;
    }

    private NumgenRuleSegment segment(int sortOrder, String type, String literalValue, String variableKey, String dateFormat, Integer seqWidth, String padChar) {
        NumgenRuleSegment segment = new NumgenRuleSegment();
        segment.setSortOrder(sortOrder);
        segment.setSegmentType(type);
        segment.setSegmentName(type + "-" + sortOrder);
        segment.setLiteralValue(literalValue);
        segment.setVariableKey(variableKey);
        segment.setDateFormat(dateFormat);
        segment.setSeqWidth(seqWidth);
        segment.setPadChar(padChar);
        return segment;
    }
}
