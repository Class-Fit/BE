package com.example.classfit.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PageResponseTest {

    @Test
    void convertsPageToPageResponse() {
        // given
        List<String> content = List.of("수영", "축구");

        Page<String> page = new PageImpl<>(
                content,
                PageRequest.of(1, 2),
                5
        );

        // when
        PageResponse<String> response = PageResponse.from(page);

        // then
        assertThat(response.content()).containsExactly("수영", "축구");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalCount()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }

    @Test
    void convertsEmptyPageToPageResponse() {
        // given
        Page<String> page = Page.empty(PageRequest.of(0, 20));

        // when
        PageResponse<String> response = PageResponse.from(page);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalCount()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

}
