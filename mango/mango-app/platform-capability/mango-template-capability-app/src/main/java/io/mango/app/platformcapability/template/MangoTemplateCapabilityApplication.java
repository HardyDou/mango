package io.mango.app.platformcapability.template;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Template 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoTemplateCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoTemplateCapabilityApplication.class, args);
    }
}
