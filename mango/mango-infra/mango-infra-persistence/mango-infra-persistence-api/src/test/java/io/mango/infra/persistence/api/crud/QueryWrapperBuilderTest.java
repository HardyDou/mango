package io.mango.infra.persistence.api.crud;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.mango.common.po.PageQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QueryWrapperBuilderTest {

    private final QueryWrapperBuilder builder = new QueryWrapperBuilder();

    @Test
    void buildsAnnotatedConditionsAndIgnoresEmptyAndPagingFields() {
        SampleQuery query = new SampleQuery();
        query.name = "mango";
        query.statuses = List.of("ACTIVE", "LOCKED");
        query.createdRange = List.of(10L, 20L);
        query.blankValue = "  ";
        query.ignoredValue = "ignored";
        query.setPage(3L);
        query.setSize(50L);

        QueryWrapper<Object> wrapper = builder.build(query);

        assertThat(wrapper.getSqlSegment())
                .contains("display_name LIKE")
                .contains("status IN")
                .contains("created_at BETWEEN")
                .doesNotContain("blank_value", "ignored_value", "page", "size");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains("%mango%", "ACTIVE", "LOCKED", 10L, 20L);
    }

    @Test
    void supportsPrimitiveArraysForInConditions() {
        PrimitiveArrayQuery query = new PrimitiveArrayQuery();
        query.ids = new int[]{7, 9};

        QueryWrapper<Object> wrapper = builder.build(query);

        assertThat(wrapper.getSqlSegment()).contains("id IN");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(7, 9);
    }

    @Test
    void ignoresEmptyPrimitiveArraysForInConditions() {
        PrimitiveArrayQuery query = new PrimitiveArrayQuery();
        query.ids = new int[0];

        QueryWrapper<Object> wrapper = builder.build(query);

        assertThat(wrapper.getSqlSegment()).isEmpty();
        assertThat(wrapper.getParamNameValuePairs()).isEmpty();
    }

    @Test
    void buildsMapConditionsUsingDatabaseColumnNamesAndNormalizedSingleValues() {
        QueryWrapper<Object> wrapper = builder.build(Map.of(
                "tenantId", List.of(1001L),
                "displayName", "Mango",
                "page", 2,
                "empty", "  "));

        assertThat(wrapper.getSqlSegment())
                .contains("tenant_id =", "display_name =")
                .doesNotContain("page", "empty");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(1001L, "Mango");
    }

    private static final class SampleQuery extends PageQuery {

        @QueryField(column = "display_name", type = QueryType.LIKE)
        private String name;

        @QueryField(column = "status", type = QueryType.IN)
        private List<String> statuses;

        @QueryField(column = "created_at", type = QueryType.BETWEEN)
        private List<Long> createdRange;

        private String blankValue;

        @QueryIgnore
        private String ignoredValue;
    }

    private static final class PrimitiveArrayQuery {

        @QueryField(column = "id", type = QueryType.IN)
        private int[] ids;
    }
}
