package io.mango.architecture.pmd;

import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.java.ast.ASTAnnotation;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTConstructorCall;
import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameter;
import net.sourceforge.pmd.lang.java.ast.ASTMethodCall;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTReturnStatement;
import net.sourceforge.pmd.lang.java.ast.ASTThrowStatement;
import net.sourceforge.pmd.lang.java.ast.ASTTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.Annotatable;
import net.sourceforge.pmd.lang.java.ast.JModifier;
import net.sourceforge.pmd.lang.java.ast.ModifierOwner;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.java.symbols.JClassSymbol;
import net.sourceforge.pmd.lang.java.types.JArrayType;
import net.sourceforge.pmd.lang.java.types.JClassType;
import net.sourceforge.pmd.lang.java.types.JTypeMirror;
import net.sourceforge.pmd.lang.java.types.JWildcardType;
import net.sourceforge.pmd.reporting.RuleContext;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/** One PMD 7 visitor for all source-semantic Mango architecture rules. */
public final class MangoJavaArchitectureRule extends AbstractJavaRule {

    private static final String RESULT_R = "io.mango.common.result.R";
    private static final String REQUIRE = "io.mango.common.result.Require";
    private static final String BIZ_CODE = "io.mango.common.result.BizCode";
    private static final String MANGO_CRUD_SERVICE =
            "io.mango.infra.persistence.api.crud.MangoCrudService";
    private static final String MANGO_TYPED_CRUD_SERVICE =
            "io.mango.infra.persistence.api.crud.MangoTypedCrudService";

    private static final String REST_CONTROLLER =
            "org.springframework.web.bind.annotation.RestController";
    private static final String VALIDATED = "org.springframework.validation.annotation.Validated";
    private static final String REQUEST_BODY =
            "org.springframework.web.bind.annotation.RequestBody";
    private static final String REQUEST_PARAM =
            "org.springframework.web.bind.annotation.RequestParam";
    private static final String REQUEST_HEADER =
            "org.springframework.web.bind.annotation.RequestHeader";
    private static final String MODEL_ATTRIBUTE =
            "org.springframework.web.bind.annotation.ModelAttribute";
    private static final String GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping";
    private static final String REQUEST_MAPPING =
            "org.springframework.web.bind.annotation.RequestMapping";
    private static final String PATH_VARIABLE =
            "org.springframework.web.bind.annotation.PathVariable";
    private static final String FEIGN_CLIENT = "org.springframework.cloud.openfeign.FeignClient";
    private static final String SCHEMA = "io.swagger.v3.oas.annotations.media.Schema";
    private static final String TAG = "io.swagger.v3.oas.annotations.tags.Tag";
    private static final String OPERATION = "io.swagger.v3.oas.annotations.Operation";
    private static final String PARAMETER = "io.swagger.v3.oas.annotations.Parameter";
    private static final String PARAMETER_OBJECT = "org.springdoc.core.annotations.ParameterObject";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String DESCRIPTION_ATTRIBUTE = "description";
    private static final String SUMMARY_ATTRIBUTE = "summary";
    private static final String SERVICE_IMPL_SUFFIX = "ServiceImpl";
    private static final String SERVICE_SUFFIX = "Service";
    private static final String CONTROLLER_SUFFIX = "Controller";
    private static final String FEIGN_CLIENT_SUFFIX = "FeignClient";
    private static final String COMMAND_SUFFIX = "Command";
    private static final String QUERY_SUFFIX = "Query";
    private static final String REQUEST_SUFFIX = "Request";
    private static final String RESPONSE_SUFFIX = "Response";
    private static final String VIEW_SUFFIX = "VO";
    private static final String PAGE_RESULT_SUFFIX = "PageResult";
    private static final String DTO_SUFFIX = "DTO";
    private static final String CODE_SUFFIX = "Code";
    private static final String RESULT_OK_METHOD = "ok";
    private static final String ENTITY_SUFFIX = "Entity";
    private static final String PO_SUFFIX = "PO";
    private static final String JAVA_OBJECT = "java.lang.Object";
    private static final String JAVA_STRING = "java.lang.String";
    private static final String JAVA_MAP = "java.util.Map";
    private static final String JAVA_SERIALIZABLE = "java.io.Serializable";
    private static final String JAKARTA_SERVLET_PREFIX = "jakarta.servlet.";
    private static final String JAVAX_SERVLET_PREFIX = "javax.servlet.";
    private static final String SPRING_PREFIX = "org.springframework.";
    private static final String SPRING_HTTP_PREFIX = "org.springframework.http.";
    private static final String SPRING_WEB_PREFIX = "org.springframework.web.";
    private static final String SPRING_MULTIPART_PREFIX = "org.springframework.web.multipart.";
    private static final String SPRING_SERVICE = "org.springframework.stereotype.Service";
    private static final String MYBATIS_MAPPER = "org.apache.ibatis.annotations.Mapper";
    private static final int MAX_DIRECT_PARAMETERS = 2;
    private static final int MAP_TYPE_ARGUMENT_COUNT = 2;
    private static final int MISSING_PARAMETER_INDEX = -1;
    private static final int AMBIGUOUS_PARAMETER_INDEX = -2;
    private static final Set<String> VALID_ANNOTATIONS = Set.of("jakarta.validation.Valid");
    private static final Set<String> SQL_ANNOTATIONS =
            Set.of(
                    "org.apache.ibatis.annotations.Select",
                    "org.apache.ibatis.annotations.Insert",
                    "org.apache.ibatis.annotations.Update",
                    "org.apache.ibatis.annotations.Delete",
                    "org.apache.ibatis.annotations.SelectProvider",
                    "org.apache.ibatis.annotations.InsertProvider",
                    "org.apache.ibatis.annotations.UpdateProvider",
                    "org.apache.ibatis.annotations.DeleteProvider");
    private static final Set<String> HTTP_ANNOTATIONS =
            Set.of(
                    "org.springframework.web.bind.annotation.RequestMapping",
                    "org.springframework.web.bind.annotation.GetMapping",
                    "org.springframework.web.bind.annotation.PostMapping",
                    "org.springframework.web.bind.annotation.PutMapping",
                    "org.springframework.web.bind.annotation.PatchMapping",
                    "org.springframework.web.bind.annotation.DeleteMapping");
    private static final Set<String> WRITE_HTTP_ANNOTATIONS =
            Set.of(
                    "org.springframework.web.bind.annotation.PostMapping",
                    "org.springframework.web.bind.annotation.PutMapping",
                    "org.springframework.web.bind.annotation.PatchMapping",
                    "org.springframework.web.bind.annotation.DeleteMapping");
    private static final Set<String> API_TRANSPORT_ANNOTATIONS =
            Set.of(
                    "org.springframework.web.bind.annotation.RequestMapping",
                    "org.springframework.web.bind.annotation.GetMapping",
                    "org.springframework.web.bind.annotation.PostMapping",
                    "org.springframework.web.bind.annotation.PutMapping",
                    "org.springframework.web.bind.annotation.PatchMapping",
                    "org.springframework.web.bind.annotation.DeleteMapping",
                    REQUEST_BODY,
                    REQUEST_PARAM,
                    REQUEST_HEADER,
                    MODEL_ATTRIBUTE,
                    PATH_VARIABLE,
                    "org.springdoc.core.annotations.ParameterObject",
                    "org.springframework.cloud.openfeign.SpringQueryMap",
                    "io.swagger.v3.oas.annotations.Operation",
                    "io.swagger.v3.oas.annotations.tags.Tag");
    private static final Set<String> BUSINESS_ACTION_PREFIXES =
            Set.of(
                    "create",
                    "update",
                    "delete",
                    "remove",
                    "save",
                    "submit",
                    "approve",
                    "reject",
                    "publish",
                    "cancel",
                    "enable",
                    "disable",
                    "assign",
                    "bind",
                    "unbind",
                    "claim",
                    "release",
                    "execute",
                    "process",
                    "pay",
                    "register",
                    "change",
                    "close",
                    "archive",
                    "refund");
    private static final Set<String> API_SCALAR_TYPES =
            Set.of(
                    "java.lang.Boolean",
                    "java.lang.Byte",
                    "java.lang.Short",
                    "java.lang.Integer",
                    "java.lang.Long",
                    "java.lang.Float",
                    "java.lang.Double",
                    "java.lang.Character",
                    "java.lang.String",
                    "java.lang.Void",
                    "java.math.BigDecimal",
                    "java.math.BigInteger",
                    "java.util.UUID",
                    "java.time.LocalDate",
                    "java.time.LocalDateTime",
                    "java.time.LocalTime",
                    "java.time.OffsetDateTime",
                    "java.time.OffsetTime",
                    "java.time.ZonedDateTime",
                    "java.time.Year",
                    "java.time.YearMonth",
                    "java.time.MonthDay",
                    "java.time.Instant",
                    "java.time.Duration",
                    "java.time.Period");
    private static final Set<String> API_RESPONSE_CONTAINERS =
            Set.of("java.util.Collection", "java.util.List", "java.util.Set");
    private static final Set<String> API_MODEL_FIELD_CONTAINERS =
            Set.of("java.util.Collection", "java.util.List", "java.util.Set", "java.util.Optional");

