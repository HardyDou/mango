package io.mango.infra.web.integration;

import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.web.api.Inner;
import io.mango.infra.web.util.InternalCallSignature;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = WebBoundaryIntegrationTest.TestApplication.class,
        properties = {
                "spring.main.banner-mode=off",
                "spring.jackson.deserialization.fail-on-unknown-properties=true",
                "mango.web.inner.secret=web-e2e-secret"
        })
@ExtendWith(OutputCaptureExtension.class)
@Tag("flow")
@Tag("infra-web")
class WebBoundaryIntegrationTest {

    private static final String SECRET = "web-e2e-secret";

    private final TestRestTemplate restTemplate;

    @Autowired
    WebBoundaryIntegrationTest(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Test
    void jacksonContract_boundaryValues_serializesAsStrings() {
        ResponseEntity<String> response = restTemplate.getForEntity("/test/web/value", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"id\":\"9007199254740993\""));
        assertTrue(response.getBody().contains("\"createdAt\":\"2026-07-15 10:11:12\""));
    }

    @Test
    void exceptionAdvice_failureBranches_preserveHttpAndBusinessContracts() {
        assertFailure("/test/web/biz", HttpMethod.GET, null, HttpStatus.OK, 4101, "business rejected");
        assertFailure("/test/web/sql", HttpMethod.GET, null,
                HttpStatus.INTERNAL_SERVER_ERROR, 500, "数据库操作异常");
        assertFailure("/test/web/system", HttpMethod.GET, null,
                HttpStatus.INTERNAL_SERVER_ERROR, 500, "系统异常");
        assertFailure("/test/web/body", HttpMethod.POST, new HttpEntity<>("{broken", jsonHeaders()),
                HttpStatus.BAD_REQUEST, 400, "请求体格式错误，请检查 JSON 语法和字段格式");
        assertFailure("/test/web/constraint?value=", HttpMethod.GET, null,
                HttpStatus.BAD_REQUEST, 400, "测试参数不能为空");
        assertFailure("/test/web/value", HttpMethod.POST, null,
                HttpStatus.METHOD_NOT_ALLOWED, 405, "不支持的请求方法: POST");
        assertFailure("/test/web/missing", HttpMethod.GET, null,
                HttpStatus.NOT_FOUND, 404, "资源不存在");
    }

    @Test
    void requestBody_unknownNestedField_reportsFullPathWithoutEchoingInput() {
        String sensitiveValue = "customer-token-unknown-value";
        String body = """
                {"materials":[{"materialCode":"M-1","materialCategoryCode":"%s","quantity":1}],
                 "submittedAt":"2026-08-03 12:30:00"}
                """.formatted(sensitiveValue);

        assertSafeBodyFailure(body, "请求字段 materials[0].materialCategoryCode 不受支持", sensitiveValue);
    }

    @Test
    void requestBody_nestedArrayTypeMismatch_reportsSafeTypeCategory() {
        String sensitiveValue = "customer-password-type-value";
        String body = """
                {"materials":[{"materialCode":"M-1","quantity":"%s"}],
                 "submittedAt":"2026-08-03 12:30:00"}
                """.formatted(sensitiveValue);

        assertSafeBodyFailure(body, "请求字段 materials[0].quantity 类型不正确，期望 NUMBER", sensitiveValue);
    }

    @Test
    void requestBody_invalidDateTime_reportsFieldWithoutEchoingInput() {
        String sensitiveValue = "customer-token-invalid-date";
        String body = """
                {"materials":[{"materialCode":"M-1","quantity":1}],"submittedAt":"%s"}
                """.formatted(sensitiveValue);

        assertSafeBodyFailure(body, "请求字段 submittedAt 日期时间格式不正确", sensitiveValue);
    }

    @Test
    void requestBody_malformedJson_returnsSafeTopLevelError() {
        assertSafeBodyFailure("{\"materials\":[{\"materialCode\":\"secret-value\"}",
                "请求体格式错误，请检查 JSON 语法和字段格式", "secret-value");
    }

    @Test
    void sseTimeout_closesTheCommittedStreamWithoutJsonExceptionNoise(CapturedOutput output) {
        ResponseEntity<String> response = restTemplate.getForEntity("/test/web/sse-timeout", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.TEXT_EVENT_STREAM, response.getHeaders().getContentType());
        assertTrue(response.getBody().contains("connected"));
        assertFalse(output.getAll().contains("HttpMessageNotWritableException"), output.getAll());
        assertFalse(output.getAll().contains("uri=/test/web/sse-timeout, query="), output.getAll());
    }

