package io.mango.system.starter;

import io.mango.infra.iplocation.api.IpLocationResolver;
import io.mango.infra.persistence.web.starter.excel.ExcelDictionaryProvider;
import io.mango.system.core.aspect.OperationLogAspect;
import io.mango.system.core.middleware.TenantFilter;
import io.mango.system.core.service.IDictService;
import io.mango.system.core.service.ISysLogService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
    "io.mango.system.core.mapper",
    "io.mango.area.core.mapper",
    "io.mango.i18n.core.mapper"
})
@ComponentScan({
    "io.mango.system.core",
    "io.mango.system.starter.controller",
    "io.mango.system.starter.resource",
    "io.mango.area.core",
    "io.mango.i18n.core",
    "io.mango.i18n.starter.controller"
})
public class SystemAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "mango.tenant", name = "enabled", havingValue = "true")
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.log.operation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OperationLogAspect operationLogAspect(ISysLogService logService,
                                                 ObjectProvider<IpLocationResolver> ipLocationResolverProvider) {
        return new OperationLogAspect(logService, ipLocationResolverProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(ExcelDictionaryProvider.class)
    public ExcelDictionaryProvider systemExcelDictionaryProvider(IDictService dictService) {
        return (dictType, label, metadata, context) -> {
            var options = dictService.getOptions(dictType);
            if (options == null) {
                throw new IllegalStateException("无法读取 Excel 字典: " + dictType);
            }
            String normalizedLabel = "";
            if (label != null) {
                normalizedLabel = label.trim();
            }
            String expectedLabel = normalizedLabel;
            var matches = options.stream()
                    .filter(option -> option.getLabel() != null && option.getLabel().trim().equals(expectedLabel))
                    .toList();
            if (matches.size() > 1) {
                throw new IllegalArgumentException("Excel 字典存在重复 label: " + dictType + "/" + normalizedLabel);
            }
            if (matches.isEmpty()) {
                return null;
            }
            return matches.getFirst().getValue();
        };
    }
}
