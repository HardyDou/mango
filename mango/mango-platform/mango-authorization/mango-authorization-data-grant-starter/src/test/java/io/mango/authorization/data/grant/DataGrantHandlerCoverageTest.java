package io.mango.authorization.data.grant;

import static org.assertj.core.api.Assertions.assertThat;

import io.mango.authorization.api.annotation.DataGrant;
import io.mango.common.result.R;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Grant 端点配对规则测试：带 {@link DataGrant} 的详情端点必须能找到同 domain 的列表端点。
 *
 * <p>除了验证规则本身，还会扫描当前类路径上的所有控制器，防止试点或后续接入漏配列表端点。</p>
 */
class DataGrantHandlerCoverageTest {

    @Test
    void classpathControllersSatisfyPairingRule() {
        DataGrantHandlerScanner.Coverage coverage = DataGrantHandlerScanner.scanClasspath();

        assertThat(coverage.violations())
                .as("详情端点缺少同域列表端点（扫描跳过 %s）", DataGrantHandlerScanner.skippedClasses())
                .isEmpty();
    }

    @Test
    void pairedListAndDetailPass() {
        DataGrantHandlerScanner.Coverage coverage = DataGrantHandlerScanner.scan(List.of(PairedController.class));

        assertThat(coverage.violations()).isEmpty();
        assertThat(coverage.listHandlers()).containsKey("paired:item");
        assertThat(coverage.detailHandlers()).containsKey("paired:item");
    }

    @Test
    void detailWithoutListIsReported() {
        DataGrantHandlerScanner.Coverage coverage = DataGrantHandlerScanner
                .scan(List.of(DetailOnlyController.class));

        assertThat(coverage.violations()).hasSize(1);
        assertThat(coverage.violations().get(0))
                .contains("detailWithoutList", "DetailOnlyController")
                .contains("detail-only:item");
    }

    @Test
    void detailWithListInAnotherDomainIsReported() {
        DataGrantHandlerScanner.Coverage coverage = DataGrantHandlerScanner
                .scan(List.of(CrossDomainController.class));

        assertThat(coverage.violations()).hasSize(1);
        assertThat(coverage.violations().get(0)).contains("crossDetail", "cross-domain:detail");
    }

    @Test
    void detailWithUnannotatedListEndpointIsReported() {
        DataGrantHandlerScanner.Coverage coverage = DataGrantHandlerScanner
                .scan(List.of(UnannotatedListController.class));

        assertThat(coverage.violations()).hasSize(1);
        assertThat(coverage.violations().get(0))
                .contains("unannotatedListDetail", "UnannotatedListController")
                .contains("unannotated-list:item");
    }

    @Test
    void listOnlyIsNotAViolation() {
        DataGrantHandlerScanner.Coverage coverage = DataGrantHandlerScanner.scan(List.of(ListOnlyController.class));

        assertThat(coverage.violations()).isEmpty();
        assertThat(coverage.listHandlers()).containsKey("list-only:item");
        assertThat(coverage.detailHandlers()).isEmpty();
    }

    @Test
    void handlersWithoutAnnotationAreIgnored() {
        DataGrantHandlerScanner.Coverage coverage = DataGrantHandlerScanner.scan(List.of(PlainController.class));

        assertThat(coverage.listHandlers()).isEmpty();
        assertThat(coverage.detailHandlers()).isEmpty();
    }

    /**
     * 列表与详情同域的合法控制器。
     */
    @RestController
    @RequestMapping("/paired")
    static class PairedController {

        @PostMapping("/page")
        @DataGrant(domain = "paired:item")
        R<String> page() {
            return R.ok("page");
        }

        @GetMapping("/detail")
        @DataGrant(domain = "paired:item")
        R<String> detail() {
            return R.ok("detail");
        }
    }

    /**
     * 只有详情端点的控制器。
     */
    @RestController
    @RequestMapping("/detail-only")
    static class DetailOnlyController {

        @GetMapping("/{id}/detail")
        @DataGrant(domain = "detail-only:item")
        R<String> detailWithoutList(@PathVariable("id") Long id) {
            return R.ok(Long.toString(id));
        }
    }

    /**
     * 列表与详情使用不同 domain 的控制器。
     */
    @RestController
    @RequestMapping("/cross-domain")
    static class CrossDomainController {

        @PostMapping("/page")
        @DataGrant(domain = "cross-domain:list")
        R<String> crossList() {
            return R.ok("page");
        }

        @GetMapping("/detail")
        @DataGrant(domain = "cross-domain:detail")
        R<String> crossDetail() {
            return R.ok("detail");
        }
    }

    /**
     * 只有列表端点的控制器。
     */
    @RestController
    @RequestMapping("/list-only")
    static class ListOnlyController {

        @PostMapping("/page")
        @DataGrant(domain = "list-only:item")
        R<String> page() {
            return R.ok("page");
        }
    }

    /**
     * 没有任何 Grant 注解的控制器。
     */
    @RestController
    @RequestMapping("/plain")
    static class PlainController {

        @GetMapping("/detail")
        R<String> detail() {
            return R.ok("detail");
        }
    }

    @Test
    void scannerIndexesHandlerNames() {
        DataGrantHandlerScanner.Coverage coverage = DataGrantHandlerScanner.scan(List.of(PairedController.class));

        assertThat(coverage.listHandlers().get("paired:item"))
                .containsExactly(PairedController.class.getName() + "#page");
        assertThat(coverage.detailHandlers().get("paired:item"))
                .containsExactly(PairedController.class.getName() + "#detail");
    }

    @Test
    void scannerIgnoresNonControllerTypes() {
        DataGrantHandlerScanner.Coverage coverage = DataGrantHandlerScanner.scan(Set.of(String.class));

        assertThat(coverage.listHandlers()).isEmpty();
        assertThat(coverage.detailHandlers()).isEmpty();
    }
    /**
     * 反例：详情端点带注解，但同域的列表端点忘了加注解。
     *
     * <p>列表端点未加 {@link DataGrant} 时不进入签发集合，详情端点将永远拿不到有效 Grant，因此必须报违规。</p>
     */
    @RestController
    @RequestMapping("/unannotated-list")
    static class UnannotatedListController {

        @GetMapping
        R<List<String>> unannotatedList() {
            return R.ok(List.of());
        }

        @GetMapping("/{id}")
        @DataGrant(domain = "unannotated-list:item")
        R<String> unannotatedListDetail(@PathVariable("id") Long id) {
            return R.ok("item-" + id);
        }
    }
}
