package io.mango.app.platformcapability.domain;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Domain 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoDomainCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoDomainCapabilityApplication.class, args);
    }
}