    public MangoJavaArchitectureRule() {
        setLanguage(LanguageRegistry.PMD.getLanguageById("java"));
        setName("MangoJavaArchitecture");
        setMessage("Mango Java/Spring architecture violation");
    }

    @Override
    public Object visit(ASTCompilationUnit node, Object data) {
        RuleContext context = asCtx(data);
        for (ASTTypeDeclaration type : node.descendants(ASTTypeDeclaration.class)) {
            inspectPathRules(type, context);
            inspectProtocolModel(type, context);
            if (hasAnnotation(type, REST_CONTROLLER)) {
                inspectController(type, context);
            }
            if (type.isInterface() && type.getSimpleName().endsWith("Api")) {
                inspectApiContract(type, context);
            }
            if (isService(type)) {
                inspectService(type, context);
            }
            if (isServiceContract(type)) {
                inspectServiceContract(type, context);
            }
            if (isMapper(type)) {
                inspectMapper(type, context);
            }
        }
        return data;
    }

    private void inspectController(ASTTypeDeclaration type, RuleContext context) {
        inspectControllerAnnotations(type, context);
        inspectControllerFields(type, context);
        for (ASTMethodDeclaration method : type.getDeclarations(ASTMethodDeclaration.class)) {
            inspectControllerMethod(type, method, context);
        }
        inspectControllerResultFactories(type, context);
    }

    private void inspectControllerAnnotations(ASTTypeDeclaration type, RuleContext context) {
        if (!hasAnnotation(type, VALIDATED)) {
            violation(context, type, "MANGO-ARCH-CTRL-001 Controller requires @Validated");
        }
        if (!hasAnnotation(type, TAG)) {
            violation(context, type, "MANGO-ARCH-OPENAPI-001 Controller requires @Tag");
        } else if (!hasChineseAttribute(type, TAG, NAME_ATTRIBUTE)
                || !hasChineseAttribute(type, TAG, DESCRIPTION_ATTRIBUTE)) {
            violation(
                    context,
                    type,
                    "MANGO-ARCH-OPENAPI-005 @Tag requires non-blank Chinese name and description");
        }
    }

    private void inspectControllerFields(ASTTypeDeclaration type, RuleContext context) {
        for (ASTFieldDeclaration field : type.getDeclarations(ASTFieldDeclaration.class)) {
            JTypeMirror fieldType = field.getTypeNode().getTypeMirror();
            if (field.isStatic() || !isAllowedControllerServicePort(fieldType)) {
                violation(
                        context,
                        field,
                        "MANGO-ARCH-CTRL-002 Controller must depend on a service interface: "
                                + canonicalName(fieldType));
            }
        }
    }

    private void inspectControllerMethod(
            ASTTypeDeclaration type, ASTMethodDeclaration method, RuleContext context) {
        inspectHttpReturn(method, context);
        if (hasAnnotation(method, REQUEST_MAPPING)) {
            violation(
                    context,
                    method,
                    "MANGO-ARCH-ADAPTER-004 adapter methods must use a concrete HTTP verb"
                            + " annotation");
        }
        inspectControllerOperation(method, context);
        if (isHttpMethod(method)) {
            inspectControllerHttpBehavior(type, method, context);
        }
        for (ASTFormalParameter parameter : method.descendants(ASTFormalParameter.class)) {
            inspectControllerParameter(method, parameter, context);
        }
    }

    private void inspectControllerOperation(ASTMethodDeclaration method, RuleContext context) {
        if (!isHttpMethod(method)) {
            return;
        }
        if (!hasAnnotation(method, OPERATION)) {
            violation(context, method, "MANGO-ARCH-OPENAPI-002 HTTP method requires @Operation");
            return;
        }
        if (!hasChineseAttribute(method, OPERATION, SUMMARY_ATTRIBUTE)
                || !hasChineseAttribute(method, OPERATION, DESCRIPTION_ATTRIBUTE)) {
            violation(
                    context,
                    method,
                    "MANGO-ARCH-OPENAPI-006 @Operation requires non-blank Chinese summary and"
                            + " description");
        }
    }

    private void inspectControllerHttpBehavior(
            ASTTypeDeclaration type, ASTMethodDeclaration method, RuleContext context) {
        method.descendants(ASTReturnStatement.class)
                .filter(statement -> !isHandledControllerReturn(statement))
                .forEach(
                        statement ->
                                violation(
                                        context,
                                        statement,
                                        "MANGO-ARCH-CTRL-004 HTTP Controller must directly return"
                                                + " canonical R.ok(...)"));
        method.descendants(ASTThrowStatement.class)
                .forEach(
                        statement ->
                                violation(
                                        context,
                                        statement,
                                        "MANGO-ARCH-CTRL-010 Controller must delegate business"
                                                + " failures to Service Require"));
        if (implementsApiContract(type) && !hasValidServiceDelegation(type, method)) {
            violation(
                    context,
                    method,
                    "MANGO-ARCH-CTRL-013 API Controller must return service-interface results"
                            + " without hardcoded payloads");
        }
    }

    private void inspectControllerParameter(
            ASTMethodDeclaration method, ASTFormalParameter parameter, RuleContext context) {
        boolean writeRequestModel =
                hasWriteHttpAnnotation(method) && isWriteRequestModel(parameter.getTypeMirror());
        boolean requestBody = hasAnnotation(parameter, REQUEST_BODY);
        inspectControllerBodyParameter(method, parameter, writeRequestModel, requestBody, context);
        inspectSimpleHttpParameter(parameter, context);
        inspectQueryParameter(parameter, context);
    }

