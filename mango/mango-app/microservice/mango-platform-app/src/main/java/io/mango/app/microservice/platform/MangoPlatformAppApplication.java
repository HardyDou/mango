package io.mango.app.microservice.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango 平台支撑能力部署入口。
 *
 * @author hardy
 */
@SpringBootApplication
public class MangoPlatformAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MangoPlatformAppApplication.class, args);
    }
}
