package io.mango.plugin.check;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Resolves {@code setTenantId} receiver types without compiling project sources. */
final class TenantSetterAnalyzer {

    private static final String API_PACKAGE_SEGMENT = ".api.";
    private static final String ARRAY_SUFFIX = "[]";
    private static final String DISABLE_ANNOTATION_PROCESSING = "-proc:none";
    private static final String INNER_THIS_SUFFIX = ".this";
    private static final String SET_TENANT_ID_METHOD = "setTenantId";
    private static final String THIS_EXPRESSION = "this";
    private static final int UNKNOWN_SOURCE_LINE = 1;
    private static final Set<String> API_PROTOCOL_TYPE_SUFFIXES =
            Set.of("Command", "Query", "VO", "DTO", "Request", "Response");

    private TenantSetterAnalyzer() {}

    static AnalysisResult analyze(List<Path> candidateFiles) throws IOException {
        Set<Path> normalizedCandidates = normalize(candidateFiles);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new AnalysisResult(Map.of(), normalizedCandidates);
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = createTask(compiler, fileManager, diagnostics, candidateFiles);
            List<CompilationUnitTree> units = parse(task);
            Set<Path> invalidSources = invalidSources(diagnostics);
            return inspect(task, units, normalizedCandidates, invalidSources);
        }
    }

    private static JavacTask createTask(
            JavaCompiler compiler,
            StandardJavaFileManager fileManager,
            DiagnosticCollector<JavaFileObject> diagnostics,
            List<Path> candidateFiles) {
        Iterable<? extends JavaFileObject> sources =
                fileManager.getJavaFileObjectsFromPaths(candidateFiles);
        return (JavacTask)
                compiler.getTask(
                        null,
                        fileManager,
                        diagnostics,
                        List.of(DISABLE_ANNOTATION_PROCESSING),
                        null,
                        sources);
    }

    private static List<CompilationUnitTree> parse(JavacTask task) throws IOException {
        List<CompilationUnitTree> units = new ArrayList<>();
        task.parse().forEach(units::add);
        return units;
    }

    private static AnalysisResult inspect(
            JavacTask task,
            List<CompilationUnitTree> units,
            Set<Path> candidates,
            Set<Path> invalidSources) {
        Map<Path, Integer> unsafeLines = new LinkedHashMap<>();
        Set<Path> analyzedSources = new HashSet<>();
        Set<Path> unresolvedSources = new LinkedHashSet<>(invalidSources);
        SourcePositions positions = Trees.instance(task).getSourcePositions();

        for (CompilationUnitTree unit : units) {
            Path source = sourcePath(unit.getSourceFile());
            analyzedSources.add(source);
            if (invalidSources.contains(source)) {
                continue;
            }
            int line = firstUnsafeLine(unit, positions);
            if (line > 0) {
                unsafeLines.put(source, line);
            }
        }
        candidates.stream()
                .filter(candidate -> !analyzedSources.contains(candidate))
                .forEach(unresolvedSources::add);
        return new AnalysisResult(unsafeLines, unresolvedSources);
    }

    private static Set<Path> invalidSources(
            DiagnosticCollector<JavaFileObject> diagnostics) {
        Set<Path> invalidSources = new HashSet<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR && diagnostic.getSource() != null) {
                invalidSources.add(sourcePath(diagnostic.getSource()));
            }
        }
        return invalidSources;
    }

    private static int firstUnsafeLine(
            CompilationUnitTree unit, SourcePositions sourcePositions) {
        TenantSetterScanner scanner =
                new TenantSetterScanner(unit, sourcePositions, explicitImports(unit));
        scanner.scan(unit, null);
        return scanner.unsafeLine();
    }

    private static Map<String, String> explicitImports(CompilationUnitTree unit) {
        Map<String, String> imports = new LinkedHashMap<>();
        for (ImportTree importTree : unit.getImports()) {
            String importedType = importTree.getQualifiedIdentifier().toString();
            if (!importTree.isStatic() && !importedType.endsWith(".*")) {
                imports.put(simpleTypeName(importedType), importedType);
            }
        }
        return imports;
    }

    private static boolean isApiProtocolType(
            String declaredType,
            Map<String, String> explicitImports,
            CompilationUnitTree unit) {
        String simpleType = simpleTypeName(declaredType);
        if (API_PROTOCOL_TYPE_SUFFIXES.stream().noneMatch(simpleType::endsWith)) {
            return false;
        }
        if (declaredType.contains(API_PACKAGE_SEGMENT)) {
            return true;
        }
        String explicitImport = explicitImports.get(simpleType);
        if (explicitImport != null) {
            return explicitImport.contains(API_PACKAGE_SEGMENT);
        }
        return unit.getPackageName() != null
                && unit.getPackageName().toString().contains(API_PACKAGE_SEGMENT);
    }

    private static String simpleTypeName(String declaredType) {
        String type = declaredType.trim();
        int genericStart = type.indexOf('<');
        if (genericStart >= 0) {
            type = type.substring(0, genericStart);
        }
        while (type.endsWith(ARRAY_SUFFIX)) {
            type = type.substring(0, type.length() - ARRAY_SUFFIX.length());
        }
        int packageSeparator = type.lastIndexOf('.');
        return packageSeparator < 0 ? type : type.substring(packageSeparator + 1);
    }

    private static Set<Path> normalize(List<Path> files) {
        Set<Path> normalized = new LinkedHashSet<>();
        files.stream()
                .map(file -> file.toAbsolutePath().normalize())
                .forEach(normalized::add);
        return normalized;
    }

    private static Path sourcePath(JavaFileObject source) {
        return Paths.get(source.toUri()).toAbsolutePath().normalize();
    }

    record AnalysisResult(Map<Path, Integer> unsafeLines, Set<Path> unresolvedFiles) {
        AnalysisResult {
            unsafeLines = Map.copyOf(unsafeLines);
            unresolvedFiles = Set.copyOf(unresolvedFiles);
        }
    }

    private static final class TenantSetterScanner extends TreeScanner<Void, Void> {

        private final CompilationUnitTree unit;
        private final SourcePositions sourcePositions;
        private final Map<String, String> explicitImports;
        private final Deque<Map<String, String>> lexicalScopes = new ArrayDeque<>();
        private final Deque<Map<String, String>> classFields = new ArrayDeque<>();
        private int unsafeLine;

        private TenantSetterScanner(
                CompilationUnitTree unit,
                SourcePositions sourcePositions,
                Map<String, String> explicitImports) {
            this.unit = unit;
            this.sourcePositions = sourcePositions;
            this.explicitImports = explicitImports;
        }

        private int unsafeLine() {
            return unsafeLine;
        }

        @Override
        public Void visitClass(ClassTree classTree, Void unused) {
            classFields.push(declaredFields(classTree));
            try {
                return super.visitClass(classTree, unused);
            } finally {
                classFields.pop();
            }
        }

        @Override
        public Void visitMethod(MethodTree method, Void unused) {
            return scanWithScope(
                    declaredVariables(method.getParameters()),
                    () -> super.visitMethod(method, unused));
        }

        @Override
        public Void visitBlock(BlockTree block, Void unused) {
            return scanWithScope(new LinkedHashMap<>(), () -> super.visitBlock(block, unused));
        }

        @Override
        public Void visitLambdaExpression(LambdaExpressionTree lambda, Void unused) {
            return scanWithScope(
                    declaredVariables(lambda.getParameters()),
                    () -> super.visitLambdaExpression(lambda, unused));
        }

        @Override
        public Void visitForLoop(ForLoopTree loop, Void unused) {
            return scanWithScope(new LinkedHashMap<>(), () -> super.visitForLoop(loop, unused));
        }

        @Override
        public Void visitEnhancedForLoop(EnhancedForLoopTree loop, Void unused) {
            return scanWithScope(
                    new LinkedHashMap<>(), () -> super.visitEnhancedForLoop(loop, unused));
        }

        @Override
        public Void visitCatch(CatchTree catchTree, Void unused) {
            return scanWithScope(new LinkedHashMap<>(), () -> super.visitCatch(catchTree, unused));
        }

        @Override
        public Void visitVariable(VariableTree variable, Void unused) {
            if (!lexicalScopes.isEmpty() && variable.getType() != null) {
                lexicalScopes
                        .peek()
                        .put(variable.getName().toString(), variable.getType().toString());
            }
            return super.visitVariable(variable, unused);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
            if (unsafeLine > 0) {
                return null;
            }
            if (isTenantSetter(invocation) && !isApiProtocolReceiver(invocation)) {
                unsafeLine = invocationLine(invocation);
                return null;
            }
            return super.visitMethodInvocation(invocation, unused);
        }

        private boolean isTenantSetter(MethodInvocationTree invocation) {
            return invocation.getMethodSelect() instanceof MemberSelectTree method
                    && method.getIdentifier().contentEquals(SET_TENANT_ID_METHOD);
        }

        private boolean isApiProtocolReceiver(MethodInvocationTree invocation) {
            MemberSelectTree method = (MemberSelectTree) invocation.getMethodSelect();
            String receiverType = receiverType(method.getExpression());
            return receiverType != null
                    && isApiProtocolType(receiverType, explicitImports, unit);
        }

        private int invocationLine(MethodInvocationTree invocation) {
            long start = sourcePositions.getStartPosition(unit, invocation);
            return start < 0 ? UNKNOWN_SOURCE_LINE : (int) unit.getLineMap().getLineNumber(start);
        }

        private String receiverType(ExpressionTree receiver) {
            if (receiver instanceof IdentifierTree identifier) {
                return resolveVariable(identifier.getName().toString());
            }
            if (receiver instanceof MemberSelectTree member && isThisReference(member)) {
                return resolveField(member.getIdentifier().toString());
            }
            if (receiver instanceof NewClassTree newClass) {
                return newClass.getIdentifier().toString();
            }
            if (receiver instanceof TypeCastTree cast) {
                return cast.getType().toString();
            }
            if (receiver instanceof ParenthesizedTree parenthesized) {
                return receiverType(parenthesized.getExpression());
            }
            return null;
        }

        private boolean isThisReference(MemberSelectTree member) {
            String expression = member.getExpression().toString();
            return THIS_EXPRESSION.equals(expression) || expression.endsWith(INNER_THIS_SUFFIX);
        }

        private String resolveVariable(String name) {
            for (Map<String, String> scope : lexicalScopes) {
                String type = scope.get(name);
                if (type != null) {
                    return type;
                }
            }
            return resolveField(name);
        }

        private String resolveField(String name) {
            for (Map<String, String> fields : classFields) {
                String type = fields.get(name);
                if (type != null) {
                    return type;
                }
            }
            return null;
        }

        private Void scanWithScope(Map<String, String> scope, Supplier<Void> operation) {
            lexicalScopes.push(scope);
            try {
                return operation.get();
            } finally {
                lexicalScopes.pop();
            }
        }

        private Map<String, String> declaredFields(ClassTree classTree) {
            Map<String, String> fields = new LinkedHashMap<>();
            for (Tree member : classTree.getMembers()) {
                if (member instanceof VariableTree variable && variable.getType() != null) {
                    fields.put(variable.getName().toString(), variable.getType().toString());
                }
            }
            return fields;
        }

        private Map<String, String> declaredVariables(List<? extends VariableTree> variables) {
            Map<String, String> declarations = new LinkedHashMap<>();
            for (VariableTree variable : variables) {
                if (variable.getType() != null) {
                    declarations.put(
                            variable.getName().toString(), variable.getType().toString());
                }
            }
            return declarations;
        }
    }
}