    private void inspectControllerBodyParameter(
            ASTMethodDeclaration method,
            ASTFormalParameter parameter,
            boolean writeRequestModel,
            boolean requestBody,
            RuleContext context) {
        if (hasAnnotation(method, GET_MAPPING) && requestBody) {
            violation(context, parameter, "MANGO-ARCH-CTRL-006 GET must not declare @RequestBody");
        }
        if (writeRequestModel && !requestBody) {
            violation(
                    context,
                    parameter,
                    "MANGO-ARCH-CTRL-007 write Command/Request requires @RequestBody");
        }
        if (writeRequestModel && requestBodyIsOptional(parameter)) {
            violation(
                    context,
                    parameter,
                    "MANGO-ARCH-CTRL-007 write Command/Request body must be required");
        }
        if (requiresValidAnnotation(requestBody, writeRequestModel)
                && !hasValidAnnotation(parameter)) {
            violation(
                    context,
                    parameter,
                    "MANGO-ARCH-CTRL-003 @RequestBody parameter requires @Valid");
        }
    }

    private void inspectQueryParameter(ASTFormalParameter parameter, RuleContext context) {
        if (simpleName(canonicalName(parameter.getTypeMirror())).endsWith(QUERY_SUFFIX)
                && !hasAnnotation(parameter, PARAMETER_OBJECT)) {
            violation(
                    context,
                    parameter,
                    "MANGO-ARCH-OPENAPI-004 Query parameter requires @ParameterObject");
        }
    }

    private boolean hasWriteHttpAnnotation(ASTMethodDeclaration method) {
        return WRITE_HTTP_ANNOTATIONS.stream().anyMatch(name -> hasAnnotation(method, name));
    }

    private boolean requiresValidAnnotation(boolean requestBody, boolean writeRequestModel) {
        return requestBody || writeRequestModel;
    }

    private boolean hasValidAnnotation(ASTFormalParameter parameter) {
        return VALID_ANNOTATIONS.stream().anyMatch(name -> hasAnnotation(parameter, name));
    }

    private void inspectSimpleHttpParameter(ASTFormalParameter parameter, RuleContext context) {
        if (!isSimpleHttpParameter(parameter)) {
            return;
        }
        if (!hasAnnotation(parameter, PARAMETER)) {
            violation(
                    context,
                    parameter,
                    "MANGO-ARCH-OPENAPI-003 simple HTTP parameter requires @Parameter");
            return;
        }
        if (!hasChineseAttribute(parameter, PARAMETER, DESCRIPTION_ATTRIBUTE)) {
            violation(
                    context,
                    parameter,
                    "MANGO-ARCH-OPENAPI-007 @Parameter requires a non-blank Chinese description");
        }
    }

    private boolean isSimpleHttpParameter(ASTFormalParameter parameter) {
        return hasAnnotation(parameter, REQUEST_PARAM) || hasAnnotation(parameter, REQUEST_HEADER);
    }

    private void inspectControllerResultFactories(ASTTypeDeclaration type, RuleContext context) {
        for (ASTMethodCall call : type.descendants(ASTMethodCall.class)) {
            if (isCallOn(call, RESULT_R) && !RESULT_OK_METHOD.equals(call.getMethodName())) {
                violation(
                        context,
                        call,
                        "MANGO-ARCH-CTRL-004 Controller may only construct success results with"
                                + " R.ok");
            }
        }
        type.descendants(ASTConstructorCall.class)
                .filter(call -> isType(call.getTypeMirror(), RESULT_R))
                .forEach(
                        call ->
                                violation(
                                        context,
                                        call,
                                        "MANGO-ARCH-CTRL-004 Controller may only construct success"
                                                + " results with R.ok"));
    }

    private void inspectService(ASTTypeDeclaration type, RuleContext context) {
        if (type.getSimpleName().endsWith(SERVICE_IMPL_SUFFIX)) {
            violation(
                    context,
                    type,
                    "MANGO-ARCH-SVC-005 Service implementation must be named XxxService, not"
                            + " XxxServiceImpl");
        }
        for (ASTMethodDeclaration method : type.getDeclarations(ASTMethodDeclaration.class)) {
            inspectServiceMethod(method, context);
        }
        for (ASTMethodCall call : type.descendants(ASTMethodCall.class)) {
            inspectServiceCall(call, context);
        }
        type.descendants(ASTConstructorCall.class)
                .filter(call -> isType(call.getTypeMirror(), RESULT_R))
                .forEach(
                        call ->
                                violation(
                                        context,
                                        call,
                                        "MANGO-ARCH-SVC-002 Service must not construct R"
                                                + " instances"));
        type.descendants(ASTThrowStatement.class)
                .forEach(
                        statement ->
                                violation(
                                        context,
                                        statement,
                                        "MANGO-ARCH-SVC-006 Service business failures must use"
                                                + " Require, not throw directly"));
    }

    private void inspectServiceMethod(ASTMethodDeclaration method, RuleContext context) {
        if (!method.isVoid() && isType(method.getResultTypeNode().getTypeMirror(), RESULT_R)) {
            violation(context, method, "MANGO-ARCH-SVC-001 Service must not return R<T>");
        }
        if (method.getVisibility() == ModifierOwner.Visibility.V_PUBLIC
                && method.getArity() > MAX_DIRECT_PARAMETERS) {
            violation(
                    context,
                    method,
                    "MANGO-ARCH-SVC-010 Service method must consolidate more than two business"
                            + " parameters");
        }
        if (requiresBusinessPrecondition(method) && !containsRequireCall(method)) {
            violation(
                    context,
                    method,
                    "MANGO-ARCH-SVC-004 business action requires a Require precondition");
        }
    }

    private void inspectServiceCall(ASTMethodCall call, RuleContext context) {
        if (isCallOn(call, RESULT_R)) {
            violation(context, call, "MANGO-ARCH-SVC-002 Service must not call R methods");
        }
        if (!isCallOn(call, REQUIRE)) {
            return;
        }
        if ("rethrow".equals(call.getMethodName())) {
            return;
        }
        int codeParameterIndex = businessCodeParameterIndex(call);
        if (!hasValidBusinessCodeArgument(call, codeParameterIndex)) {
            violation(
                    context,
                    call,
                    "MANGO-ARCH-SVC-003 Require must receive a module XxxCode implementing"
                            + " BizCode; resolved="
                            + resolvedBusinessCodeParameter(call)
                            + resolvedArgumentDetails(call, codeParameterIndex));
        }
    }

    private boolean hasValidBusinessCodeArgument(ASTMethodCall call, int index) {
        if (index < 0 || call.getArguments().size() <= index) {
            return false;
        }
        return isBusinessCodeArgument(call, index);
    }

