package io.mango.file.preview.starter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * 将 kkFileView 内置资源加入 Mango 认证层公共路径。
 */
public class FilePreviewPermitPathBeanPostProcessor implements BeanPostProcessor {

    private static final Set<String> SECURITY_PROPERTIES_TYPES = Set.of(
            "io.mango.auth.starter.config.AuthSecurityProperties",
            "io.mango.authorization.starter.autoconfigure.SecurityProperties"
    );
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
            "/file-preview/sources",
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
