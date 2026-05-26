package io.mango.notice.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = "io.mango.notice")
@MapperScan("io.mango.notice.core.mapper")
public class NoticeAutoConfiguration {
}
