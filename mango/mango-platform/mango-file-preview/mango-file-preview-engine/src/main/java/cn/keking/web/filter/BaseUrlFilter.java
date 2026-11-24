package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author chenjh
 * @since 2020/5/13 18:27
 */
public class BaseUrlFilter implements Filter {

    private static final String FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    private static final String FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    private static final String FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";

    private static String BASE_URL;

    public static String getBaseUrl() {
        String baseUrl;
        try {
            baseUrl = (String) RequestContextHolder.currentRequestAttributes().getAttribute("baseUrl", 0);
        } catch (Exception e) {
            baseUrl = BASE_URL;
        }
        return baseUrl;
    }


    /**
     * 按请求还原浏览器可达的基地址。
     *
     * <p>缺省用 {@code scheme://serverName:serverPort + contextPath}，与历史行为一致；
     * 经反向代理（含本地前端 dev server）转发时，请求的 Host 已被改写成后端地址，
     * 直接使用会把内网地址写进预览页面，导致浏览器加载不到脚本与取数接口。
     * 因此优先采用标准转发头 {@code X-Forwarded-Host}、{@code X-Forwarded-Proto}、
     * {@code X-Forwarded-Prefix}，缺失时才回落到请求本身的取值。</p>
     *
     * @param request 当前请求
     * @return 不带结尾斜杠的基地址
     */
    private static String resolveRequestBaseUrl(HttpServletRequest request) {
        String scheme = firstHeaderValue(request.getHeader(FORWARDED_PROTO_HEADER));
        String host = firstHeaderValue(request.getHeader(FORWARDED_HOST_HEADER));
        String prefix = firstHeaderValue(request.getHeader(FORWARDED_PREFIX_HEADER));
        if (StringUtils.isEmpty(scheme)) {
            scheme = request.getScheme();
        }
        if (StringUtils.isEmpty(host)) {
            host = request.getServerName() + ":" + request.getServerPort();
        }
        if (prefix == null) {
            prefix = request.getContextPath();
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return scheme + "://" + host + prefix;
    }

    private static String firstHeaderValue(String header) {
        if (StringUtils.isEmpty(header)) {
            return null;
        }
        int comma = header.indexOf(',');
        return (comma < 0 ? header : header.substring(0, comma)).trim();
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        String baseUrl;
        String configBaseUrl = ConfigConstants.getBaseUrl();

        final HttpServletRequest servletRequest = (HttpServletRequest) request;
        //1、支持通过 http header 中 X-Base-Url 来动态设置 baseUrl 以支持多个域名/项目的共享使用
        final String urlInHeader = servletRequest.getHeader("X-Base-Url");
        if (StringUtils.isNotEmpty(urlInHeader)) {
            baseUrl = urlInHeader;
        } else if (configBaseUrl != null && !ConfigConstants.DEFAULT_VALUE.equalsIgnoreCase(configBaseUrl)) {
            //2、如果配置文件中配置了 baseUrl 且不为 default 则以配置文件为准
            baseUrl = configBaseUrl;
        } else {
            //3、默认按请求拼接 baseUrl；反代场景优先使用转发头，避免把内网地址写进页面
            baseUrl = resolveRequestBaseUrl(servletRequest) + "/";
        }

        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl.concat("/");
        }

        BASE_URL = baseUrl;
        request.setAttribute("baseUrl", baseUrl);
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
