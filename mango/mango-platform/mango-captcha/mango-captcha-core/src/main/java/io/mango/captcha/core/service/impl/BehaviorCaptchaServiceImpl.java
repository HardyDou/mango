package io.mango.captcha.core.service.impl;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResult;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.core.service.BehaviorCaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * 无感行为验证规则评分实现。
 */
@Service
@RequiredArgsConstructor
public class BehaviorCaptchaServiceImpl implements BehaviorCaptchaService {

    private static final double INITIAL_SCORE = 1.0D;
    private static final double PASS_SCORE = 0.7D;
    private static final double SECONDARY_SCORE = 0.4D;
    private static final long MIN_OPERATE_TIME_MILLIS = 400L;
    private static final int MIN_MOUSE_POINTS = 8;
    private static final int MIN_DEVICE_FIELDS = 4;
    private static final double HIGH_STRAIGHT_RATE = 0.92D;
    private static final double LOW_SPEED_VARIANCE = 0.0008D;
    private static final double SCORE_MIN = 0.0D;
    private static final double SCORE_MAX = 1.0D;
    private static final int SCORE_SCALE = 100;
    private static final String ACTION_ALLOW = "ALLOW";
    private static final String ACTION_SECONDARY_VERIFY = "SECONDARY_VERIFY";
    private static final String ACTION_DENY = "DENY";
    private static final String RISK_LOW = "LOW";
    private static final String RISK_MEDIUM = "MEDIUM";
    private static final String RISK_HIGH = "HIGH";

    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${mango.captcha.behavior.ttl:300}")
    private long ttl = 300L;

    public BehaviorCaptchaServiceImpl(ObjectMapper objectMapper) {
        this(objectMapper, Clock.systemUTC());
    }

    @Override
    public CaptchaResponse generate() {
        CaptchaResponse response = new CaptchaResponse();
        response.setType(CaptchaType.BEHAVIOR);
        response.setExpireTime(ttl);
        response.setExtra(createPublicExtra());
        return response;
    }

    @Override
    public BehaviorCaptchaVerifyResult verify(String challengeJson, String payloadJson) {
        try {
            JsonNode challenge = objectMapper.readTree(challengeJson);
            JsonNode payload = objectMapper.readTree(payloadJson);
            return score(challenge, payload);
        } catch (JsonProcessingException ex) {
            return failed("PAYLOAD_INVALID");
        }
    }

    @Override
    public String createChallengeJson(String key) {
        var challenge = objectMapper.createObjectNode();
        challenge.put("key", key);
        challenge.put("nonce", UUID.randomUUID().toString(true));
        challenge.put("issuedAt", clock.millis());
        challenge.put("expiresAt", clock.millis() + ttl * 1000L);
        try {
            return objectMapper.writeValueAsString(challenge);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Create behavior captcha challenge failed", ex);
        }
    }

    private BehaviorCaptchaVerifyResult score(JsonNode challenge, JsonNode payload) {
        double score = INITIAL_SCORE;
        List<String> reasons = new ArrayList<>();

        long issuedAt = challenge.path("issuedAt").asLong(0L);
        long expiresAt = challenge.path("expiresAt").asLong(0L);
        if (issuedAt <= 0L || expiresAt < clock.millis()) {
            return failed("CHALLENGE_EXPIRED");
        }

        JsonNode behavior = payload.path("behavior");
        long startTime = behavior.path("startTime").asLong(0L);
        long endTime = payload.path("ts").asLong(0L);
        long operateTime = endTime - startTime;
        if (operateTime > 0L && operateTime < MIN_OPERATE_TIME_MILLIS) {
            return result(0.1D, List.of("OPERATE_TOO_FAST"));
        }
        if (startTime < issuedAt - 1000L) {
            score -= 0.15D;
            reasons.add("TIME_SEQUENCE_INVALID");
        }

        JsonNode mouseTrack = behavior.path("mouseTrack");
        JsonNode clickList = behavior.path("clickList");
        if (!mouseTrack.isArray() || mouseTrack.size() < MIN_MOUSE_POINTS) {
            score -= 0.2D;
            reasons.add("MOUSE_TRACK_TOO_SHORT");
        } else {
            double straightRate = calculateStraightRate(mouseTrack);
            if (straightRate > HIGH_STRAIGHT_RATE) {
                score -= 0.3D;
                reasons.add("TRACK_TOO_STRAIGHT");
            }
            double speedVariance = calculateSpeedVariance(mouseTrack);
            if (speedVariance < LOW_SPEED_VARIANCE) {
                score -= 0.25D;
                reasons.add("SPEED_TOO_STABLE");
            }
        }

        if (clickList.isArray() && clickList.size() > 0 && (!mouseTrack.isArray() || mouseTrack.isEmpty())) {
            score -= 0.2D;
            reasons.add("CLICK_WITHOUT_MOVE");
        }

        if (countDeviceFields(payload.path("device")) < MIN_DEVICE_FIELDS) {
            score -= 0.15D;
            reasons.add("DEVICE_FINGER_INCOMPLETE");
        }

        return result(score, reasons);
    }

