package io.mango.infra.realtime.starter;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

final class RealtimeConditions {

    private static final String PREFIX = "mango.infra.realtime";

    private RealtimeConditions() {
    }

    static class Enabled extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return outcome(isEnabled(context.getEnvironment()), "realtime enabled");
        }
    }

    static class PublishEnabled extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return outcome(isPublishEnabled(context.getEnvironment()), "realtime publish enabled");
        }
    }

    static class ConnectionProtocolEnabled extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return outcome(isConnectionProtocolEnabled(context.getEnvironment()), "realtime connection protocol enabled");
        }
    }

    static class SseEnabled extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return outcome(isSseEnabled(context.getEnvironment()), "realtime SSE enabled");
        }
    }

    static class WebSocketEnabled extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return outcome(isWebSocketEnabled(context.getEnvironment()), "realtime WebSocket enabled");
        }
    }

    static class PollingEnabled extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return outcome(isPollingEnabled(context.getEnvironment()), "realtime polling enabled");
        }
    }

    static class RemoteEndpointEnabled extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return outcome(isRemoteEndpointEnabled(context.getEnvironment()), "realtime remote endpoint enabled");
        }
    }

    static class NegotiateEnabled extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return outcome(isNegotiateEnabled(context.getEnvironment()), "realtime negotiation enabled");
        }
    }

    private static boolean isEnabled(Environment environment) {
        return environment.getProperty(PREFIX + ".enabled", Boolean.class, true);
    }

    private static boolean isPublishEnabled(Environment environment) {
        return isEnabled(environment) && (isConnectionProtocolEnabled(environment) || isPollingEnabled(environment));
    }

    private static boolean isConnectionProtocolEnabled(Environment environment) {
        return isSseEnabled(environment) || isWebSocketEnabled(environment);
    }

    private static boolean isSseEnabled(Environment environment) {
        if (!isEnabled(environment)) {
            return false;
        }
        RealtimeMode mode = mode(environment);
        if (mode == RealtimeMode.SSE) {
            return true;
        }
        return mode == RealtimeMode.AUTO && environment.getProperty(PREFIX + ".sse.enabled", Boolean.class, true);
    }

    private static boolean isWebSocketEnabled(Environment environment) {
        if (!isEnabled(environment)) {
            return false;
        }
        RealtimeMode mode = mode(environment);
        if (mode == RealtimeMode.WEBSOCKET) {
            return true;
        }
        return mode == RealtimeMode.AUTO && environment.getProperty(PREFIX + ".websocket.enabled", Boolean.class, true);
    }

    private static boolean isPollingEnabled(Environment environment) {
        if (!isEnabled(environment)) {
            return false;
        }
        RealtimeMode mode = mode(environment);
        if (mode == RealtimeMode.POLLING) {
            return true;
        }
        return mode == RealtimeMode.AUTO && environment.getProperty(PREFIX + ".polling.enabled", Boolean.class, true);
    }

    private static boolean isRemoteEndpointEnabled(Environment environment) {
        return isPublishEnabled(environment)
                && environment.getProperty(PREFIX + ".remote.endpoint-enabled", Boolean.class, true);
    }

    private static boolean isNegotiateEnabled(Environment environment) {
        return isEnabled(environment)
                && isPublishEnabled(environment)
                && environment.getProperty(PREFIX + ".negotiate.enabled", Boolean.class, true);
    }

    private static RealtimeMode mode(Environment environment) {
        String value = environment.getProperty(PREFIX + ".mode", RealtimeMode.AUTO.name());
        return RealtimeMode.valueOf(value.trim().toUpperCase());
    }

    private static ConditionOutcome outcome(boolean match, String message) {
        return match ? ConditionOutcome.match(message) : ConditionOutcome.noMatch(message);
    }
}
