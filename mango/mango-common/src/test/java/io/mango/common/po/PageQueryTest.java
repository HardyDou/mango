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

        assertThat(query.getSize()).isEqualTo(100);
    }
}
