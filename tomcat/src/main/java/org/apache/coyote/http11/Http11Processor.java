package org.apache.coyote.http11;

import nextstep.jwp.db.InMemoryUserRepository;
import nextstep.jwp.exception.UncheckedServletException;
import nextstep.jwp.model.User;
import org.apache.coyote.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class Http11Processor implements Runnable, Processor {

    private static final Logger log = LoggerFactory.getLogger(Http11Processor.class);

    private final Socket connection;

    public Http11Processor(final Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        log.info("connect host: {}, port: {}", connection.getInetAddress(), connection.getPort());
        process(connection);
    }

    @Override
    public void process(final Socket connection) {
        try (final var inputStream = connection.getInputStream();
             final var outputStream = connection.getOutputStream();
             final var bufferedReader = new BufferedReader(new InputStreamReader(inputStream))
        ) {
            final HttpRequest httpRequest = HttpRequest.parse(bufferedReader);
            final HttpResponse httpResponse = handleRequest(httpRequest);
            final var response = httpResponse.toString();

            outputStream.write(response.getBytes());
            outputStream.flush();
        } catch (IOException | UncheckedServletException e) {
            log.error(e.getMessage(), e);
        }
    }

    private HttpResponse handleRequest(final HttpRequest httpRequest) throws IOException {
        final RequestPath requestPath = httpRequest.getRequestPath();

        if (requestPath.getResource().equals("/login")) {
            if (httpRequest.getHttpMethod().equals(HttpMethod.GET)) {
                if (requestPath.isParamEmpty()) {
                    final URL url = getClass().getClassLoader()
                            .getResource("static" + requestPath.getResource() + HttpExtensionType.HTML.getExtension());
                    final Path path = new File(url.getPath()).toPath();
                    final String content = new String(Files.readAllBytes(path));
                    return HttpResponse.of(HttpStatusCode.OK, ResponseBody.of(HttpExtensionType.HTML.getExtension(), content));
                }
            }
            if (httpRequest.getHttpMethod().equals(HttpMethod.POST)) {
                if (executeLogin(httpRequest.getRequestBody())) {
                    HttpCookie cookie = HttpCookie.from(httpRequest.getRequestHeaders().geHeaderValue("Cookie"));
                    if (!cookie.contains("JSESSIONID")) {
                        cookie.setCookie("JSESSIONID", UUID.randomUUID().toString());
                    }

                    final URL url = getClass().getClassLoader()
                            .getResource("static" + "/index" + HttpExtensionType.HTML.getExtension());
                    final Path path = new File(url.getPath()).toPath();
                    final String content = new String(Files.readAllBytes(path));
                    final ResponseBody responseBody = ResponseBody.of(HttpExtensionType.HTML.getExtension(), content);
                    return HttpResponse.withCookie(HttpStatusCode.FOUND, responseBody, cookie);
                }
                final URL url = getClass().getClassLoader()
                        .getResource("static" + "/401" + HttpExtensionType.HTML.getExtension());
                final Path path = new File(url.getPath()).toPath();
                final String content = new String(Files.readAllBytes(path));
                final ResponseBody responseBody = ResponseBody.of(HttpExtensionType.HTML.getExtension(), content);
                return HttpResponse.of(HttpStatusCode.UNAUTHORIZED, responseBody);
            }
        }

        if (requestPath.getResource().equals("/register") && httpRequest.getHttpMethod().equals(HttpMethod.GET)) {
            final URL url = getClass().getClassLoader()
                    .getResource("static" + "/register" + HttpExtensionType.HTML.getExtension());
            final Path path = new File(url.getPath()).toPath();
            final String content = new String(Files.readAllBytes(path));
            final ResponseBody responseBody = ResponseBody.of(HttpExtensionType.HTML.getExtension(), content);
            return HttpResponse.of(HttpStatusCode.OK, responseBody);
        }

        if (requestPath.getResource().equals("/register") && httpRequest.getHttpMethod().equals(HttpMethod.POST)) {
            final Map<String, String> requestBody = httpRequest.getRequestBody();

            final String account = requestBody.get("account");
            final String password = requestBody.get("password");
            final String email = requestBody.get("email");

            if (InMemoryUserRepository.checkExistingId(account)) {
                final URL url = getClass().getClassLoader()
                        .getResource("static" + "/409" + HttpExtensionType.HTML.getExtension());
                final Path path = new File(url.getPath()).toPath();
                final String content = new String(Files.readAllBytes(path));
                final ResponseBody responseBody = ResponseBody.of(HttpExtensionType.HTML.getExtension(), content);
                return HttpResponse.of(HttpStatusCode.OK, responseBody);
            }
            final User user = new User(account, password, email);

            InMemoryUserRepository.save(user);


            final URL url = getClass().getClassLoader()
                    .getResource("static" + "/index" + HttpExtensionType.HTML.getExtension());
            final Path path = new File(url.getPath()).toPath();
            final String content = new String(Files.readAllBytes(path));
            final ResponseBody responseBody = ResponseBody.of(HttpExtensionType.HTML.getExtension(), content);
            return HttpResponse.of(HttpStatusCode.OK, responseBody);
        }

        if (requestPath.getResource().equals("/")) {
            final ResponseBody responseBody = ResponseBody.of(HttpExtensionType.HTML.getExtension(), "Hello world!");
            return HttpResponse.of(HttpStatusCode.OK, responseBody);
        }

        final URL url = getClass().getClassLoader()
                .getResource("static" + requestPath.getResource());
        final Path path = new File(url.getPath()).toPath();
        try {
            final String content = new String(Files.readAllBytes(path));
            return HttpResponse.of(HttpStatusCode.OK, ResponseBody.of(HttpExtensionType.from(requestPath.getResource()).getExtension(), content));

        } catch (IOException exception) {
            log.error(exception.getMessage());
            throw new UncheckedServletException("유효하지 않은 리소스에 대한 접근입니다.");
        }
    }

    private boolean executeLogin(Map<String, String> params) {
        try {
            final String userName = params.get("account");
            final String password = params.get("password");

            final User user = InMemoryUserRepository.findByAccount(userName)
                    .orElseThrow(() -> new UncheckedServletException("존재하지 않는 userName입니다."));

            if (!user.checkPassword(password)) {
                throw new UncheckedServletException("비밀번호가 일치하지 않습니다.");
            }
            log.info(user.toString());
            return true;
        } catch (UncheckedServletException exception) {
            return false;
        }
    }
}
