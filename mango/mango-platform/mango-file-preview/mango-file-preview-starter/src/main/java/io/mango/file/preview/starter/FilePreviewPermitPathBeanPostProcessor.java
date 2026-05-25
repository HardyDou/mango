package io.mango.file.preview.starter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 将 kkFileView 内置资源加入 Mango 认证层公共路径。
 */
public class FilePreviewPermitPathBeanPostProcessor implements BeanPostProcessor {

    private static final String AUTH_SECURITY_PROPERTIES =
            "io.mango.auth.starter.config.AuthSecurityProperties";
    private static final List<String> PERMIT_PATHS = List.of(
            "/onlinePreview",
            "/onlinePreview/**",
            "/picturesPreview",
            "/picturesPreview/**",
            "/getCorsFile",
            "/getCorsFile/**",
            "/file-preview/files/preview-entry",
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
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!AUTH_SECURITY_PROPERTIES.equals(bean.getClass().getName())) {
            return bean;
        }
        try {
            Method getter = bean.getClass().getMethod("getPermitPaths");
            Object value = getter.invoke(bean);
            if (value instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<String> permitPaths = (List<String>) list;
                for (String path : PERMIT_PATHS) {
                    if (!permitPaths.contains(path)) {
                        permitPaths.add(path);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to append file preview permit paths", e);
        }
        return bean;
    }
}
