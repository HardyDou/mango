package io.mango.notice.starter.remote;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@AutoConfiguration
@EnableFeignClients(basePackageClasses = NoticeFeignClient.class)
public class NoticeRemoteAutoConfiguration {
}
