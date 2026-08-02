package {{basePackage}};

import io.mango.infra.bootstrap.starter.MangoApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class {{projectPascal}}Application {

    public static void main(String[] args) {
        MangoApplication.run({{projectPascal}}Application.class, args);
    }
}
