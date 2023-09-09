package org.apache.coyote.http11.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.HashMap;
import java.util.Map;
import org.apache.coyote.http11.ExtensionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestPathTest {

    @DisplayName("queryParameter가 없는 경우")
    @Test
    void from_emptyQueryParameter() {
        // given
        final String uri = "/index.html";
        final String expectedResource = "/index";
        final ExtensionType expectedExtensionType = ExtensionType.HTML;

        // when
        final RequestPath requestPath = RequestPath.from(uri);

        // then
        assertAll(
                () -> assertThat(requestPath.getResource())
                        .isEqualTo(expectedResource + expectedExtensionType.getExtension()),
                () -> assertThat(requestPath.getQueryParameter()).isEmpty()
        );
    }

    @DisplayName("queryParameter가 있는 경우")
    @Test
    void from_hasQueryParameter() {
        // given
        final String uri = "/login?account=gugu&password=password";
        final String expectedResource = "/login";
        final ExtensionType expectedExtensionType = ExtensionType.HTML;
        final Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("account", "gugu");
        expectedParams.put("password", "password");

        // when
        final RequestPath requestPath = RequestPath.from(uri);

        // then
        assertAll(
                () -> assertThat(requestPath.getResource()).isEqualTo(expectedResource),
                () -> assertThat(requestPath.getQueryParameter()).isEqualTo(expectedParams)
        );
    }
}
