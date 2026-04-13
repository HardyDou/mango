package io.mango.org.starter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Org module auto configuration
 *
 * @author Mango
 */
@Configuration
@ComponentScan({
        "io.mango.org.core.controller",
        "io.mango.org.core.service"
})
public class MangoOrgAutoConfiguration {
}
