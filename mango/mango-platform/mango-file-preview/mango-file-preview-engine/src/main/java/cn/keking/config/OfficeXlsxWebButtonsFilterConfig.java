package cn.keking.config;

import cn.keking.web.filter.OfficeXlsxWebButtonsAttributeFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/** Registers the XLSX Web preview template attribute without changing legacy filters. */
@Configuration
public class OfficeXlsxWebButtonsFilterConfig {

    @Bean
    public FilterRegistrationBean<OfficeXlsxWebButtonsAttributeFilter> officeXlsxWebButtonsAttributeFilter() {
        FilterRegistrationBean<OfficeXlsxWebButtonsAttributeFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OfficeXlsxWebButtonsAttributeFilter());
        registration.setUrlPatterns(Set.of("/onlinePreview"));
        return registration;
    }
}
