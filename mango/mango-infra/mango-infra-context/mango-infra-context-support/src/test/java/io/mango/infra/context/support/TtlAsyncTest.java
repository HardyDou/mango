package io.mango.infra.context.support;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Async;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TtlAsyncTest {

    @Test
    void annotation_targetsTheMangoContextExecutor() {
        Async async = AnnotatedElementUtils.findMergedAnnotation(TtlAsync.class, Async.class);

        assertEquals(MangoContextExecutors.CONTEXT, async.value());
        assertEquals("mangoContextExecutor", MangoContextExecutors.CONTEXT);
    }
}
