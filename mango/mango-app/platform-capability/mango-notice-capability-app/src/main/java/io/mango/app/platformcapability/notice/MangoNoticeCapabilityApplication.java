package io.mango.app.platformcapability.notice;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Notice 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoNoticeCapabilityApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoNoticeCapabilityApplication.class, args);
    }
}
