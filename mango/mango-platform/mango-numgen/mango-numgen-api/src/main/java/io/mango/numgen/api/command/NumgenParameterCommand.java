package io.mango.numgen.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 编号生成动态参数项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "编号生成动态参数项")
public class NumgenParameterCommand implements Serializable {

    private static final Set<String> MUTATING_MAP_METHODS = Set.of(
            "clear", "compute", "computeIfAbsent", "computeIfPresent", "merge", "put", "putAll",
            "putIfAbsent", "remove", "replace", "replaceAll");

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "动态参数键不能为空")
    @Schema(description = "动态参数键")
    private String key;

    @NotNull(message = "动态参数值不能为空")
    @Schema(description = "动态参数值")
    private String value;

    /**
     * Creates a live mutable Map view without exposing an implementation type from the API module.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> mutableMap(List<NumgenParameterCommand> entries) {
        return (Map<String, Object>) Proxy.newProxyInstance(
                Map.class.getClassLoader(),
                new Class<?>[]{Map.class},
                (proxy, method, args) -> {
                    Map<String, Object> snapshot = snapshot(entries);
                    try {
                        Object result = method.invoke(snapshot, args);
                        if (MUTATING_MAP_METHODS.contains(method.getName())) {
                            replaceEntries(entries, snapshot);
                        }
                        return result;
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
    }

    private static Map<String, Object> snapshot(List<NumgenParameterCommand> entries) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        entries.forEach(entry -> snapshot.put(entry.getKey(), entry.getValue()));
        return snapshot;
    }

    private static void replaceEntries(
            List<NumgenParameterCommand> entries, Map<String, Object> replacement) {
        List<NumgenParameterCommand> updated = new ArrayList<>();
        replacement.forEach((key, value) -> updated.add(
                new NumgenParameterCommand(key, value == null ? null : String.valueOf(value))));
        entries.clear();
        entries.addAll(updated);
    }
}
