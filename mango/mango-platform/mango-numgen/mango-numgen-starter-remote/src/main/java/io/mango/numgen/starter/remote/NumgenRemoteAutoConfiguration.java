package io.mango.numgen.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "io.mango.numgen.starter.remote")
public class NumgenRemoteAutoConfiguration {
}
