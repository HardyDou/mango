package io.mango.common.vo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    void getList_returnsDefensiveCopy() {
        PageResult<String> result = new PageResult<>();
        result.setList(List.of("a", "b"));

        List<String> copy = result.getList();
        copy.add("c");

        assertThat(result.getList()).containsExactly("a", "b");
    }

    @Test
    void setList_handlesNullAsEmptyList() {
        PageResult<String> result = new PageResult<>();

        result.setList(null);

        assertThat(result.getList()).isEmpty();
    }

    @Test
    void of_calculatesPages() {
        PageResult<String> result = PageResult.of(new ArrayList<>(List.of("a")), 21, 2, 10);

        assertThat(result.getPages()).isEqualTo(3);
        assertThat(result.getTotal()).isEqualTo(21);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(10);
    }
}
