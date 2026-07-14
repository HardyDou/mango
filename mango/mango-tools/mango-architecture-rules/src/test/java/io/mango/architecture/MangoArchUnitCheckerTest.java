package io.mango.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import java.net.URISyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.transaction.annotation.Transactional;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.infra.persistence.api.crud.MangoCrudService;
import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;
import io.mango.infra.persistence.api.crud.MangoTypedCrudService;
import io.mango.infra.persistence.api.entity.BaseEntity;
import io.mango.infra.persistence.api.entity.TenantEntity;
import io.mango.common.result.R;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import evil.SpoofedOrderService;

class MangoArchUnitCheckerTest {

    private final MangoArchUnitChecker checker = new MangoArchUnitChecker();

    @Test
    void compliantControllerPasses() {
        JavaClasses classes = importClasses(OrderController.class, OrderApi.class, IOrderService.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER))).isEmpty();
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

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.CORE)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-SVC-005", "MANGO-ARCH-TYPE-005", "MANGO-ARCH-TYPE-008");
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

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER_REMOTE)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly(
                        "MANGO-ARCH-FEIGN-002",
                        "MANGO-ARCH-FEIGN-003",
                        "MANGO-ARCH-FEIGN-004");
    }

    @Test
    void validFeignContractPasses() {
        JavaClasses classes = importClasses(OrderFeignClient.class, OrderApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER_REMOTE))).isEmpty();
    }

    @Test
    void feignCannotDeclareConstants() {
        JavaClasses classes = importClasses(StatefulFeignClient.class, EndpointApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER_REMOTE)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-FEIGN-009");
    }

    @Test
    void feignCannotDeclareDefaultImplementations() {
        JavaClasses classes = importClasses(DefaultFeignClient.class, EndpointApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER_REMOTE)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-FEIGN-009");
    }

    @Test
    void explicitlyRegisteredReverseControllerPassesPlacementRule() {
        JavaClasses classes = importClasses(ReverseController.class, OrderApi.class);
        MangoArchUnitChecker configured = new MangoArchUnitChecker(
                java.util.Set.of(ReverseController.class.getName()));

        assertThat(configured.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER_REMOTE))).isEmpty();
    }

    @Test
    void apiContractOutsideApiModuleIsRejected() {
        JavaClasses classes = importClasses(OrderApi.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-TYPE-009");
    }

    @Test
    void serviceContractOutsideCoreIsRejected() {
        JavaClasses classes = importClasses(IOrderService.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.API))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-TYPE-011");
    }

    @Test
    void controllerMustImplementExactlyOneApi() {
        JavaClasses classes = importClasses(MultiApiController.class, OrderApi.class, ExtraApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-TYPE-002");
    }

    @Test
    void controllerCannotHideEndpointsInASecondNonApiInterface() {
        JavaClasses classes = importClasses(
                HiddenEndpointController.class, OrderApi.class, HiddenHttpContract.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-TYPE-002");
    }

    @Test
    void apiAndServiceContractsCannotHideRulesInParentInterfaces() {
        JavaClasses classes = importClasses(
                InheritedApi.class,
                HiddenApiContract.class,
                IInheritedService.class,
                HiddenServiceContract.class);

        assertThat(checker.check(classes, javaClass -> {
            if (javaClass.getName().equals(InheritedApi.class.getName())) {
                return ModuleRole.API;
            }
            return ModuleRole.CORE;
        })).extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-API-007", "MANGO-ARCH-SVC-016");
    }

    @Test
    void controllerMustMapEveryApiMethod() {
        JavaClasses classes = importClasses(MissingMappingController.class, DetailApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-CTRL-005");
    }

    @Test
    void controllerMustNotExposeMethodsOutsideApi() {
        JavaClasses classes = importClasses(ExtraMappingController.class, DetailApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-CTRL-005");
    }

    @Test
    void controllerMustNotHidePublicBusinessMethodsOutsideApi() {
        JavaClasses classes = importClasses(PublicHelperController.class, DetailApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-CTRL-005");
    }

    @Test
    void requestParamBindingRequiresExplicitTransportName() {
        JavaClasses classes = importClasses(UnnamedBindingController.class, DetailApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-ADAPTER-002");
    }

    @Test
    void controllerRootMustDeclareExactlyOnePath() {
        JavaClasses classes = importClasses(MultiRootController.class, EndpointApi.class);
        MangoArchUnitChecker.ModuleContract contract = new MangoArchUnitChecker.ModuleContract(
                "mango-order-starter", "mango-order", "orders");

        assertThat(checker.check(
                classes,
                javaClass -> role(javaClass, ModuleRole.STARTER),
                ignored -> contract))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-CTRL-008");
    }

    @Test
    void controllerInheritanceCannotHideMethodsOutsideApi() {
        JavaClasses classes = importClasses(InheritedController.class, ControllerBase.class, OrderApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-CTRL-009");
    }

    @Test
    void controllerStaticStateIsRejected() {
        JavaClasses classes = importClasses(StaticStateController.class, OrderApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-TYPE-003");
    }

    @Test
    void adapterMethodCannotUseGenericRequestMapping() {
        JavaClasses classes = importClasses(GenericMappingController.class, EndpointApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-ADAPTER-004");
    }

    @Test
    void controllerRootMappingConditionsAreForbidden() {
        JavaClasses classes = importClasses(ConditionalRootController.class, EndpointApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-CTRL-012");
    }

    @Test
    void adapterMethodMappingConditionsAreForbidden() {
        JavaClasses classes = importClasses(ConditionalFeignClient.class, EndpointApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER_REMOTE)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-ADAPTER-006");
    }

    @Test
    void feignMustPreserveExactGenericApiContract() {
        JavaClasses classes = importClasses(
                RawGenericFeignClient.class,
                GenericApi.class,
                GenericVO.class,
                CreateAdapterCommand.class,
                R.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER_REMOTE)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-FEIGN-008", "MANGO-ARCH-FEIGN-008");
    }

    @Test
    void remoteOnlyFeignMustUseVerbAndModelCompatibleBindings() {
        JavaClasses classes = importClasses(
                GetBodyFeignClient.class,
                WriteQueryFeignClient.class,
                WrongQueryEncodingFeignClient.class,
                CommandApi.class,
                QueryApi.class,
                CreateAdapterCommand.class,
                OrderQuery.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER_REMOTE)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly(
                        "MANGO-ARCH-ADAPTER-007",
                        "MANGO-ARCH-ADAPTER-007",
                        "MANGO-ARCH-ADAPTER-007");
    }

    @Test
    void commandRequestBodyCannotBeOptional() {
        JavaClasses classes = importClasses(
                OptionalBodyController.class, CommandApi.class, CreateAdapterCommand.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-ADAPTER-005");
    }

    @Test
    void feignMustMapEveryApiMethod() {
        JavaClasses classes = importClasses(MissingMappingFeignClient.class, DetailApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER_REMOTE)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-FEIGN-008");
    }

    @Test
    void controllerAndFeignMustExposeTheSameEndpoint() {
        JavaClasses classes = importClasses(
                EndpointController.class, EndpointFeignClient.class, EndpointApi.class);

        assertThat(checker.check(classes, javaClass -> {
            if (javaClass.isInterface() && javaClass.getSimpleName().endsWith("Api")) {
                return ModuleRole.API;
            }
            return javaClass.isAnnotatedWith(FeignClient.class)
                    ? ModuleRole.STARTER_REMOTE
                    : ModuleRole.STARTER;
        })).extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-ADAPTER-001");
    }

    @Test
    void controllerAndFeignMustUseTheSameParameterBinding() {
        JavaClasses classes = importClasses(
                BindingController.class, BindingFeignClient.class, BindingApi.class);

        assertThat(checker.check(classes, javaClass -> {
            if (javaClass.isInterface() && javaClass.getSimpleName().endsWith("Api")) {
                return ModuleRole.API;
            }
            return javaClass.isAnnotatedWith(FeignClient.class)
                    ? ModuleRole.STARTER_REMOTE
                    : ModuleRole.STARTER;
        })).extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-ADAPTER-001");
    }

    @Test
    void controllerAndFeignMustUseTheSameTransportParameterName() {
        JavaClasses classes = importClasses(
                NamedBindingController.class, NamedBindingFeignClient.class, BindingApi.class);

        assertThat(checker.check(classes, javaClass -> {
            if (javaClass.isInterface() && javaClass.getSimpleName().endsWith("Api")) {
                return ModuleRole.API;
            }
            return javaClass.isAnnotatedWith(FeignClient.class)
                    ? ModuleRole.STARTER_REMOTE
                    : ModuleRole.STARTER;
        })).extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-ADAPTER-001");
    }

    @Test
    void controllerConcreteServiceAndApiFieldsAreRejected() {
        JavaClasses classes = importClasses(
                BadFieldController.class, OrderApi.class, ExtraApi.class, OrderService.class);

        assertThat(checker.check(classes, javaClass ->
                javaClass.getName().equals(OrderService.class.getName())
                        ? ModuleRole.CORE : role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-TYPE-003", "MANGO-ARCH-TYPE-003");
    }

    @Test
    void apiModuleCannotContainLocalImplementationTypes() {
        JavaClasses classes = importClasses(OrderManager.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.API))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-TYPE-010");
    }

    @Test
    void apiModuleCannotHideLocalImplementationAsHelper() {
        JavaClasses classes = importClasses(OrderHelper.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.API))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-TYPE-010");
    }

    @Test
    void apiModuleUsesAConcreteTypeWhitelist() {
        JavaClasses classes = importClasses(OrderCalculator.class, OrderEngine.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.API))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-TYPE-010", "MANGO-ARCH-TYPE-010");
    }

    @Test
    void persistenceApiFoundationMayExposeCanonicalConcreteTypes() {
        JavaClasses classes = importClasses(BaseEntity.class, MangoCrudServiceImpl.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.API))
                .extracting(ArchitectureIssue::ruleId)
                .doesNotContain("MANGO-ARCH-TYPE-010");
    }

    @Test
    void serviceContractImplementationCannotHideAsUseCase() {
        JavaClasses classes = importClasses(OrderUseCase.class, IOrderService.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-SVC-005");
    }

    @Test
    void baseMapperImplementationCannotHideAsStore() {
        JavaClasses classes = importClasses(OrderStore.class, OrderEntity.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-MAPPER-004", "MANGO-ARCH-MAPPER-006");
    }

    @Test
    void tenantEntityCannotHideAsRecord() {
        JavaClasses classes = importClasses(OrderRecord.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-ENTITY-001", "MANGO-ARCH-ENTITY-002");
    }

    @Test
    void mvcControllerVariantCannotReplaceRestController() {
        JavaClasses classes = importClasses(MvcOrderController.class, OrderApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-CTRL-011");
    }

    @Test
    void composedRestControllerAnnotationIsForbidden() {
        JavaClasses classes = importClasses(
                ComposedOrderController.class, ComposedRestController.class, OrderApi.class);

        assertThat(checker.check(classes, javaClass -> role(javaClass, ModuleRole.STARTER)))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-CTRL-011");
    }

    @Test
    void correctlyNamedServiceImplementingInterfacePasses() {
        JavaClasses classes = importClasses(CompliantOrderService.class, IOrderService.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE)).isEmpty();
    }

    @Test
    void businessServiceImplementationRequiresServiceStereotype() {
        JavaClasses classes = importClasses(UnmanagedOrderService.class, IOrderService.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-BEAN-001");
    }

    @Test
    void frameworkServiceMayUseConditionalBeanRegistration() {
        JavaClasses classes = importClasses(
                FrameworkClockService.class, FrameworkServiceConfiguration.class);

        assertThat(checker.check(classes, javaClass ->
                javaClass.getName().equals(FrameworkClockService.class.getName())
                        ? ModuleRole.CORE : ModuleRole.STARTER)).isEmpty();
    }

    @Test
    void frameworkServiceBeanMustBeConditional() {
        JavaClasses classes = importClasses(
                UnconditionalClockService.class, UnconditionalServiceConfiguration.class);

        assertThat(checker.check(classes, javaClass ->
                javaClass.getName().equals(UnconditionalClockService.class.getName())
                        ? ModuleRole.CORE : ModuleRole.STARTER))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-BEAN-002");
    }

    @Test
    void serviceCannotCombineStereotypeAndBeanRegistration() {
        JavaClasses classes = importClasses(
                DuplicateOrderService.class,
                DuplicateServiceConfiguration.class,
                IOrderService.class);

        assertThat(checker.check(classes, javaClass ->
                javaClass.getName().equals(DuplicateOrderService.class.getName())
                        || javaClass.isInterface()
                        ? ModuleRole.CORE : ModuleRole.STARTER))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-BEAN-003");
    }

    @Test
    void controllerCannotConstructManagedServiceDirectly() {
        JavaClasses classes = importClasses(
                NewingOrderController.class,
                CompliantOrderService.class,
                IOrderService.class,
                OrderApi.class);

        assertThat(checker.check(classes, javaClass -> {
            if (javaClass.getName().equals(CompliantOrderService.class.getName())
                    || javaClass.getName().equals(IOrderService.class.getName())) {
                return ModuleRole.CORE;
            }
            return javaClass.isInterface() ? ModuleRole.API : ModuleRole.STARTER;
        })).extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-BEAN-004");
    }

    @Test
    void serviceMayConstructNonServiceValueEvenWhenSameTypeIsRegisteredAsBean() {
        JavaClasses classes = importClasses(
                CollectionAllocatingService.class,
                RegisteredValueConfiguration.class);

        assertThat(checker.check(classes, javaClass ->
                javaClass.getName().equals(CollectionAllocatingService.class.getName())
                        ? ModuleRole.CORE : ModuleRole.STARTER))
                .extracting(ArchitectureIssue::ruleId)
                .doesNotContain("MANGO-ARCH-BEAN-004");
    }

    @Test
    void crossCuttingAnnotationRequiresSpringRegistration() {
        JavaClasses classes = importClasses(UnmanagedTransactionalWorker.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-BEAN-005");
    }

    @Test
    void mutableStaticServiceLocatorIsRejected() {
        JavaClasses classes = importClasses(StaticServiceLocator.class, IOrderService.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-BEAN-006");
    }

    @Test
    void serviceCannotInheritBusinessBehaviorFromCustomSuperclass() {
        JavaClasses classes = importClasses(
                InheritedOrderService.class, OrderSupport.class, IOrderService.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-SVC-015");
    }

    @Test
    void crudServiceMustUseMangoCrudBase() {
        JavaClasses classes = importClasses(
                BadCrudService.class, ICrudService.class, MangoCrudService.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-SVC-007", "MANGO-ARCH-SVC-008");
    }

    @Test
    void typedCrudServiceUsingMangoCrudBasePasses() {
        JavaClasses classes = importClasses(
                CompliantCrudService.class,
                ITypedCrudService.class,
                CrudMapper.class,
                CrudEntity.class,
                MangoTypedCrudService.class,
                MangoCrudService.class,
                MangoCrudServiceImpl.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE)).isEmpty();
    }

    @Test
    void typedCrudGenericMismatchIsRejected() {
        JavaClasses classes = importClasses(
                BadTypedCrudService.class,
                IBadTypedCrudService.class,
                CrudMapper.class,
                CrudEntity.class,
                MangoTypedCrudService.class,
                MangoCrudService.class,
                MangoCrudServiceImpl.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-SVC-011");
    }

    @Test
    void directMybatisServiceImplCannotBypassMangoCrudContract() {
        JavaClasses classes = importClasses(LegacyCrudService.class, IOrderService.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-SVC-014", "MANGO-ARCH-SVC-015");
    }

    @Test
    void handWrittenStandardCrudSurfaceRequiresTypedContract() {
        JavaClasses classes = importClasses(HandWrittenCrudService.class, IOrderService.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-SVC-008");
    }

    @Test
    void malformedEntityAndMapperShapesAreRejected() {
        JavaClasses classes = importClasses(BadShapeEntity.class, BadShapeMapper.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly(
                        "MANGO-ARCH-ENTITY-002",
                        "MANGO-ARCH-ENTITY-003",
                        "MANGO-ARCH-MAPPER-004",
                        "MANGO-ARCH-MAPPER-005");
    }

    @Test
    void approvedGlobalEntityRequiresExactManifestTable() {
        JavaClasses classes = importClasses(GlobalSettingEntity.class);
        MangoArchUnitChecker approved = new MangoArchUnitChecker(
                Set.of(), Map.of(GlobalSettingEntity.class.getName(), "global_setting"));
        MangoArchUnitChecker mismatch = new MangoArchUnitChecker(
                Set.of(), Map.of(GlobalSettingEntity.class.getName(), "wrong_table"));

        assertThat(approved.check(classes, ignored -> ModuleRole.CORE)).isEmpty();
        assertThat(mismatch.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-ENTITY-004");
    }

    @Test
    void sameSimpleNameCrudContractsCannotSpoofCanonicalMangoTypes() {
        JavaClasses classes = importClasses(SpoofedOrderService.class);

        assertThat(checker.check(classes, ignored -> ModuleRole.CORE))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-SVC-009", "MANGO-ARCH-SVC-015");
    }

    @Test
    void feignMustMatchResolvedModuleContract() {
        JavaClasses classes = importClasses(OrderFeignClient.class, OrderApi.class);
        MangoArchUnitChecker.ModuleContract contract = new MangoArchUnitChecker.ModuleContract(
                "mango-order-starter-remote", "mango-order", "order");

        assertThat(checker.check(
                classes,
                javaClass -> role(javaClass, ModuleRole.STARTER_REMOTE),
                ignored -> contract))
                .extracting(ArchitectureIssue::ruleId)
                .containsExactly("MANGO-ARCH-FEIGN-007", "MANGO-ARCH-FEIGN-007");
    }

    @Test
    void standardWorktreeAndOrdinaryPathsProduceIdenticalResults(@TempDir Path temporaryDirectory)
            throws URISyntaxException, IOException {
        Path testClasses = Path.of(MangoArchUnitCheckerTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        Path ordinaryClasses = temporaryDirectory.resolve("ordinary/target/test-classes");
        copyTree(testClasses, ordinaryClasses);

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

    private ModuleRole role(JavaClass javaClass, ModuleRole defaultRole) {
        if (javaClass.isInterface() && javaClass.getSimpleName().endsWith("Api")) {
            return ModuleRole.API;
        }
        return javaClass.isInterface() && javaClass.getSimpleName().matches("I[A-Z].*Service")
                ? ModuleRole.CORE : defaultRole;
    }

    interface OrderApi {
    }

    interface IOrderService {
    }

    interface ExtraApi {
    }

    interface HiddenApiContract {
        default String hidden() { return "hidden"; }
    }

    interface InheritedApi extends HiddenApiContract {
    }

    interface HiddenServiceContract {
        default String hidden() { return "hidden"; }
    }

    interface IInheritedService extends HiddenServiceContract {
    }

    interface HiddenHttpContract {
        @GetMapping("/hidden")
        default String hidden() { return "hidden"; }
    }

    interface DetailApi {
        String detail(Long id);
    }

    interface EndpointApi {
        String detail();
    }

    interface BindingApi {
        String create(String value);
    }

    interface CommandApi {
        String create(CreateAdapterCommand command);
    }

    interface QueryApi {
        String find(OrderQuery query);
    }

    static final class OrderQuery {
    }

    interface GenericApi {
        R<List<GenericVO>> detail();
        R<String> create(List<CreateAdapterCommand> commands);
    }

    static final class GenericVO {
    }

    static final class CreateAdapterCommand {
    }

    @Mapper
    interface OrderMapper extends BaseMapper<OrderEntity> {
    }

    interface OrderStore extends BaseMapper<OrderEntity> {
    }

    @TableName("orders")
    static final class OrderEntity extends TenantEntity {
    }

    static final class OrderRecord extends TenantEntity {
    }

    interface ICrudService extends MangoCrudService<Object> {
    }

    interface ITypedCrudService extends MangoTypedCrudService<
            CrudEntity, CreateCrudCommand, UpdateCrudCommand, CrudQuery, CrudVO, Long> {
    }

    interface IBadTypedCrudService extends MangoTypedCrudService<
            String, CreateCrudCommand, UpdateCrudCommand, CrudQuery, CrudVO, Long> {
    }

    static final class CreateCrudCommand {
    }

    static final class UpdateCrudCommand {
    }

    static final class CrudQuery {
    }

    static final class CrudVO {
    }

    @TableName("crud")
    static final class CrudEntity extends TenantEntity {
    }

    @Mapper
    interface CrudMapper extends BaseMapper<CrudEntity> {
    }

    @Service
    static final class OrderService implements IOrderService {
    }

    static final class OrderManager {
    }

    static final class OrderHelper {
    }

    static final class OrderCalculator {
    }

    abstract static class OrderEngine {
    }

    @Service
    static final class OrderUseCase implements IOrderService {
    }

    abstract static class OrderSupport {
        public R<String> inheritedResult() {
            return null;
        }
    }

    @Service
    static final class InheritedOrderService extends OrderSupport implements IOrderService {
    }

    @Controller
    static final class MvcOrderController implements OrderApi {
    }

    @ComposedRestController
    static final class ComposedOrderController implements OrderApi {
    }

    @RestController
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
    @interface ComposedRestController {
    }

    @RestController
    static final class OrderController implements OrderApi {
        private final IOrderService orderService = null;
    }

    @RestController
    static final class MultiApiController implements OrderApi, ExtraApi {
    }

    @RestController
    static final class HiddenEndpointController implements OrderApi, HiddenHttpContract {
    }

    @RestController
    static final class MissingMappingController implements DetailApi {
        @Override
        public String detail(@RequestParam("id") Long id) {
            return null;
        }
    }

    @RestController
    static final class ExtraMappingController implements DetailApi {
        @Override
        @GetMapping
        public String detail(@RequestParam("id") Long id) {
            return null;
        }

        @GetMapping
        public String health() {
            return "ok";
        }
    }

    @RestController
    static final class PublicHelperController implements DetailApi {
        @Override
        @GetMapping
        public String detail(@RequestParam("id") Long id) {
            return null;
        }

        public void approvePayment() {
        }
    }

    @RestController
    static final class UnnamedBindingController implements DetailApi {
        @Override
        @GetMapping
        public String detail(@RequestParam Long id) {
            return null;
        }
    }

    @RestController
    @RequestMapping({"/orders", "/wrong"})
    static final class MultiRootController implements EndpointApi {
        @Override
        @GetMapping("/detail")
        public String detail() {
            return null;
        }
    }

    static class ControllerBase {
        public void approvePayment() {
        }
    }

    @RestController
    static final class InheritedController extends ControllerBase implements OrderApi {
    }

    @RestController
    static final class StaticStateController implements OrderApi {
        private static final String TARGET = "orders";
    }

    @RestController
    static final class GenericMappingController implements EndpointApi {
        @Override
        @RequestMapping("/detail")
        public String detail() {
            return null;
        }
    }

    @RestController
    @RequestMapping(path = "/orders", headers = "X-Mode=internal")
    static final class ConditionalRootController implements EndpointApi {
        @Override
        @GetMapping("/detail")
        public String detail() {
            return null;
        }
    }

    @RestController
    static final class OptionalBodyController implements CommandApi {
        @Override
        @PostMapping("/create")
        public String create(@RequestBody(required = false) CreateAdapterCommand command) {
            return null;
        }
    }

    @RestController
    @RequestMapping("/orders")
    static final class EndpointController implements EndpointApi {
        @Override
        @GetMapping("/detail")
        public String detail() {
            return null;
        }
    }

    @RestController
    @RequestMapping("/orders")
    static final class BindingController implements BindingApi {
        @Override
        @PostMapping("/create")
        public String create(@RequestBody String value) {
            return value;
        }
    }

    @RestController
    @RequestMapping("/orders")
    static final class NamedBindingController implements BindingApi {
        @Override
        @GetMapping("/detail")
        public String create(@RequestParam("id") String value) {
            return value;
        }
    }

    @RestController
    static final class BadFieldController implements OrderApi {
        private final OrderService orderService = null;
        private final ExtraApi extraApi = null;
    }

    @Service
    static final class CompliantOrderService implements IOrderService {
    }

    static final class UnmanagedOrderService implements IOrderService {
    }

    static final class FrameworkClockService {
    }

    @Configuration(proxyBeanMethods = false)
    static class FrameworkServiceConfiguration {
        @Bean
        @ConditionalOnMissingBean
        FrameworkClockService frameworkClockService() {
            return new FrameworkClockService();
        }
    }

    static final class UnconditionalClockService {
    }

    @Configuration(proxyBeanMethods = false)
    static class UnconditionalServiceConfiguration {
        @Bean
        UnconditionalClockService unconditionalClockService() {
            return new UnconditionalClockService();
        }
    }

    @Service
    static final class DuplicateOrderService implements IOrderService {
    }

    @Configuration(proxyBeanMethods = false)
    static class DuplicateServiceConfiguration {
        @Bean
        @ConditionalOnMissingBean
        DuplicateOrderService duplicateOrderService() {
            return new DuplicateOrderService();
        }
    }

    @RestController
    static final class NewingOrderController implements OrderApi {
        void createService() {
            new CompliantOrderService();
        }
    }

    @Service
    static final class CollectionAllocatingService {
        java.util.LinkedHashMap<String, String> values() {
            return new java.util.LinkedHashMap<>();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static final class RegisteredValueConfiguration {
        @Bean
        java.util.LinkedHashMap<String, String> registeredValues() {
            return new java.util.LinkedHashMap<>();
        }
    }

    static final class UnmanagedTransactionalWorker {
        @Transactional
        void execute() {
        }
    }

    static final class StaticServiceLocator {
        private static IOrderService current;
    }

    @Service
    static final class BadCrudService implements ICrudService {
    }

    @Service
    static final class CompliantCrudService extends MangoCrudServiceImpl<CrudMapper, CrudEntity>
            implements ITypedCrudService {
    }

    @Service
    static final class BadTypedCrudService extends MangoCrudServiceImpl<CrudMapper, CrudEntity>
            implements IBadTypedCrudService {
    }

    @Service
    static final class LegacyCrudService extends ServiceImpl<Object, Object> implements IOrderService {
    }

    @Service
    static final class HandWrittenCrudService implements IOrderService {
        public Object create(Object command) { return null; }
        public boolean update(Object command) { return false; }
        public boolean delete(Object command) { return false; }
        public Object page(Object query) { return null; }
        public Object detail(Long id) { return null; }
    }

    static final class BadShapeEntity {
    }

    @TableName("global_setting")
    static final class GlobalSettingEntity {
    }

    static final class BadShapeMapper {
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

    @FeignClient(name = "order", contextId = "orderFeignClient", path = "/internal/orders")
    interface OrderFeignClient extends OrderApi {
    }

    @FeignClient(name = "order", contextId = "statefulFeignClient", path = "/orders")
    interface StatefulFeignClient extends EndpointApi {
        String TARGET = "orders";

        @Override
        @GetMapping("/detail")
        String detail();
    }

    @FeignClient(name = "order", contextId = "defaultFeignClient", path = "/orders")
    interface DefaultFeignClient extends EndpointApi {
        @Override
        @GetMapping("/detail")
        default String detail() {
            return "";
        }
    }

    @FeignClient(name = "order", contextId = "missingMappingFeignClient", path = "/orders")
    interface MissingMappingFeignClient extends DetailApi {
        @Override
        String detail(Long id);
    }

    @FeignClient(name = "order", contextId = "bindingFeignClient", path = "/orders")
    interface BindingFeignClient extends BindingApi {
        @Override
        @PostMapping("/create")
        String create(@RequestParam("value") String value);
    }

    @FeignClient(name = "order", contextId = "namedBindingFeignClient", path = "/orders")
    interface NamedBindingFeignClient extends BindingApi {
        @Override
        @GetMapping("/detail")
        String create(@RequestParam("orderId") String value);
    }

    @FeignClient(name = "order", contextId = "endpointFeignClient", path = "/orders")
    interface EndpointFeignClient extends EndpointApi {
        @Override
        @GetMapping("/different")
        String detail();
    }

    @FeignClient(name = "order", contextId = "conditionalFeignClient", path = "/orders")
    interface ConditionalFeignClient extends EndpointApi {
        @Override
        @GetMapping(value = "/detail", produces = "application/json")
        String detail();
    }

    @SuppressWarnings("rawtypes")
    @FeignClient(name = "order", contextId = "rawGenericFeignClient", path = "/orders")
    interface RawGenericFeignClient extends GenericApi {
        @Override
        @GetMapping("/detail")
        R detail();

        @Override
        @PostMapping("/create")
        R<String> create(@RequestBody List commands);
    }

    @FeignClient(name = "order", contextId = "getBodyFeignClient", path = "/orders")
    interface GetBodyFeignClient extends CommandApi {
        @Override
        @GetMapping("/create")
        String create(@RequestBody CreateAdapterCommand command);
    }

    @FeignClient(name = "order", contextId = "writeQueryFeignClient", path = "/orders")
    interface WriteQueryFeignClient extends CommandApi {
        @Override
        @PostMapping("/create")
        String create(@RequestParam("command") CreateAdapterCommand command);
    }

    @FeignClient(name = "order", contextId = "wrongQueryEncodingFeignClient", path = "/orders")
    interface WrongQueryEncodingFeignClient extends QueryApi {
        @Override
        @GetMapping("/find")
        String find(@ParameterObject OrderQuery query);
    }

    @RestController
    static final class ReverseController implements OrderApi {
    }
}
