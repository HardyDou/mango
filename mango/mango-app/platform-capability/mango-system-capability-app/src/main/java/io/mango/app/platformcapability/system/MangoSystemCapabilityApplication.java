package io.mango.app.platformcapability.system;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango System 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoSystemCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoSystemCapabilityApplication.class, args);
    }
}
