package io.mango.infra.realtime.api.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeInboundMessageListenerTest {

    @Test
    void isRuntimeMethodAnnotationMatchingScannerContract() throws Exception {
        Target target = RealtimeInboundMessageListener.class.getAnnotation(Target.class);
        Retention retention = RealtimeInboundMessageListener.class.getAnnotation(Retention.class);

        assertThat(target.value()).containsExactly(ElementType.METHOD);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(RealtimeInboundMessageListener.class.getDeclaredMethod("types").getDefaultValue()).isNull();
        assertThat(RealtimeInboundMessageListener.class.getDeclaredMethod("order").getDefaultValue()).isEqualTo(0);
    }
}
