package io.mango.app.platformcapability.captcha;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Captcha 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoCaptchaCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoCaptchaCapabilityApplication.class, args);
    }
}
