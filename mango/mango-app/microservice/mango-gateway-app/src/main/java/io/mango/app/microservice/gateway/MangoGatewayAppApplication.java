package io.mango.app.microservice.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango 网关部署入口。
 *
 * @author hardy
 */
@SpringBootApplication
public class MangoGatewayAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MangoGatewayAppApplication.class, args);
    }
}
