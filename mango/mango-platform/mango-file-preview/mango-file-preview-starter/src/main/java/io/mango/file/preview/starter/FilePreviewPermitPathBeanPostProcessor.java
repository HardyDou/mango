package io.mango.file.preview.starter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 将 kkFileView 内置资源加入 Mango 认证层公共路径。
 *
 * <p>这里保留通配放行是因为清单主体是第三方静态资源（`pdfjs/**`、`js/**`、`images/**` 等）
 * 与 kkFileView 自带的根路径处理器，它们不是 API 接口，不能声明为 `API_RESOURCE`。
 * 清单中的 `/file-preview/files/**` 等业务入口仍是待收敛项，收敛前不得删除本类。
 */
public class FilePreviewPermitPathBeanPostProcessor implements BeanPostProcessor {

    private static final Set<String> SECURITY_PROPERTIES_TYPES = Set.of(
            "io.mango.auth.starter.config.AuthSecurityProperties",
            "io.mango.authorization.starter.autoconfigure.SecurityProperties"
    );
    /**
     * 引擎落盘文件的对外访问前缀。
     *
     * <p>与引擎 {@code cn.keking.service.FileHandlerService#PUBLIC_FILE_PATH_PREFIX} 和引擎
     * {@code cn.keking.config.WebConfig} 的资源映射必须一致，由 {@code FilePreviewEngineResourceRegistrarTest} 锁定。
     * 这是唯一允许匿名访问的落盘文件路径，根级宽匹配不在放行范围内。</p>
     */
    static final String PUBLIC_FILE_PATH_PREFIX = "/preview-file";
    private static final List<String> PERMIT_PATHS = List.of(
            "/onlinePreview",
            "/onlinePreview/**",
            "/picturesPreview",
            "/picturesPreview/**",
            "/getCorsFile",
            "/getCorsFile/**",
            "/directory",
            "/directory/**",
            "/compressed-file",
            "/compressed-file/**",
            "/file-preview/files/preview-entry",
            "/file-preview/files/preview-progress",
            "/file-preview/files/preview-content",
            "/file-preview/files/preview-artifact",
            "/file-preview/sources",
            "/file-preview/generated",
            // 引擎落盘文件的唯一放行前缀：页面内图片、音视频、转换产物都挂在这里，根级宽匹配仍不放行。
            PUBLIC_FILE_PATH_PREFIX + "/**",
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

    static List<String> permitPaths() {
        return PERMIT_PATHS;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!isSecurityProperties(bean.getClass())) {
            return bean;
        }
        try {
            Method getter = bean.getClass().getMethod("getPermitPaths");
            Object value = getter.invoke(bean);
            if (value instanceof List<?> list) {
                List<String> permitPaths = new ArrayList<>(list.size() + PERMIT_PATHS.size());
                for (Object path : list) {
                    if (path instanceof String stringPath) {
                        permitPaths.add(stringPath);
                    }
                }
                for (String path : PERMIT_PATHS) {
                    if (!permitPaths.contains(path)) {
                        permitPaths.add(path);
                    }
                }
                Method setter = bean.getClass().getMethod("setPermitPaths", List.class);
                setter.invoke(bean, permitPaths);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to append file preview permit paths", e);
        }
        return bean;
    }

    private static boolean isSecurityProperties(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            if (SECURITY_PROPERTIES_TYPES.contains(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    static boolean supportsSecurityPropertiesType(String className) {
        return SECURITY_PROPERTIES_TYPES.contains(className);
    }
}
