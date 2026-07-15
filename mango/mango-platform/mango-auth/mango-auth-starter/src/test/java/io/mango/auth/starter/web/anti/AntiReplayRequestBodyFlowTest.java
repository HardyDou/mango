package io.mango.auth.starter.web.anti;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.core.anti.IdempotencyGuard;
import io.mango.auth.core.anti.ReplayGuard;
import io.mango.auth.core.anti.SignatureValidator;
import io.mango.common.result.R;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("flow")
@Tag("auth")
@DisplayName("防重放签名请求体流程测试")
class AntiReplayRequestBodyFlowTest {

    @Test
    @DisplayName("签名校验读取 JSON 后控制器仍应收到完整请求体")
    void signedJsonBodyShouldRemainReadableByController() throws Exception {
        String appKey = "flow-app";
        String secret = "flow-secret";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String body = "{\"name\":\"mango\"}";
        SignatureValidator validator = new SignatureValidator();
        String signatureData = validator.buildSignatureData(appKey, secret, timestamp, body);
        String signature = validator.computeSignature("MD5", signatureData);

        AntiReplayProperties properties = new AntiReplayProperties();
        properties.setAppSecrets(Map.of(appKey, secret));
        AntiReplayFilter filter = new AntiReplayFilter(
                mock(ReplayGuard.class),
                mock(IdempotencyGuard.class),
                validator,
                new ConfiguredAppSecretProvider(properties),
                new ObjectMapper());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EchoController())
                .addFilters(filter)
                .build();

        mockMvc.perform(post("/flow/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Sign-Algorithm", "MD5")
                        .header("X-App-Key", appKey)
                        .header("X-Request-Timestamp", timestamp)
                        .header("X-Sign", signature)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("mango"));
    }

    @RestController
    static class EchoController {

        @PostMapping("/flow/echo")
        R<String> echo(@RequestBody EchoBody body) {
            return R.ok(body.name());
        }
    }

    record EchoBody(String name) {
    }
}
