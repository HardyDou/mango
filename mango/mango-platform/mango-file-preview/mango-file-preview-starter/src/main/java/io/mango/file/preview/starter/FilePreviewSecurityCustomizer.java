package io.mango.file.preview.starter;

import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.List;

/**
 * 让 kkFileView 内置页面资源绕过业务鉴权链。
 */
public class FilePreviewSecurityCustomizer implements WebSecurityCustomizer {

    private static final List<String> IGNORED_PATHS = List.of(
            "/onlinePreview",
            "/onlinePreview/**",
            "/picturesPreview",
            "/picturesPreview/**",
            "/getCorsFile",
            "/getCorsFile/**",
            "/file-preview/sources/**",
            "/pdfjs/**",
            "/js/**",
            "/css/**",
            "/images/**",
            "/bootstrap/**",
            "/highlight/**",
            "/xlsx/**",
            "/static/**",
            "/favicon.ico"
    );

    @Override
    public void customize(WebSecurity web) {
        web.ignoring().requestMatchers(IGNORED_PATHS.stream()
                .map(AntPathRequestMatcher::new)
                .toArray(AntPathRequestMatcher[]::new));
    }
}
