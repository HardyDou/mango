package io.mango.authorization.data.grant;

import io.mango.authorization.api.annotation.DataGrant;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试期扫描 {@link DataGrant} 端点的一致性。
 *
 * <p>Grant 能生效的前提是「同一个 domain 既有列表端点（签发）又有详情端点（校验）」。列表端点缺失时，
 * 详情端点永远拿不到有效 Grant，请求会被 403 或 404 拒绝，因此这里把该约束变成测试断言。</p>
 *
 * <p>内部类（名称含 {@code $}，如各测试类里的夹具控制器）不参与类路径扫描，避免测试夹具触发误报。</p>
 */
final class DataGrantHandlerScanner {

    private static final String SCAN_ROOT = "io/mango";

    private static final String CLASS_SUFFIX = ".class";

    private static final List<String> SKIPPED = Collections.synchronizedList(new ArrayList<>());

    private DataGrantHandlerScanner() {
    }

    /**
     * 扫描类路径上的全部控制器并计算覆盖情况。
     *
     * @return 覆盖情况
     */
    static Coverage scanClasspath() {
        return scan(scanControllers());
    }

    /**
     * 分析给定控制器类型的 Grant 端点分布。
     *
     * @param controllerTypes 控制器类型集合
     * @return 覆盖情况
     */
    static Coverage scan(Collection<Class<?>> controllerTypes) {
        Map<String, List<String>> listHandlers = new TreeMap<>();
        Map<String, List<String>> detailHandlers = new TreeMap<>();
        for (Class<?> type : controllerTypes) {
            if (!isController(type)) {
                continue;
            }
            RequestMapping typeMapping = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
            for (Method method : ReflectionUtils.getAllDeclaredMethods(type)) {
                RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method,
                        RequestMapping.class);
                if (methodMapping == null) {
                    continue;
                }
                DataGrant dataGrant = AnnotatedElementUtils.findMergedAnnotation(method, DataGrant.class);
                if (dataGrant == null) {
                    dataGrant = AnnotatedElementUtils.findMergedAnnotation(type, DataGrant.class);
                }
                if (dataGrant == null) {
                    continue;
                }
                String handler = type.getName() + "#" + method.getName();
                boolean detail = isDetailHandler(typeMapping, methodMapping);
                Map<String, List<String>> target = detail ? detailHandlers : listHandlers;
                target.computeIfAbsent(dataGrant.domain(), key -> new ArrayList<>()).add(handler);
            }
        }
        return new Coverage(listHandlers, detailHandlers);
    }

    /**
     * 扫描类路径下所有可加载的控制器类型。
     *
     * @return 控制器类型
     */
    static List<Class<?>> scanControllers() {
        ClassLoader classLoader = DataGrantHandlerScanner.class.getClassLoader();
        Set<String> classNames = new TreeSet<>();
        try {
            Enumeration<URL> roots = classLoader.getResources(SCAN_ROOT);
            while (roots.hasMoreElements()) {
                URL url = roots.nextElement();
                if ("file".equals(url.getProtocol())) {
                    collectFromDirectory(Path.of(url.toURI()), classNames);
                } else if ("jar".equals(url.getProtocol())) {
                    collectFromJar(url, classNames);
                }
            }
        } catch (IOException | URISyntaxException exception) {
            throw new IllegalStateException("扫描控制器类路径失败", exception);
        }
        List<Class<?>> controllers = new ArrayList<>();
        for (String className : classNames) {
            try {
                Class<?> type = Class.forName(className, false, classLoader);
                if (isController(type)) {
                    controllers.add(type);
                }
            } catch (ClassNotFoundException | LinkageError error) {
                SKIPPED.add(className);
            }
        }
        return controllers;
    }

    /**
     * 扫描时因依赖缺失无法加载、被跳过的类名。
     *
     * @return 类名列表
     */
    static List<String> skippedClasses() {
        return List.copyOf(SKIPPED);
    }

    private static void collectFromDirectory(Path root, Set<String> classNames) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> root.relativize(path).toString())
                    .filter(DataGrantHandlerScanner::isTopLevelClassFile)
                    .map(DataGrantHandlerScanner::toClassName)
                    .forEach(classNames::add);
        }
    }

    private static void collectFromJar(URL url, Set<String> classNames) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        try (JarFile jarFile = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && isTopLevelClassFile(entry.getName())) {
                    classNames.add(toClassName(entry.getName()));
                }
            }
        }
    }

    private static boolean isTopLevelClassFile(String path) {
        return path.endsWith(CLASS_SUFFIX) && !path.contains("$") && !path.endsWith("module-info" + CLASS_SUFFIX);
    }

    private static String toClassName(String path) {
        return path.substring(0, path.length() - CLASS_SUFFIX.length())
                .replace('/', '.')
                .replace('\\', '.');
    }

    private static boolean isController(Class<?> type) {
        return AnnotatedElementUtils.hasAnnotation(type, RestController.class)
                || AnnotatedElementUtils.hasAnnotation(type, Controller.class);
    }

    private static boolean isDetailHandler(RequestMapping typeMapping, RequestMapping methodMapping) {
        if (!isGet(methodMapping)) {
            return false;
        }
        for (String path : combinePaths(typeMapping, methodMapping)) {
            if (path.contains("{id}") || path.endsWith("/detail")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGet(RequestMapping methodMapping) {
        RequestMethod[] methods = methodMapping.method();
        if (methods.length == 0) {
            return true;
        }
        for (RequestMethod method : methods) {
            if (method == RequestMethod.GET) {
                return true;
            }
        }
        return false;
    }

    private static List<String> combinePaths(RequestMapping typeMapping, RequestMapping methodMapping) {
        List<String> prefixes = paths(typeMapping);
        List<String> suffixes = paths(methodMapping);
        List<String> result = new ArrayList<>(prefixes.size() * suffixes.size());
        for (String prefix : prefixes) {
            for (String suffix : suffixes) {
                result.add(normalize(prefix + suffix));
            }
        }
        return result;
    }

    private static List<String> paths(RequestMapping mapping) {
        if (mapping == null || mapping.path().length == 0) {
            return List.of("");
        }
        return List.of(mapping.path());
    }

    private static String normalize(String path) {
        String normalized = path.startsWith("/") || path.isEmpty() ? path : "/" + path;
        return normalized.endsWith("/") && normalized.length() > 1
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    /**
     * Grant 端点覆盖情况。
     *
     * @param listHandlers   列表端点：domain 到 {@code Class#method} 的映射
     * @param detailHandlers 详情端点：domain 到 {@code Class#method} 的映射
     */
    record Coverage(Map<String, List<String>> listHandlers, Map<String, List<String>> detailHandlers) {

        /**
         * 详情端点缺少同域列表端点的问题清单。
         *
         * @return 问题描述，空表示覆盖合法
         */
        List<String> violations() {
            List<String> violations = new ArrayList<>();
            detailHandlers.forEach((domain, handlers) -> {
                if (!listHandlers.containsKey(domain)) {
                    violations.add("详情端点 " + String.join(", ", handlers) + " 使用的 domain=" + domain
                            + " 没有任何列表端点声明同一 domain 的 @DataGrant");
                }
            });
            return violations;
        }
    }
}
