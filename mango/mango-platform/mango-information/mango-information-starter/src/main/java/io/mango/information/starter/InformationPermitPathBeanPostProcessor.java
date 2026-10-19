package io.mango.information.starter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 信息采集公开入口免登录放行。
 * <p>
 * 公开上传与预览是以表单为边界的匿名入口，和 CMS 公开文件入口保持一致做法：
 * 在认证配置里追加 permit path，接口内部再校验表单状态与文件归属。
 */
public class InformationPermitPathBeanPostProcessor implements BeanPostProcessor {

    private static final String AUTH_SECURITY_PROPERTIES =
            "io.mango.auth.starter.config.AuthSecurityProperties";
    private static final List<String> PERMIT_PATHS = List.of(
            "/information/open/**"
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
            throw new IllegalStateException("Failed to append information permit paths", e);
        }
        return bean;
    }
}
