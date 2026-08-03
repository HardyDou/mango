package io.mango.infra.web.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlobalExceptionHandlerTest {

    @Test
    void unsafeUnknownFieldName_fallsBackWithoutReflectingUntrustedContent() {
        ObjectMapper objectMapper = new ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        String sensitiveValue = "customer-token-unit-value";
        String unsafeField = "bad\\nfield";
        JsonProcessingException jacksonFailure = assertThrows(JsonProcessingException.class,
                () -> objectMapper.readValue("{\"" + unsafeField + "\":\"" + sensitiveValue + "\"}", Payload.class));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/web/body");

        R<Void> response = new GlobalExceptionHandler().handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("request body cannot be read", jacksonFailure), request);

        assertEquals(400, response.getCode());
        assertFalse(response.isSuccess());
        assertEquals("请求体格式错误，请检查 JSON 语法和字段格式", response.getMsg());
        assertFalse(response.toString().contains("bad"));
        assertFalse(response.toString().contains(sensitiveValue));
        assertFalse(response.toString().contains("com.fasterxml.jackson"));
    }

    private record Payload(Integer count) {
    }
}
