package io.mango.cms.starter.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.mango.authorization.api.annotation.DataGrant;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * CMS 管理端数据权限 Grant 注解配对校验。
 *
 * <p>{@code DataGrantHandlerScanner} 只扫描自身模块的测试 classpath，看不到 CMS 控制器，
 * 因此 CMS 侧的「详情有注解、列表也必须同 domain 注解」这条不变量必须在本模块单独钉住：
 * 漏标列表注解时本用例变红，而不是等到 {@code enforce} 部署下才发现详情恒为 {@code ABSENT}。</p>
 */
class CmsDataGrantAnnotationPairingTest {

    /** 期望接入的数据域：控制器 -> domain。 */
    private static final Map<Class<?>, String> EXPECTED_DOMAINS = new LinkedHashMap<>();

    static {
        EXPECTED_DOMAINS.put(CmsSiteAdminController.class, "cms:site");
        EXPECTED_DOMAINS.put(CmsSiteSettingController.class, "cms:site");
        EXPECTED_DOMAINS.put(CmsContentController.class, "cms:content");
        EXPECTED_DOMAINS.put(CmsContentCategoryController.class, "cms:content-category");
        EXPECTED_DOMAINS.put(CmsContentTagController.class, "cms:content-tag");
        EXPECTED_DOMAINS.put(CmsAdvertisementController.class, "cms:advertisement");
        EXPECTED_DOMAINS.put(CmsAdDeliveryController.class, "cms:ad-delivery");
        EXPECTED_DOMAINS.put(CmsNavigationController.class, "cms:navigation");
    }

    /** 未接入的控制器：不得出现任何 {@code @DataGrant}。 */
    private static final List<Class<?>> EXCLUDED_CONTROLLERS = List.of(
            // 公开通道：三个 site 应用匿名直调，无身份即无 Grant。
            CmsSiteController.class,
            // 列表端点是 /site-categories/tree，返回 List 而非 PageResult，无法签发。
            CmsSiteCategoryController.class,
            // 只有分页列表与写入动作，没有可校验的单条详情。
            CmsContentPublishController.class);

    @Test
    void everyAnnotatedDomainHasBothListAndDetailAnnotatedWithSameDomain() {
        Map<String, List<Method>> listByDomain = new LinkedHashMap<>();
        Map<String, List<Method>> detailByDomain = new LinkedHashMap<>();
        for (Class<?> controller : EXPECTED_DOMAINS.keySet()) {
            collect(controller, listByDomain, detailByDomain);
        }
        assertThat(listByDomain.keySet()).as("至少一个域完成了列表注解").isNotEmpty();
        listByDomain.forEach((domain, methods) -> assertThat(detailByDomain)
                .as("数据域 %s 的列表端点已签发 Grant，但缺少同域详情端点", domain)
                .containsKey(domain));
        detailByDomain.forEach((domain, methods) -> assertThat(listByDomain)
                .as("数据域 %s 的详情端点已校验 Grant，但缺少同域列表端点（详情会恒为 ABSENT）", domain)
                .containsKey(domain));
    }

    @Test
    void everyControllerInPackageIsClassified() throws Exception {
        // 白名单只遍历已知控制器：新增 Cms*Controller 却不接入时，其它用例会全绿。这里要求
        // 包内每个控制器都必须显式归类（接入或排除），否则失败并提示做决定。
        List<String> unclassified = new ArrayList<>();
        for (String className : controllerClassNamesInPackage()) {
            Class<?> controller = Class.forName(className);
            if (!EXPECTED_DOMAINS.containsKey(controller) && !EXCLUDED_CONTROLLERS.contains(controller)) {
                unclassified.add(controller.getSimpleName());
            }
        }
        assertThat(unclassified)
                .as("新控制器需决定是否接入数据权限 Grant：接入请加入 EXPECTED_DOMAINS，不接入请加入 EXCLUDED_CONTROLLERS")
                .isEmpty();
    }

    @Test
    void excludedControllersCarryNoDataGrant() {
        for (Class<?> controller : EXCLUDED_CONTROLLERS) {
            assertThat(annotatedMethods(controller))
                    .as("%s 必须保持无 @DataGrant（公开通道或列表为树结构，接入即恒 403）", controller.getSimpleName())
                    .isEmpty();
        }
    }

    @Test
    void annotationsOnlyOnPageAndDetailEndpoints() {
        for (Class<?> controller : EXPECTED_DOMAINS.keySet()) {
            for (Method method : annotatedMethods(controller)) {
                GetMapping mapping = method.getAnnotation(GetMapping.class);
                assertThat(mapping).as("%s#%s 必须标注在 GET 端点上", controller.getSimpleName(), method.getName())
                        .isNotNull();
                assertThat(mapping.value())
                        .as("%s#%s 只允许标注在 /page 或 /detail 端点", controller.getSimpleName(), method.getName())
                        .allSatisfy(path -> assertThat(path).matches(".+/(page|detail)$"));
            }
        }
    }

    @Test
    void annotationCountMatchesSupportedDomainPairs() {
        int total = 0;
        for (Class<?> controller : EXPECTED_DOMAINS.keySet()) {
            total += annotatedMethods(controller).size();
        }
        // 7 对（列表 + 详情）+ 站点设置详情（其列表是 CmsSiteAdminController#pageSites）
        assertThat(total).isEqualTo(15);
    }

    @Test
    void siteSettingDetailDeclaresParentIdField() throws NoSuchMethodException {
        Method detail = CmsSiteSettingController.class.getMethod("detailSiteSetting", Long.class);
        DataGrant grant = detail.getAnnotation(DataGrant.class);
        assertThat(grant).isNotNull();
        assertThat(grant.domain()).isEqualTo("cms:site");
        assertThat(grant.idField())
                .as("站点设置的 URL 主键是父实体（站点）ID，必须显式声明 idField")
                .isEqualTo("siteId");
    }

    private static List<String> controllerClassNamesInPackage() throws Exception {
        Path sourceDir = Paths.get("src/main/java/io/mango/cms/starter/controller");
        assertThat(sourceDir)
                .as("测试依赖模块源码目录（Maven basedir）：%s", sourceDir.toAbsolutePath())
                .isDirectory();
        try (Stream<Path> files = Files.list(sourceDir)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("Cms") && name.endsWith("Controller.java"))
                    .map(name -> "io.mango.cms.starter.controller."
                            + name.substring(0, name.length() - ".java".length()))
                    .sorted()
                    .toList();
        }
    }

    private static void collect(Class<?> controller, Map<String, List<Method>> listByDomain,
            Map<String, List<Method>> detailByDomain) {
        for (Method method : annotatedMethods(controller)) {
            String domain = method.getAnnotation(DataGrant.class).domain();
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            String path = mapping.value()[0];
            if (path.endsWith("/page")) {
                listByDomain.computeIfAbsent(domain, key -> new ArrayList<>()).add(method);
            } else if (path.endsWith("/detail")) {
                detailByDomain.computeIfAbsent(domain, key -> new ArrayList<>()).add(method);
            }
        }
    }

    private static List<Method> annotatedMethods(Class<?> controller) {
        List<Method> annotated = new ArrayList<>();
        for (Method method : controller.getDeclaredMethods()) {
            if (method.isAnnotationPresent(DataGrant.class)) {
                annotated.add(method);
            }
        }
        assertThat(controller.getAnnotation(RequestMapping.class)).isNotNull();
        return annotated;
    }
}
