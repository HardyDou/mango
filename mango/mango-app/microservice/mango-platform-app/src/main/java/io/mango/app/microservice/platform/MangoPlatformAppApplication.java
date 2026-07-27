package io.mango.app.microservice.platform;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango 平台支撑能力部署入口。
 *
 * @author hardy
 */
@SpringBootApplication
public class MangoPlatformAppApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoPlatformAppApplication.class, args);
    }
}
