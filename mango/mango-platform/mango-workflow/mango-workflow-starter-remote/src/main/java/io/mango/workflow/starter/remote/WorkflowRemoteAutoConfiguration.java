package io.mango.workflow.starter.remote;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 工作流远程调用自动配置。
 */
@AutoConfiguration
@EnableFeignClients(basePackageClasses = WorkflowProcessFeignClient.class)
public class WorkflowRemoteAutoConfiguration {
}
