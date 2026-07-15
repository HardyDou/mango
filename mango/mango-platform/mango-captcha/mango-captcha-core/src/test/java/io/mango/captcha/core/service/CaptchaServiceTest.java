package io.mango.captcha.core.service;

import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResponse;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.spi.EmailProvider;
import io.mango.captcha.api.spi.SmsProvider;
import io.mango.captcha.core.generator.ArithmeticCaptchaGenerator;
import io.mango.captcha.core.generator.BehaviorCaptchaEngine;
import io.mango.captcha.core.generator.BlockPuzzleCaptchaGenerator;
import io.mango.captcha.core.generator.ClickWordCaptchaGenerator;
import io.mango.captcha.core.service.impl.CaptchaService;
import io.mango.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证码服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    private CaptchaService captchaService;

    @Mock
    private io.mango.infra.kv.api.IKvStore kvStore;

    @Mock
    private ArithmeticCaptchaGenerator arithmeticCaptchaService;

    @Mock
    private BlockPuzzleCaptchaGenerator blockPuzzleCaptchaService;

    @Mock
    private ClickWordCaptchaGenerator clickWordCaptchaService;

    @Mock
    private BehaviorCaptchaEngine behaviorCaptchaService;

    @Mock
    private SmsProvider smsProvider;

    @Mock
    private EmailProvider emailProvider;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaService(
                kvStore,
                arithmeticCaptchaService,
                blockPuzzleCaptchaService,
                clickWordCaptchaService,
                behaviorCaptchaService,
                Arrays.asList(smsProvider),
                Arrays.asList(emailProvider),
                new ObjectMapper()
        );
    }

    @Test
    void generate_arithmeticType_savesToStorage() {
        CaptchaResponse arithmeticResponse = new CaptchaResponse();
        arithmeticResponse.setImage("data:image/png;base64,xxx");
        arithmeticResponse.setExtra("5");
        when(arithmeticCaptchaService.generate()).thenReturn(arithmeticResponse);

        CaptchaResponse result = captchaService.generateArithmetic();

        assertNotNull(result);
        assertEquals(CaptchaType.ARITHMETIC, result.getType());
        verify(kvStore).set(startsWith("captcha:"), eq("5"), anyLong());
    }

    @Test
    void generate_blockPuzzleType_savesXToStorage() {
        CaptchaResponse puzzleResponse = new CaptchaResponse();
        puzzleResponse.setBackgroundImage("data:image/png;base64,xxx");
        puzzleResponse.setX(100);
        when(blockPuzzleCaptchaService.generate()).thenReturn(puzzleResponse);

        CaptchaResponse result = captchaService.generateBlockPuzzle();

        assertNotNull(result);
        assertEquals(CaptchaType.BLOCK_PUZZLE, result.getType());
        verify(kvStore).set(startsWith("captcha:"), eq("100"), anyLong());
    }

    @Test
    void generate_clickWordType_savesAnswerToStorageAndReturnsPublicExtra() {
        CaptchaResponse clickWordResponse = new CaptchaResponse();
        clickWordResponse.setImage("data:image/png;base64,xxx");
        clickWordResponse.setTarget("云,山,月");
        clickWordResponse.setExtra("{\"width\":320,\"height\":180,\"tolerance\":24,\"points\":[{\"word\":\"云\",\"x\":80,\"y\":60},{\"word\":\"山\",\"x\":160,\"y\":110},{\"word\":\"月\",\"x\":250,\"y\":70}]}");
        when(clickWordCaptchaService.generate()).thenReturn(clickWordResponse);

        CaptchaResponse result = captchaService.generateClickWord();

        assertNotNull(result);
        assertEquals(CaptchaType.CLICK_WORD, result.getType());
        assertEquals("云,山,月", result.getTarget());
        assertTrue(result.getExtra().contains("\"pointCount\":3"));
        verify(kvStore).set(startsWith("captcha:"), contains("\"points\""), anyLong());
    }

    @Test
    void generate_behaviorType_savesChallengeToStorage() {
        CaptchaResponse behaviorResponse = new CaptchaResponse();
        behaviorResponse.setExpireTime(300L);
        behaviorResponse.setExtra("{\"mode\":\"silent\"}");
        when(behaviorCaptchaService.generate()).thenReturn(behaviorResponse);
        when(behaviorCaptchaService.createChallengeJson(anyString())).thenReturn("{\"key\":\"behavior-key\"}");

        CaptchaResponse result = captchaService.generateBehavior();

        assertNotNull(result);
        assertEquals(CaptchaType.BEHAVIOR, result.getType());
        assertEquals("{\"mode\":\"silent\"}", result.getExtra());
        verify(kvStore).set(startsWith("captcha:"), eq("{\"key\":\"behavior-key\"}"), eq(300L));
    }

    @Test
    void verify_withCorrectCode_returnsTrue() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("test-key");
        request.setType(CaptchaType.ARITHMETIC);
        request.setCode("123456");
        when(kvStore.get("captcha:test-key")).thenReturn("123456");

        boolean result = captchaService.verify(request);

        assertTrue(result);
        verify(kvStore).delete("captcha:test-key");
    }

    @Test
    void verify_withWrongCode_throwsBusinessError() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("test-key");
        request.setType(CaptchaType.ARITHMETIC);
        request.setCode("wrong");
        when(kvStore.get("captcha:test-key")).thenReturn("123456");

        assertThrows(BizException.class, () -> captchaService.verify(request));
        verify(kvStore, never()).delete(anyString());
    }

    @Test
    void verify_withExpiredKey_throwsBusinessError() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("expired-key");
        request.setType(CaptchaType.ARITHMETIC);
        request.setCode("123456");
        when(kvStore.get("captcha:expired-key")).thenReturn(null);

        assertThrows(BizException.class, () -> captchaService.verify(request));
    }

    @Test
    void verify_clickWordWithCorrectPoints_returnsTrue() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("click-key");
        request.setType(CaptchaType.CLICK_WORD);
        request.setPointJson("{\"points\":[{\"x\":82,\"y\":61},{\"x\":158,\"y\":108},{\"x\":252,\"y\":69}]}");
        when(kvStore.get("captcha:click-key")).thenReturn("{\"width\":320,\"height\":180,\"tolerance\":24,\"points\":[{\"word\":\"云\",\"x\":80,\"y\":60},{\"word\":\"山\",\"x\":160,\"y\":110},{\"word\":\"月\",\"x\":250,\"y\":70}]}");

        boolean result = captchaService.verify(request);

        assertTrue(result);
        verify(kvStore).delete("captcha:click-key");
    }

    @Test
    void verify_clickWordWithWrongPoints_throwsBusinessError() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("click-key");
        request.setType(CaptchaType.CLICK_WORD);
        request.setPointJson("{\"points\":[{\"x\":20,\"y\":20},{\"x\":158,\"y\":108},{\"x\":252,\"y\":69}]}");
        when(kvStore.get("captcha:click-key")).thenReturn("{\"width\":320,\"height\":180,\"tolerance\":24,\"points\":[{\"word\":\"云\",\"x\":80,\"y\":60},{\"word\":\"山\",\"x\":160,\"y\":110},{\"word\":\"月\",\"x\":250,\"y\":70}]}");

        assertThrows(BizException.class, () -> captchaService.verify(request));
        verify(kvStore, never()).delete("captcha:click-key");
    }

    @Test
    void verify_behaviorWithPassingScore_returnsTrue() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("behavior-key");
        request.setType(CaptchaType.BEHAVIOR);
        request.setPointJson("{\"behavior\":{\"mouseTrack\":[]}}");
        BehaviorCaptchaVerifyResponse behaviorResult = new BehaviorCaptchaVerifyResponse();
        behaviorResult.setPassed(true);
        when(kvStore.get("captcha:behavior-key")).thenReturn("{\"key\":\"behavior-key\"}");
        when(behaviorCaptchaService.verify(anyString(), anyString())).thenReturn(behaviorResult);

        boolean result = captchaService.verify(request);

        assertTrue(result);
        verify(kvStore).delete("captcha:behavior-key");
    }

    @Test
    void verifyBehavior_returnsScoreResult() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("behavior-key");
        request.setType(CaptchaType.BEHAVIOR);
        request.setPointJson("{\"behavior\":{\"mouseTrack\":[]}}");
        BehaviorCaptchaVerifyResponse behaviorResult = new BehaviorCaptchaVerifyResponse();
        behaviorResult.setScore(0.86D);
        behaviorResult.setPassed(true);
        when(kvStore.get("captcha:behavior-key")).thenReturn("{\"key\":\"behavior-key\"}");
        when(behaviorCaptchaService.verify(anyString(), anyString())).thenReturn(behaviorResult);

        BehaviorCaptchaVerifyResponse result = captchaService.verifyBehavior(request);

        assertTrue(result.isPassed());
        assertEquals("behavior-key", result.getKey());
        assertEquals(0.86D, result.getScore());
    }

    @Test
    void send_withSmsType_generatesCodeAndSaves() {
        when(smsProvider.send(anyString(), any(), any())).thenReturn(true);

        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setType(CaptchaType.SMS);
        request.setTarget("13800138000");
        request.setBusinessType("LOGIN");
        request.setExpireSeconds(300L);
        String key = captchaService.send(request);

        assertNotNull(key);
        assertTrue(key.startsWith("captcha:LOGIN:"));
        verify(kvStore).set(eq("captcha:LOGIN:13800138000"), anyString(), eq(300L));
        verify(smsProvider).send(eq("13800138000"), isNull(), anyString());
    }

    @Test
    void send_withEmailType_generatesCodeAndSaves() {
        when(emailProvider.send(anyString(), any(), any())).thenReturn(true);

        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setType(CaptchaType.EMAIL);
        request.setTarget("test@example.com");
        request.setBusinessType("REGISTER");
        request.setExpireSeconds(300L);
        String key = captchaService.send(request);

        assertNotNull(key);
        assertTrue(key.startsWith("captcha:REGISTER:"));
        verify(kvStore).set(eq("captcha:REGISTER:test@example.com"), anyString(), eq(300L));
    }

    @Test
    void sendSms_providerReturnsFalse_returnsNullAndDoesNotSave() {
        when(smsProvider.send(anyString(), any(), any())).thenReturn(false);

        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setType(CaptchaType.SMS);
        request.setTarget("13800138000");
        request.setBusinessType("LOGIN");
        String key = captchaService.send(request);

        assertNull(key);
        verify(kvStore, never()).set(anyString(), anyString(), anyLong());
    }

    @Test
    void sendEmail_providerReturnsFalse_returnsNullAndDoesNotSave() {
        when(emailProvider.send(anyString(), any(), any())).thenReturn(false);

        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setType(CaptchaType.EMAIL);
        request.setTarget("test@example.com");
        request.setBusinessType("REGISTER");
        String key = captchaService.send(request);

        assertNull(key);
        verify(kvStore, never()).set(anyString(), anyString(), anyLong());
    }

    @Test
    void send_withSmsTypeAndDefaultExpire_generatesCodeAndSaves() {
        when(smsProvider.send(anyString(), any(), any())).thenReturn(true);
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setType(CaptchaType.SMS);
        request.setTarget("13800138000");
        request.setBusinessType("LOGIN");

        String key = captchaService.send(request);

        assertNotNull(key);
        assertTrue(key.startsWith("captcha:LOGIN:"));
        verify(kvStore).set(eq("captcha:LOGIN:13800138000"), anyString(), anyLong());
        verify(smsProvider).send(eq("13800138000"), isNull(), anyString());
    }

    @Test
    void send_withEmailTypeAndDefaultExpire_generatesCodeAndSaves() {
        when(emailProvider.send(anyString(), any(), any())).thenReturn(true);
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setType(CaptchaType.EMAIL);
        request.setTarget("test@example.com");
        request.setBusinessType("REGISTER");

        String key = captchaService.send(request);

        assertNotNull(key);
        assertTrue(key.startsWith("captcha:REGISTER:"));
        verify(kvStore).set(eq("captcha:REGISTER:test@example.com"), anyString(), anyLong());
        verify(emailProvider).send(eq("test@example.com"), eq("验证码"), anyString());
    }

    @Test
    void getTypes_returnsAllTypes() {
        List<CaptchaType> types = captchaService.getTypes().getTypes();

        assertNotNull(types);
        assertEquals(6, types.size());
        assertTrue(types.contains(CaptchaType.ARITHMETIC));
        assertTrue(types.contains(CaptchaType.BLOCK_PUZZLE));
        assertTrue(types.contains(CaptchaType.CLICK_WORD));
        assertTrue(types.contains(CaptchaType.BEHAVIOR));
        assertTrue(types.contains(CaptchaType.SMS));
        assertTrue(types.contains(CaptchaType.EMAIL));
    }

    @Test
    void getTypes_returnsStorageType() {
        String storage = captchaService.getTypes().getCurrentStorage();

        assertNotNull(storage);
        assertFalse(storage.isEmpty());
    }
}
