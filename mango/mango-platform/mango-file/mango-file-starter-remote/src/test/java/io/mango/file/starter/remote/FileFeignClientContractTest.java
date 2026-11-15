package io.mango.file.starter.remote;

import io.mango.file.api.FileApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 远端适配契约：{@link FileFeignClient} 必须为本模块暴露的每个 {@link FileApi} 方法声明 HTTP 映射。
 *
 * <p>架构门禁用同一事实检查 Controller 与 FeignClient 的端点一致性（`MANGO-ARCH-ADAPTER-001`、
 * `MANGO-ARCH-FEIGN-008`）。缺少映射时门禁只有在完整 Reactor 预算检查时才失败，反馈链路长，
 * 这里用普通单测把同一约束前移到模块测试。
 */
class FileFeignClientContractTest {

    private static final List<Class<?>> MAPPING_ANNOTATIONS = List.of(
            GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class, RequestMapping.class);

    @Test
    void everyFileApiMethodDeclaresMappingOnFileFeignClient() {
        List<String> missing = new ArrayList<>();
        for (Method apiMethod : FileApi.class.getDeclaredMethods()) {
            Method feignMethod = resolveFeignMethod(apiMethod);
            if (feignMethod == null || !hasMappingAnnotation(feignMethod)) {
                missing.add(apiMethod.getName());
            }
        }
        assertThat(missing)
                .as("FileFeignClient 缺少映射的 FileApi 方法")
                .isEmpty();
    }

    private Method resolveFeignMethod(Method apiMethod) {
        try {
            Method candidate = FileFeignClient.class.getMethod(apiMethod.getName(), apiMethod.getParameterTypes());
            return candidate.getDeclaringClass() == FileFeignClient.class ? candidate : null;
        } catch (NoSuchMethodException notDeclared) {
            return null;
        }
    }

    private boolean hasMappingAnnotation(Method method) {
        return Arrays.stream(method.getAnnotations())
                .anyMatch(annotation -> MAPPING_ANNOTATIONS.contains(annotation.annotationType()));
    }
}
