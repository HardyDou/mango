package io.mango.app.platformcapability.authorization;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Authorization 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoAuthorizationCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoAuthorizationCapabilityApplication.class, args);
    }
}
