package org.apache.coyote.http11.request;

import java.util.Arrays;
import nextstep.jwp.exception.NotAllowedMethodException;

public enum HttpMethod {

    GET,
    POST,
    PUT,
    PATCH,
    DELETE;

    public static HttpMethod from(final String request) {
        return Arrays.stream(values())
                .filter(httpMethodType -> httpMethodType.name().equals(request))
                .findAny()
                .orElseThrow(NotAllowedMethodException::new);
    }
}