    private void inspectServiceContract(ASTTypeDeclaration type, RuleContext context) {
        for (ASTMethodDeclaration method : type.getDeclarations(ASTMethodDeclaration.class)) {
            if (!method.isVoid() && isType(method.getResultTypeNode().getTypeMirror(), RESULT_R)) {
                violation(context, method, "MANGO-ARCH-SVC-001 Service must not return R<T>");
            }
            if (method.getArity() > MAX_DIRECT_PARAMETERS) {
                violation(
                        context,
                        method,
                        "MANGO-ARCH-SVC-010 Service method must consolidate more than two business"
                                + " parameters");
            }
            if (method.getBody() != null
                    || method.isStatic()
                    || method.hasModifiers(JModifier.DEFAULT, JModifier.PRIVATE)) {
                violation(
                        context,
                        method,
                        "MANGO-ARCH-SVC-012 service interfaces must declare abstract contracts"
                                + " only");
            }
            method.descendants(ASTAnnotation.class)
                    .filter(
                            annotation ->
                                    HTTP_ANNOTATIONS.contains(
                                            canonicalName(annotation.getTypeMirror())))
                    .forEach(
                            annotation ->
                                    violation(
                                            context,
                                            annotation,
                                            "MANGO-ARCH-SVC-013 service interfaces must be"
                                                    + " transport-neutral"));
            method.descendants(ASTMethodCall.class)
                    .filter(call -> isCallOn(call, RESULT_R))
                    .forEach(
                            call ->
                                    violation(
                                            context,
                                            call,
                                            "MANGO-ARCH-SVC-002 Service must not call R methods"));
        }
    }

    private void inspectApiContract(ASTTypeDeclaration type, RuleContext context) {
        inspectApiAnnotations(type, context);
        for (ASTFieldDeclaration field : type.getDeclarations(ASTFieldDeclaration.class)) {
            violation(
                    context,
                    field,
                    "MANGO-ARCH-API-005 XxxApi must not declare state or constants");
        }
        for (ASTMethodDeclaration method : type.getDeclarations(ASTMethodDeclaration.class)) {
            inspectApiMethod(method, context);
        }
    }

    private void inspectApiAnnotations(ASTTypeDeclaration type, RuleContext context) {
        if (hasAnnotation(type, FEIGN_CLIENT)) {
            violation(context, type, "MANGO-ARCH-HTTP-003 XxxApi must not declare @FeignClient");
        }
        type.descendants(ASTAnnotation.class)
                .filter(
                        annotation ->
                                API_TRANSPORT_ANNOTATIONS.contains(
                                        canonicalName(annotation.getTypeMirror())))
                .forEach(
                        annotation ->
                                violation(
                                        context,
                                        annotation,
                                        "MANGO-ARCH-API-001 XxxApi must be transport-neutral; "
                                                + canonicalName(annotation.getTypeMirror())
                                                + " is forbidden"));
    }

    private void inspectApiMethod(ASTMethodDeclaration method, RuleContext context) {
        inspectReturnContract(method, context);
        if (method.getBody() != null
                || method.isStatic()
                || method.hasModifiers(JModifier.DEFAULT, JModifier.PRIVATE)) {
            violation(
                    context,
                    method,
                    "MANGO-ARCH-API-005 XxxApi must declare abstract contract methods only");
        }
        if (method.getArity() > MAX_DIRECT_PARAMETERS) {
            violation(
                    context,
                    method,
                    "MANGO-ARCH-API-004 API method must consolidate more than two client"
                            + " parameters");
        }
        for (ASTFormalParameter parameter : method.descendants(ASTFormalParameter.class)) {
            inspectApiParameter(parameter, context);
        }
    }

    private void inspectApiParameter(ASTFormalParameter parameter, RuleContext context) {
        JTypeMirror parameterType = parameter.getTypeMirror();
        boolean persistenceModel = containsPersistenceModel(parameterType);
        boolean dtoModel = containsDtoModel(parameterType);
        inspectApiParameterModel(parameter, persistenceModel, dtoModel, context);
        inspectApiParameterShape(parameter, parameterType, persistenceModel, dtoModel, context);
    }

    private void inspectApiParameterModel(
            ASTFormalParameter parameter,
            boolean persistenceModel,
            boolean dtoModel,
            RuleContext context) {
        if (persistenceModel) {
            violation(
                    context, parameter, "MANGO-ARCH-HTTP-003 API method must not accept Entity/PO");
        } else if (dtoModel) {
            violation(context, parameter, "MANGO-ARCH-HTTP-006 API method must not accept DTO");
        }
    }

    private void inspectApiParameterShape(
            ASTFormalParameter parameter,
            JTypeMirror parameterType,
            boolean persistenceModel,
            boolean dtoModel,
            RuleContext context) {
        if (containsTransportContext(parameterType)) {
            violation(
                    context,
                    parameter,
                    "MANGO-ARCH-API-003 XxxApi must not accept server transport context");
            return;
        }
        if (persistenceModel || dtoModel) {
            return;
        }
        boolean allowedInput = isAllowedApiInput(parameterType);
        if (!allowedInput) {
            violation(
                    context,
                    parameter,
                    "MANGO-ARCH-API-006 API input must be a scalar, enum, Command, Query or"
                            + " Request");
            return;
        }
        if (isCompositeApiInput(parameterType)) {
            if (!hasValidAnnotation(parameter)) {
                violation(
                        context,
                        parameter,
                        "MANGO-ARCH-API-002 Command/Query/Request parameter requires @Valid");
            }
            return;
        }
        if (!hasJakartaConstraint(parameter)) {
            violation(
                    context,
                    parameter,
                    "MANGO-ARCH-API-002 simple API parameter requires a jakarta.validation"
                            + " constraint");
        }
    }

    private void inspectHttpReturn(ASTMethodDeclaration method, RuleContext context) {
        if (!isHttpMethod(method)) {
            return;
        }
        inspectReturnContract(method, context);
    }

    private void inspectReturnContract(ASTMethodDeclaration method, RuleContext context) {
        if (method.isVoid()) {
            violation(context, method, "MANGO-ARCH-HTTP-001 HTTP/API method must return R<T>");
            return;
        }
        JTypeMirror returnType = method.getResultTypeNode().getTypeMirror();
        if (!isType(returnType, RESULT_R)) {
            violation(context, method, "MANGO-ARCH-HTTP-001 HTTP method must return R<T>");
            return;
        }
        if (isRawResult(returnType)) {
            violation(context, method, "MANGO-ARCH-HTTP-004 raw R return type is forbidden");
        }
        boolean persistenceModel = containsPersistenceModel(returnType);
        boolean dtoModel = containsDtoModel(returnType);
        boolean dynamicMap = containsDynamicMap(returnType);
        if (persistenceModel) {
            violation(context, method, "MANGO-ARCH-HTTP-002 HTTP method must not expose Entity/PO");
        }
        if (dtoModel) {
            violation(context, method, "MANGO-ARCH-HTTP-006 HTTP method must not expose DTO");
        }
        if (dynamicMap) {
            violation(
                    context,
                    method,
                    "MANGO-ARCH-HTTP-005 Map<String, Object> is forbidden in HTTP contracts");
        }
        if (hasInvalidResultPayload(returnType, persistenceModel, dtoModel, dynamicMap)) {
            violation(
                    context,
                    method,
                    "MANGO-ARCH-HTTP-007 R payload must be a scalar, enum, VO, Response, collection"
                            + " or PageResult");
        }
    }

    private boolean isRawResult(JTypeMirror returnType) {
        if (!(returnType instanceof JClassType classType)) {
            return false;
        }
        return classType.isRaw() || classType.getTypeArgs().isEmpty();
    }

