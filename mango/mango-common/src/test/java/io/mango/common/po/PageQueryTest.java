package io.mango.common.po;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageQueryTest {

    @Test
    void getPage_returnsAtLeastOne() {
        PageQuery query = new PageQuery();
        query.setPage(0);

        assertThat(query.getPage()).isEqualTo(1);
    }

    @Test
    void getSize_clampsToDefaultWhenNonPositive() {
        PageQuery query = new PageQuery();
        query.setSize(0);

        assertThat(query.getSize()).isEqualTo(10);
    }

    @Test
    void getSize_clampsToMaxSize() {
        PageQuery query = new PageQuery();
        query.setSize(1000);

        assertThat(query.getSize()).isEqualTo(500);
    }

    @Test
    void pageQuery_shouldBeQuery() {
        PageQuery query = new PageQuery();

        assertThat(query).isInstanceOf(Query.class);
    }

    @Test
    void setPageNum_aliasesPage() {
        PageQuery query = new PageQuery();
        query.setPageNum(3L);

        assertThat(query.getPage()).isEqualTo(3);
    }

    @Test
    void setPageSize_aliasesSizeAndKeepsClamp() {
        PageQuery query = new PageQuery();
        query.setPageSize(50L);

        assertThat(query.getSize()).isEqualTo(50);

        query.setPageSize(1000L);

        assertThat(query.getSize()).isEqualTo(500);
    }

    @Test
    void setAliases_ignoreNullValues() {
        PageQuery query = new PageQuery();
        query.setPage(2);
        query.setSize(20);

        query.setPageNum(null);
        query.setPageSize(null);

        assertThat(query.getPage()).isEqualTo(2);
        assertThat(query.getSize()).isEqualTo(20);
    }
}
