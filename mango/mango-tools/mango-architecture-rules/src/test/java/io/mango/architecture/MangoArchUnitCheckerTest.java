package io.mango.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import java.net.URISyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

class MangoArchUnitCheckerTest {

    private final MangoArchUnitChecker checker = new MangoArchUnitChecker();

    @Test
    void compliantControllerPasses() {
        JavaClasses classes = importClasses(OrderController.class, OrderApi.class, IOrderService.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.STARTER)).isEmpty();
    }

    @Test
    void restControllerAdviceIsNotTreatedAsController() {
        JavaClasses classes = importClasses(GlobalExceptionHandler.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.OTHER)).isEmpty();
    }

    @Test
    void controllerWithoutApiInCoreAndWithMapperIsRejected() {
        JavaClasses classes = importClasses(BadController.class, OrderMapper.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly(
                        "MANGO-ARCH-TYPE-001",
                        "MANGO-ARCH-TYPE-002",
                        "MANGO-ARCH-TYPE-003");
    }

    @Test
    void serviceImplementingHttpApiIsRejected() {
        JavaClasses classes = importClasses(BadServiceImpl.class, OrderApi.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-TYPE-005", "MANGO-ARCH-TYPE-008");
    }

    @Test
    void missingClassDirectoryFailsClosed() {
        assertThatThrownBy(() -> checker.check(Map.of(
                Path.of("target/does-not-exist"), ModuleRole.CORE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MANGO-ARCH-ENGINE-003");
    }

    @Test
    void feignWithInvalidContractAndPropertiesIsRejected() {
        JavaClasses classes = importClasses(BadFeignClient.class, OrderApi.class, ExtraApi.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.STARTER_REMOTE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly(
                        "MANGO-ARCH-FEIGN-002",
                        "MANGO-ARCH-FEIGN-003",
                        "MANGO-ARCH-FEIGN-004");
    }

    @Test
    void validFeignContractPasses() {
        JavaClasses classes = importClasses(OrderFeignClient.class, OrderApi.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.STARTER_REMOTE)).isEmpty();
    }

    @Test
    void explicitlyRegisteredReverseControllerPassesPlacementRule() {
        JavaClasses classes = importClasses(ReverseController.class, OrderApi.class);
        MangoArchUnitChecker configured = new MangoArchUnitChecker(
                java.util.Set.of(ReverseController.class.getName()));

        assertThat(configured.check(classes, ignored -> ModuleRole.STARTER_REMOTE)).isEmpty();
    }

    @Test
    void standardWorktreeAndOrdinaryPathsProduceIdenticalResults(@TempDir Path temporaryDirectory)
            throws URISyntaxException, IOException {
        Path testClasses = Path.of(MangoArchUnitCheckerTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        Path ordinaryClasses = temporaryDirectory.resolve("ordinary/target/test-classes");
        copyTree(testClasses, ordinaryClasses);

        assertThat(testClasses.toString()).contains(".mango/worktrees");
        List<ArchitectureIssue> worktreeIssues = checker.check(Map.of(testClasses, ModuleRole.OTHER));
        List<ArchitectureIssue> ordinaryIssues = checker.check(Map.of(ordinaryClasses, ModuleRole.OTHER));
        assertThat(worktreeIssues).isNotEmpty().isEqualTo(ordinaryIssues);
    }

    private void copyTree(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private JavaClasses importClasses(Class<?>... classes) {
        return new ClassFileImporter().importClasses(List.of(classes));
    }

    interface OrderApi {
    }

    interface IOrderService {
    }

    interface ExtraApi {
    }

    interface OrderMapper {
    }

    @RestController
    static final class OrderController implements OrderApi {
        private final IOrderService orderService = null;
    }

    @RestControllerAdvice
    static final class GlobalExceptionHandler {
    }

    @RestController
    static final class BadController {
        private final OrderMapper orderMapper = null;
    }

    @Service
    static final class BadServiceImpl implements OrderApi {
    }

    @FeignClient(name = "", contextId = "", path = "relative")
    interface BadFeignClient extends OrderApi, ExtraApi {
    }

    @FeignClient(name = "order", contextId = "orderClient", path = "/internal/orders")
    interface OrderFeignClient extends OrderApi {
    }

    @RestController
    static final class ReverseController implements OrderApi {
    }
}