    private boolean hasInvalidResultPayload(
            JTypeMirror returnType,
            boolean persistenceModel,
            boolean dtoModel,
            boolean dynamicMap) {
        if (persistenceModel || dtoModel || dynamicMap) {
            return false;
        }
        if (!(returnType instanceof JClassType classType) || classType.isRaw()) {
            return false;
        }
        if (classType.getTypeArgs().size() != 1) {
            return false;
        }
        return !isAllowedApiResponse(classType.getTypeArgs().get(0));
    }

    private void inspectMapper(ASTTypeDeclaration type, RuleContext context) {
        type.descendants(ASTAnnotation.class)
                .filter(
                        annotation ->
                                SQL_ANNOTATIONS.contains(canonicalName(annotation.getTypeMirror())))
                .forEach(
                        annotation ->
                                violation(
                                        context,
                                        annotation,
                                        "MANGO-ARCH-MAPPER-001 Mapper must not declare annotation"
                                                + " SQL"));
        for (ASTFormalParameter parameter : type.descendants(ASTFormalParameter.class)) {
            if (containsApiModel(parameter.getTypeMirror())) {
                violation(
                        context,
                        parameter,
                        "MANGO-ARCH-MAPPER-002 Mapper must not accept API model: "
                                + canonicalName(parameter.getTypeMirror()));
            } else if (containsForbiddenMapperBoundary(parameter.getTypeMirror())) {
                violation(
                        context,
                        parameter,
                        "MANGO-ARCH-MAPPER-007 Mapper parameters must use typed persistence"
                                + " contracts");
            }
        }
        for (ASTMethodDeclaration method : type.getDeclarations(ASTMethodDeclaration.class)) {
            if (!method.isVoid() && containsApiModel(method.getResultTypeNode().getTypeMirror())) {
                violation(
                        context,
                        method,
                        "MANGO-ARCH-MAPPER-003 Mapper must not return Command/Query/VO");
            } else if (!method.isVoid()
                    && containsForbiddenMapperBoundary(
                            method.getResultTypeNode().getTypeMirror())) {
                violation(
                        context,
                        method,
                        "MANGO-ARCH-MAPPER-007 Mapper results must use typed persistence"
                                + " contracts");
            }
        }
    }

    private void inspectPathRules(ASTTypeDeclaration type, RuleContext context) {
        type.descendants(ASTFormalParameter.class)
                .filter(parameter -> hasAnnotation(parameter, PATH_VARIABLE))
                .forEach(
                        parameter ->
                                violation(
                                        context,
                                        parameter,
                                        "MANGO-ARCH-PATH-001 @PathVariable is forbidden; use query"
                                                + " parameter or command"));
        type.descendants(ASTAnnotation.class)
                .filter(
                        annotation ->
                                HTTP_ANNOTATIONS.contains(
                                        canonicalName(annotation.getTypeMirror())))
                .forEach(annotation -> inspectPathAnnotation(annotation, context));
        ASTAnnotation feignClient = type.getAnnotation(FEIGN_CLIENT);
        if (feignClient != null) {
            Object path = feignClient.getAttribute("path");
            if (path instanceof ASTExpression expression
                    && expression.getConstValue() instanceof String text) {
                if (containsRuntimePathPlaceholder(text)) {
                    violation(
                            context,
                            feignClient,
                            "MANGO-ARCH-PATH-003 runtime path placeholders are forbidden in HTTP"
                                    + " adapters");
                } else if (containsUriTemplate(text)) {
                    violation(
                            context,
                            feignClient,
                            "MANGO-ARCH-PATH-002 URI template variables are forbidden in new APIs");
                }
            }
        }
    }

    private void inspectPathAnnotation(ASTAnnotation annotation, RuleContext context) {
        List<String> paths =
                annotation.getFlatValues().toList().stream()
                        .map(this::constantAnnotationValue)
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
        if (paths.stream().anyMatch(this::containsRuntimePathPlaceholder)) {
            violation(
                    context,
                    annotation,
                    "MANGO-ARCH-PATH-003 runtime path placeholders are forbidden in HTTP adapters");
        } else if (paths.stream().anyMatch(this::containsUriTemplate)) {
            violation(
                    context,
                    annotation,
                    "MANGO-ARCH-PATH-002 URI template variables are forbidden in new APIs");
        }
    }

    private Object constantAnnotationValue(Object value) {
        if (value instanceof ASTExpression expression) {
            return expression.getConstValue();
        }
        return null;
    }

    private boolean containsUriTemplate(String path) {
        return path.contains("{") || path.contains("}");
    }

    private boolean containsRuntimePathPlaceholder(String path) {
        return path.contains("${") || path.contains("#{");
    }

    private boolean requestBodyIsOptional(ASTFormalParameter parameter) {
        ASTAnnotation requestBody = parameter.getAnnotation(REQUEST_BODY);
        if (requestBody == null) {
            return false;
        }
        Object required = requestBody.getAttribute("required");
        return required instanceof ASTExpression expression
                && Boolean.FALSE.equals(expression.getConstValue());
    }

    private void inspectProtocolModel(ASTTypeDeclaration type, RuleContext context) {
        String name = type.getSimpleName();
        boolean input = isInputModelName(name);
        if (!input && !isOutputModelName(name)) {
            return;
        }
        String canonicalType = canonicalName(type.getTypeMirror());
        if (isFrameworkProtocolType(canonicalType)) {
            return;
        }
        inspectProtocolStructure(type, input, context);
        if (type.isRecord()) {
            return;
        }
        for (ASTFieldDeclaration field : type.getDeclarations(ASTFieldDeclaration.class)) {
            if (!field.isStatic()) {
                inspectProtocolField(field, input, context);
            }
        }
    }

    private boolean isInputModelName(String name) {
        return name.endsWith(COMMAND_SUFFIX)
                || name.endsWith(QUERY_SUFFIX)
                || name.endsWith(REQUEST_SUFFIX);
    }

    private boolean isOutputModelName(String name) {
        return name.endsWith(VIEW_SUFFIX) || name.endsWith(RESPONSE_SUFFIX);
    }

    private boolean isFrameworkProtocolType(String canonicalType) {
        if (canonicalType.startsWith(JAKARTA_SERVLET_PREFIX)) {
            return true;
        }
        if (canonicalType.startsWith(JAVAX_SERVLET_PREFIX)) {
            return true;
        }
        return canonicalType.startsWith(SPRING_PREFIX);
    }

    private void inspectProtocolStructure(
            ASTTypeDeclaration type, boolean input, RuleContext context) {
        if (type.isInterface() || hasForbiddenProtocolInheritance(type, input)) {
            violation(
                    context,
                    type,
                    "MANGO-ARCH-MODEL-006 protocol model must be a class with only protocol-model"
                            + " inheritance");
        }
        if (type.isRecord()) {
            violation(
                    context,
                    type,
                    "MANGO-ARCH-MODEL-003 protocol records are forbidden; use an explicit class");
        }
    }

