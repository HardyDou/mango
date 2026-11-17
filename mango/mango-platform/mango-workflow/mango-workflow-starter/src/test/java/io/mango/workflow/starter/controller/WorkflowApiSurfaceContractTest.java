package io.mango.workflow.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.workflow.api.WorkflowBusinessApplyApi;
import io.mango.workflow.api.WorkflowBusinessProcessApi;
import io.mango.workflow.api.WorkflowCategoryApi;
import io.mango.workflow.api.WorkflowDefinitionApi;
import io.mango.workflow.api.WorkflowProcessApi;
import io.mango.workflow.api.WorkflowTaskRuntimeApi;
import io.mango.workflow.api.WorkflowTemplateApi;
import io.mango.workflow.api.WorkflowTemplateCategoryApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowApiSurfaceContractTest {

    /**
     * 公开 API 契约指纹：{@link #PUBLIC_APIS} 中「方法名 → 泛型返回类型」的 SHA-256。
     *
     * <p>指纹只覆盖公开接口的方法名、泛型返回类型与控制器端点的 verb/path/返回类型/权限，不覆盖 DTO 字段；
     * 给 VO、Query、Request 加字段不会改变指纹。有意增删公开方法或改变返回契约时，把新指纹粘到此处，
     * 并在 PR 描述里说明契约变更；确认不是有意变更时，回退改动而不是更新常量。
     */
    private static final String API_FINGERPRINT =
            "053297d7a998ef45b6c40093a2d4e1fb25d04d0323159773424b5c1fe148a8a1";

    /**
     * 公开 HTTP 端点指纹：{@link #PUBLIC_CONTROLLERS} 中「verb + path + 方法名 → 返回类型 + {@code ApiAccess}」的
     * SHA-256。
     */
    private static final String HTTP_FINGERPRINT =
            "277ab3b80baff855b0e4fdedc094ec7e73f67415e5951ca15835c273449da417";

    private static final List<Class<?>> PUBLIC_APIS = List.of(
            WorkflowBusinessApplyApi.class,
            WorkflowBusinessProcessApi.class,
            WorkflowCategoryApi.class,
            WorkflowDefinitionApi.class,
            WorkflowProcessApi.class,
            WorkflowTaskRuntimeApi.class,
            WorkflowTemplateApi.class,
            WorkflowTemplateCategoryApi.class);

    private static final List<Class<?>> PUBLIC_CONTROLLERS = List.of(
            WorkflowBusinessApplyController.class,
            WorkflowBusinessProcessController.class,
            WorkflowCategoryController.class,
            WorkflowDefinitionController.class,
            WorkflowProcessController.class,
            WorkflowTaskController.class,
            WorkflowTemplateCategoryController.class,
            WorkflowTemplateController.class);

    @Test
    void existingApisKeepMethodNamesAndReturnContracts() {
        String actual = apiFingerprint();
        assertThat(actual)
                .as("公开 API 契约变了（%s）。确认是有意变更就把 API_FINGERPRINT 常量改成上面的实际指纹，"
                        + "并在 PR 描述里写明新增/删除/改签名的公开方法；不是有意变更就回退改动。", actual)
                .isEqualTo(API_FINGERPRINT);
    }

    @Test
    void existingHttpEndpointsKeepPathsVerbsReturnsAndPermissions() {
        String actual = httpFingerprint();
        assertThat(actual)
                .as("公开 HTTP 端点契约变了（%s）。确认是有意变更就把 HTTP_FINGERPRINT 常量改成上面的实际指纹，"
                        + "并在 PR 描述里写明端点、权限的变化；不是有意变更就回退改动。", actual)
                .isEqualTo(HTTP_FINGERPRINT);
    }

    private static String apiFingerprint() {
        StringBuilder contract = new StringBuilder();
        PUBLIC_APIS.stream()
                .sorted(Comparator.comparing(Class::getName))
                .forEach(api -> {
                    contract.append("API ").append(api.getName()).append('\n');
                    Arrays.stream(api.getDeclaredMethods())
                            .sorted(Comparator.comparing(WorkflowApiSurfaceContractTest::methodSortKey))
                            .forEach(method -> contract.append(method.getName())
                                    .append(" -> ")
                                    .append(method.getGenericReturnType().getTypeName())
                                    .append('\n'));
                });
        return sha256(contract.toString());
    }

    private static String httpFingerprint() {
        StringBuilder contract = new StringBuilder();
        PUBLIC_CONTROLLERS.stream()
                .sorted(Comparator.comparing(Class::getName))
                .forEach(controller -> appendController(contract, controller));
        return sha256(contract.toString());
    }

    private static void appendController(StringBuilder contract, Class<?> controller) {
        RequestMapping root = controller.getAnnotation(RequestMapping.class);
        String rootPath = root == null ? "" : firstPath(root.path(), root.value());
        contract.append("CONTROLLER ").append(controller.getName()).append(' ').append(rootPath).append('\n');
        ApiAccess classAccess = controller.getAnnotation(ApiAccess.class);
        if (classAccess != null) {
            contract.append("ACCESS ").append(classAccess).append('\n');
        }
        Arrays.stream(controller.getDeclaredMethods())
                .filter(WorkflowApiSurfaceContractTest::isHttpMethod)
                .sorted(Comparator.comparing(WorkflowApiSurfaceContractTest::methodSortKey))
                .forEach(method -> appendEndpoint(contract, rootPath, method));
    }

    private static void appendEndpoint(StringBuilder contract, String rootPath, Method method) {
        contract.append(verb(method)).append(' ')
                .append(rootPath).append(methodPath(method)).append(' ')
                .append(method.getName()).append(" -> ")
                .append(method.getGenericReturnType().getTypeName()).append('\n');
        ApiAccess access = method.getAnnotation(ApiAccess.class);
        if (access != null) {
            contract.append("ACCESS ").append(access).append('\n');
        }
    }

    private static boolean isHttpMethod(Method method) {
        return mapping(method) != null;
    }

    private static String verb(Method method) {
        Annotation annotation = mapping(method);
        if (annotation instanceof GetMapping) {
            return "GET";
        }
        if (annotation instanceof PostMapping) {
            return "POST";
        }
        if (annotation instanceof PutMapping) {
            return "PUT";
        }
        if (annotation instanceof PatchMapping) {
            return "PATCH";
        }
        if (annotation instanceof DeleteMapping) {
            return "DELETE";
        }
        throw new IllegalStateException("Unsupported mapping: " + annotation);
    }

    private static String methodPath(Method method) {
        Annotation annotation = mapping(method);
        if (annotation instanceof GetMapping value) {
            return firstPath(value.path(), value.value());
        }
        if (annotation instanceof PostMapping value) {
            return firstPath(value.path(), value.value());
        }
        if (annotation instanceof PutMapping value) {
            return firstPath(value.path(), value.value());
        }
        if (annotation instanceof PatchMapping value) {
            return firstPath(value.path(), value.value());
        }
        if (annotation instanceof DeleteMapping value) {
            return firstPath(value.path(), value.value());
        }
        return "";
    }

    private static Annotation mapping(Method method) {
        for (Class<? extends Annotation> type : List.of(
                GetMapping.class, PostMapping.class, PutMapping.class, PatchMapping.class, DeleteMapping.class)) {
            Annotation annotation = method.getAnnotation(type);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    private static String firstPath(String[] path, String[] value) {
        if (path.length > 0) {
            return path[0];
        }
        if (value.length > 0) {
            return value[0];
        }
        return "";
    }

    private static String methodSortKey(Method method) {
        return method.getName() + Arrays.toString(method.getGenericParameterTypes());
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
