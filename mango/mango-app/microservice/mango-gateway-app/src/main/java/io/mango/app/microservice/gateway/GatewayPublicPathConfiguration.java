package io.mango.app.microservice.gateway;

import io.mango.common.result.R;
import io.mango.gateway.api.GatewayConstant;
import io.mango.gateway.api.SysPublicPathApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 网关部署入口的最小公共路径提供器。
 *
 * @author hardy
 */
@Configuration
public class GatewayPublicPathConfiguration {

    @Bean
    @ConditionalOnMissingBean(SysPublicPathApi.class)
    public SysPublicPathApi sysPublicPathApi() {
        List<String> anonymousPaths = Arrays.asList(GatewayConstant.WHITE_LIST);
        return new SysPublicPathApi() {
            @Override
            public R<List<String>> getAnonymousPaths() {
                return R.ok(anonymousPaths);
            }

            @Override
            public R<List<String>> getLoginRequiredPaths() {
                return R.ok(List.of());
            }

            @Override
            public R<List<String>> listInternalPaths() {
                return R.ok(List.of());
            }

            @Override
            public R<Boolean> isPublicPath(String path) {
                return R.ok(anonymousPaths.stream().anyMatch(pattern -> matchPattern(pattern, path)));
            }
        };
    }

    private static boolean matchPattern(String pattern, String path) {
        if (pattern.equals(path)) {
            return true;
        }
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            int slashIndex = path.indexOf('/', prefix.length());
            return path.startsWith(prefix) && (slashIndex == -1 || slashIndex == path.length() - 1);
        }
        return false;
    }
}
