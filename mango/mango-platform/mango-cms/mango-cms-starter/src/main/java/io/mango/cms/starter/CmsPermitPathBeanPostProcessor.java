package io.mango.cms.starter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.List;

public class CmsPermitPathBeanPostProcessor implements BeanPostProcessor {

    private static final String AUTH_SECURITY_PROPERTIES =
            "io.mango.auth.starter.config.AuthSecurityProperties";
    private static final List<String> PERMIT_PATHS = List.of(
            "/cms/open/**"
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
            throw new IllegalStateException("Failed to append CMS permit paths", e);
        }
        return bean;
    }
}