    private void inspectProtocolField(
            ASTFieldDeclaration field, boolean input, RuleContext context) {
        if (!hasAnnotation(field, SCHEMA)
                || !hasChineseAttribute(field, SCHEMA, DESCRIPTION_ATTRIBUTE)) {
            violation(
                    context,
                    field,
                    "MANGO-ARCH-MODEL-001 API model field requires @Schema(description)");
        }
        if (input && !hasProtocolFieldConstraint(field)) {
            violation(
                    context,
                    field,
                    "MANGO-ARCH-MODEL-002 Command/Query/Request field requires a "
                            + "jakarta.validation constraint");
        }
        if (requiresCascadeValidation(field, input) && !hasValidAnnotation(field)) {
            violation(
                    context,
                    field,
                    "MANGO-ARCH-MODEL-007 nested input model field requires @Valid cascade"
                            + " validation");
        }
        if (!isAllowedProtocolField(field.getTypeNode().getTypeMirror(), input)) {
            violation(context, field, invalidProtocolFieldMessage(input));
        }
    }

    private boolean requiresCascadeValidation(ASTFieldDeclaration field, boolean input) {
        return input && containsCompositeApiInput(field.getTypeNode().getTypeMirror());
    }

    private boolean hasValidAnnotation(Annotatable owner) {
        return VALID_ANNOTATIONS.stream().anyMatch(name -> hasAnnotation(owner, name));
    }

    private String invalidProtocolFieldMessage(boolean input) {
        if (input) {
            return "MANGO-ARCH-MODEL-004 input model field must be scalar, enum, nested input"
                    + " model or supported container";
        }
        return "MANGO-ARCH-MODEL-005 output model field must be scalar, enum, VO/Response or"
                + " supported container";
    }

    private boolean hasForbiddenProtocolInheritance(ASTTypeDeclaration type, boolean input) {
        if (type.getSuperClassTypeNode() != null) {
            JTypeMirror superclass = type.getSuperClassTypeNode().getTypeMirror();
            if (!isAllowedProtocolSupertype(superclass, input)) {
                return true;
            }
        }
        return type.getSuperInterfaceTypeNodes().toList().stream()
                .map(node -> node.getTypeMirror())
                .anyMatch(supertype -> !JAVA_SERIALIZABLE.equals(canonicalName(supertype)));
    }

    private boolean isAllowedProtocolSupertype(JTypeMirror type, boolean input) {
        String name = canonicalName(type);
        String simple = simpleName(name);
        if (JAVA_OBJECT.equals(name)) {
            return true;
        }
        if (input) {
            return isInputModelName(simple);
        }
        return isOutputModelName(simple);
    }

    private boolean isAllowedProtocolField(JTypeMirror type, boolean input) {
        if (type == null || type instanceof JWildcardType) {
            return false;
        }
        if (type.isPrimitive() || type.isBoxedPrimitive()) {
            return true;
        }
        return isAllowedReferenceProtocolField(type, input);
    }

    private boolean isAllowedReferenceProtocolField(JTypeMirror type, boolean input) {
        if (type instanceof JArrayType arrayType) {
            return isAllowedProtocolField(arrayType.getComponentType(), input);
        }
        String name = canonicalName(type);
        String simple = simpleName(name);
        if (isScalarOrEnum(type, name)) {
            return true;
        }
        if (isForbiddenProtocolFieldType(type, name)) {
            return false;
        }
        return isAllowedComplexProtocolField(type, name, simple, input);
    }

    private boolean isAllowedComplexProtocolField(
            JTypeMirror type, String name, String simple, boolean input) {
        if (isAllowedNestedProtocolModel(simple, input)) {
            return true;
        }
        if (!(type instanceof JClassType classType)) {
            return false;
        }
        return isAllowedProtocolContainer(classType, name, simple, input);
    }

    private boolean isScalarOrEnum(JTypeMirror type, String canonicalName) {
        if (API_SCALAR_TYPES.contains(canonicalName)) {
            return true;
        }
        return type.getSymbol() instanceof JClassSymbol symbol && symbol.isEnum();
    }

    private boolean isForbiddenProtocolFieldType(JTypeMirror type, String name) {
        if (JAVA_OBJECT.equals(name) || JAVA_MAP.equals(name)) {
            return true;
        }
        if (containsPersistenceModel(type) || containsDtoModel(type)) {
            return true;
        }
        return containsTransportContext(type);
    }

    private boolean isAllowedNestedProtocolModel(String simpleName, boolean input) {
        if (input) {
            return isInputModelName(simpleName);
        }
        return isOutputModelName(simpleName);
    }

    private boolean isAllowedProtocolContainer(
            JClassType type, String name, String simpleName, boolean input) {
        if (type.isRaw() || type.getTypeArgs().size() != 1) {
            return false;
        }
        boolean supported = API_MODEL_FIELD_CONTAINERS.contains(name);
        if (!input && simpleName.endsWith(PAGE_RESULT_SUFFIX)) {
            supported = true;
        }
        if (!supported) {
            return false;
        }
        return isAllowedProtocolField(type.getTypeArgs().get(0), input);
    }

    private boolean hasJakartaConstraint(Annotatable owner) {
        return owner.getDeclaredAnnotations()
                .any(
                        annotation ->
                                canonicalName(annotation.getTypeMirror())
                                        .startsWith("jakarta.validation.constraints."));
    }

    private boolean hasProtocolFieldConstraint(ASTFieldDeclaration field) {
        if (hasJakartaConstraint(field)) {
            return true;
        }
        return containsCompositeApiInput(field.getTypeNode().getTypeMirror())
                && hasValidAnnotation(field);
    }

    private boolean containsCompositeApiInput(JTypeMirror type) {
        return containsTypeMatching(
                type,
                name -> {
                    String simple = simpleName(name);
                    return simple.endsWith("Command")
                            || simple.endsWith("Query")
                            || simple.endsWith("Request");
                });
    }

    private boolean isCompositeApiInput(JTypeMirror type) {
        String simple = simpleName(canonicalName(type));
        return simple.endsWith("Command") || simple.endsWith("Query") || simple.endsWith("Request");
    }

    private boolean isWriteRequestModel(JTypeMirror type) {
        return containsTypeMatching(
                type,
                name -> {
                    String simple = simpleName(name);
                    return simple.endsWith("Command") || simple.endsWith("Request");
                });
    }

    private boolean containsTransportContext(JTypeMirror type) {
        return containsTypeMatching(
                type,
                name ->
                        name.startsWith(JAKARTA_SERVLET_PREFIX)
                                || name.startsWith(JAVAX_SERVLET_PREFIX)
                                || name.startsWith(SPRING_HTTP_PREFIX)
                                || name.startsWith(SPRING_MULTIPART_PREFIX));
    }

    private boolean isAllowedApiInput(JTypeMirror type) {
        if (type == null) {
            return false;
        }
        if (type.isPrimitive() || type.isBoxedPrimitive()) {
            return true;
        }
        String name = canonicalName(type);
        String simple = simpleName(name);
        if (API_SCALAR_TYPES.contains(name) || isInputModelName(simple)) {
            return true;
        }
        return type.getSymbol() instanceof JClassSymbol symbol && symbol.isEnum();
    }

    private boolean isAllowedApiResponse(JTypeMirror type) {
        if (type == null) {
            return false;
        }
        if (type instanceof JArrayType arrayType) {
            return isAllowedApiResponse(arrayType.getComponentType());
        }
        if (isSimpleApiResponse(type)) {
            return true;
        }
        if (!(type instanceof JClassType classType)) {
            return false;
        }
        String name = canonicalName(type);
        return isAllowedResponseContainer(classType, name, simpleName(name));
    }

