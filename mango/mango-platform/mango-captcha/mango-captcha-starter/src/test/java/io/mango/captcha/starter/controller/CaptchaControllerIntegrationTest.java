package io.mango.captcha.starter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.enums.CaptchaCode;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.captcha.core.service.ICaptchaService;
import io.mango.infra.kv.api.IKvStore;
import io.mango.captcha.starter.config.CaptchaAutoConfiguration;
import io.mango.infra.web.starter.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 验证码接口集成测试
 * Issue B Fix: 公共接口在 /captcha/*，认证接口在 /auth/captcha/*
 */
@SpringBootTest(classes = CaptchaAutoConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mango.captcha.sms.fixed-code-enabled=true",
        "mango.captcha.sms.fixed-code=0000"
})
@Import({TestConfig.class, GlobalExceptionHandler.class, RedisAutoConfiguration.class, DataSourceAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class})
class CaptchaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaptchaApi captchaApi;

    @Autowired
    private ICaptchaService captchaService;

    @Autowired
    private IKvStore kvStore;

    @Test
    void captchaApiBean_isProvidedByController() {
        assertThat(captchaApi).isInstanceOf(CaptchaController.class);
        assertThat(captchaService).isNotInstanceOf(CaptchaApi.class);
    }

    @Test
    void getTypes_returnsAllCaptchaTypes() throws Exception {
        mockMvc.perform(get("/captcha/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.types").isArray())
                .andExpect(jsonPath("$.data.types[*]").value(
                        org.hamcrest.Matchers.containsInAnyOrder(
                                "ARITHMETIC", "GRAPHIC", "BLOCK_PUZZLE", "CLICK_WORD", "BEHAVIOR", "SMS", "EMAIL"
                        )
                ))
                .andExpect(jsonPath("$.data.currentStorage").exists());
    }

    @Test
    void generateArithmetic_returnsValidCaptcha() throws Exception {
        MvcResult result = mockMvc.perform(get("/captcha/arithmetic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("ARITHMETIC"))
                .andExpect(jsonPath("$.data.graphicType").value("ALPHANUMERIC"))
                .andExpect(jsonPath("$.data.key").exists())
                .andExpect(jsonPath("$.data.image").exists())
                .andReturn();

        // 验证返回的 key 可以用于后续验证
        String response = result.getResponse().getContentAsString();
        assert response.contains("\"key\"");
    }

    @Test
    void generateGraphic_returnsSimpleAlphanumericByDefault() throws Exception {
        mockMvc.perform(get("/captcha/graphic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("GRAPHIC"))
                .andExpect(jsonPath("$.data.graphicType").value("ALPHANUMERIC"))
                .andExpect(jsonPath("$.data.difficulty").value("SIMPLE"))
                .andExpect(jsonPath("$.data.key").exists())
                .andExpect(jsonPath("$.data.image").exists())
                .andExpect(jsonPath("$.data.extra").doesNotExist());
    }

    @Test
    void generateGraphic_acceptsSessionTypeAndDifficulty() throws Exception {
        mockMvc.perform(get("/captcha/graphic")
                        .param("type", "ALPHANUMERIC")
                        .param("level", "MEDIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("GRAPHIC"))
                .andExpect(jsonPath("$.data.graphicType").value("ALPHANUMERIC"))
                .andExpect(jsonPath("$.data.difficulty").value("MEDIUM"));
    }

    @Test
    void generateBlockPuzzle_returnsValidCaptcha() throws Exception {
        mockMvc.perform(get("/captcha/block-puzzle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("BLOCK_PUZZLE"))
                .andExpect(jsonPath("$.data.key").exists())
                .andExpect(jsonPath("$.data.backgroundImage").exists())
                .andExpect(jsonPath("$.data.sliderImage").exists())
                .andExpect(jsonPath("$.data.y").exists());
    }

    @Test
    void generateClickWord_returnsValidCaptcha() throws Exception {
        mockMvc.perform(get("/captcha/click-word"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("CLICK_WORD"))
                .andExpect(jsonPath("$.data.key").exists())
                .andExpect(jsonPath("$.data.image").exists())
                .andExpect(jsonPath("$.data.target").exists())
                .andExpect(jsonPath("$.data.extra").exists());
    }

    @Test
    void generateBehavior_returnsValidCaptcha() throws Exception {
        mockMvc.perform(get("/captcha/behavior"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("BEHAVIOR"))
                .andExpect(jsonPath("$.data.key").exists())
                .andExpect(jsonPath("$.data.expireTime").exists())
                .andExpect(jsonPath("$.data.extra").exists());
    }

    @Test
    void sendSms_withConfiguredFixedCode_savesAndVerifies() {
        CaptchaSendRequest sendRequest = new CaptchaSendRequest();
        sendRequest.setType(CaptchaType.SMS);
        sendRequest.setTarget("13800138000");
        sendRequest.setBusinessType("LOGIN");

        String key = captchaService.send(sendRequest);

        assertThat(key).isEqualTo("captcha:LOGIN:13800138000");
        CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest();
        verifyRequest.setKey(key.substring("captcha:".length()));
        verifyRequest.setType(CaptchaType.SMS);
        verifyRequest.setCode("0000");
        assertThat(captchaService.verify(verifyRequest)).isTrue();
    }

    @Test
    void sendSms_withValidMobile_returnsKey() throws Exception {
        // SMS/EMAIL 发送是内部 API，不暴露为 HTTP 端点
        // 此测试验证控制器不提供该端点（404）
        mockMvc.perform(post("/auth/captcha/send")
                        .param("type", "SMS")
                        .param("target", "13800138000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sendEmail_withValidEmail_returnsKey() throws Exception {
        // SMS/EMAIL 发送是内部 API，不暴露为 HTTP 端点
        // 此测试验证控制器不提供该端点（404）
        mockMvc.perform(post("/auth/captcha/send")
                        .param("type", "EMAIL")
                        .param("target", "test@example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void verify_withInvalidKey_returnsFail() throws Exception {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("invalid-key");
        request.setType(CaptchaType.ARITHMETIC);
        request.setCode("999");

        mockMvc.perform(post("/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(CaptchaCode.CAPTCHA_INVALID.getCode()));
    }

    @Test
    void verify_withEmptyKey_returnsValidationError() throws Exception {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("");
        request.setType(CaptchaType.ARITHMETIC);

        mockMvc.perform(post("/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verify_arithmeticCaptcha_withCorrectCode_returnsSuccess() throws Exception {
        // 1. 先生成一个算术验证码
        MvcResult generateResult = mockMvc.perform(get("/captcha/arithmetic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String response = generateResult.getResponse().getContentAsString();
        var node = objectMapper.readTree(response);
        String key = node.get("data").get("key").asText();
        String answer = objectMapper.readTree(kvStore.get("captcha:" + key)).path("answer").asText();

        // 2. 使用正确答案验证
        CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest();
        verifyRequest.setKey(key);
        verifyRequest.setType(CaptchaType.ARITHMETIC);
        verifyRequest.setCode(answer);

        mockMvc.perform(post("/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void verify_withUsedKey_returnsFail() throws Exception {
        // 1. 生成验证码
        MvcResult generateResult = mockMvc.perform(get("/captcha/arithmetic"))
                .andExpect(status().isOk())
                .andReturn();

        String response = generateResult.getResponse().getContentAsString();
        var node = objectMapper.readTree(response);
        String key = node.get("data").get("key").asText();
        String answer = objectMapper.readTree(kvStore.get("captcha:" + key)).path("answer").asText();

        // 2. 第一次验证成功
        CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest();
        verifyRequest.setKey(key);
        verifyRequest.setType(CaptchaType.ARITHMETIC);
        verifyRequest.setCode(answer);

        mockMvc.perform(post("/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. 第二次验证失败（验证码已被使用）
        mockMvc.perform(post("/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
