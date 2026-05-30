package {{basePackage}}.app.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {{projectPascal}} 单体部署入口。
 */
@SpringBootApplication(scanBasePackages = "{{basePackage}}")
public class {{projectPascal}}MonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run({{projectPascal}}MonolithApplication.class, args);
    }
}
