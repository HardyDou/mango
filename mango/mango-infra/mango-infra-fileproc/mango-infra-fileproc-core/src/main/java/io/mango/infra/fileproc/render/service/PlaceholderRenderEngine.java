package io.mango.infra.fileproc.render.service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Double-brace placeholder rendering engine.
 */
public class PlaceholderRenderEngine {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*)\\s*}}");
    private static final Pattern BRACKET_PLACEHOLDER = Pattern.compile("\\[\\s*([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*)\\s*]");
    private static final String VARIABLE_PATH_SEPARATOR_REGEX = "\\.";

    public List<String> extract(String content) {
        if (content == null || content.trim().isEmpty()) {
            return List.of();
        }
        Matcher matcher = PLACEHOLDER.matcher(content);
        Set<String> variables = new LinkedHashSet<>();
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        Matcher bracketMatcher = BRACKET_PLACEHOLDER.matcher(content);
        while (bracketMatcher.find()) {
            variables.add(bracketMatcher.group(1));
        }
        return new ArrayList<>(variables);
    }

    public String render(String content, Map<String, Object> variables) {
        if (content == null) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = resolve(name, variables);
            if (value == null) {
                throw new RenderToolException("缺少模板变量：" + name);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private Object resolve(String name, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        Object current = variables;
        for (String part : name.split(VARIABLE_PATH_SEPARATOR_REGEX)) {
            current = readPart(current, part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private Object readPart(Object current, String part) {
        if (current instanceof Map<?, ?> map) {
            return ((Map<String, Object>) map).get(part);
        }
        String methodName = "get" + part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1);
        try {
            Method method = findGetter(current.getClass(), methodName);
            return method.invoke(current);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Method findGetter(Class<?> type, String methodName) throws NoSuchMethodException {
        Method method = type.getMethod(methodName);
        if (!Modifier.isPublic(type.getModifiers())) {
            method.setAccessible(true);
        }
        return method;
    }
}
