package io.mango.captcha.core.generator;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResponse;
import io.mango.captcha.api.dto.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * 无感行为验证规则评分实现。
 */
@RequiredArgsConstructor
public class DefaultBehaviorCaptchaEngine implements BehaviorCaptchaEngine {

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
    private static final int SQUARE_EXPONENT = 2;
    private static final long DEFAULT_TTL_SECONDS = 300L;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long CLOCK_SKEW_MILLIS = 1000L;
    private static final int MIN_DISTANCE_POINTS = 3;
    private static final int MIN_SPEED_SAMPLES = 2;
    private static final double FAST_OPERATION_SCORE = 0.1D;
    private static final double TIME_SEQUENCE_PENALTY = 0.15D;
    private static final double SHORT_TRACK_PENALTY = 0.2D;
    private static final double STRAIGHT_TRACK_PENALTY = 0.3D;
    private static final double STABLE_SPEED_PENALTY = 0.25D;
    private static final double CLICK_WITHOUT_MOVE_PENALTY = 0.2D;
    private static final double DEVICE_FINGERPRINT_PENALTY = 0.15D;
    private static final String ACTION_ALLOW = "ALLOW";
    private static final String ACTION_SECONDARY_VERIFY = "SECONDARY_VERIFY";
    private static final String ACTION_DENY = "DENY";
    private static final String RISK_LOW = "LOW";
    private static final String RISK_MEDIUM = "MEDIUM";
    private static final String RISK_HIGH = "HIGH";

    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${mango.captcha.behavior.ttl:300}")
    private long ttl = DEFAULT_TTL_SECONDS;

