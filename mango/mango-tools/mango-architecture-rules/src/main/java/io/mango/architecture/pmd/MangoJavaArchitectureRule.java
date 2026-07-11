package io.mango.architecture.pmd;

import java.util.Set;
import java.util.Locale;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.java.ast.ASTAnnotation;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameter;
import net.sourceforge.pmd.lang.java.ast.ASTMethodCall;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.Annotatable;
import net.sourceforge.pmd.lang.java.ast.ModifierOwner;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.java.symbols.JClassSymbol;
import net.sourceforge.pmd.lang.java.types.JTypeMirror;
import net.sourceforge.pmd.reporting.RuleContext;

/** One PMD 7 visitor for all source-semantic Mango architecture rules. */
public final class MangoJavaArchitectureRule extends AbstractJavaRule {

    private static final String REST_CONTROLLER =
            "org.springframework.web.bind.annotation.RestController";
    private static final String VALIDATED =
            "org.springframework.validation.annotation.Validated";
    private static final String REQUEST_BODY =
            "org.springframework.web.bind.annotation.RequestBody";
    private static final Set<String> VALID_ANNOTATIONS = Set.of(
            "jakarta.validation.Valid", "javax.validation.Valid");
    private static final Set<String> SQL_ANNOTATIONS = Set.of(
            "org.apache.ibatis.annotations.Select",
            "org.apache.ibatis.annotations.Insert",
            "org.apache.ibatis.annotations.Update",
            "org.apache.ibatis.annotations.Delete");
    private static final Set<String> HTTP_ANNOTATIONS = Set.of(
            "org.springframework.web.bind.annotation.RequestMapping",
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.DeleteMapping");
    private static final Set<String> BUSINESS_ACTION_PREFIXES = Set.of(
            "create", "update", "delete", "remove", "save", "submit", "approve", "reject",
            "publish", "cancel", "enable", "disable", "assign", "bind", "unbind", "claim",
            "release", "execute");

    public MangoJavaArchitectureRule() {
        setLanguage(LanguageRegistry.PMD.getLanguageById("java"));
        setName("MangoJavaArchitecture");
        setMessage("Mango Java/Spring architecture violation");
    }

    @Override
    public Object visit(ASTCompilationUnit node, Object data) {
        RuleContext context = asCtx(data);
        for (ASTTypeDeclaration type : node.getTypeDeclarations()) {
            if (hasAnnotation(type, REST_CONTROLLER)) {
                inspectController(type, context);
            }
            if (type.isInterface() && type.getSimpleName().endsWith("Api")) {
                inspectHttpReturns(type, context);
            }
            if (isService(type)) {
                inspectService(type, context);
            }
            if (isMapper(type)) {
                inspectMapper(type, context);
            }
        }
        return data;
    }

    private void inspectController(ASTTypeDeclaration type, RuleContext context) {
        if (!hasAnnotation(type, VALIDATED)) {
            violation(context, type, "MANGO-ARCH-CTRL-001 Controller requires @Validated");
        }
        for (ASTFieldDeclaration field : type.getDeclarations(ASTFieldDeclaration.class)) {
            String fieldType = canonicalName(field.getTypeNode().getTypeMirror());
            if (isForbiddenControllerField(fieldType)) {
                violation(context, field,
                        "MANGO-ARCH-CTRL-002 Controller must depend on a service interface: " + fieldType);
            }
        }
        for (ASTMethodDeclaration method : type.getDeclarations(ASTMethodDeclaration.class)) {
            inspectHttpReturn(method, context);
            for (ASTFormalParameter parameter : method.descendants(ASTFormalParameter.class)) {
                if (hasAnnotation(parameter, REQUEST_BODY)
                        && VALID_ANNOTATIONS.stream().noneMatch(name -> hasAnnotation(parameter, name))) {
                    violation(context, parameter,
                            "MANGO-ARCH-CTRL-003 @RequestBody parameter requires @Valid");
                }
            }
        }
        for (ASTMethodCall call : type.descendants(ASTMethodCall.class)) {
            if (isCallOn(call, "R") && "fail".equals(call.getMethodName())) {
                violation(context, call, "MANGO-ARCH-CTRL-004 Controller must not call R.fail");
            }
        }
    }

    private void inspectService(ASTTypeDeclaration type, RuleContext context) {
        for (ASTMethodDeclaration method : type.getDeclarations(ASTMethodDeclaration.class)) {
            if (!method.isVoid() && isType(method.getResultTypeNode().getTypeMirror(), "R")) {
                violation(context, method, "MANGO-ARCH-SVC-001 Service must not return R<T>");
            }
            if (requiresBusinessPrecondition(method) && !containsRequireCall(method)) {
                violation(context, method,
                        "MANGO-ARCH-SVC-004 business action requires a Require precondition");
            }
        }
        for (ASTMethodCall call : type.descendants(ASTMethodCall.class)) {
            if (isCallOn(call, "R")) {
                violation(context, call, "MANGO-ARCH-SVC-002 Service must not call R methods");
            }
            if (isCallOn(call, "Require")) {
                int codeParameterIndex = "fail".equals(call.getMethodName()) ? 0 : 1;
                if (call.getArguments().size() <= codeParameterIndex
                        || !isBusinessCodeParameter(call)) {
                    violation(context, call,
                            "MANGO-ARCH-SVC-003 Require must receive a BizCode/ErrorCode; resolved="
                                    + resolvedBusinessCodeParameter(call)
                                    + resolvedArgumentDetails(call, codeParameterIndex));
                }
            }
        }
    }

    private void inspectHttpReturns(ASTTypeDeclaration type, RuleContext context) {
        for (ASTMethodDeclaration method : type.getDeclarations(ASTMethodDeclaration.class)) {
            inspectHttpReturn(method, context);
        }
    }

    private void inspectHttpReturn(ASTMethodDeclaration method, RuleContext context) {
        if (!isHttpMethod(method) || method.isVoid()) {
            return;
        }
        JTypeMirror returnType = method.getResultTypeNode().getTypeMirror();
        if (!isType(returnType, "R")) {
            violation(context, method, "MANGO-ARCH-HTTP-001 HTTP method must return R<T>");
            return;
        }
        if (returnType instanceof net.sourceforge.pmd.lang.java.types.JClassType classType
                && classType.getTypeArgs().stream().anyMatch(this::isPersistenceModel)) {
            violation(context, method,
                    "MANGO-ARCH-HTTP-002 HTTP method must not expose Entity/PO");
        }
    }

    private void inspectMapper(ASTTypeDeclaration type, RuleContext context) {
        type.descendants(ASTAnnotation.class)
                .filter(annotation -> SQL_ANNOTATIONS.contains(canonicalName(annotation.getTypeMirror())))
                .forEach(annotation -> violation(context, annotation,
                        "MANGO-ARCH-MAPPER-001 Mapper must not declare annotation SQL"));
        for (ASTFormalParameter parameter : type.descendants(ASTFormalParameter.class)) {
            String parameterType = canonicalName(parameter.getTypeMirror());
            if (parameterType.endsWith("Command") || parameterType.endsWith("Query")
                    || parameterType.endsWith("VO")) {
                violation(context, parameter,
                        "MANGO-ARCH-MAPPER-002 Mapper must not accept API model: " + parameterType);
            }
        }
    }

    private boolean isService(ASTTypeDeclaration type) {
        return hasAnnotation(type, "org.springframework.stereotype.Service")
                || type.getSimpleName().endsWith("ServiceImpl");
    }

    private boolean isMapper(ASTTypeDeclaration type) {
        return hasAnnotation(type, "org.apache.ibatis.annotations.Mapper")
                || type.getSimpleName().endsWith("Mapper");
    }

    private boolean requiresBusinessPrecondition(ASTMethodDeclaration method) {
        if (method.getVisibility() != ModifierOwner.Visibility.V_PUBLIC
                || method.getArity() == 0 || method.getBody() == null) {
            return false;
        }
        String normalized = method.getName().toLowerCase(Locale.ROOT);
        return BUSINESS_ACTION_PREFIXES.stream().anyMatch(normalized::startsWith);
    }

    private boolean containsRequireCall(ASTMethodDeclaration method) {
        return method.getBody().descendants(ASTMethodCall.class)
                .any(call -> isCallOn(call, "Require"));
    }

    private boolean isForbiddenControllerField(String name) {
        return name.endsWith("Mapper") || name.endsWith("Entity")
                || name.endsWith("ServiceImpl") || name.endsWith("FeignClient");
    }

    private boolean isCallOn(ASTMethodCall call, String simpleTypeName) {
        if (call.getOverloadSelectionInfo().isFailed()) {
            return false;
        }
        JTypeMirror declaringType = call.getOverloadSelectionInfo().getMethodType().getDeclaringType();
        return isType(declaringType, simpleTypeName);
    }

    private boolean isHttpMethod(ASTMethodDeclaration method) {
        return HTTP_ANNOTATIONS.stream().anyMatch(name -> hasAnnotation(method, name));
    }

    private boolean hasAnnotation(Annotatable owner, String canonicalName) {
        return owner.getDeclaredAnnotations().any(annotation ->
                canonicalName.equals(canonicalName(annotation.getTypeMirror())));
    }

    private boolean isBusinessCodeParameter(ASTMethodCall call) {
        String name = resolvedBusinessCodeParameter(call);
        return name.equals("BizCode") || name.endsWith(".BizCode")
                || name.equals("ErrorCode") || name.endsWith(".ErrorCode");
    }

    private String resolvedBusinessCodeParameter(ASTMethodCall call) {
        int codeParameterIndex = "fail".equals(call.getMethodName()) ? 0 : 1;
        if (call.getOverloadSelectionInfo().isFailed()
                || call.getOverloadSelectionInfo().getMethodType()
                .getFormalParameters().size() <= codeParameterIndex) {
            return "<unresolved>";
        }
        JTypeMirror formalType = call.getOverloadSelectionInfo()
                .getMethodType().getFormalParameters().get(codeParameterIndex);
        return canonicalName(formalType);
    }

    private String resolvedArgumentDetails(ASTMethodCall call, int index) {
        if (!"<unresolved>".equals(resolvedBusinessCodeParameter(call))
                || call.getArguments().size() <= index) {
            return "";
        }
        JTypeMirror argumentType = call.getArguments().get(index).getTypeMirror();
        String superTypes = argumentType == null ? "" : argumentType.getSuperTypeSet().stream()
                .map(this::canonicalName).sorted().collect(java.util.stream.Collectors.joining("|"));
        return "; argument=" + canonicalName(argumentType) + "; supers=" + superTypes;
    }

    private boolean isPersistenceModel(JTypeMirror type) {
        String name = canonicalName(type);
        return name.endsWith("Entity") || name.endsWith("PO");
    }

    private boolean isType(JTypeMirror type, String simpleTypeName) {
        String name = canonicalName(type);
        return name.equals(simpleTypeName) || name.endsWith("." + simpleTypeName);
    }

    private String canonicalName(JTypeMirror type) {
        if (type == null) {
            return "";
        }
        if (type.getSymbol() instanceof JClassSymbol symbol) {
            return symbol.getCanonicalName();
        }
        return type.toString();
    }

    private void violation(RuleContext context, net.sourceforge.pmd.lang.ast.Node node, String message) {
        context.addViolationWithMessage(node, message);
    }
}
