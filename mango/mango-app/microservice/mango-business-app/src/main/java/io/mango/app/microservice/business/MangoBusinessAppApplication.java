package io.mango.app.microservice.business;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango 业务能力部署入口。
 *
 * @author hardy
 */
@SpringBootApplication
public class MangoBusinessAppApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoBusinessAppApplication.class, args);
    }
}
