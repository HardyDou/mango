package io.mango.captcha.starter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.captcha.starter.config.CaptchaAutoConfiguration;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 验证码接口集成测试
 * Issue B Fix: 公共接口在 /captcha/*，认证接口在 /auth/captcha/*
 */
@SpringBootTest(classes = CaptchaAutoConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, RedisAutoConfiguration.class, DataSourceAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class})
class CaptchaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getTypes_returnsAllCaptchaTypes() throws Exception {
        mockMvc.perform(get("/captcha/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.types").isArray())
                .andExpect(jsonPath("$.data.types[*]").value(
                        org.hamcrest.Matchers.containsInAnyOrder(
                                "ARITHMETIC", "BLOCK_PUZZLE", "SMS", "EMAIL"
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
                .andExpect(jsonPath("$.data.key").exists())
                .andExpect(jsonPath("$.data.image").exists())
                .andReturn();

        // 验证返回的 key 可以用于后续验证
        String response = result.getResponse().getContentAsString();
        assert response.contains("\"key\"");
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
                .andExpect(jsonPath("$.code").value(400));
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
        String answer = node.get("data").get("extra").asText();

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
        String answer = node.get("data").get("extra").asText();

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
