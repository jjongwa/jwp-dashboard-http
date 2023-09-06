package org.apache.coyote.http11.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import nextstep.jwp.exception.UncheckedServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestLineTest {

    @DisplayName("Request URI를 읽고 올바른 Request 객체를 생성한다.")
    @Test
    void from() {
        // given
        final String line = "GET /index.html HTTP/1.1 ";
        final HttpMethod expectedMethodType = HttpMethod.GET;
        final RequestPath expectedRequestPath = RequestPath.from("/index.html");

        // when
        final RequestLine actual = RequestLine.from(line);

        // then
        assertAll(
                () -> assertThat(actual.getHttpMethod()).isEqualTo(expectedMethodType),
                () -> assertThat(actual.getRequestPath().getResource()).isEqualTo(expectedRequestPath.getResource()),
                () -> assertThat(actual.getRequestPath().getQueryParameter()).isEqualTo(expectedRequestPath.getQueryParameter())
        );
    }

    @DisplayName("Request URI가 존재하지 않으면 에외 처리한다.")
    @Test
    void from_nullRequest() {
        assertThatThrownBy(() -> RequestLine.from(null))
                .isInstanceOf(UncheckedServletException.class)
                .hasMessage("request가 존재하지 않습니다.");
    }
}