    private boolean isSimpleApiResponse(JTypeMirror type) {
        if (type.isPrimitive() || type.isBoxedPrimitive() || type.isVoid()) {
            return true;
        }
        String name = canonicalName(type);
        String simple = simpleName(name);
        if (API_SCALAR_TYPES.contains(name) || isOutputModelName(simple)) {
            return true;
        }
        return type.getSymbol() instanceof JClassSymbol symbol && symbol.isEnum();
    }

    private boolean isAllowedResponseContainer(
            JClassType type, String canonicalName, String simpleName) {
        if (type.getTypeArgs().size() != 1) {
            return false;
        }
        boolean allowed = API_RESPONSE_CONTAINERS.contains(canonicalName);
        if (simpleName.endsWith(PAGE_RESULT_SUFFIX)) {
            allowed = true;
        }
        if (!allowed) {
            return false;
        }
        return isAllowedApiResponse(type.getTypeArgs().get(0));
    }

    private boolean isService(ASTTypeDeclaration type) {
        if (type.hasModifiers(JModifier.ABSTRACT)) {
            return false;
        }
        if (hasAnnotation(type, SPRING_SERVICE)
                || type.getSimpleName().endsWith(SERVICE_IMPL_SUFFIX)) {
            return true;
        }
        return !type.isInterface() && type.getSimpleName().endsWith(SERVICE_SUFFIX);
    }

    private boolean isServiceContract(ASTTypeDeclaration type) {
        return type.isInterface() && type.getSimpleName().matches("I[A-Z].*Service");
    }

    private boolean isMapper(ASTTypeDeclaration type) {
        return hasAnnotation(type, MYBATIS_MAPPER) || type.getSimpleName().endsWith("Mapper");
    }

    private boolean requiresBusinessPrecondition(ASTMethodDeclaration method) {
        if (method.getVisibility() != ModifierOwner.Visibility.V_PUBLIC) {
            return false;
        }
        if (method.getArity() == 0 || method.getBody() == null) {
            return false;
        }
        boolean commandMethod =
                method.descendants(ASTFormalParameter.class)
                        .any(parameter -> isWriteRequestModel(parameter.getTypeMirror()));
        String normalized = method.getName().toLowerCase(Locale.ROOT);
        return commandMethod || BUSINESS_ACTION_PREFIXES.stream().anyMatch(normalized::startsWith);
    }

    private boolean containsRequireCall(ASTMethodDeclaration method) {
        return method.getBody()
                .descendants(ASTMethodCall.class)
                .any(call -> isCallOn(call, REQUIRE));
    }

    private boolean isHandledControllerReturn(ASTReturnStatement statement) {
        ASTExpression expression = statement.getExpr();
        if (expression instanceof ASTMethodCall call && isCallOn(call, RESULT_R)) {
            return true;
        }
        return expression instanceof ASTConstructorCall call
                && isType(call.getTypeMirror(), RESULT_R);
    }

    private boolean implementsApiContract(ASTTypeDeclaration type) {
        return type.getSuperInterfaceTypeNodes().toList().stream()
                .anyMatch(node -> node.getSimpleName().endsWith("Api"));
    }

    private boolean hasValidServiceDelegation(
            ASTTypeDeclaration controller, ASTMethodDeclaration method) {
        if (!hasServiceField(controller)) {
            return false;
        }
        List<ASTReturnStatement> returns = method.descendants(ASTReturnStatement.class).toList();
        if (returns.isEmpty()) {
            return false;
        }
        return returns.stream().allMatch(statement -> isValidServiceReturn(method, statement));
    }

    private boolean hasServiceField(ASTTypeDeclaration controller) {
        return controller
                .getDeclarations(ASTFieldDeclaration.class)
                .any(field -> isAllowedControllerServicePort(field.getTypeNode().getTypeMirror()));
    }

    private boolean isValidServiceReturn(
            ASTMethodDeclaration method, ASTReturnStatement statement) {
        if (!(statement.getExpr() instanceof ASTMethodCall resultCall)) {
            return false;
        }
        if (!isCallOn(resultCall, RESULT_R)
                || !RESULT_OK_METHOD.equals(resultCall.getMethodName())) {
            return false;
        }
        if (resultCall.getArguments().isEmpty()) {
            return method.descendants(ASTMethodCall.class).any(this::isServicePortCall);
        }
        if (resultCall.getArguments().size() != 1) {
            return false;
        }
        if (!(resultCall.getArguments().get(0) instanceof ASTMethodCall serviceCall)) {
            return false;
        }
        return isServicePortCall(serviceCall);
    }

    private boolean isServicePortCall(ASTMethodCall call) {
        ASTExpression qualifier = call.getQualifier();
        if (qualifier != null
                && simpleName(canonicalName(qualifier.getTypeMirror()))
                        .matches("I[A-Z].*Service")) {
            return true;
        }
        if (call.getOverloadSelectionInfo().isFailed()) {
            return false;
        }
        String declaringType =
                canonicalName(call.getOverloadSelectionInfo().getMethodType().getDeclaringType());
        return simpleName(declaringType).matches("I[A-Z].*Service")
                || MANGO_CRUD_SERVICE.equals(declaringType)
                || MANGO_TYPED_CRUD_SERVICE.equals(declaringType);
    }

    private boolean isAllowedControllerServicePort(JTypeMirror type) {
        String simpleName = simpleName(canonicalName(type));
        return simpleName.matches("I[A-Z].*Service");
    }

    private boolean isCallOn(ASTMethodCall call, String canonicalTypeName) {
        if (call.getOverloadSelectionInfo().isFailed()) {
            return false;
        }
        JTypeMirror declaringType =
                call.getOverloadSelectionInfo().getMethodType().getDeclaringType();
        return isType(declaringType, canonicalTypeName);
    }

    private boolean isHttpMethod(ASTMethodDeclaration method) {
        return HTTP_ANNOTATIONS.stream().anyMatch(name -> hasAnnotation(method, name));
    }

    private boolean hasAnnotation(Annotatable owner, String canonicalName) {
        return owner.getDeclaredAnnotations()
                .any(annotation -> canonicalName.equals(canonicalName(annotation.getTypeMirror())));
    }

    private boolean hasChineseAttribute(
            Annotatable owner, String annotationType, String attributeName) {
        ASTAnnotation annotation = owner.getAnnotation(annotationType);
        if (annotation == null) {
            return false;
        }
        Object value = annotation.getAttribute(attributeName);
        if (!(value instanceof ASTExpression expression)
                || !(expression.getConstValue() instanceof String text)
                || text.isBlank()) {
            return false;
        }
        return text.codePoints()
                .anyMatch(
                        codePoint ->
                                Character.UnicodeScript.of(codePoint)
                                        == Character.UnicodeScript.HAN);
    }

