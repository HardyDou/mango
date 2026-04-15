package io.mango.org.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackageClasses = PostFeignClient.class)
public class PostRemoteAutoConfiguration {
}