    private double calculateStraightRate(JsonNode points) {
        if (points.size() < 3) {
            return SCORE_MAX;
        }
        JsonNode first = points.get(0);
        JsonNode last = points.get(points.size() - 1);
        double directDistance = distance(first, last);
        double pathDistance = 0.0D;
        for (int i = 1; i < points.size(); i++) {
            pathDistance += distance(points.get(i - 1), points.get(i));
        }
        if (pathDistance <= 0.0D) {
            return SCORE_MAX;
        }
        return directDistance / pathDistance;
    }

    private double calculateSpeedVariance(JsonNode points) {
        List<Double> speeds = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            JsonNode previous = points.get(i - 1);
            JsonNode current = points.get(i);
            long timeGap = current.path("t").asLong() - previous.path("t").asLong();
            if (timeGap > 0L) {
                speeds.add(distance(previous, current) / timeGap);
            }
        }
        if (speeds.size() < 2) {
            return 0.0D;
        }
        double average = speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
        return speeds.stream()
                .mapToDouble(speed -> Math.pow(speed - average, 2))
                .average()
                .orElse(0.0D);
    }

    private double distance(JsonNode first, JsonNode second) {
        double xGap = first.path("x").asDouble() - second.path("x").asDouble();
        double yGap = first.path("y").asDouble() - second.path("y").asDouble();
        return Math.hypot(xGap, yGap);
    }

    private int countDeviceFields(JsonNode device) {
        int count = 0;
        count += hasText(device.path("ua")) ? 1 : 0;
        count += hasText(device.path("screen")) ? 1 : 0;
        count += hasText(device.path("timezone")) ? 1 : 0;
        count += hasText(device.path("language")) ? 1 : 0;
        count += hasText(device.path("finger")) ? 1 : 0;
        return count;
    }

    private boolean hasText(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank();
    }

    private BehaviorCaptchaVerifyResult failed(String reason) {
        return result(0.0D, List.of(reason));
    }

    private BehaviorCaptchaVerifyResult result(double rawScore, List<String> reasons) {
        double score = Math.max(SCORE_MIN, Math.min(SCORE_MAX, rawScore));
        score = Math.round(score * SCORE_SCALE) / (double) SCORE_SCALE;

        BehaviorCaptchaVerifyResult result = new BehaviorCaptchaVerifyResult();
        result.setScore(score);
        result.setPassed(score >= PASS_SCORE);
        result.setRiskLevel(score >= PASS_SCORE ? RISK_LOW : score >= SECONDARY_SCORE ? RISK_MEDIUM : RISK_HIGH);
        result.setSuggestAction(score >= PASS_SCORE ? ACTION_ALLOW : score >= SECONDARY_SCORE ? ACTION_SECONDARY_VERIFY : ACTION_DENY);
        result.setReason(reasons.isEmpty() ? "OK" : String.join(",", reasons));
        return result;
    }

    private String createPublicExtra() {
        var extra = objectMapper.createObjectNode();
        extra.put("mode", "silent");
        extra.put("passScore", PASS_SCORE);
        extra.put("secondaryScore", SECONDARY_SCORE);
        try {
            return objectMapper.writeValueAsString(extra);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Create behavior captcha extra failed", ex);
        }
    }
}
