package io.mango.app.platformcapability.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango Job 平台能力独立启动入口。
 */
@SpringBootApplication
public class MangoJobCapabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(MangoJobCapabilityApplication.class, args);
    }
}
