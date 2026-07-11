package io.mango.infra.excel.starter;

import io.mango.infra.persistence.web.starter.excel.ExcelAdapter;
import io.mango.infra.persistence.web.starter.excel.ExcelDictionaryProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Excel 默认实现自动配置。
 */
@AutoConfiguration
public class ExcelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ExcelAdapter.class)
    public ExcelAdapter poiExcelAdapter(ApplicationContext applicationContext,
                                        ObjectProvider<ExcelDictionaryProvider> dictionaryProvider) {
        return new PoiExcelAdapter(applicationContext, dictionaryProvider);
    }
}
