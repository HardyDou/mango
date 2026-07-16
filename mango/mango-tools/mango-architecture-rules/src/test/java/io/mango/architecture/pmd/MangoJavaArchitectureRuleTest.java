package io.mango.architecture.pmd;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.io.File;
import io.mango.common.result.Require;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.reporting.Report;
import org.junit.jupiter.api.Test;

class MangoJavaArchitectureRuleTest {

    @Test
    void canonicalResultAndPreconditionContractsCannotBeSpoofedBySimpleName() {
        Report report = analyze(
                "evil/R.java", """
                package evil;
                public final class R<T> {}
                """,
                "evil/BizCode.java", """
                package evil;
                public interface BizCode {}
                """,
                "evil/OrderCode.java", """
                package evil;
                public enum OrderCode implements BizCode { INVALID }
                """,
                "evil/Require.java", """
                package evil;
                public final class Require {
                    public static void notNull(Object value, BizCode code) {}
                }
                """,
                "example/api/OrderApi.java", """
                package example.api;
                import evil.R;
                public interface OrderApi { R<String> detail(); }
                """,
                "example/core/service/SpoofedService.java", """
                package example.core.service;
                import evil.OrderCode;
                import evil.Require;
                public final class SpoofedService {
                    public void create(Object command) {
                        Require.notNull(command, OrderCode.INVALID);
                    }
                }
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-HTTP-001 HTTP method must return R<T>",
                "MANGO-ARCH-SVC-004 business action requires a Require precondition");
    }

    @Test
    void serviceReturningRIsRejectedByPmdAst() {
        Report report = analyze("OrderService.java", """
                package example;
                import io.mango.common.result.R;
                final class OrderService {
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
                    @Select("select 1") int find();
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
        Report report = analyze("BrokenService.java", "class BrokenService {");

        assertThat(report.getProcessingErrors()).isNotEmpty();
    }

    @Test
    void controllerValidationAndHttpReturnAreCheckedByResolvedAnnotations() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;
                @Tag(name = "订单", description = "订单管理") @RestController
                final class OrderController {
                    @Operation(summary = "创建订单", description = "创建一条订单")
                    @PostMapping Object create(@RequestBody Object body) { return body; }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
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
                "MANGO-ARCH-CTRL-004 HTTP Controller must directly return canonical R.ok(...)",
                "MANGO-ARCH-HTTP-001 HTTP method must return R<T>");
        assertThat(report.getProcessingErrors()).isEmpty();
    }

    @Test
    void controllerImplementationInheritsParameterValidationFromApiContract() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.mango.architecture.pmd.fixture.CreateOrderCommand;
                import io.mango.architecture.pmd.fixture.ValidatedOrderApi;
                import io.mango.common.result.R;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;
                @Validated
                @Tag(name = "订单", description = "订单管理")
                @RestController
                final class OrderController implements ValidatedOrderApi {
                    @Operation(summary = "创建订单", description = "创建一条订单")
                    @PostMapping
                    public R<String> create(@RequestBody CreateOrderCommand command) {
                        return R.ok("ok");
                    }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
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

        assertThat(messages(report))
                .doesNotContain("MANGO-ARCH-CTRL-003 @RequestBody parameter requires @Valid");
        assertThat(report.getProcessingErrors()).isEmpty();
    }

    @Test
    void controllerImplementationMustNotRepeatApiOwnedValid() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.mango.architecture.pmd.fixture.CreateOrderCommand;
                import io.mango.architecture.pmd.fixture.ValidatedOrderApi;
                import io.mango.common.result.R;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import jakarta.validation.Valid;
                import org.springdoc.core.annotations.ParameterObject;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;
                @Validated
                @Tag(name = "订单", description = "订单管理")
                @RestController
                final class OrderController implements ValidatedOrderApi {
                    @Override
                    @Operation(summary = "查询订单", description = "按查询条件获取订单")
                    @GetMapping
                    public R<String> create(@Valid @ParameterObject CreateOrderCommand command) {
                        return R.ok("ok");
                    }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
                """,
                "org/springframework/validation/annotation/Validated.java", """
                package org.springframework.validation.annotation;
                public @interface Validated {}
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springdoc/core/annotations/ParameterObject.java", """
                package org.springdoc.core.annotations;
                public @interface ParameterObject {}
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping {}
                """);

        assertThat(messages(report))
                .contains("MANGO-ARCH-CTRL-003 overriding Controller parameter must not repeat API-owned @Valid");
        assertThat(report.getProcessingErrors()).isEmpty();
    }

    @Test
    void requireWithBusinessCodePassesAndStringCodeFails() {
        Report report = analyze(
                "example/core/service/PaymentService.java", """
                package example.core.service;
                import example.api.enums.PaymentCode;
                import io.mango.common.result.Require;
                public final class PaymentService {
                    public void valid(Object value) { Require.notNull(value, PaymentCode.INVALID); }
                    public void range(long value) { Require.inRange(value, 1, 10, PaymentCode.INVALID); }
                    public Object validFail() { return Require.fail(PaymentCode.INVALID); }
                    public Object validRethrow(RuntimeException exception) { return Require.rethrow(exception); }
                    public void invalid(Object value) { Require.notNull(value, "INVALID"); }
                }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-SVC-003 Require must receive a module XxxCode implementing BizCode; resolved=<missing>");
        assertThat(report.getProcessingErrors()).isEmpty();
    }

    @Test
    void businessActionWithoutRequireIsRejected() {
        Report report = analyze("example/OrderService.java", """
                package example;
                final class OrderService {
                    public void createOrder(Object command) { System.out.println(command); }
                }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-SVC-004 business action requires a Require precondition");
    }

    @Test
    void commandServiceMethodRequiresRequireRegardlessOfVerbName() {
        Report report = analyze("example/PaymentService.java", """
                package example;
                final class PaymentCommand {}
                final class PaymentService {
                    public void handle(PaymentCommand command) { System.out.println(command); }
                }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-SVC-004 business action requires a Require precondition");
    }

    @Test
    void deleteCommandMustUseRequestBody() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.mango.common.result.R;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.Parameter;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import jakarta.validation.Valid;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.DeleteMapping;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;
                final class BatchDeleteCommand {}
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class OrderController {
                    @Operation(summary = "批量删除", description = "批量删除订单")
                    @DeleteMapping R<Void> delete(
                            @Valid @Parameter(description = "删除参数") @RequestParam
                            BatchDeleteCommand command) { return R.ok(); }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/Parameter.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Parameter { String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
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
                "org/springframework/web/bind/annotation/DeleteMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface DeleteMapping {}
                """,
                "org/springframework/web/bind/annotation/RequestParam.java", """
                package org.springframework.web.bind.annotation;
                public @interface RequestParam {}
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-CTRL-007 write Command/Request requires @RequestBody");
    }

    @Test
    void adapterMethodCannotUseGenericRequestMapping() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.mango.common.result.R;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class OrderController {
                    @Operation(summary = "订单详情", description = "查询订单详情")
                    @RequestMapping R<String> detail() { return R.ok("ok"); }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
                """,
                "org/springframework/validation/annotation/Validated.java", """
                package org.springframework.validation.annotation;
                public @interface Validated {}
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springframework/web/bind/annotation/RequestMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface RequestMapping {}
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-ADAPTER-004 adapter methods must use a concrete HTTP verb annotation");
    }

    @Test
    void compliantControllerProducesNoPmdViolations() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import jakarta.validation.Valid;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;
                import io.mango.common.result.R;
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class OrderController {
                    @Operation(summary = "创建订单", description = "创建一条订单")
                    @PostMapping R<String> create(@Valid @RequestBody Object body) { return R.ok("ok"); }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
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
    void apiControllerMustReturnTheDirectServiceResult() {
        Report report = analyze(
                "example/OrderControllers.java", """
                package example;
                import io.mango.common.result.R;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;
                interface OrderApi { R<String> detail(); }
                interface IOrderService { String detail(); }
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class NoServiceController implements OrderApi {
                    @Operation(summary = "订单详情", description = "查询订单详情")
                    @GetMapping R<String> detail() { return R.ok("hardcoded"); }
                }
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class HardcodedController implements OrderApi {
                    private IOrderService orderService;
                    @Operation(summary = "订单详情", description = "查询订单详情")
                    @GetMapping public R<String> detail() { return R.ok("hardcoded"); }
                }
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class DummyCallController implements OrderApi {
                    private IOrderService orderService;
                    @Operation(summary = "订单详情", description = "查询订单详情")
                    @GetMapping public R<String> detail() {
                        orderService.detail();
                        return R.ok("hardcoded");
                    }
                }
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class DelegatingController implements OrderApi {
                    private IOrderService orderService;
                    @Operation(summary = "订单详情", description = "查询订单详情")
                    @GetMapping public R<String> detail() { return R.ok(orderService.detail()); }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
                """,
                "org/springframework/validation/annotation/Validated.java", """
                package org.springframework.validation.annotation;
                public @interface Validated {}
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping {}
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-CTRL-013 API Controller must return service-interface results without hardcoded payloads",
                "MANGO-ARCH-CTRL-013 API Controller must return service-interface results without hardcoded payloads",
                "MANGO-ARCH-CTRL-013 API Controller must return service-interface results without hardcoded payloads");
    }

    @Test
    void serviceCallingRMethodsIsRejected() {
        Report report = analyze("example/OrderService.java", """
                package example;
                import io.mango.common.result.R;
                final class OrderService {
                    void create() { R.ok("ok"); R.fail("bad"); }
                }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-SVC-002 Service must not call R methods",
                "MANGO-ARCH-SVC-002 Service must not call R methods");
    }

    @Test
    void httpApiExposingEntityIsRejected() {
        Report report = analyze("example/OrderApi.java", """
                package example;
                import io.mango.common.result.R;
                final class OrderEntity {}
                interface OrderApi {
                    R<OrderEntity> get();
                }
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

    @Test
    void mapperCannotUseObjectOrDynamicMapContracts() {
        Report report = analyze("example/OrderMapper.java", """
                package example;
                import java.util.Map;
                interface OrderMapper {
                    Map<String, Object> detail(Object input);
                }
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-MAPPER-007 Mapper parameters must use typed persistence contracts",
                "MANGO-ARCH-MAPPER-007 Mapper results must use typed persistence contracts");
    }

    @Test
    void pathVariablesAndUriTemplatesAreRejected() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.RestController;
                import io.mango.common.result.R;
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class OrderController {
                    @Operation(summary = "订单详情", description = "查询订单详情")
                    @GetMapping("/{id}") R<String> detail(@PathVariable Long id) { return R.ok("ok"); }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
                """,
                "org/springframework/validation/annotation/Validated.java", """
                package org.springframework.validation.annotation;
                public @interface Validated {}
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping { String[] value() default {}; }
                """,
                "org/springframework/web/bind/annotation/PathVariable.java", """
                package org.springframework.web.bind.annotation;
                public @interface PathVariable {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-PATH-001 @PathVariable is forbidden; use query parameter or command",
                "MANGO-ARCH-PATH-002 URI template variables are forbidden in new APIs");
    }

    @Test
    void uriTemplateInCompileTimeConstantIsRejected() {
        Report report = analyze(
                "example/Routes.java", """
                package example;
                import org.springframework.web.bind.annotation.GetMapping;
                final class Routes {
                    private static final String DETAIL = "/" + "{id}";
                    @GetMapping(DETAIL) void detail() {}
                }
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping { String[] value() default {}; }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-PATH-002 URI template variables are forbidden in new APIs");
    }

    @Test
    void springPropertyPlaceholderIsRejectedBecauseRuntimeCanInjectUriTemplate() {
        Report report = analyze(
                "example/ConfiguredEndpoint.java", """
                package example;
                import org.springframework.web.bind.annotation.GetMapping;
                final class ConfiguredEndpoint {
                    @GetMapping("${mango.endpoint:/events}") void event() {}
                }
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping { String[] value() default {}; }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-PATH-003 runtime path placeholders are forbidden in HTTP adapters");
    }

    @Test
    void feignRuntimePathPlaceholderIsRejected() {
        Report report = analyze(
                "example/OrderFeignClient.java", """
                package example;
                import org.springframework.cloud.openfeign.FeignClient;
                @FeignClient(path = "${order.path:/orders}")
                interface OrderFeignClient {}
                """,
                "org/springframework/cloud/openfeign/FeignClient.java", """
                package org.springframework.cloud.openfeign;
                public @interface FeignClient { String path(); }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-PATH-003 runtime path placeholders are forbidden in HTTP adapters");
    }

    @Test
    void protocolModelsCannotHidePersistenceOrArbitraryPayloadFields() {
        Report report = analyze(
                "example/api/model/Models.java", """
                package example.api.model;
                import io.swagger.v3.oas.annotations.media.Schema;
                import jakarta.validation.constraints.NotNull;
                import java.util.Map;
                final class OrderEntity {}
                final class CreateOrderCommand {
                    @Schema(description = "订单实体") @NotNull
                    private OrderEntity entity;
                }
                final class OrderVO {
                    @Schema(description = "订单实体") private OrderEntity entity;
                    @Schema(description = "任意负载") private Object payload;
                    @Schema(description = "动态属性") private Map<String, Object> attributes;
                }
                """,
                "io/swagger/v3/oas/annotations/media/Schema.java", """
                package io.swagger.v3.oas.annotations.media;
                public @interface Schema { String description(); }
                """,
                "jakarta/validation/constraints/NotNull.java", """
                package jakarta.validation.constraints;
                public @interface NotNull {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-MODEL-004 input model field must be scalar, enum, nested input model or supported container",
                "MANGO-ARCH-MODEL-005 output model field must be scalar, enum, VO/Response or supported container",
                "MANGO-ARCH-MODEL-005 output model field must be scalar, enum, VO/Response or supported container",
                "MANGO-ARCH-MODEL-005 output model field must be scalar, enum, VO/Response or supported container");
    }

    @Test
    void protocolModelsCannotHidePayloadInCollectionInheritance() {
        Report report = analyze(
                "example/api/model/InheritedModels.java", """
                package example.api.model;
                import java.util.ArrayList;
                import java.util.HashMap;
                final class OrderEntity {}
                final class HashMapVO extends HashMap<String, Object> {}
                final class CreateOrderCommand extends ArrayList<OrderEntity> {}
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-MODEL-006 protocol model must be a class with only protocol-model inheritance",
                "MANGO-ARCH-MODEL-006 protocol model must be a class with only protocol-model inheritance");
    }

    @Test
    void feignPathCannotContainUriTemplate() {
        Report report = analyze(
                "example/OrderFeignClient.java", """
                package example;
                import org.springframework.cloud.openfeign.FeignClient;
                @FeignClient(path = "/orders/{tenant}")
                interface OrderFeignClient {}
                """,
                "org/springframework/cloud/openfeign/FeignClient.java", """
                package org.springframework.cloud.openfeign;
                public @interface FeignClient { String path(); }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-PATH-002 URI template variables are forbidden in new APIs");
    }

    @Test
    void writeCommandRequestBodyCannotBeOptional() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.mango.common.result.R;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import jakarta.validation.Valid;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;
                final class CreateOrderCommand {}
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class OrderController {
                    @Operation(summary = "创建订单", description = "创建一条订单")
                    @PostMapping R<String> create(
                            @Valid @RequestBody(required = false) CreateOrderCommand command) {
                        return R.ok("ok");
                    }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
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
                public @interface RequestBody { boolean required() default true; }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-CTRL-007 write Command/Request body must be required");
    }

    @Test
    void controllerAndServiceCannotConstructResultObjectsDirectly() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.mango.common.result.R;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class OrderController {
                    @Operation(summary = "订单详情", description = "查询订单详情")
                    @GetMapping R<String> detail() { return new R<>(); }
                    @Operation(summary = "空订单", description = "禁止空结果")
                    @GetMapping R<String> empty() { return null; }
                    @Operation(summary = "失败订单", description = "禁止控制器抛异常")
                    @GetMapping R<String> fail() { throw new IllegalStateException("bad"); }
                }
                """,
                "example/core/service/OrderService.java", """
                package example.core.service;
                import io.mango.common.result.R;
                final class OrderService {
                    public Object wrap() { return new R<>(); }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
                """,
                "org/springframework/validation/annotation/Validated.java", """
                package org.springframework.validation.annotation;
                public @interface Validated {}
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-CTRL-004 Controller may only construct success results with R.ok",
                "MANGO-ARCH-CTRL-004 HTTP Controller must directly return canonical R.ok(...)",
                "MANGO-ARCH-CTRL-010 Controller must delegate business failures to Service Require",
                "MANGO-ARCH-SVC-002 Service must not construct R instances");
    }

    @Test
    void pureApiContractStillRequiresRAndRejectsNestedEntity() {
        Report report = analyze("example/OrderApi.java", """
                package example;
                import io.mango.common.result.R;
                final class PageResult<T> {}
                final class OrderEntity {}
                interface OrderApi {
                    Object bare();
                    R<PageResult<OrderEntity>> detail();
                    R<String> valid();
                }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-HTTP-001 HTTP method must return R<T>",
                "MANGO-ARCH-HTTP-002 HTTP method must not expose Entity/PO");
    }

    @Test
    void markedInfraLocalCapabilitySkipsHttpProtocolRules() {
        Report report = analyze(
                "io/mango/infra/fileproc/LocalConvertApi.java", """
                package io.mango.infra.fileproc;
                import io.mango.common.contract.LocalCapabilityContract;
                import java.io.InputStream;
                @LocalCapabilityContract
                public interface LocalConvertApi {
                    byte[] convert(InputStream input);
                }
                """,
                "io/mango/infra/fileproc/LocalConvertCommand.java", """
                package io.mango.infra.fileproc;
                import io.mango.common.contract.LocalCapabilityContract;
                import java.io.InputStream;
                @LocalCapabilityContract
                public record LocalConvertCommand(InputStream input) {}
                """);

        assertThat(messages(report)).isEmpty();
    }

    @Test
    void markedInfraLocalCapabilityServiceSkipsBusinessServiceRules() {
        Report report = analyze(
                "io/mango/infra/crypto/ICryptoService.java", """
                package io.mango.infra.crypto;
                import io.mango.common.contract.LocalCapabilityContract;
                @LocalCapabilityContract
                public interface ICryptoService {
                    String encrypt(String plaintext);
                }
                """,
                "io/mango/infra/crypto/Sm4CryptoService.java", """
                package io.mango.infra.crypto;
                import io.mango.common.contract.LocalCapabilityContract;
                @LocalCapabilityContract
                public final class Sm4CryptoService implements ICryptoService {
                    public String encrypt(String plaintext) {
                        if (plaintext == null) throw new IllegalArgumentException("required");
                        return plaintext;
                    }
                }
                """);

        assertThat(messages(report)).isEmpty();
    }

    @Test
    void markerOutsideInfraDoesNotBypassBusinessServiceRules() {
        Report report = analyze(
                "example/MarkedOrderService.java", """
                package example;
                import io.mango.common.contract.LocalCapabilityContract;
                @LocalCapabilityContract
                public final class MarkedOrderService {
                    public void create(Object command) {
                        if (command == null) throw new IllegalArgumentException("required");
                    }
                }
                """);

        assertThat(messages(report)).contains(
                "MANGO-ARCH-SVC-004 business action requires a Require precondition",
                "MANGO-ARCH-SVC-006 Service business failures must use Require, not throw directly");
    }

    @Test
    void localCapabilityMarkerOutsideInfraDoesNotBypassHttpProtocolRules() {
        Report report = analyze(
                "example/LocalConvertApi.java", """
                package example;
                import io.mango.common.contract.LocalCapabilityContract;
                import java.io.InputStream;
                @LocalCapabilityContract
                public interface LocalConvertApi {
                    byte[] convert(InputStream input);
                }
                """,
                "example/LocalConvertCommand.java", """
                package example;
                import io.mango.common.contract.LocalCapabilityContract;
                import java.io.InputStream;
                @LocalCapabilityContract
                public record LocalConvertCommand(InputStream input) {}
                """);

        assertThat(messages(report)).contains(
                "MANGO-ARCH-HTTP-001 HTTP method must return R<T>",
                "MANGO-ARCH-API-006 API input must be a scalar, enum, Command, Query or Request",
                "MANGO-ARCH-MODEL-003 protocol records are forbidden; use an explicit class");
    }

    @Test
    void markedLocalCapabilityExposedByHttpControllerStillRequiresHttpContract() {
        Report report = analyze(
                "io/mango/infra/fileproc/LocalConvertApi.java", """
                package io.mango.infra.fileproc;
                import io.mango.common.contract.LocalCapabilityContract;
                @LocalCapabilityContract
                public interface LocalConvertApi {
                    byte[] convert(byte[] input);
                }
                """,
                "io/mango/infra/fileproc/LocalConvertController.java", """
                package io.mango.infra.fileproc;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                public final class LocalConvertController implements LocalConvertApi {
                    @GetMapping
                    public byte[] convert(byte[] input) { return input; }
                }
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping {}
                """);

        assertThat(messages(report)).contains(
                "MANGO-ARCH-HTTP-001 HTTP method must return R<T>",
                "MANGO-ARCH-CTRL-004 HTTP Controller must directly return canonical R.ok(...)");
    }

    @Test
    void explicitlyMarkedBinaryControllerKeepsStreamingWireContract() {
        Report report = analyze(
                "example/BinaryObjectController.java", """
                package example;
                import io.mango.common.contract.BinaryHttpAdapter;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.RestController;
                @BinaryHttpAdapter @Validated @RestController
                @Tag(name = "文件对象", description = "文件对象下载")
                public final class BinaryObjectController {
                    @GetMapping("/{objectName}")
                    @Operation(summary = "下载文件", description = "流式下载文件对象")
                    public byte[] download(@PathVariable String objectName) { return new byte[0]; }
                }
                """,
                "io/mango/common/contract/BinaryHttpAdapter.java", """
                package io.mango.common.contract;
                public @interface BinaryHttpAdapter {}
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping { String[] value() default {}; }
                """,
                "org/springframework/web/bind/annotation/PathVariable.java", """
                package org.springframework.web.bind.annotation;
                public @interface PathVariable {}
                """,
                "org/springframework/validation/annotation/Validated.java", """
                package org.springframework.validation.annotation;
                public @interface Validated {}
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """);

        assertThat(messages(report)).isEmpty();
    }

    @Test
    void controllerConcreteServiceAndApiFieldsAreRejected() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.RestController;
                final class OrderService {}
                interface PricingApi {}
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class OrderController {
                    private OrderService orderService;
                    private PricingApi pricingApi;
                }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
                """,
                "org/springframework/validation/annotation/Validated.java", """
                package org.springframework.validation.annotation;
                public @interface Validated {}
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-CTRL-002 Controller must depend on a service interface: example.OrderService",
                "MANGO-ARCH-CTRL-002 Controller must depend on a service interface: example.PricingApi");
    }

    @Test
    void controllerOpenApiAnnotationsAreRequired() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.ModelAttribute;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;
                import io.mango.common.result.R;
                final class OrderPageQuery {}
                @Validated @RestController
                final class OrderController {
                    @GetMapping R<String> find(
                            @RequestParam Long id,
                            @ModelAttribute OrderPageQuery query) { return R.ok("ok"); }
                }
                """,
                "org/springframework/validation/annotation/Validated.java", """
                package org.springframework.validation.annotation;
                public @interface Validated {}
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping {}
                """,
                "org/springframework/web/bind/annotation/ModelAttribute.java", """
                package org.springframework.web.bind.annotation;
                public @interface ModelAttribute {}
                """,
                "org/springframework/web/bind/annotation/RequestParam.java", """
                package org.springframework.web.bind.annotation;
                public @interface RequestParam {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-OPENAPI-001 Controller requires @Tag",
                "MANGO-ARCH-OPENAPI-002 HTTP method requires @Operation",
                "MANGO-ARCH-OPENAPI-003 simple HTTP parameter requires @Parameter",
                "MANGO-ARCH-OPENAPI-004 Query parameter requires @ParameterObject");
    }

    @Test
    void voidHttpMethodAndNonSuccessResultFactoryAreRejected() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RestController;
                import io.mango.common.result.R;
                @Tag(name = "订单", description = "订单管理") @Validated @RestController
                final class OrderController {
                    @Operation(summary = "导出订单", description = "导出订单数据") @GetMapping void export() {}
                    @Operation(summary = "创建订单", description = "创建一条订单")
                    @PostMapping R<String> create() { return R.fail("bad"); }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
                """,
                "org/springframework/validation/annotation/Validated.java", """
                package org.springframework.validation.annotation;
                public @interface Validated {}
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping {}
                """,
                "org/springframework/web/bind/annotation/PostMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface PostMapping {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-HTTP-001 HTTP/API method must return R<T>",
                "MANGO-ARCH-CTRL-004 Controller may only construct success results with R.ok");
    }

    @Test
    void serviceImplNamingAndDirectThrowAreRejected() {
        Report report = analyze("example/OrderServiceImpl.java", """
                package example;
                final class OrderServiceImpl {
                    public void process(Object command) { throw new IllegalStateException("bad"); }
                }
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-SVC-005 Service implementation must be named XxxService, not XxxServiceImpl",
                "MANGO-ARCH-SVC-004 business action requires a Require precondition",
                "MANGO-ARCH-SVC-006 Service business failures must use Require, not throw directly");
    }

    @Test
    void commandFieldsRequireSchemaAndValidationConstraint() {
        Report report = analyze(
                "example/CreateOrderCommand.java", """
                package example;
                import io.swagger.v3.oas.annotations.media.Schema;
                import jakarta.validation.constraints.NotBlank;
                final class CreateOrderCommand {
                    private String name;
                    @Schema(description = "编码") @NotBlank private String code;
                }
                """,
                "io/swagger/v3/oas/annotations/media/Schema.java", """
                package io.swagger.v3.oas.annotations.media;
                public @interface Schema { String description(); }
                """,
                "jakarta/validation/constraints/NotBlank.java", """
                package jakarta.validation.constraints;
                public @interface NotBlank {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-MODEL-001 API model field requires @Schema(description)",
                "MANGO-ARCH-MODEL-002 Command/Query/Request field requires a jakarta.validation constraint");
    }

    @Test
    void optionalNestedCommandUsesCascadeValidationWithoutBecomingRequired() {
        Report report = analyze(
                "example/CreateOrderCommand.java", """
                package example;
                import io.swagger.v3.oas.annotations.media.Schema;
                import jakarta.validation.Valid;
                import jakarta.validation.constraints.NotBlank;
                final class OrderExtensionCommand {
                    @Schema(description = "业务编码") @NotBlank private String businessCode;
                }
                final class CreateOrderCommand {
                    @Schema(description = "扩展信息") @Valid
                    private OrderExtensionCommand extension;
                    @Schema(description = "普通编号") @Valid
                    private Long plainId;
                }
                """,
                "io/swagger/v3/oas/annotations/media/Schema.java", """
                package io.swagger.v3.oas.annotations.media;
                public @interface Schema { String description(); }
                """,
                "jakarta/validation/Valid.java", """
                package jakarta.validation;
                public @interface Valid {}
                """,
                "jakarta/validation/constraints/NotBlank.java", """
                package jakarta.validation.constraints;
                public @interface NotBlank {}
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-MODEL-002 Command/Query/Request field requires a jakarta.validation constraint");
    }

    @Test
    void apiContractTransportAnnotationsAreRejected() {
        Report report = analyze(
                "example/OrderApi.java", """
                package example;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import jakarta.validation.constraints.NotNull;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RequestParam;
                import io.mango.common.result.R;
                @Tag @RequestMapping
                interface OrderApi {
                    @Operation @GetMapping R<String> detail(@RequestParam @NotNull Long id);
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation {}
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag {}
                """,
                "jakarta/validation/constraints/NotNull.java", """
                package jakarta.validation.constraints;
                public @interface NotNull {}
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping {}
                """,
                "org/springframework/web/bind/annotation/RequestMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface RequestMapping {}
                """,
                "org/springframework/web/bind/annotation/RequestParam.java", """
                package org.springframework.web.bind.annotation;
                public @interface RequestParam {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-API-001 XxxApi must be transport-neutral; "
                        + "io.swagger.v3.oas.annotations.tags.Tag is forbidden",
                "MANGO-ARCH-API-001 XxxApi must be transport-neutral; "
                        + "org.springframework.web.bind.annotation.RequestMapping is forbidden",
                "MANGO-ARCH-API-001 XxxApi must be transport-neutral; "
                        + "io.swagger.v3.oas.annotations.Operation is forbidden",
                "MANGO-ARCH-API-001 XxxApi must be transport-neutral; "
                        + "org.springframework.web.bind.annotation.GetMapping is forbidden",
                "MANGO-ARCH-API-001 XxxApi must be transport-neutral; "
                        + "org.springframework.web.bind.annotation.RequestParam is forbidden");
    }

    @Test
    void apiContractParametersRequireBeanValidation() {
        Report report = analyze("example/OrderApi.java", """
                package example;
                import io.mango.common.result.R;
                final class CreateOrderCommand {}
                interface OrderApi {
                    R<String> create(CreateOrderCommand command);
                    R<String> detail(Long id);
                }
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-API-002 Command/Query/Request parameter requires @Valid",
                "MANGO-ARCH-API-002 simple API parameter requires a jakarta.validation constraint");
    }

    @Test
    void queryFieldsRequireSchemaAndValidationConstraint() {
        Report report = analyze(
                "example/OrderPageQuery.java", """
                package example;
                import io.swagger.v3.oas.annotations.media.Schema;
                final class OrderPageQuery {
                    private static final long serialVersionUID = 1L;
                    private static final int DEFAULT_PAGE = 1;
                    @Schema(description = "名称") private String name;
                }
                """,
                "io/swagger/v3/oas/annotations/media/Schema.java", """
                package io.swagger.v3.oas.annotations.media;
                public @interface Schema { String description(); }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-MODEL-002 Command/Query/Request field requires a jakarta.validation constraint");
    }

    @Test
    void apiContractRejectsServerTransportContext() {
        Report report = analyze(
                "example/OrderApi.java", """
                package example;
                import io.mango.common.result.R;
                import jakarta.validation.constraints.NotNull;
                import jakarta.servlet.http.HttpServletResponse;
                interface OrderApi {
                    R<String> export(@NotNull HttpServletResponse response);
                }
                """,
                "jakarta/validation/constraints/NotNull.java", """
                package jakarta.validation.constraints;
                public @interface NotNull {}
                """,
                "jakarta/servlet/http/HttpServletResponse.java", """
                package jakarta.servlet.http;
                public interface HttpServletResponse {}
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-API-003 XxxApi must not accept server transport context");
    }

    @Test
    void mapperReturningVoIsRejected() {
        Report report = analyze("example/OrderMapper.java", """
                package example;
                final class OrderVO {}
                interface OrderMapper { OrderVO find(long id); }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-MAPPER-003 Mapper must not return Command/Query/VO");
    }

    @Test
    void mapperNestedGenericApiInputIsRejected() {
        Report report = analyze("example/OrderMapper.java", """
                package example;
                import java.util.List;
                final class CreateOrderCommand {}
                interface OrderMapper { int save(List<? extends CreateOrderCommand> values); }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-MAPPER-002 Mapper must not accept API model: java.util.List");
    }

    @Test
    void apiShapeRejectsRawResultDynamicMapExcessParametersAndImplementation() {
        Report report = analyze(
                "example/api/OrderApi.java", """
                package example.api;
                import io.mango.common.result.R;
                import jakarta.validation.constraints.NotNull;
                import java.util.Map;
                public interface OrderApi {
                    R raw();
                    R<Map<String, Object>> payload();
                    default R<String> find(
                            @NotNull Long id, @NotNull String code, @NotNull Boolean active) {
                        return null;
                    }
                }
                """,
                "jakarta/validation/constraints/NotNull.java", """
                package jakarta.validation.constraints;
                public @interface NotNull {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-HTTP-004 raw R return type is forbidden",
                "MANGO-ARCH-HTTP-005 Map<String, Object> is forbidden in HTTP contracts",
                "MANGO-ARCH-API-004 API method must consolidate more than two client parameters",
                "MANGO-ARCH-API-005 XxxApi must declare abstract contract methods only");
    }

    @Test
    void apiRejectsArbitraryDomainModelsInInputAndResultPayload() {
        Report report = analyze(
                "example/api/OrderApi.java", """
                package example.api;
                import io.mango.common.result.R;
                import jakarta.validation.constraints.NotNull;
                final class UserAggregate {}
                interface OrderApi {
                    R<UserAggregate> save(@NotNull UserAggregate aggregate);
                }
                """,
                "jakarta/validation/constraints/NotNull.java", """
                package jakarta.validation.constraints;
                public @interface NotNull {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-API-006 API input must be a scalar, enum, Command, Query or Request",
                "MANGO-ARCH-HTTP-007 R payload must be a scalar, enum, VO, Response, collection or PageResult");
    }

    @Test
    void legacyJavaxValidDoesNotSatisfySpringBootThreeContract() {
        Report report = analyze(
                "example/api/OrderApi.java", """
                package example.api;
                import io.mango.common.result.R;
                import javax.validation.Valid;
                final class CreateOrderCommand {}
                interface OrderApi {
                    R<String> create(@Valid CreateOrderCommand command);
                }
                """,
                "javax/validation/Valid.java", """
                package javax.validation;
                public @interface Valid {}
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-API-002 Command/Query/Request parameter requires @Valid");
    }

    @Test
    void controllerHttpBindingsAndChineseOpenApiTextAreEnforced() {
        Report report = analyze(
                "example/OrderController.java", """
                package example;
                import io.mango.common.result.R;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import jakarta.validation.Valid;
                import org.springdoc.core.annotations.ParameterObject;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;
                final class FindOrderQuery {}
                final class CreateOrderCommand {}
                @Tag(name = "", description = "") @Validated @RestController
                final class OrderController {
                    @Operation(summary = "", description = "") @GetMapping
                    R<String> find(@ParameterObject @Valid @RequestBody FindOrderQuery query) { return R.ok("ok"); }
                    @Operation(summary = "", description = "") @PostMapping
                    R<String> create(CreateOrderCommand command) { return R.ok("ok"); }
                }
                """,
                "io/swagger/v3/oas/annotations/Operation.java", """
                package io.swagger.v3.oas.annotations;
                public @interface Operation { String summary(); String description(); }
                """,
                "io/swagger/v3/oas/annotations/tags/Tag.java", """
                package io.swagger.v3.oas.annotations.tags;
                public @interface Tag { String name(); String description(); }
                """,
                "jakarta/validation/Valid.java", """
                package jakarta.validation;
                public @interface Valid {}
                """,
                "org/springdoc/core/annotations/ParameterObject.java", """
                package org.springdoc.core.annotations;
                public @interface ParameterObject {}
                """,
                "org/springframework/validation/annotation/Validated.java", """
                package org.springframework.validation.annotation;
                public @interface Validated {}
                """,
                "org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """,
                "org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping {}
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
                "MANGO-ARCH-OPENAPI-005 @Tag requires non-blank Chinese name and description",
                "MANGO-ARCH-OPENAPI-006 @Operation requires non-blank Chinese summary and description",
                "MANGO-ARCH-OPENAPI-006 @Operation requires non-blank Chinese summary and description",
                "MANGO-ARCH-CTRL-006 GET must not declare @RequestBody",
                "MANGO-ARCH-CTRL-003 @RequestBody parameter requires @Valid",
                "MANGO-ARCH-CTRL-007 write Command/Request requires @RequestBody");
    }

    @Test
    void blankSchemaAndProtocolRecordAreRejected() {
        Report report = analyze(
                "example/CreateOrderCommand.java", """
                package example;
                import io.swagger.v3.oas.annotations.media.Schema;
                import jakarta.validation.constraints.NotNull;
                final class CreateOrderCommand {
                    @Schema(description = "") @NotNull private String name;
                }
                """,
                "example/RecordOrderCommand.java", """
                package example;
                record RecordOrderCommand(String name) {}
                """,
                "io/swagger/v3/oas/annotations/media/Schema.java", """
                package io.swagger.v3.oas.annotations.media;
                public @interface Schema { String description(); }
                """,
                "jakarta/validation/constraints/NotNull.java", """
                package jakarta.validation.constraints;
                public @interface NotNull {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-MODEL-001 API model field requires @Schema(description)",
                "MANGO-ARCH-MODEL-003 protocol records are forbidden; use an explicit class");
    }

    @Test
    void nestedApiInputsCannotHideCommandsEntitiesOrTransportContexts() {
        Report report = analyze(
                "example/api/OrderApi.java", """
                package example.api;
                import io.mango.common.result.R;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.validation.constraints.NotEmpty;
                import jakarta.validation.constraints.NotNull;
                import java.util.List;
                import java.util.Optional;
                final class CreateOrderCommand {}
                final class OrderEntity {}
                interface OrderApi {
                    R<String> create(@NotEmpty List<CreateOrderCommand> commands);
                    R<String> save(@NotEmpty List<OrderEntity> entities);
                    R<String> inspect(@NotNull Optional<HttpServletRequest> request);
                }
                """,
                "jakarta/validation/constraints/NotEmpty.java", """
                package jakarta.validation.constraints;
                public @interface NotEmpty {}
                """,
                "jakarta/validation/constraints/NotNull.java", """
                package jakarta.validation.constraints;
                public @interface NotNull {}
                """,
                "jakarta/servlet/http/HttpServletRequest.java", """
                package jakarta.servlet.http;
                public interface HttpServletRequest {}
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-API-006 API input must be a scalar, enum, Command, Query or Request",
                "MANGO-ARCH-HTTP-003 API method must not accept Entity/PO",
                "MANGO-ARCH-API-003 XxxApi must not accept server transport context");
    }

    @Test
    void nestedInputModelsRequireCascadeValidation() {
        Report report = analyze(
                "example/api/model/NestedCommands.java", """
                package example.api.model;
                import io.swagger.v3.oas.annotations.media.Schema;
                import jakarta.validation.constraints.NotEmpty;
                import jakarta.validation.constraints.NotNull;
                import java.util.List;
                final class ItemCommand {
                    @Schema(description = "商品名称") @NotNull private String name;
                }
                final class CreateOrderCommand {
                    @Schema(description = "商品列表") @NotEmpty
                    private List<ItemCommand> items;
                }
                """,
                "io/swagger/v3/oas/annotations/media/Schema.java", """
                package io.swagger.v3.oas.annotations.media;
                public @interface Schema { String description(); }
                """,
                "jakarta/validation/constraints/NotEmpty.java", """
                package jakarta.validation.constraints;
                public @interface NotEmpty {}
                """,
                "jakarta/validation/constraints/NotNull.java", """
                package jakarta.validation.constraints;
                public @interface NotNull {}
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-MODEL-007 nested input model field requires @Valid cascade validation");
    }

    @Test
    void serviceInterfaceCannotHideResultImplementationOrExcessParameters() {
        Report report = analyze("example/core/service/IOrderService.java", """
                package example.core.service;
                import io.mango.common.result.R;
                interface IOrderService {
                    default R<String> update(Long id, String name, Boolean active) {
                        return R.ok(name);
                    }
                }
                """);

        assertThat(messages(report)).containsExactlyInAnyOrder(
                "MANGO-ARCH-SVC-001 Service must not return R<T>",
                "MANGO-ARCH-SVC-002 Service must not call R methods",
                "MANGO-ARCH-SVC-010 Service method must consolidate more than two business parameters",
                "MANGO-ARCH-SVC-012 service interfaces must declare abstract contracts only");
    }

    @Test
    void mapperProviderSqlIsRejected() {
        Report report = analyze(
                "example/OrderMapper.java", """
                package example;
                import org.apache.ibatis.annotations.SelectProvider;
                interface OrderMapper {
                    @SelectProvider(type = Object.class, method = "sql") int find();
                }
                """,
                "org/apache/ibatis/annotations/SelectProvider.java", """
                package org.apache.ibatis.annotations;
                public @interface SelectProvider { Class<?> type(); String method(); }
                """);

        assertThat(messages(report)).containsExactly(
                "MANGO-ARCH-MAPPER-001 Mapper must not declare annotation SQL");
    }

    @Test
    void anonymousAndLocalProtocolLikeTypesDoNotCrashInspection() {
        Report report = analyze("example/ConfigParser.java", """
                package example;
                final class ConfigParser {
                    Object parse() {
                        class LocalResponse {}
                        return new LocalResponse() {};
                    }
                }
                """);

        assertThat(report.getProcessingErrors()).isEmpty();
    }

    private Report analyze(String... pathAndSource) {
        PMDConfiguration configuration = new PMDConfiguration();
        configuration.setDefaultLanguageVersion(
                LanguageRegistry.PMD.getLanguageVersionById("java", "21"));
        configuration.setIgnoreIncrementalAnalysis(true);
        try {
            String commonClasspath = Path.of(Require.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toString();
            String testClasspath = Path.of(MangoJavaArchitectureRuleTest.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toString();
            configuration.prependAuxClasspath(commonClasspath + File.pathSeparator + testClasspath);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to resolve mango-common test classpath", exception);
        }
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
