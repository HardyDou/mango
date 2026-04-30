package io.mango.infra.persistence.api.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersistencePageQueryTest {

    @Test
    void pageAndSize_shouldBeNormalized() {
        PersistencePageQuery query = new PersistencePageQuery();
        query.setPage(0);
        query.setSize(1000);

        assertThat(query.getPage()).isEqualTo(1);
        assertThat(query.getSize()).isEqualTo(500);
    }

    @Test
    void sortDirection_shouldFallbackToDesc() {
        PersistenceSort sort = new PersistenceSort();
        sort.setField(" created_at ");
        sort.setDirection("invalid");

        assertThat(sort.getField()).isEqualTo("created_at");
        assertThat(sort.getDirection()).isEqualTo("desc");
    }

    @Test
    void pageResult_shouldCalculatePages() {
        PersistencePageResult<String> result = PersistencePageResult.of(List.of("a"), 21, 2, 10);

        assertThat(result.getPages()).isEqualTo(3);
        assertThat(result.getRecords()).containsExactly("a");
    }
}