    public DefaultBehaviorCaptchaEngine(ObjectMapper objectMapper) {
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
    public BehaviorCaptchaVerifyResponse verify(String challengeJson, String payloadJson) {
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
        challenge.put("expiresAt", clock.millis() + ttl * MILLIS_PER_SECOND);
        try {
            return objectMapper.writeValueAsString(challenge);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Create behavior captcha challenge failed", ex);
        }
    }

    private BehaviorCaptchaVerifyResponse score(JsonNode challenge, JsonNode payload) {
        BehaviorCaptchaVerifyResponse terminalResult = terminalResult(challenge, payload);
        if (terminalResult != null) {
            return terminalResult;
        }
        List<String> reasons = new ArrayList<>();
        double score = INITIAL_SCORE;
        score = applyTimeSequencePenalty(score, challenge, payload, reasons);
        JsonNode behavior = payload.path("behavior");
        JsonNode mouseTrack = behavior.path("mouseTrack");
        score = applyMouseTrackPenalty(score, mouseTrack, reasons);
        score = applyClickPenalty(score, behavior.path("clickList"), mouseTrack, reasons);
        score = applyDevicePenalty(score, payload.path("device"), reasons);
        return result(score, reasons);
    }

    private BehaviorCaptchaVerifyResponse terminalResult(JsonNode challenge, JsonNode payload) {
        long issuedAt = challenge.path("issuedAt").asLong(0L);
        long expiresAt = challenge.path("expiresAt").asLong(0L);
        if (issuedAt <= 0L || expiresAt < clock.millis()) {
            return failed("CHALLENGE_EXPIRED");
        }
        JsonNode behavior = payload.path("behavior");
        long operateTime = payload.path("ts").asLong(0L) - behavior.path("startTime").asLong(0L);
        if (operateTime > 0L && operateTime < MIN_OPERATE_TIME_MILLIS) {
            return result(FAST_OPERATION_SCORE, List.of("OPERATE_TOO_FAST"));
        }
        return null;
    }

    private double applyTimeSequencePenalty(double score, JsonNode challenge, JsonNode payload,
                                            List<String> reasons) {
        long issuedAt = challenge.path("issuedAt").asLong(0L);
        long startTime = payload.path("behavior").path("startTime").asLong(0L);
        if (startTime < issuedAt - CLOCK_SKEW_MILLIS) {
            reasons.add("TIME_SEQUENCE_INVALID");
            return score - TIME_SEQUENCE_PENALTY;
        }
        return score;
    }

    private double applyMouseTrackPenalty(double score, JsonNode mouseTrack, List<String> reasons) {
        if (!mouseTrack.isArray() || mouseTrack.size() < MIN_MOUSE_POINTS) {
            reasons.add("MOUSE_TRACK_TOO_SHORT");
            return score - SHORT_TRACK_PENALTY;
        }
        double adjusted = score;
        if (calculateStraightRate(mouseTrack) > HIGH_STRAIGHT_RATE) {
            adjusted -= STRAIGHT_TRACK_PENALTY;
            reasons.add("TRACK_TOO_STRAIGHT");
        }
        if (calculateSpeedVariance(mouseTrack) < LOW_SPEED_VARIANCE) {
            adjusted -= STABLE_SPEED_PENALTY;
            reasons.add("SPEED_TOO_STABLE");
        }
        return adjusted;
    }

    private double applyClickPenalty(double score, JsonNode clickList, JsonNode mouseTrack, List<String> reasons) {
        if (hasClickWithoutMovement(clickList, mouseTrack)) {
            reasons.add("CLICK_WITHOUT_MOVE");
            return score - CLICK_WITHOUT_MOVE_PENALTY;
        }
        return score;
    }

    private boolean hasClickWithoutMovement(JsonNode clickList, JsonNode mouseTrack) {
        return clickList.isArray() && !clickList.isEmpty() && hasNoMovement(mouseTrack);
    }

    private boolean hasNoMovement(JsonNode mouseTrack) {
        return !mouseTrack.isArray() || mouseTrack.isEmpty();
    }

    private double applyDevicePenalty(double score, JsonNode device, List<String> reasons) {
        if (countDeviceFields(device) < MIN_DEVICE_FIELDS) {
            reasons.add("DEVICE_FINGER_INCOMPLETE");
            return score - DEVICE_FINGERPRINT_PENALTY;
        }
        return score;
    }

    private double calculateStraightRate(JsonNode points) {
        if (points.size() < MIN_DISTANCE_POINTS) {
            return SCORE_MAX;
        }
        JsonNode first = points.get(0);
        JsonNode last = points.get(points.size() - 1);
        double directDistance = distance(first, last);
        double pathDistance = SCORE_MIN;
        for (int i = 1; i < points.size(); i++) {
            pathDistance += distance(points.get(i - 1), points.get(i));
        }
        if (pathDistance <= SCORE_MIN) {
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
        if (speeds.size() < MIN_SPEED_SAMPLES) {
            return SCORE_MIN;
        }
        double average = speeds.stream().mapToDouble(Double::doubleValue).average().orElse(SCORE_MIN);
        return speeds.stream()
                .mapToDouble(speed -> Math.pow(speed - average, SQUARE_EXPONENT))
                .average()
                .orElse(SCORE_MIN);
    }

    private double distance(JsonNode first, JsonNode second) {
        double xGap = first.path("x").asDouble() - second.path("x").asDouble();
        double yGap = first.path("y").asDouble() - second.path("y").asDouble();
        return Math.hypot(xGap, yGap);
    }

    private int countDeviceFields(JsonNode device) {
        int count = 0;
        count += present(device.path("ua"));
        count += present(device.path("screen"));
        count += present(device.path("timezone"));
        count += present(device.path("language"));
        count += present(device.path("finger"));
        return count;
    }

    private int present(JsonNode node) {
        if (hasText(node)) {
            return 1;
        }
        return 0;
    }

    private boolean hasText(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank();
    }

    private BehaviorCaptchaVerifyResponse failed(String reason) {
        return result(SCORE_MIN, List.of(reason));
    }

    private BehaviorCaptchaVerifyResponse result(double rawScore, List<String> reasons) {
        double score = Math.max(SCORE_MIN, Math.min(SCORE_MAX, rawScore));
        score = Math.round(score * SCORE_SCALE) / (double) SCORE_SCALE;

        BehaviorCaptchaVerifyResponse result = new BehaviorCaptchaVerifyResponse();
        result.setScore(score);
        result.setPassed(score >= PASS_SCORE);
        applyDecision(result, score);
        if (reasons.isEmpty()) {
            result.setReason("OK");
        } else {
            result.setReason(String.join(",", reasons));
        }
        return result;
    }

    private void applyDecision(BehaviorCaptchaVerifyResponse result, double score) {
        if (score >= PASS_SCORE) {
            result.setRiskLevel(RISK_LOW);
            result.setSuggestAction(ACTION_ALLOW);
        } else if (score >= SECONDARY_SCORE) {
            result.setRiskLevel(RISK_MEDIUM);
            result.setSuggestAction(ACTION_SECONDARY_VERIFY);
        } else {
            result.setRiskLevel(RISK_HIGH);
            result.setSuggestAction(ACTION_DENY);
        }
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