    private boolean isBusinessCodeArgument(ASTMethodCall call, int index) {
        if (call.getArguments().size() <= index) {
            return false;
        }
        JTypeMirror argumentType = call.getArguments().get(index).getTypeMirror();
        String argumentName = canonicalName(argumentType);
        if (!simpleName(argumentName).endsWith(CODE_SUFFIX)) {
            return false;
        }
        if (!isEnumType(argumentType)) {
            return false;
        }
        String expectedPrefix = expectedBusinessCodePrefix(call);
        return !expectedPrefix.isEmpty() && argumentName.startsWith(expectedPrefix);
    }

    private boolean isEnumType(JTypeMirror type) {
        return type != null && type.getSymbol() instanceof JClassSymbol symbol && symbol.isEnum();
    }

    private String expectedBusinessCodePrefix(ASTMethodCall call) {
        ASTTypeDeclaration serviceType = call.ancestors(ASTTypeDeclaration.class).first();
        if (serviceType == null) {
            return "";
        }
        String serviceName = canonicalName(serviceType.getTypeMirror());
        int coreMarker = serviceName.indexOf(".core.");
        if (coreMarker < 0) {
            return "";
        }
        return serviceName.substring(0, coreMarker) + ".api.enums.";
    }

    private int businessCodeParameterIndex(ASTMethodCall call) {
        if (call.getOverloadSelectionInfo().isFailed()) {
            return MISSING_PARAMETER_INDEX;
        }
        int result = MISSING_PARAMETER_INDEX;
        List<JTypeMirror> parameters =
                call.getOverloadSelectionInfo().getMethodType().getFormalParameters();
        for (int index = 0; index < parameters.size(); index++) {
            if (!BIZ_CODE.equals(canonicalName(parameters.get(index)))) {
                continue;
            }
            if (result >= 0) {
                return AMBIGUOUS_PARAMETER_INDEX;
            }
            result = index;
        }
        return result;
    }

    private String resolvedBusinessCodeParameter(ASTMethodCall call) {
        int codeParameterIndex = businessCodeParameterIndex(call);
        if (codeParameterIndex == AMBIGUOUS_PARAMETER_INDEX) {
            return "<ambiguous>";
        }
        if (codeParameterIndex < 0) {
            return "<missing>";
        }
        JTypeMirror formalType =
                call.getOverloadSelectionInfo()
                        .getMethodType()
                        .getFormalParameters()
                        .get(codeParameterIndex);
        return canonicalName(formalType);
    }

    private String resolvedArgumentDetails(ASTMethodCall call, int index) {
        if (index < 0 || call.getArguments().size() <= index) {
            return "";
        }
        JTypeMirror argumentType = call.getArguments().get(index).getTypeMirror();
        String superTypes = resolvedSuperTypes(argumentType);
        return "; argument=" + canonicalName(argumentType) + "; supers=" + superTypes;
    }

    private String resolvedSuperTypes(JTypeMirror type) {
        if (type == null) {
            return "";
        }
        return type.getSuperTypeSet().stream()
                .map(this::canonicalName)
                .sorted()
                .collect(java.util.stream.Collectors.joining("|"));
    }

    private boolean isPersistenceModel(JTypeMirror type) {
        String name = canonicalName(type);
        return name.endsWith(ENTITY_SUFFIX) || name.endsWith(PO_SUFFIX);
    }

    private boolean containsPersistenceModel(JTypeMirror type) {
        return containsTypeMatching(
                type, name -> name.endsWith(ENTITY_SUFFIX) || name.endsWith(PO_SUFFIX));
    }

    private boolean containsDtoModel(JTypeMirror type) {
        return containsTypeMatching(type, name -> simpleName(name).endsWith(DTO_SUFFIX));
    }

    private boolean containsApiModel(JTypeMirror type) {
        return containsTypeMatching(
                type,
                name -> {
                    String simple = simpleName(name);
                    return isInputModelName(simple)
                            || isOutputModelName(simple)
                            || simple.endsWith(DTO_SUFFIX);
                });
    }

    private boolean containsForbiddenMapperBoundary(JTypeMirror type) {
        return containsTypeMatching(
                type,
                name -> {
                    return isForbiddenMapperBoundaryName(name);
                });
    }

    private boolean isForbiddenMapperBoundaryName(String name) {
        if (JAVA_OBJECT.equals(name) || JAVA_MAP.equals(name)) {
            return true;
        }
        if (name.startsWith(JAKARTA_SERVLET_PREFIX) || name.startsWith(JAVAX_SERVLET_PREFIX)) {
            return true;
        }
        if (name.startsWith(SPRING_HTTP_PREFIX) || name.startsWith(SPRING_WEB_PREFIX)) {
            return true;
        }
        String simple = simpleName(name);
        if (simple.endsWith(CONTROLLER_SUFFIX) || simple.endsWith(FEIGN_CLIENT_SUFFIX)) {
            return true;
        }
        return simple.endsWith(SERVICE_SUFFIX);
    }

    private boolean containsTypeMatching(JTypeMirror type, Predicate<String> predicate) {
        if (type == null) {
            return false;
        }
        if (predicate.test(canonicalName(type))) {
            return true;
        }
        if (type instanceof JClassType classType) {
            return classType.getTypeArgs().stream()
                    .anyMatch(argument -> containsTypeMatching(argument, predicate));
        }
        if (type instanceof JArrayType arrayType) {
            return containsTypeMatching(arrayType.getComponentType(), predicate);
        }
        if (!(type instanceof JWildcardType wildcardType) || wildcardType.isUnbounded()) {
            return false;
        }
        return containsTypeMatching(wildcardType.getBound(), predicate);
    }

    private boolean containsDynamicMap(JTypeMirror type) {
        if (!(type instanceof JClassType classType)) {
            if (type instanceof JArrayType arrayType) {
                return containsDynamicMap(arrayType.getComponentType());
            }
            if (!(type instanceof JWildcardType wildcardType) || wildcardType.isUnbounded()) {
                return false;
            }
            return containsDynamicMap(wildcardType.getBound());
        }
        if (isDynamicMap(classType)) {
            return true;
        }
        return classType.getTypeArgs().stream().anyMatch(this::containsDynamicMap);
    }

    private boolean isDynamicMap(JClassType type) {
        if (!JAVA_MAP.equals(canonicalName(type))) {
            return false;
        }
        if (type.getTypeArgs().size() != MAP_TYPE_ARGUMENT_COUNT) {
            return false;
        }
        String keyType = canonicalName(type.getTypeArgs().get(0));
        String valueType = canonicalName(type.getTypeArgs().get(1));
        return JAVA_STRING.equals(keyType) && JAVA_OBJECT.equals(valueType);
    }

    private boolean isType(JTypeMirror type, String canonicalTypeName) {
        return canonicalTypeName.equals(canonicalName(type));
    }

    private String canonicalName(JTypeMirror type) {
        if (type == null) {
            return "";
        }
        if (type.getSymbol() instanceof JClassSymbol symbol) {
            String name = symbol.getCanonicalName();
            if (name != null) {
                return name;
            }
        }
        String fallback = type.toString();
        if (fallback == null) {
            return "";
        }
        return fallback;
    }

    private String simpleName(String canonicalName) {
        int separator = canonicalName.lastIndexOf('.');
        if (separator < 0) {
            return canonicalName;
        }
        return canonicalName.substring(separator + 1);
    }

    private void violation(
            RuleContext context, net.sourceforge.pmd.lang.ast.Node node, String message) {
        context.addViolationWithMessage(node, message);
    }
}
