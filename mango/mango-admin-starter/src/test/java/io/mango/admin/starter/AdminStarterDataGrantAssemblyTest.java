package io.mango.admin.starter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 数据权限 Grant 在执行侧的装配守护。
 *
 * <p>{@code @DataGrant} 注解来自 {@code mango-authorization-api}（compile 依赖），但签发与校验
 * 需要 {@code mango-authorization-data-grant-starter} 在运行时 classpath 上：
 * 只把该 starter 声明为 test scope 时编译与单测全绿，真实部署却既不签发也不校验。
 * 本用例把「admin-starter 以 compile 依赖携带它」钉在 POM 上，避免沉默回归。</p>
 */
class AdminStarterDataGrantAssemblyTest {

    private static final String ARTIFACT = "mango-authorization-data-grant-starter";

    @Test
    void dataGrantStarterIsProductionDependency() throws Exception {
        NodeList dependencies = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile())
                .getElementsByTagName("dependency");

        List<String> scopes = new ArrayList<>();
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            if (ARTIFACT.equals(childText(dependency, "artifactId"))) {
                String scope = childText(dependency, "scope");
                scopes.add(scope == null ? "compile" : scope);
            }
        }

        assertTrue(scopes.contains("compile"),
                ARTIFACT + " 必须是 admin-starter 的 compile 依赖，否则 CMS 等域注解在运行时是死代码，实际依赖范围：" + scopes);
    }

    @Test
    void dataGrantAutoConfigurationIsRegistered() throws IOException {
        String resource = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
        // 该资源在每个 starter jar 里都有一份，必须遍历全部而非只看第一个。
        Enumeration<URL> urls = getClass().getClassLoader().getResources(resource);
        List<String> registered = new ArrayList<>();
        while (urls.hasMoreElements()) {
            try (InputStream stream = urls.nextElement().openStream()) {
                new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .forEach(registered::add);
            }
        }
        assertTrue(registered.contains("io.mango.authorization.data.grant.DataGrantAutoConfiguration"),
                "数据权限 Grant 自动配置未注册，已注册项：" + registered);
    }

    @Test
    void interceptorAndAnnotationTypesAreLoadable() {
        assertTrue(isPresent("io.mango.authorization.data.grant.DataGrantInterceptor"),
                "运行时 classpath 缺少 DataGrantInterceptor");
        assertTrue(isPresent("io.mango.authorization.api.annotation.DataGrant"),
                "编译期缺少 @DataGrant 注解类型");
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className, false, AdminStarterDataGrantAssemblyTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static String childText(Element parent, String tag) {
        NodeList children = parent.getElementsByTagName(tag);
        if (children.getLength() == 0) {
            return null;
        }
        String text = children.item(0).getTextContent();
        return text == null ? null : text.trim();
    }
}
