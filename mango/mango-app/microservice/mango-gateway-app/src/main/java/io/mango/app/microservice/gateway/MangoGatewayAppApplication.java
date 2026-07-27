package io.mango.app.microservice.gateway;

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango 网关部署入口。
 *
 * @author hardy
 */
@SpringBootApplication
public class MangoGatewayAppApplication {

    public static void main(String[] args) {
        MangoApplication.run(MangoGatewayAppApplication.class, args);
    }
}
