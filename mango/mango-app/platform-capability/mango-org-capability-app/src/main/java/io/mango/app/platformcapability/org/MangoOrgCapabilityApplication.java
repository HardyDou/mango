package io.mango.app.platformcapability.org;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Org 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoOrgCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoOrgCapabilityApplication.class, args);
    }
}
