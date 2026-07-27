package io.mango.app.platformcapability.job;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Job 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoJobCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoJobCapabilityApplication.class, args);
    }
}
