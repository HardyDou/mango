package io.mango.captcha.starter.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaInitializationBoundaryTest {

    @Test
    void captchaUsesKvStoreAndDoesNotPublishUnusedDatabaseMigration() {
        ClassLoader classLoader = CaptchaInitializationBoundaryTest.class.getClassLoader();

        assertThat(classLoader.getResource("db/migration/captcha/V1__init_captcha.sql")).isNull();
    }
}
