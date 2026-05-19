package io.mango.captcha.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResult;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.core.service.impl.BehaviorCaptchaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BehaviorCaptchaServiceTest {

    private BehaviorCaptchaService behaviorCaptchaService;

    @BeforeEach
    void setUp() {
        behaviorCaptchaService = new BehaviorCaptchaServiceImpl(new ObjectMapper());
    }

    @Test
    void generate_returnsBehaviorCaptcha() {
        CaptchaResponse response = behaviorCaptchaService.generate();

        assertEquals(CaptchaType.BEHAVIOR, response.getType());
        assertNotNull(response.getExtra());
        assertTrue(response.getExpireTime() > 0L);
    }

    @Test
    void verify_withHumanLikeTrack_returnsPassingScore() {
        String challenge = behaviorCaptchaService.createChallengeJson("behavior-key");
        String payload = """
                {
                  "behavior": {
                    "startTime": 1893456000100,
                    "mouseTrack": [
                      {"x": 10, "y": 10, "t": 1893456000100},
                      {"x": 24, "y": 34, "t": 1893456000200},
                      {"x": 42, "y": 18, "t": 1893456000340},
                      {"x": 55, "y": 47, "t": 1893456000490},
                      {"x": 78, "y": 42, "t": 1893456000610},
                      {"x": 88, "y": 73, "t": 1893456000790},
                      {"x": 112, "y": 64, "t": 1893456000960},
                      {"x": 126, "y": 96, "t": 1893456001190},
                      {"x": 152, "y": 88, "t": 1893456001410}
                    ],
                    "clickList": [{"x": 140, "y": 94, "t": 1893456001600}],
                    "keyList": [{"key": "a", "t": 1893456000300}]
                  },
                  "device": {
                    "ua": "Mozilla",
                    "screen": "1440-900",
                    "timezone": "Asia/Shanghai",
                    "language": "zh-CN",
                    "finger": "data:image/png;base64,xxx"
                  },
                  "ts": 1893456001700
                }
                """;

        BehaviorCaptchaVerifyResult result = behaviorCaptchaService.verify(challenge, payload);

        assertTrue(result.isPassed());
        assertEquals("LOW", result.getRiskLevel());
        assertEquals("ALLOW", result.getSuggestAction());
    }

    @Test
    void verify_withTooFastOperation_returnsLowScore() {
        String challenge = behaviorCaptchaService.createChallengeJson("behavior-key");
        String payload = """
                {
                  "behavior": {
                    "startTime": 1893456000100,
                    "mouseTrack": [{"x": 10, "y": 10, "t": 1893456000100}],
                    "clickList": [{"x": 10, "y": 10, "t": 1893456000200}]
                  },
                  "device": {},
                  "ts": 1893456000200
                }
                """;

        BehaviorCaptchaVerifyResult result = behaviorCaptchaService.verify(challenge, payload);

        assertFalse(result.isPassed());
        assertEquals(0.1D, result.getScore());
        assertEquals("DENY", result.getSuggestAction());
    }
}