    @Test
    void innerCall_validSignature_passesOnceAndRejectsReplay() {
        String path = "/test/web/inner";
        String rawQuery = "tag=2&tag=1";
        String timestamp = Long.toString(System.currentTimeMillis());
        String nonce = "web-e2e-nonce";
        String signature = InternalCallSignature.sign(timestamp, nonce, "GET", path,
                InternalCallSignature.canonicalizeRawQuery(rawQuery), SECRET);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Call", "true");
        headers.set("X-Internal-Timestamp", timestamp);
        headers.set("X-Internal-Nonce", nonce);
        headers.set("X-Internal-Signature", signature);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> first = restTemplate.exchange(path + "?" + rawQuery,
                HttpMethod.GET, request, String.class);
        ResponseEntity<String> replay = restTemplate.exchange(path + "?" + rawQuery,
                HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals("1,2", first.getBody());
        assertEquals(HttpStatus.FORBIDDEN, replay.getStatusCode());
        assertTrue(replay.getBody().contains("Nonce already used"));
    }

    @Test
    void innerCall_unsignedRequest_rejectsInnerAndAllowsPublic() {
        ResponseEntity<String> inner = restTemplate.getForEntity("/test/web/inner?tag=1", String.class);
        ResponseEntity<String> publicResponse = restTemplate.getForEntity("/test/web/value", String.class);

        assertEquals(HttpStatus.FORBIDDEN, inner.getStatusCode());
        assertEquals(HttpStatus.OK, publicResponse.getStatusCode());
    }

    private void assertFailure(String path, HttpMethod method, HttpEntity<?> request,
                               HttpStatus status, int code, String message) {
        ResponseEntity<R> response = restTemplate.exchange(path, method, request, R.class);
        assertEquals(status, response.getStatusCode());
        assertEquals(code, response.getBody().getCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals(message, response.getBody().getMsg());
    }

    private void assertSafeBodyFailure(String requestBody, String message, String sensitiveValue) {
        ResponseEntity<R> response = restTemplate.exchange("/test/web/typed-body", HttpMethod.POST,
                new HttpEntity<>(requestBody, jsonHeaders()), R.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals(message, response.getBody().getMsg());
        String responseText = response.getBody().toString();
        assertFalse(responseText.contains(sensitiveValue));
        assertFalse(responseText.contains("com.fasterxml.jackson"));
        assertFalse(responseText.contains("GlobalExceptionHandler"));
        assertFalse(responseText.contains("at io.mango"));
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        return headers;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(WebBoundaryController.class)
    static class TestApplication {

        @Bean
        IKvStore testKvStore() {
            return new AtomicKvStore();
        }
    }

    @RestController
    @RequestMapping("/test/web")
    @Validated
    static class WebBoundaryController {

        @GetMapping("/value")
        BoundaryValue value() {
            return new BoundaryValue(9_007_199_254_740_993L, LocalDateTime.of(2026, 7, 15, 10, 11, 12));
        }

        @GetMapping("/biz")
        void businessFailure() {
            throw new BizException(4101, "business rejected");
        }

        @GetMapping("/sql")
        void sqlFailure() throws SQLException {
            throw new SQLException("sensitive database detail");
        }

        @GetMapping("/system")
        void systemFailure() {
            throw new IllegalStateException("sensitive system detail");
        }

        @GetMapping(value = "/sse-timeout", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        SseEmitter sseTimeout() throws Exception {
            SseEmitter emitter = new SseEmitter(100L);
            emitter.send(SseEmitter.event().name("message").data("connected"));
            return emitter;
        }

        @PostMapping("/body")
        void body(@RequestBody Map<String, Object> body) {
            // Parsing the request body is the boundary contract under test.
        }

        @PostMapping("/typed-body")
        void typedBody(@RequestBody BoundaryRequest body) {
            // Typed Jackson parsing is the boundary contract under test.
        }

        @GetMapping("/constraint")
        void constraint(@RequestParam("value") @NotBlank(message = "测试参数不能为空") String value) {
            // Method validation is the boundary contract under test.
        }

        @Inner
        @GetMapping("/inner")
        String inner(@RequestParam List<String> tag) {
            return tag.stream().sorted().reduce((left, right) -> left + "," + right).orElse("");
        }
    }

    record BoundaryValue(Long id, LocalDateTime createdAt) {
    }

    record BoundaryRequest(List<MaterialRequest> materials, LocalDateTime submittedAt) {
    }

    record MaterialRequest(String materialCode, Integer quantity) {
    }

    private static final class AtomicKvStore implements IKvStore {

        private final Map<String, String> values = new ConcurrentHashMap<>();

        @Override
        public boolean put(String key, String value, long expireSeconds) {
            return values.putIfAbsent(key, value) == null;
        }

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public long increment(String key, long windowSeconds) {
            throw new UnsupportedOperationException("increment is not used by the web boundary");
        }

        @Override
        public void delete(String key) {
            values.remove(key);
        }

        @Override
        public boolean exists(String key) {
            return values.containsKey(key);
        }
    }
}
