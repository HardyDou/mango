package io.mango.architecture.pmd;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.reporting.Report;
import org.junit.jupiter.api.Test;

class MangoJavaArchitectureRuleTest {

    @Test
    void serviceReturningRIsRejectedByPmdAst() {
        Report report = analyze("OrderServiceImpl.java", """
                package example;
                final class R<T> {}
                final class OrderServiceImpl {
                    R<String> create() { return null; }
                }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-SVC-001 Service must not return R<T>");
        assertThat(report.getProcessingErrors()).isEmpty();
    }

    @Test
    void mapperAnnotationSqlIsRejectedByPmdAst() {
        Report report = analyze("OrderMapper.java", """
                package example;
                import org.apache.ibatis.annotations.Select;
                interface OrderMapper {
                    @Select("select 1") Object find();
                }
                """, "org/apache/ibatis/annotations/Select.java", """
                package org.apache.ibatis.annotations;
                public @interface Select { String value(); }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-MAPPER-001 Mapper must not declare annotation SQL");
        assertThat(report.getProcessingErrors()).isEmpty();
    }

    @Test
    void invalidJavaFailsClosedAsProcessingError() {
        Report report = analyze("BrokenServiceImpl.java", "class BrokenServiceImpl {");

        assertThat(report.getProcessingErrors()).isNotEmpty();
    }

    @Test
    void controllerValidationAndHttpReturnAreCheckedByResolvedAnnotations() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                final class OrderController {
                    @PostMapping Object create(@RequestBody Object body) { return body; }
                }
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springframework/web/bind/annotation/PostMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface PostMapping {}
                """,
                "org/springframework/web/bind/annotation/RequestBody.java", """
                package org.springframework.web.bind.annotation;
                public @interface RequestBody {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-CTRL-001 Controller requires @Validated",
                "MANGO-ARCH-CTRL-003 @RequestBody parameter requires @Valid",
                "MANGO-ARCH-HTTP-001 HTTP method must return R<T>");
        assertThat(report.getProcessingErrors()).isEmpty();
    }

    @Test
    void requireWithBusinessCodePassesAndStringCodeFails() {
        Report report = analyze("example/PaymentServiceImpl.java", """
                package example;
                interface BizCode {}
                enum PaymentCode implements BizCode { INVALID }
                final class Require {
                    static void notNull(Object value, BizCode code) {}
                    static void notNull(Object value, String code) {}
                    static <T> T fail(BizCode code) { return null; }
                }
                final class PaymentServiceImpl {
                    public void valid(Object value) { Require.notNull(value, PaymentCode.INVALID); }
                    public Object validFail() { return Require.fail(PaymentCode.INVALID); }
                    public void invalid(Object value) { Require.notNull(value, "INVALID"); }
                }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-SVC-003 Require must receive a BizCode/ErrorCode; resolved=java.lang.String");
        assertThat(report.getProcessingErrors()).isEmpty();
    }

    @Test
    void businessActionWithoutRequireIsRejected() {
        Report report = analyze("example/OrderServiceImpl.java", """
                package example;
                final class OrderServiceImpl {
                    public void createOrder(Object command) { System.out.println(command); }
                }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-SVC-004 business action requires a Require precondition");
    }

    @Test
    void compliantControllerProducesNoPmdViolations() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import jakarta.validation.Valid;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;
                final class R<T> {}
                @Validated @RestController
                final class OrderController {
                    @PostMapping R<String> create(@Valid @RequestBody Object body) { return null; }
                }
                """,
                "jakarta/validation/Valid.java", """
                package jakarta.validation;
                public @interface Valid {}
                """,
                "org/springframework/validation/annotation/Validated.java", """
                package org.springframework.validation.annotation;
                public @interface Validated {}
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springframework/web/bind/annotation/PostMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface PostMapping {}
                """,
                "org/springframework/web/bind/annotation/RequestBody.java", """
                package org.springframework.web.bind.annotation;
                public @interface RequestBody {}
                """);

        assertThat(messages(report)).isEmpty();
        assertThat(report.getProcessingErrors()).isEmpty();
    }

    @Test
    void serviceCallingRMethodsIsRejected() {
        Report report = analyze("example/OrderServiceImpl.java", """
                package example;
                final class R<T> {
                    static <T> R<T> ok(T value) { return null; }
                    static <T> R<T> fail(String value) { return null; }
                }
                final class OrderServiceImpl {
                    void create() { R.ok("ok"); R.fail("bad"); }
                }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-SVC-002 Service must not call R methods",
                "MANGO-ARCH-SVC-002 Service must not call R methods");
    }

    @Test
    void httpApiExposingEntityIsRejected() {
        Report report = analyze(
                "example/OrderApi.java", """
                package example;
                import org.springframework.web.bind.annotation.GetMapping;
                final class R<T> {}
                final class OrderEntity {}
                interface OrderApi {
                    @GetMapping R<OrderEntity> get();
                }
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping {}
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-HTTP-002 HTTP method must not expose Entity/PO");
    }

    @Test
    void mapperWithoutAnnotationSqlOrApiModelPasses() {
        Report report = analyze("example/OrderMapper.java", """
                package example;
                final class OrderRecord {}
                interface OrderMapper { OrderRecord find(long id); }
                """);

        assertThat(messages(report)).isEmpty();
    }

    private Report analyze(String... pathAndSource) {
        PMDConfiguration configuration = new PMDConfiguration();
        configuration.setDefaultLanguageVersion(
                LanguageRegistry.PMD.getLanguageVersionById("java", "21"));
        configuration.setIgnoreIncrementalAnalysis(true);
        try (PmdAnalysis analysis = PmdAnalysis.create(configuration)) {
            analysis.addRuleSet(RuleSet.forSingleRule(new MangoJavaArchitectureRule()));
            for (int index = 0; index < pathAndSource.length; index += 2) {
                analysis.files().addSourceFile(
                        FileId.fromPathLikeString(pathAndSource[index]), pathAndSource[index + 1]);
            }
            return analysis.performAnalysisAndCollectReport();
        }
    }

    private List<String> messages(Report report) {
        List<String> messages = new ArrayList<>();
        report.getViolations().forEach(violation -> messages.add(violation.getDescription()));
        return messages;
    }
}
