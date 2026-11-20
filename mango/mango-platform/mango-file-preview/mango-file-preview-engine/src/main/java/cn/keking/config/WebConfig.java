package cn.keking.config;

import cn.keking.web.filter.*;
import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * @author: chenjh
 * @since: 2019/4/16 20:04
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebConfig.class);
    private static final int CHINESE_PATH_FILTER_ORDER = 10;
    private static final int BASE_URL_FILTER_ORDER = 20;
    private static final int URL_CHECK_FILTER_ORDER = 30;
    /**
     * 访问外部文件配置。
     *
     * <p>落盘文件的对外地址统一挂在 {@link cn.keking.service.FileHandlerService#PUBLIC_FILE_PATH_PREFIX} 下
     * （页面里的图片、音视频、转换产物都用这个前缀），安全放行清单只覆盖该前缀；根级 {@code /**} 仅保留给
     * {@code url} 兼容通道的既有页面，不在放行清单内。</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String filePath = ConfigConstants.getFileDir();
        String resourceLocation = fileResourceLocation(filePath);
        LOGGER.info("Add resource locations: {}", resourceLocation);
        registry.addResourceHandler(cn.keking.service.FileHandlerService.PUBLIC_FILE_PATH_PREFIX + "/**")
                .addResourceLocations(resourceLocation);
        registry.addResourceHandler("/**").addResourceLocations(
                "classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/",
                "classpath:/public/", resourceLocation);
    }

    static String fileResourceLocation(String filePath) {
        String resourceLocation = Path.of(filePath).toAbsolutePath().normalize().toUri().toString();
        if (resourceLocation.endsWith("/")) {
            return resourceLocation;
        }
        return resourceLocation + "/";
    }


    @Bean
    public FilterRegistrationBean<ChinesePathFilter> getChinesePathFilter() {
        ChinesePathFilter filter = new ChinesePathFilter();
        FilterRegistrationBean<ChinesePathFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(CHINESE_PATH_FILTER_ORDER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<TrustHostFilter> getTrustHostFilter() {
        Set<String> filterUri = new HashSet<>();
        filterUri.add("/onlinePreview");
        filterUri.add("/picturesPreview");
        filterUri.add("/getCorsFile");
        TrustHostFilter filter = new TrustHostFilter();
        FilterRegistrationBean<TrustHostFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<TrustDirFilter> getTrustDirFilter() {
        Set<String> filterUri = new HashSet<>();
        filterUri.add("/onlinePreview");
        filterUri.add("/picturesPreview");
        filterUri.add("/getCorsFile");
        TrustDirFilter filter = new TrustDirFilter();
        FilterRegistrationBean<TrustDirFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<BaseUrlFilter> getBaseUrlFilter() {
        Set<String> filterUri = new HashSet<>();
        BaseUrlFilter filter = new BaseUrlFilter();
        FilterRegistrationBean<BaseUrlFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        registrationBean.setOrder(BASE_URL_FILTER_ORDER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<UrlCheckFilter> getUrlCheckFilter() {
        UrlCheckFilter filter = new UrlCheckFilter();
        FilterRegistrationBean<UrlCheckFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(URL_CHECK_FILTER_ORDER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<AttributeSetFilter> getWatermarkConfigFilter() {
        Set<String> filterUri = new HashSet<>();
        filterUri.add("/index");
        filterUri.add("/");
        filterUri.add("/onlinePreview");
        filterUri.add("/picturesPreview");
        AttributeSetFilter filter = new AttributeSetFilter();
        FilterRegistrationBean<AttributeSetFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        registrationBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD);
        return registrationBean;
    }
}
