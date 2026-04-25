package io.mango.app.microservice.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mango 业务能力部署入口。
 *
 * @author hardy
 */
@SpringBootApplication
public class MangoBusinessAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MangoBusinessAppApplication.class, args);
    }
}
