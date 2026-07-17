package io.mango.link.starter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.List;

public class LinkPermitPathBeanPostProcessor implements BeanPostProcessor {

    private static final String AUTH_SECURITY_PROPERTIES =
            "io.mango.auth.starter.config.AuthSecurityProperties";
    private static final List<String> PERMIT_PATHS = List.of(
            "/link/open/**"
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
                for (String path : PERMIT_PATHS) {
                    if (!list.contains(path)) {
                        Method add = List.class.getMethod("add", Object.class);
                        add.invoke(value, path);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to append link permit paths", e);
        }
        return bean;
    }
}
