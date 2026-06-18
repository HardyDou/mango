package io.mango.notice.starter.remote;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@EnableFeignClients(basePackageClasses = NoticeFeignClient.class)
@ComponentScan(basePackageClasses = NoticeSendEventListener.class)
public class NoticeRemoteAutoConfiguration {
}
