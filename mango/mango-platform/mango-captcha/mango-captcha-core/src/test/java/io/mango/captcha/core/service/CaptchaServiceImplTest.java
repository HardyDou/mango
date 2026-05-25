package io.mango.captcha.core.service;

import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResult;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.spi.EmailProvider;
import io.mango.captcha.api.spi.SmsProvider;
import io.mango.captcha.core.service.impl.CaptchaServiceImpl;
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
class CaptchaServiceImplTest {

    @Mock
    private io.mango.infra.kv.api.IKvStore kvStore;

    @Mock
    private ArithmeticCaptchaService arithmeticCaptchaService;

    @Mock
    private BlockPuzzleCaptchaService blockPuzzleCaptchaService;

    @Mock
    private ClickWordCaptchaService clickWordCaptchaService;

    @Mock
    private BehaviorCaptchaService behaviorCaptchaService;

    @Mock
    private SmsProvider smsProvider;

    @Mock
    private EmailProvider emailProvider;

    private CaptchaServiceImpl captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaServiceImpl(
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

        CaptchaResponse result = captchaService.generate(CaptchaType.ARITHMETIC, null);

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

        CaptchaResponse result = captchaService.generate(CaptchaType.BLOCK_PUZZLE, null);

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

        CaptchaResponse result = captchaService.generate(CaptchaType.CLICK_WORD, null);

        assertNotNull(result);
        assertEquals(CaptchaType.CLICK_WORD, result.getType());
        assertEquals("云,山,月", result.getTarget());
        assertTrue(result.getExtra().contains("\"pointCount\":3"));
        verify(kvStore).set(startsWith("captcha:"), contains("\"points\""), anyLong());
    }

    @Test
    void generate_smsType_setsTarget() {
        CaptchaResponse result = captchaService.generate(CaptchaType.SMS, "13800138000");

        assertNotNull(result);
        assertEquals(CaptchaType.SMS, result.getType());
        assertEquals("13800138000", result.getTarget());
    }

    @Test
    void generate_behaviorType_savesChallengeToStorage() {
        CaptchaResponse behaviorResponse = new CaptchaResponse();
        behaviorResponse.setExpireTime(300L);
        behaviorResponse.setExtra("{\"mode\":\"silent\"}");
        when(behaviorCaptchaService.generate()).thenReturn(behaviorResponse);
        when(behaviorCaptchaService.createChallengeJson(anyString())).thenReturn("{\"key\":\"behavior-key\"}");

        CaptchaResponse result = captchaService.generate(CaptchaType.BEHAVIOR, null);

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
    void verify_withWrongCode_returnsFalse() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("test-key");
        request.setType(CaptchaType.ARITHMETIC);
        request.setCode("wrong");
        when(kvStore.get("captcha:test-key")).thenReturn("123456");

        boolean result = captchaService.verify(request);

        assertFalse(result);
        verify(kvStore, never()).delete(anyString());
    }

    @Test
    void verify_withExpiredKey_returnsFalse() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("expired-key");
        request.setType(CaptchaType.ARITHMETIC);
        request.setCode("123456");
        when(kvStore.get("captcha:expired-key")).thenReturn(null);

        boolean result = captchaService.verify(request);

        assertFalse(result);
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
    void verify_clickWordWithWrongPoints_returnsFalse() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("click-key");
        request.setType(CaptchaType.CLICK_WORD);
        request.setPointJson("{\"points\":[{\"x\":20,\"y\":20},{\"x\":158,\"y\":108},{\"x\":252,\"y\":69}]}");
        when(kvStore.get("captcha:click-key")).thenReturn("{\"width\":320,\"height\":180,\"tolerance\":24,\"points\":[{\"word\":\"云\",\"x\":80,\"y\":60},{\"word\":\"山\",\"x\":160,\"y\":110},{\"word\":\"月\",\"x\":250,\"y\":70}]}");

        boolean result = captchaService.verify(request);

        assertFalse(result);
        verify(kvStore, never()).delete("captcha:click-key");
    }

    @Test
    void verify_behaviorWithPassingScore_returnsTrue() {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey("behavior-key");
        request.setType(CaptchaType.BEHAVIOR);
        request.setPointJson("{\"behavior\":{\"mouseTrack\":[]}}");
        BehaviorCaptchaVerifyResult behaviorResult = new BehaviorCaptchaVerifyResult();
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
        BehaviorCaptchaVerifyResult behaviorResult = new BehaviorCaptchaVerifyResult();
        behaviorResult.setScore(0.86D);
        behaviorResult.setPassed(true);
        when(kvStore.get("captcha:behavior-key")).thenReturn("{\"key\":\"behavior-key\"}");
        when(behaviorCaptchaService.verify(anyString(), anyString())).thenReturn(behaviorResult);

        BehaviorCaptchaVerifyResult result = captchaService.verifyBehavior(request);

        assertTrue(result.isPassed());
        assertEquals("behavior-key", result.getKey());
        assertEquals(0.86D, result.getScore());
    }

    @Test
    void sendSms_generatesCodeAndSaves() {
        when(smsProvider.send(anyString(), any(), any())).thenReturn(true);

        String key = captchaService.sendSms("13800138000", "LOGIN", 300);

        assertNotNull(key);
        assertTrue(key.startsWith("captcha:sms:LOGIN:"));
        verify(kvStore).set(eq("captcha:sms:LOGIN:13800138000"), anyString(), eq(300L));
        verify(smsProvider).send(eq("13800138000"), isNull(), anyString());
    }

    @Test
    void sendEmail_generatesCodeAndSaves() {
        when(emailProvider.send(anyString(), any(), any())).thenReturn(true);

        String key = captchaService.sendEmail("test@example.com", "REGISTER", 300);

        assertNotNull(key);
        assertTrue(key.startsWith("captcha:email:REGISTER:"));
        verify(kvStore).set(eq("captcha:email:REGISTER:test@example.com"), anyString(), eq(300L));
    }

    @Test
    void sendSms_providerReturnsFalse_returnsNullAndDoesNotSave() {
        when(smsProvider.send(anyString(), any(), any())).thenReturn(false);

        String key = captchaService.sendSms("13800138000", "LOGIN", 300);

        assertNull(key);
        verify(kvStore, never()).set(anyString(), anyString(), anyLong());
    }

    @Test
    void sendEmail_providerReturnsFalse_returnsNullAndDoesNotSave() {
        when(emailProvider.send(anyString(), any(), any())).thenReturn(false);

        String key = captchaService.sendEmail("test@example.com", "REGISTER", 300);

        assertNull(key);
        verify(kvStore, never()).set(anyString(), anyString(), anyLong());
    }

    @Test
    void send_withSmsType_generatesCodeAndSaves() {
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
    void send_withEmailType_generatesCodeAndSaves() {
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
    void getSupportedTypes_returnsAllTypes() {
        List<CaptchaType> types = captchaService.getSupportedTypes();

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
    void getCurrentStorage_returnsStorageType() {
        String storage = captchaService.getCurrentStorage();

        assertNotNull(storage);
        assertFalse(storage.isEmpty());
    }
}
