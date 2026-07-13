package io.mango.plugin.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.mango.plugin.architecture.GlobalEntityManifestLoader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Mango project-specific check Mojo.
 *
 * <p>`mango:check` only owns Mango-specific rules. Generic Java static analysis such as method
 * length, class length, duplicate code, complexity, and common language best practices are
 * delegated to PMD/P3C, Checkstyle, and SpotBugs.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, aggregator = true)
public class CheckMojo extends AbstractMojo {

    private static final String BLOCK_POLICY = "block";
    private static final String JSON_OUTPUT = "json";
    private static final String POM_FILE_NAME = "pom.xml";
    private static final String POM_PATH_SUFFIX = "/pom.xml";
    private static final String MODULE_PROPERTIES_PATH =
            "src/main/resources/META-INF/mango/module.properties";
    private static final String MODULE_STARTER_ARTIFACT = "mango-infra-module-starter";
    private static final String ROOT_PATH = "/";
    private static final String REVERSE_ROOT_PREFIX = "/_";
    private static final String JAVA_FILE_SUFFIX = ".java";
    private static final String JAVA_SOURCE_PATH = "/src/main/java/";
    private static final String MAIN_RESOURCES_PATH = "/src/main/resources/";
    private static final String XML_FILE_SUFFIX = ".xml";
    private static final String MAPPER_DIRECTORY = "/mapper/";
    private static final String MAPPER_XML_SUFFIX = "Mapper.xml";
    private static final String MYBATIS_RAW_SUBSTITUTION = "${";
    private static final String COMMA_DELIMITER = ",";
    private static final String LINE_SEPARATOR_REGEX = "\\R";
    private static final String MIGRATION_VERSION_SEPARATOR_REGEX = "[._-]";
    private static final String CURRENT_DIRECTORY_PREFIX = "./";
    private static final String SQL_STATEMENT_TERMINATOR = ";";
    private static final String ID_COLUMN = "id";
    private static final String VARARGS_SUFFIX = "...";
    private static final String ARRAY_SUFFIX = "[]";
    private static final String DIGITS_REGEX = "\\d+";
    private static final String TARGET_DIRECTORY_NAME = "target";
    private static final char PARAMETER_SEPARATOR = ',';
    private static final char OPEN_ANGLE_BRACKET = '<';
    private static final char CLOSE_ANGLE_BRACKET = '>';
    private static final char OPEN_PARENTHESIS = '(';
    private static final char CLOSE_PARENTHESIS = ')';
    private static final char SINGLE_QUOTE = '\'';
    private static final char DOUBLE_QUOTE = '"';
    private static final char HYPHEN = '-';
    private static final char ASTERISK = '*';
    private static final char FORWARD_SLASH = '/';
    private static final int METHOD_PARAMETERS_GROUP = 2;
    private static final int MIN_QUOTED_LENGTH = 2;
    private static final String API_SUFFIX = "-api";
    private static final String SUPPORT_SUFFIX = "-support";
    private static final String CORE_SUFFIX = "-core";
    private static final String STARTER_REMOTE_SUFFIX = "-starter-remote";
    private static final String SYNC_STARTER_SUFFIX = "-sync-starter";
    private static final String STARTER_SUFFIX = "-starter";
    private static final String APP_SUFFIX = "-app";
    private static final String MANGO_ARTIFACT_PREFIX = "mango-";
    private static final String API_TYPE_SUFFIX = "Api";
    private static final String ENTITY_TYPE_SUFFIX = "Entity";
    private static final String MAPPER_TYPE_SUFFIX = "Mapper";
    private static final String CONTROLLER_TYPE_SUFFIX = "Controller";
    private static final String SERVICE_TYPE_SUFFIX = "Service";
    private static final String SERVICE_IMPL_TYPE_SUFFIX = "ServiceImpl";
    private static final String FEIGN_CLIENT_TYPE_SUFFIX = "FeignClient";
    private static final String FEIGN_CLIENT_ANNOTATION = "@FeignClient";
    private static final String TABLE_NAME_ANNOTATION = "@TableName";
    private static final String REST_CONTROLLER_ANNOTATION = "@RestController";
    private static final String VALIDATED_ANNOTATION = "@Validated";
    private static final String REQUEST_BODY_ANNOTATION = "@RequestBody";
    private static final String VALID_ANNOTATION = "@Valid";
    private static final String RESULT_FAIL_CALL = "R.fail(";
    private static final String RESULT_OK_CALL = "R.ok(";
    private static final int MAX_DIRECT_API_PARAMETERS = 2;
    private static final Set<String> IGNORED_DECLARED_RETURN_TYPES =
            Set.of("return", "throw", "new");
    private static final Set<String> FORBIDDEN_API_TYPE_SUFFIXES =
            Set.of(
                    ENTITY_TYPE_SUFFIX,
                    MAPPER_TYPE_SUFFIX,
                    CONTROLLER_TYPE_SUFFIX,
                    SERVICE_IMPL_TYPE_SUFFIX,
                    FEIGN_CLIENT_TYPE_SUFFIX);
    private static final Set<String> LOCAL_COLLABORATION_SUFFIXES =
            Set.of(SERVICE_TYPE_SUFFIX, "Manager", "Registry", "Session", "Dispatcher");
    private static final Set<String> LOCAL_IMPLEMENTATION_WORDS =
            Set.of(SERVICE_TYPE_SUFFIX, "Manager", "Registry", "Session", "Dispatcher");
    private static final Set<String> HTTP_CONTRACT_ANNOTATIONS =
            Set.of(
                    "@RequestMapping",
                    "@GetMapping",
                    "@PostMapping",
                    "@PutMapping",
                    "@DeleteMapping",
                    "@PatchMapping");
    private static final Set<ModuleType> CONTROLLER_MODULE_TYPES =
            Set.of(
                    ModuleType.ROOT,
                    ModuleType.OTHER,
                    ModuleType.STARTER,
                    ModuleType.STARTER_REMOTE);
    private static final Set<ModuleType> CORE_MODULE_TYPES =
            Set.of(ModuleType.ROOT, ModuleType.OTHER, ModuleType.CORE);
    private static final List<ModuleSuffix> MODULE_SUFFIXES =
            List.of(
                    new ModuleSuffix(API_SUFFIX, ModuleType.API),
                    new ModuleSuffix(SUPPORT_SUFFIX, ModuleType.SUPPORT),
                    new ModuleSuffix(CORE_SUFFIX, ModuleType.CORE),
                    new ModuleSuffix(STARTER_REMOTE_SUFFIX, ModuleType.STARTER_REMOTE),
                    new ModuleSuffix(SYNC_STARTER_SUFFIX, ModuleType.SYNC_STARTER),
                    new ModuleSuffix(STARTER_SUFFIX, ModuleType.STARTER),
                    new ModuleSuffix(APP_SUFFIX, ModuleType.APP));

    private static final String SOURCE_MANGO_CHECK = "mango-check";
    private static final String SOURCE_PMD = "pmd";
    private static final String SOURCE_CHECKSTYLE = "checkstyle";
    private static final String SOURCE_SPOTBUGS = "spotbugs";
    private static final Set<String> CODE_LEVEL_SOURCES =
            Set.of(SOURCE_PMD, SOURCE_CHECKSTYLE, SOURCE_SPOTBUGS);
    private static final String DOC_AUTO_CHECK_MAPPING = "auto-check-mapping.md";
    private static final String DOC_NAMING_RULES = "naming-rules.md";
    private static final String DOC_MODULE_RULES = "module-rules.md";
    private static final String DOC_API_RULES = "api-rules.md";
    private static final String DOC_TEST_RULES = "test-rules.md";
    private static final String DOC_PERSISTENCE_RULES = "persistence-rules.md";
    private static final String DOC_MODULE_MENU_RULES = "backend/11-module-menu.md";
    private static final String DOC_STATIC_ANALYSIS = "auto-check-mapping.md";
    private static final String MANGO_GROUP_ID_PREFIX = "io.mango";
    private static final String RESOURCE_GROUP_ID = "io.mango.platform.resource";
    private static final Set<String> RESOURCE_RUNTIME_ARTIFACTS =
            Set.of(
                    "mango-resource-core",
                    "mango-resource-support",
                    "mango-resource-starter",
                    "mango-resource-sync-starter",
                    "mango-resource-starter-remote");
    private static final String PATH_VARIABLE_ANNOTATION = "@Path" + "Variable";
    private static final String CHECKSTYLE_REPORT = "checkstyle-result.xml";
    private static final String PMD_REPORT = "pmd.xml";
    private static final String SPOTBUGS_REPORT = "spotbugsXml.xml";
    private static final Pattern MODULE_NAME_PATTERN = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern FEIGN_CLIENT_PATTERN =
            Pattern.compile("@FeignClient\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern REQUEST_MAPPING_PATTERN =
            Pattern.compile(
                    "@(?:RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\s*\\((.*?)\\)",
                    Pattern.DOTALL);
    private static final Pattern API_METHOD_PATTERN =
            Pattern.compile(
                    "\\bR\\s*<[^;]+?>\\s+([A-Za-z0-9_]+)\\s*\\(([^;{}]*)\\)\\s*;", Pattern.DOTALL);
    private static final Pattern SERVICE_INTERFACE_METHOD_PATTERN =
            Pattern.compile(
                    "(?:^|[\\n\\r])\\s*(?:@[A-Za-z0-9_$.]+(?:\\s*\\([^;{}]*?\\))?\\s*)*"
                            + "(?:public\\s+)?(?:default\\s+)?(?:[A-Za-z0-9_$.<>?,\\[\\]\\s]+)\\s+"
                            + "([A-Za-z0-9_]+)\\s*\\(([^;{}]*)\\)\\s*(?:;|\\{)",
                    Pattern.DOTALL);
    private static final Pattern SERVICE_IMPL_PUBLIC_METHOD_PATTERN =
            Pattern.compile(
                    "(?:^|[\\n\\r])\\s*(?:@[A-Za-z0-9_$.]+(?:\\s*\\([^;{}]*?\\))?\\s*)*"
                            + "public\\s+(?:[A-Za-z0-9_$.<>?,\\[\\]\\s]+)\\s+"
                            + "([A-Za-z0-9_]+)\\s*\\(([^;{}]*)\\)\\s*\\{",
                    Pattern.DOTALL);
    private static final Pattern JAVA_METHOD_PATTERN =
            Pattern.compile(
                    "(?:^|[\\n\\r])\\s*(?:@[A-Za-z0-9_$.]+(?:\\s*\\([^;{}]*?\\))?\\s*)*"
                            + "(?:public\\s+)?(?:default\\s+)?(?:[A-Za-z0-9_$.<>?,\\[\\]\\s]+)\\s+"
                            + "([A-Za-z0-9_]+)\\s*\\(([^;{}]*)\\)\\s*(?:;|\\{)",
                    Pattern.DOTALL);
    private static final Pattern MAPPER_SQL_ANNOTATION_PATTERN =
            Pattern.compile(
                    "@(?:org\\.apache\\.ibatis\\.annotations\\.)?"
                        + "(Select|Insert|Update|Delete|SelectProvider|InsertProvider|UpdateProvider|DeleteProvider)\\b");
    private static final Pattern DIRECT_MYBATIS_SERVICE_IMPL_PATTERN =
            Pattern.compile(
                    "\\bextends\\s+(?:com\\.baomidou\\.mybatisplus\\.extension\\.service\\.impl\\.)?ServiceImpl\\b");
    private static final Pattern MYBATIS_SELECT_PAGE_PATTERN =
            Pattern.compile("\\.selectPage\\s*\\(");
    private static final Pattern MYBATIS_NEW_PAGE_PATTERN =
            Pattern.compile(
                    "\\bnew\\s+(?:com\\.baomidou\\.mybatisplus\\.extension\\.plugins\\.pagination\\.)?Page\\s*<");
    private static final Pattern TENANT_CONDITION_PATTERN =
            Pattern.compile(
                    "(?:\\.eq\\s*\\([^;\\n"
                            + "]*(?:TenantId\\b|\"tenant_id\"|'tenant_id')|\\btenant_id\\b\\s*=)",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_SCOPE_CONDITION_PATTERN =
            Pattern.compile(
                    "(?:\\.eq\\s*\\([^;\\n"
                        + "]*(?:CreatedBy|OrgId)\\b|\\.(?:in|eq)\\s*\\([^;\\n"
                        + "]*(?:\"(?:created_by|org_id)\"|'(?:created_by|org_id)')|\\b(?:created_by|org_id)\\b\\s*(?:=|in\\b))",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern SET_TENANT_ID_PATTERN = Pattern.compile("\\.setTenantId\\s*\\(");
    private static final Pattern YAML_RESOURCE_TYPE_PATTERN =
            Pattern.compile("^\\s{6}([A-Z0-9_]+):\\s*$");
    private static final Pattern YAML_RESOURCE_ID_PATTERN =
            Pattern.compile("^\\s{8}-\\s+id:\\s*\"?([0-9]+)\"?\\s*$");
    private static final Pattern YAML_RESOURCE_BIZ_KEY_PATTERN =
            Pattern.compile("^\\s{10}biz-key:\\s*(.+?)\\s*$");
    private static final Pattern SUPPORT_PERSISTENCE_CONTENT_PATTERN =
            Pattern.compile(
                    "(@TableName\\b|\\bBaseMapper\\b|\\bServiceImpl\\b|\\bMangoCrudServiceImpl\\b|"
                            + "org\\.apache\\.ibatis|com\\.baomidou\\.mybatisplus|org\\.flywaydb|"
                            + "\\bDataSource\\b|\\bJdbcTemplate\\b|java\\.sql\\.)");
    private static final Pattern SUPPORT_AUTO_CONFIGURATION_PATTERN =
            Pattern.compile(
                    "(@AutoConfiguration\\b|@Configuration\\b|org\\.springframework\\.boot\\.autoconfigure|"
                        + "org\\.springframework\\.boot\\.autoconfigure)");
    private static final Pattern TYPE_DECLARATION_PATTERN =
            Pattern.compile(
                    "\\b(?:public\\s+)?(?:sealed\\s+)?(?:abstract\\s+)?(?:class|interface|record|enum)\\s+([A-Za-z0-9_]+)");
    private static final Pattern API_FIELD_PATTERN =
            Pattern.compile(
                    "\\bprivate\\s+(?:final\\s+)?(?:[A-Za-z0-9_$.]+\\.)?([A-Za-z0-9_]+Api)\\s+[A-Za-z0-9_]+\\s*;");
    private static final Pattern SERVICE_IMPLEMENTS_API_PATTERN =
            Pattern.compile(
                    "\\bclass\\s+([A-Za-z0-9_]+Service)\\b[^\\{;]*\\bimplements\\b[^\\{;]*\\b[A-Za-z0-9_]+Api\\b",
                    Pattern.DOTALL);
    private static final Pattern FEIGN_EXTENDS_API_PATTERN =
            Pattern.compile(
                    "\\binterface\\s+([A-Za-z0-9_]+FeignClient)\\b[^\\{;]*\\bextends\\b[^\\{;]*\\b[A-Za-z0-9_]+Api\\b",
                    Pattern.DOTALL);
    private static final Pattern FEIGN_INTERFACE_DECLARATION_PATTERN =
            Pattern.compile(
                    "\\binterface\\s+([A-Za-z0-9_]+FeignClient)\\b([^\\{;]*)\\{", Pattern.DOTALL);
    private static final Pattern CONTROLLER_IMPLEMENTS_API_PATTERN =
            Pattern.compile(
                    "\\bclass\\s+[A-Za-z0-9_]+Controller\\b[^\\{;]*\\bimplements\\b[^\\{;]*\\b[A-Za-z0-9_]+Api\\b",
                    Pattern.DOTALL);
    private static final Pattern CONTROLLER_FIELD_PATTERN =
            Pattern.compile(
                    "\\bprivate\\s+(?:final\\s+)?(?:[A-Za-z0-9_$.]+\\.)?([A-Za-z0-9_]+)\\s+[A-Za-z0-9_]+\\s*;");
    private static final Pattern MAPPED_METHOD_PATTERN =
            Pattern.compile(
                    "(?s)@(?:GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\\b[^\\n"
                        + "]*(?:\\R\\s*@[A-Za-z0-9_$.]+(?:\\([^;{}]*?\\))?)*\\R\\s*"
                        + "(?:public\\s+)?([A-Za-z0-9_$.<>?,\\[\\]]+)\\s+([A-Za-z0-9_]+)\\s*\\(");
    private static final Pattern API_DECLARED_METHOD_PATTERN =
            Pattern.compile(
                    "(?m)^\\s*(?:@[A-Za-z0-9_$.]+(?:\\([^;{}]*?\\))?\\s*)*"
                        + "(?:public\\s+)?([A-Za-z0-9_$.<>?,\\[\\]]+)\\s+([A-Za-z0-9_]+)\\s*\\([^;{}]*\\)\\s*;");
    private static final Pattern SERVICE_DIRECT_THROW_PATTERN =
            Pattern.compile(
                    "\\bthrow\\s+new\\s+(?:[A-Za-z0-9_$.]+\\.)?(?:RuntimeException|IllegalArgumentException|IllegalStateException|BizException)\\s*\\(");
    private static final Pattern SERVICE_R_RETURN_PATTERN =
            Pattern.compile(
                    "(?m)^\\s*(?:@[A-Za-z0-9_$.]+(?:\\s*\\([^;{}]*?\\))?\\s*)*"
                            + "public\\s+(?:[A-Za-z0-9_$.]+\\.)?R\\s*<");
    private static final Pattern REQUIRE_CALL_PATTERN =
            Pattern.compile("\\bRequire\\.[A-Za-z0-9_]+\\s*\\(([^;]+)\\)");
    private static final Pattern BIZ_CODE_REFERENCE_PATTERN =
            Pattern.compile("\\b[A-Za-z0-9_]+Code\\.[A-Z][A-Z0-9_]*\\b");
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
    private static final Pattern CREATE_TABLE_PATTERN =
            Pattern.compile(
                    "(?is)create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?(?:`?([a-zA-Z0-9_]+)`?\\.)?`?([a-zA-Z0-9_]+)`?\\s*\\(");
    private static final Pattern TABLE_NAME_ANNOTATION_PATTERN =
            Pattern.compile(
                    "@(?:com\\.baomidou\\.mybatisplus\\.annotation\\.)?TableName\\s*\\(([^)]*)\\)");
    private static final Pattern TABLE_NAME_LITERAL_VALUE_PATTERN =
            Pattern.compile("^\\s*(?:value\\s*=\\s*)?\"([a-z][a-z0-9_]*)\"\\s*$");
    private static final Pattern ALTER_TABLE_PATTERN =
            Pattern.compile(
                    "(?is)alter\\s+table\\s+(?:`?([a-zA-Z0-9_]+)`?\\.)?`?([a-zA-Z0-9_]+)`?");
    private static final Pattern SQL_TABLE_REFERENCE_PATTERN =
            Pattern.compile(
                    "(?i)\\b(?:from|join|update|into)\\s+(?:`?[a-z][a-z0-9_]*`?\\.)?`?([a-z][a-z0-9_]*)`?");
    private static final Pattern MIGRATION_VERSION_PATTERN =
            Pattern.compile("(?i)^v([0-9][0-9._-]*?)__");
    private static final Pattern ID_PRIMARY_KEY_PATTERN =
            Pattern.compile("(?is)primary\\s+key\\s*\\(\\s*`?id`?\\s*\\)");
    private static final Pattern SQL_COLUMN_PATTERN =
            Pattern.compile("(?im)^\\s*`?([a-zA-Z][a-zA-Z0-9_]*)`?\\s+");
    private static final Pattern SQL_COLUMN_DEFINITION_PATTERN =
            Pattern.compile("(?im)^\\s*`?([a-zA-Z][a-zA-Z0-9_]*)`?\\s+([^,\\n]+)");
    private static final Pattern TYPE_TOKEN_PATTERN = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$.]*");
    private static final Pattern ADD_COLUMN_CLAUSE_PATTERN =
            Pattern.compile("(?is)^\\s*add\\s+column\\s+`?([a-zA-Z][a-zA-Z0-9_]*)`?\\s+([^,;']+)");
    private static final Pattern RENAME_COLUMN_CLAUSE_PATTERN =
            Pattern.compile(
                    "(?is)^\\s*rename\\s+column\\s+`?([a-zA-Z][a-zA-Z0-9_]*)`?\\s+to\\s+`?([a-zA-Z][a-zA-Z0-9_]*)`?");
    private static final Pattern MODIFY_COLUMN_CLAUSE_PATTERN =
            Pattern.compile(
                    "(?is)^\\s*modify\\s+(?:column\\s+)?`?([a-zA-Z][a-zA-Z0-9_]*)`?\\s+([^,;']+)");
    private static final Pattern DROP_COLUMN_CLAUSE_PATTERN =
            Pattern.compile("(?is)^\\s*drop\\s+column\\s+`?([a-zA-Z][a-zA-Z0-9_]*)`?");
    private static final Pattern ADD_PRIMARY_KEY_CLAUSE_PATTERN =
            Pattern.compile("(?is)^\\s*add\\s+primary\\s+key\\s*\\(\\s*`?id`?\\s*\\)");
    private static final Pattern DROP_PRIMARY_KEY_CLAUSE_PATTERN =
            Pattern.compile("(?is)^\\s*drop\\s+primary\\s+key\\b");
    private static final List<String> REQUIRED_PERSISTENCE_COLUMNS =
            List.of("created_by", "created_at", "updated_by", "updated_at", "tenant_id", "org_id");
    private static final Set<String> GLOBAL_ENTITY_OPTIONAL_COLUMNS = Set.of("tenant_id", "org_id");
    private static final Map<String, String> FRAMEWORK_PERSISTENCE_EXCLUDED_TABLES =
            Map.of(
                    "infra_kv_entry", "/mango-infra/mango-infra-kv/mango-infra-kv-core/",
                    "sys_login_log", "/mango-platform/mango-system/mango-system-core/",
                    "sys_operation_log", "/mango-platform/mango-system/mango-system-core/");
    private static final List<String> DIRECT_JDBC_TYPES =
            List.of("Connection", "Statement", "PreparedStatement", "ResultSet");
    private static final Set<String> IGNORED_SCAN_SEGMENTS =
            Set.of(
                    ".git",
                    ".mango",
                    ".runtime",
                    "node_modules",
                    "target",
                    "dist",
                    "coverage",
                    "playwright-report",
                    "test-results");
    private static final Set<String> NON_PERSISTENCE_ENTITY_TYPES =
            Set.of("ResponseEntity", "HttpEntity", "RequestEntity");

    /**
     * Check rule: all, static, naming, module-info, path-param, permission-param, kv-key,
     * test-fixture, persistence-schema, persistence-access, persistence-crud-baseline,
     * resource-registry, module-menu. The dependency, module-boundary, remote-adapter,
     * api-contract, mapper-sql-style and service-contract rules are compatibility-only diagnostics;
     * {@code mvn verify} is the authoritative Java architecture gate.
     */
    @Parameter(property = "rule", defaultValue = "all")
    private String rule;

    /** Output format: text, json. */
    @Parameter(property = "output", defaultValue = "text")
    private String output;

    /** Report output path. */
    @Parameter(property = "reportFile")
    private String reportFile;

    /** Source root. */
    @Parameter(property = "baseDir", defaultValue = "${project.basedir}")
    private String baseDir;

    /** Current Maven project. */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /** Maven session. */
    @Parameter(defaultValue = "${session}", readonly = true)
    private org.apache.maven.execution.MavenSession session;

    /** Timeout in seconds for each delegated PMD/Checkstyle/SpotBugs Maven command. */
    @Parameter(property = "mango.check.staticTimeoutSeconds", defaultValue = "600")
    private long staticTimeoutSeconds;

    /** Gate mode: all, no-new-violations. */
    @Parameter(property = "mango.check.gate", defaultValue = "all")
    private String gate;

    /** Comma-separated changed files used by no-new-violations gate. */
    @Parameter(property = "mango.check.changedFiles")
    private String changedFiles;

    /**
     * Restrict no-new-violations classification and scope-sensitive mango-check rules to changed
     * files. The no-new-violations gate requires changedFiles or a resolvable baseRef when enabled.
     */
    @Parameter(property = "mango.check.changedOnly", defaultValue = "false")
    private boolean changedOnly;

    /** Git base ref used to resolve changed files when changedFiles is not provided. */
    @Parameter(property = "mango.check.baseRef")
    private String baseRef;

    /** Optional previous mango:check JSON report for baseline comparison. */
    @Parameter(property = "mango.check.baselineFile")
    private String baselineFile;

    /** Static-analysis delegation failure policy: block, report. */
    @Parameter(property = "mango.check.staticFailurePolicy", defaultValue = "block")
    private String staticFailurePolicy;

    @Parameter private Path globalEntityManifest;

    @Parameter(defaultValue = "false")
    private boolean requireExecutionRoot;

    @Parameter private String requiredRule;

    @Parameter private String requiredGate;

    @Parameter(defaultValue = "false")
    private boolean requireBlockingStaticFailures;

    /**
     * Locks governed business verification to a full repository scope. This parameter is
     * intentionally POM-only so command-line properties cannot disable the lock.
     */
    @Parameter(defaultValue = "false")
    private boolean requireFullScope;

    /**
     * Comma-separated Maven module paths whose code-level static issues are reported but excluded
     * from gate.
     */
    @Parameter(property = "mango.check.codeLevelExcludedModules")
    private String codeLevelExcludedModules;

    /**
     * Explicit exceptions for Resource Registry starter dependencies. Format:
     * artifactId=reason,artifactId=reason.
     */
    @Parameter(property = "mango.check.resourceStarterDependencyExceptions")
    private String resourceStarterDependencyExceptions;

    /**
     * Retained for backward-compatible tests and configuration migration. Generic method-length
     * checks are delegated to PMD/P3C/Checkstyle.
     */
    @Parameter(property = "maxMethodLength", defaultValue = "50")
    private int maxMethodLength;

    /**
     * Retained for backward-compatible tests and configuration migration. Generic class-length
     * checks are delegated to PMD/P3C/Checkstyle.
     */
    @Parameter(property = "maxClassLength", defaultValue = "500")
    private int maxClassLength;

    private final ObjectMapper objectMapper =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private CheckResult result;

    @Override
    public void execute() throws MojoExecutionException {
        resolveBaseDirectory();
        validateGovernedConfiguration();
        getLog().info("Running Mango Check - rule: " + rule);
        result = new CheckResult();
        gateFinalizer(null).initializeResultOptions(result);
        executeSelectedRule();
        gateFinalizer(resolveBasePath()).finalizeResult(result);
        outputResult();
        if (reportFile != null) {
            saveReport();
        }
        failWhenBlocked();
        getLog().info("Check completed. " + result.issues.size() + " issue(s) found.");
    }

    private void resolveBaseDirectory() {
        if (baseDir == null || baseDir.isEmpty()) {
            if (session != null && session.getExecutionRootDirectory() != null) {
                baseDir = session.getExecutionRootDirectory();
            } else {
                baseDir = System.getProperty("user.dir");
            }
        }
    }

    private void validateGovernedConfiguration() throws MojoExecutionException {
        validateExecutionRoot();
        validateRequiredOption("rule", requiredRule, rule);
        validateRequiredOption("gate", requiredGate, gate);
        if (requireBlockingStaticFailures && !BLOCK_POLICY.equalsIgnoreCase(staticFailurePolicy)) {
            throw new MojoExecutionException(
                    "Governed mango:check staticFailurePolicy must remain block");
        }
        if (requireFullScope && changedOnly) {
            throw new MojoExecutionException(
                    "Governed mango:check requires full scope; changedOnly=true is forbidden");
        }
        if (requireFullScope && hasText(codeLevelExcludedModules)) {
            throw new MojoExecutionException(
                    "Governed mango:check requires full scope; codeLevelExcludedModules is"
                            + " forbidden");
        }
    }

    private void validateExecutionRoot() throws MojoExecutionException {
        if (requireExecutionRoot
                && session != null
                && session.getExecutionRootDirectory() != null) {
            Path configuredRoot = Path.of(baseDir).toAbsolutePath().normalize();
            Path executionRoot =
                    Path.of(session.getExecutionRootDirectory()).toAbsolutePath().normalize();
            if (!configuredRoot.equals(executionRoot)) {
                throw new MojoExecutionException(
                        "Governed mango:check baseDir must equal Maven execution root: "
                                + executionRoot);
            }
        }
    }

    private void validateRequiredOption(String option, String required, String actual)
            throws MojoExecutionException {
        if (hasText(required) && !required.equalsIgnoreCase(actual)) {
            throw new MojoExecutionException(
                    "Governed mango:check "
                            + option
                            + " must remain "
                            + required
                            + ", actual="
                            + actual);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void executeSelectedRule() throws MojoExecutionException {
        RuleAction action = ruleActions().get(rule.toLowerCase(Locale.ROOT));
        if (action == null) {
            throw new MojoExecutionException("Unknown mango:check rule: " + rule);
        }
        action.run();
    }

    private Map<String, RuleAction> ruleActions() {
        Map<String, RuleAction> actions = new LinkedHashMap<>();
        actions.put("duplicate", () -> unsupportedGenericRule(rule));
        actions.put("method-length", () -> unsupportedGenericRule(rule));
        actions.put("class-length", () -> unsupportedGenericRule(rule));
        actions.put("complexity", () -> unsupportedGenericRule(rule));
        actions.put("static", this::runStaticAnalysis);
        actions.put("naming", this::checkNaming);
        actions.put("dependency", this::checkDependencyDiagnostic);
        actions.put("module-boundary", this::checkDependencyDiagnostic);
        actions.put("module-info", this::checkModuleInfo);
        actions.put("remote-adapter", this::checkRemoteAdapterDiagnostic);
        actions.put("api-contract", this::checkApiContractDiagnostic);
        actions.put("path-param", this::checkPathParam);
        actions.put("permission-param", this::checkPermissionParam);
        actions.put("kv-key", this::checkKvKey);
        actions.put("persistence-schema", this::checkPersistenceSchema);
        actions.put("persistence-access", this::checkPersistenceAccess);
        actions.put("mapper-sql-style", this::checkMapperSqlStyleDiagnostic);
        actions.put("persistence-crud-baseline", this::checkPersistenceCrudBaseline);
        actions.put("service-contract", this::checkServiceContractDiagnostic);
        actions.put("test-fixture", this::checkTestFixture);
        actions.put("web-boundary", this::checkWebBoundary);
        actions.put("resource-registry", this::checkResourceRegistry);
        actions.put("module-menu", this::checkModuleMenu);
        actions.put("all", this::checkAll);
        return actions;
    }

    private void checkDependencyDiagnostic() {
        warnLegacyArchitectureDiagnostic();
        checkDependency();
    }

    private void checkRemoteAdapterDiagnostic() {
        warnLegacyArchitectureDiagnostic();
        checkRemoteAdapter();
    }

    private void checkApiContractDiagnostic() {
        warnLegacyArchitectureDiagnostic();
        checkApiContract();
    }

    private void checkMapperSqlStyleDiagnostic() {
        warnLegacyArchitectureDiagnostic();
        checkMapperSqlStyle();
    }

    private void checkServiceContractDiagnostic() {
        warnLegacyArchitectureDiagnostic();
        checkServiceContract();
    }

    private void checkAll() throws MojoExecutionException {
        runStaticAnalysis();
        checkNaming();
        checkModuleInfo();
        checkPathParam();
        checkPermissionParam();
        checkKvKey();
        checkPersistenceSchema();
        checkBusinessBackendStyle();
        checkTestFixture();
        checkWebBoundary();
        checkResourceRegistry();
        checkModuleMenu();
    }

    private void checkBusinessBackendStyle() {
        if (isBusinessProject(resolveBasePath())) {
            checkPersistenceAccess();
            checkPersistenceCrudBaseline();
            return;
        }
        getLog().info(
                        "Skip business backend style checks in mango:check all; run"
                                + " -Drule=persistence-access or persistence-crud-baseline"
                                + " explicitly for governance scans.");
    }

    private void outputResult() {
        if (JSON_OUTPUT.equalsIgnoreCase(output)) {
            outputJson();
        } else {
            outputText();
        }
    }

    private void failWhenBlocked() throws MojoExecutionException {
        if (!result.passed) {
            throw new MojoExecutionException(
                    "Check failed: gateStatus="
                            + result.gateStatus
                            + ", newIssues="
                            + result.newIssues.size()
                            + ", issues="
                            + result.issues.size()
                            + ", toolFailures="
                            + result.toolFailures.size());
        }
    }

    @FunctionalInterface
    private interface RuleAction {
        /**
         * Runs one selected Mango check rule.
         *
         * @throws MojoExecutionException when the rule cannot complete
         */
        void run() throws MojoExecutionException;
    }

    private void warnLegacyArchitectureDiagnostic() {
        getLog().warn(
                        "This legacy source-text architecture diagnostic is not a delivery gate."
                                + " Use mvn verify (mango:architecture) for authoritative Enforcer,"
                                + " ArchUnit and PMD 7 checks.");
    }

    private void unsupportedGenericRule(String selectedRule) {
        String description =
                "mango:check only runs Mango-specific rules. Generic rule '"
                        + selectedRule
                        + "' is handled by aggregated static analysis inside mango:check; use"
                        + " rule=static or the mapping in "
                        + DOC_AUTO_CHECK_MAPPING
                        + ".";
        getLog().warn(description);
        result.addIssue(
                "UNSUPPORTED_RULE",
                "MAJOR",
                null,
                0,
                description,
                "UNSUPPORTED_RULE",
                DOC_AUTO_CHECK_MAPPING,
                SOURCE_MANGO_CHECK);
    }

    private void runStaticAnalysis() throws MojoExecutionException {
        getLog().info("Running aggregated static analysis...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            throw new MojoExecutionException("Static analysis cannot resolve baseDir");
        }
        if (!Files.exists(rootPath.resolve("pom.xml"))) {
            throw new MojoExecutionException("Static analysis baseDir has no pom.xml: " + rootPath);
        }

        cleanStaticReports(rootPath);
        invokeMavenGoals(rootPath, List.of("pmd:check", "checkstyle:check", "spotbugs:spotbugs"));
        collectPmdIssues(rootPath);
        collectCheckstyleIssues(rootPath);
        collectSpotbugsIssues(rootPath);
    }

    private void cleanStaticReports(Path rootPath) throws MojoExecutionException {
        try {
            Files.walkFileTree(
                    rootPath,
                    new StaticReportFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                throws IOException {
                            String fileName = file.getFileName().toString();
                            if (CHECKSTYLE_REPORT.equals(fileName)
                                    || PMD_REPORT.equals(fileName)
                                    || SPOTBUGS_REPORT.equals(fileName)) {
                                Files.deleteIfExists(file);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to clean static-analysis reports", e);
        }
    }

    private void invokeMavenGoals(Path rootPath, List<String> goals) throws MojoExecutionException {
        File mavenExecutable = findMavenExecutable();
        if (mavenExecutable == null) {
            throw new MojoExecutionException("Static analysis requires a Maven executable on PATH");
        }

        List<String> reactorProjects = resolveStaticAnalysisProjects(rootPath);
        if (reactorProjects.isEmpty()) {
            getLog().info("Aggregated static analysis scope: execution root project");
        } else {
            getLog().info("Aggregated static analysis scope: " + String.join(",", reactorProjects));
        }
        for (String goal : goals) {
            try {
                invokeSingleGoal(mavenExecutable, rootPath, goal, reactorProjects);
            } catch (MojoExecutionException e) {
                if (recordStaticFailure(goal, e)) {
                    continue;
                }
                throw e;
            }
        }
    }

    private boolean recordStaticFailure(String goal, MojoExecutionException exception) {
        if (!gateFinalizer(null).shouldReportStaticFailure(result)) {
            return false;
        }
        result.addToolFailure(goal, exception.getMessage());
        getLog().warn("Static-analysis delegation reported but did not block: " + goal);
        return true;
    }

    private void invokeSingleGoal(
            File mavenExecutable, Path rootPath, String goal, List<String> reactorProjects)
            throws MojoExecutionException {
        List<String> command = new ArrayList<>();
        command.add(mavenExecutable.getAbsolutePath());
        command.add("-q");
        command.add("-f");
        command.add(rootPath.resolve("pom.xml").toString());
        command.add("-DskipTests");
        if (!reactorProjects.isEmpty()) {
            command.add("-pl");
            command.add(String.join(",", reactorProjects));
            command.add("-am");
        }
        command.add("compile");
        command.add(goal);
        getLog().info("Delegating static analysis goal: " + String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(rootPath.toFile());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            CompletableFuture<String> outputFuture =
                    readProcessOutputAsync(process.getInputStream());
            boolean completed = process.waitFor(staticTimeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                String output = terminateProcessTree(process, outputFuture);
                throw new MojoExecutionException(
                        "Static-analysis delegation timed out after "
                                + staticTimeoutSeconds
                                + "s while running "
                                + goal
                                + System.lineSeparator()
                                + "Command: "
                                + String.join(" ", command)
                                + System.lineSeparator()
                                + output);
            }
            int exitCode = process.exitValue();
            String output = awaitProcessOutput(outputFuture, 5);
            if (exitCode != 0) {
                throw new MojoExecutionException(
                        "Static-analysis delegation failed with exit code "
                                + exitCode
                                + " while running "
                                + goal
                                + System.lineSeparator()
                                + "Command: "
                                + String.join(" ", command)
                                + System.lineSeparator()
                                + output);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to start delegated static-analysis goals", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Delegated static-analysis goals were interrupted", e);
        }
    }

    private String terminateProcessTree(
            Process process, CompletableFuture<String> outputFuture) throws InterruptedException {
        List<ProcessHandle> descendants = process.descendants().toList();
        for (int index = descendants.size() - 1; index >= 0; index--) {
            descendants.get(index).destroyForcibly();
        }
        process.destroyForcibly();
        process.waitFor(1, TimeUnit.SECONDS);
        descendants.stream()
                .filter(ProcessHandle::isAlive)
                .forEach(ProcessHandle::destroyForcibly);
        try {
            return awaitProcessOutput(outputFuture, 1);
        } catch (MojoExecutionException exception) {
            outputFuture.cancel(true);
            return "Delegated process output unavailable after forced termination: "
                    + exception.getMessage();
        }
    }

    private CompletableFuture<String> readProcessOutputAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return readProcessOutput(inputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private String awaitProcessOutput(CompletableFuture<String> outputFuture, long timeoutSeconds)
            throws MojoExecutionException {
        try {
            return outputFuture.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException(
                    "Interrupted while reading delegated static-analysis output", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UncheckedIOException uncheckedIOException) {
                throw new MojoExecutionException(
                        "Failed to read delegated static-analysis output",
                        uncheckedIOException.getCause());
            }
            throw new MojoExecutionException(
                    "Failed to read delegated static-analysis output", cause);
        } catch (TimeoutException e) {
            throw new MojoExecutionException(
                    "Timed out while reading delegated static-analysis output", e);
        }
    }

    private List<String> resolveStaticAnalysisProjects(Path rootPath)
            throws MojoExecutionException {
        List<String> sessionProjects = resolveSessionReactorProjects(rootPath);
        if (!sessionProjects.isEmpty()) {
            return sessionProjects;
        }
        return discoverReactorProjects(rootPath);
    }

    private List<String> resolveSessionReactorProjects(Path rootPath) {
        if (session == null || session.getProjects() == null || session.getProjects().isEmpty()) {
            return List.of();
        }
        List<String> projects = new ArrayList<>();
        for (MavenProject project : session.getProjects()) {
            if (project == null || project.getBasedir() == null) {
                continue;
            }
            Path projectPath = project.getBasedir().toPath().toAbsolutePath().normalize();
            Path normalizedRoot = rootPath.toAbsolutePath().normalize();
            if (projectPath.equals(normalizedRoot)) {
                continue;
            }
            if (!projectPath.startsWith(normalizedRoot)) {
                continue;
            }
            String relativePath = normalizedRoot.relativize(projectPath).toString();
            if (!relativePath.isBlank()) {
                projects.add(relativePath);
            }
        }
        return projects;
    }

    private String readProcessOutput(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes());
    }

    private List<String> discoverReactorProjects(Path rootPath) throws MojoExecutionException {
        List<String> modules = new ArrayList<>();
        collectReactorProjects(rootPath, rootPath, modules);
        return modules;
    }

    private void collectReactorProjects(Path rootPath, Path currentDir, List<String> modules)
            throws MojoExecutionException {
        Path pomFile = currentDir.resolve("pom.xml");
        if (!Files.exists(pomFile)) {
            return;
        }
        for (String childModule : readPomModules(pomFile)) {
            Path moduleDir = currentDir.resolve(childModule).normalize();
            if (!Files.exists(moduleDir.resolve("pom.xml"))) {
                continue;
            }
            String relativePath = rootPath.relativize(moduleDir).toString();
            if (!relativePath.isBlank()) {
                modules.add(relativePath);
            }
            // Recursively collect nested modules
            collectReactorProjects(rootPath, moduleDir, modules);
        }
    }

    private List<String> readPomModules(Path pomFile) throws MojoExecutionException {
        List<String> modules = new ArrayList<>();
        Document document = loadXml(pomFile);
        NodeList moduleNodes = document.getElementsByTagName("module");
        for (int i = 0; i < moduleNodes.getLength(); i++) {
            String module = moduleNodes.item(i).getTextContent();
            if (module != null && !module.isBlank()) {
                modules.add(module.trim());
            }
        }
        return modules;
    }

    private File findMavenExecutable() {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return null;
        }
        for (String dir : path.split(Pattern.quote(File.pathSeparator))) {
            if (dir == null || dir.isBlank()) {
                continue;
            }
            File dirFile = new File(dir);
            if (!dirFile.isDirectory()) {
                continue;
            }
            // On Windows, check mvn.cmd first, then fall back to mvn (Git Bash/MSYS2)
            for (String candidate : new String[] {"mvn.cmd", "mvn"}) {
                File executable = new File(dirFile, candidate);
                if (executable.isFile()
                        && (File.separatorChar == '\\' || executable.canExecute())) {
                    return executable;
                }
            }
        }
        return null;
    }

    private void collectPmdIssues(Path rootPath) throws MojoExecutionException {
        List<Path> reports = requireStaticReports(rootPath, PMD_REPORT, SOURCE_PMD);
        for (Path report : reports) {
            parsePmdReport(report);
        }
    }

    private void collectCheckstyleIssues(Path rootPath) throws MojoExecutionException {
        List<Path> reports = requireStaticReports(rootPath, CHECKSTYLE_REPORT, SOURCE_CHECKSTYLE);
        for (Path report : reports) {
            parseCheckstyleReport(report);
        }
    }

    private void collectSpotbugsIssues(Path rootPath) throws MojoExecutionException {
        List<Path> reports = requireStaticReports(rootPath, SPOTBUGS_REPORT, SOURCE_SPOTBUGS);
        for (Path report : reports) {
            parseSpotbugsReport(report);
        }
    }

    List<Path> requireStaticReports(Path rootPath, String fileName, String source)
            throws MojoExecutionException {
        if (requireFullScope) {
            return requireFullScopeStaticReports(rootPath, fileName, source);
        }
        List<Path> reports = findReports(rootPath, fileName);
        if (reports.isEmpty()) {
            throw new MojoExecutionException(
                    "Static analysis produced no " + source + " report named " + fileName);
        }
        return reports;
    }

    private List<Path> requireFullScopeStaticReports(Path rootPath, String fileName, String source)
            throws MojoExecutionException {
        if (session == null || session.getProjects() == null || session.getProjects().isEmpty()) {
            throw new MojoExecutionException(
                    "Governed full-scope static analysis requires the Maven Reactor session");
        }
        Path normalizedRoot = rootPath.toAbsolutePath().normalize();
        List<Path> reports = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (MavenProject reactorProject : session.getProjects()) {
            if (reactorProject == null || !hasJavaCompileSource(reactorProject)) {
                continue;
            }
            Path basedir = reactorProject.getBasedir().toPath().toAbsolutePath().normalize();
            if (!basedir.startsWith(normalizedRoot)) {
                throw new MojoExecutionException(
                        "Governed full-scope static analysis found Reactor project outside baseDir:"
                                + " "
                                + reactorProject.getArtifactId()
                                + " at "
                                + basedir);
            }
            Path report = projectBuildDirectory(reactorProject).resolve(fileName).normalize();
            if (Files.isRegularFile(report)) {
                reports.add(report);
            } else {
                String artifactId =
                        reactorProject.getArtifactId() == null
                                ? basedir.getFileName().toString()
                                : reactorProject.getArtifactId();
                missing.add(artifactId + " -> " + report);
            }
        }
        if (!missing.isEmpty()) {
            throw new MojoExecutionException(
                    "Static analysis missing "
                            + source
                            + " report for Java Reactor module(s): "
                            + String.join(", ", missing));
        }
        return reports;
    }

    private boolean hasJavaCompileSource(MavenProject reactorProject)
            throws MojoExecutionException {
        if (reactorProject.getBasedir() == null) {
            throw new MojoExecutionException(
                    "Governed full-scope static analysis found Reactor project without basedir: "
                            + reactorProject.getArtifactId());
        }
        Set<Path> sourceRoots = new LinkedHashSet<>();
        for (String sourceRoot : reactorProject.getCompileSourceRoots()) {
            if (sourceRoot != null && !sourceRoot.isBlank()) {
                sourceRoots.add(resolveProjectPath(reactorProject, sourceRoot));
            }
        }
        if (reactorProject.getBuild() != null
                && reactorProject.getBuild().getSourceDirectory() != null
                && !reactorProject.getBuild().getSourceDirectory().isBlank()) {
            sourceRoots.add(
                    resolveProjectPath(
                            reactorProject, reactorProject.getBuild().getSourceDirectory()));
        }
        for (Path sourceRoot : sourceRoots) {
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try (var files = Files.walk(sourceRoot)) {
                if (files.anyMatch(
                        file ->
                                Files.isRegularFile(file)
                                        && file.getFileName().toString().endsWith(".java"))) {
                    return true;
                }
            } catch (IOException exception) {
                throw new MojoExecutionException(
                        "Failed to inspect Java compile source root: " + sourceRoot, exception);
            }
        }
        return false;
    }

    private Path projectBuildDirectory(MavenProject reactorProject) {
        String directory =
                reactorProject.getBuild() == null ? null : reactorProject.getBuild().getDirectory();
        if (directory == null || directory.isBlank()) {
            return reactorProject
                    .getBasedir()
                    .toPath()
                    .resolve("target")
                    .toAbsolutePath()
                    .normalize();
        }
        return resolveProjectPath(reactorProject, directory);
    }

    private Path resolveProjectPath(MavenProject reactorProject, String configuredPath) {
        Path path = Path.of(configuredPath);
        if (!path.isAbsolute()) {
            path = reactorProject.getBasedir().toPath().resolve(path);
        }
        return path.toAbsolutePath().normalize();
    }

    private List<Path> findReports(Path rootPath, String fileName) throws MojoExecutionException {
        List<Path> reports = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new StaticReportFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (fileName.equals(file.getFileName().toString())) {
                                reports.add(file);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to scan report files: " + fileName, e);
        }
        return reports;
    }

    private void parsePmdReport(Path report) throws MojoExecutionException {
        Document document = loadXml(report);
        NodeList violations = document.getElementsByTagName("violation");
        for (int i = 0; i < violations.getLength(); i++) {
            Element violation = (Element) violations.item(i);
            String filename = extractPmdFilename(violation);
            int line = parseInteger(violation.getAttribute("beginline"));
            String ruleName = violation.getAttribute("rule");
            String description = normalizeWhitespace(violation.getTextContent());
            addStaticIssue(
                    ruleName,
                    mapPmdSeverity(violation.getAttribute("priority")),
                    filename,
                    line,
                    description,
                    ruleName,
                    SOURCE_PMD);
        }
    }

    private String extractPmdFilename(Element violation) {
        Element file = (Element) violation.getParentNode();
        if (file != null && "file".equals(file.getTagName())) {
            String name = file.getAttribute("name");
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        // Fallback: try to get filename from violation element's attributes
        String filename = violation.getAttribute("filename");
        if (filename != null && !filename.isBlank()) {
            return filename;
        }
        return null;
    }

    private void parseCheckstyleReport(Path report) throws MojoExecutionException {
        Document document = loadXml(report);
        NodeList files = document.getElementsByTagName("file");
        for (int i = 0; i < files.getLength(); i++) {
            Element file = (Element) files.item(i);
            String filename = file.getAttribute("name");
            NodeList errors = file.getElementsByTagName("error");
            for (int j = 0; j < errors.getLength(); j++) {
                Element error = (Element) errors.item(j);
                String ruleName = extractCheckstyleRule(error.getAttribute("source"));
                addStaticIssue(
                        ruleName,
                        mapCheckstyleSeverity(error.getAttribute("severity")),
                        filename,
                        parseInteger(error.getAttribute("line")),
                        error.getAttribute("message"),
                        ruleName,
                        SOURCE_CHECKSTYLE);
            }
        }
    }

    private void parseSpotbugsReport(Path report) throws MojoExecutionException {
        Document document = loadXml(report);
        NodeList bugInstances = document.getElementsByTagName("BugInstance");
        for (int i = 0; i < bugInstances.getLength(); i++) {
            Element bugInstance = (Element) bugInstances.item(i);
            Element sourceLine = firstChildElement(bugInstance, "SourceLine");
            String filename = sourceLine == null ? null : sourceLine.getAttribute("sourcepath");
            int line = sourceLine == null ? 0 : parseInteger(sourceLine.getAttribute("start"));
            String type = bugInstance.getAttribute("type");
            String description = bugInstance.getAttribute("message");
            if (description == null || description.isBlank()) {
                description = normalizeWhitespace(textOfFirstChild(bugInstance, "LongMessage"));
            }
            addStaticIssue(
                    type,
                    mapSpotbugsSeverity(
                            bugInstance.getAttribute("priority"), bugInstance.getAttribute("rank")),
                    filename,
                    line,
                    description,
                    type,
                    SOURCE_SPOTBUGS);
        }
    }

    private void addStaticIssue(
            String type,
            String severity,
            String file,
            int line,
            String description,
            String rule,
            String source) {
        if (isCodeLevelExcludedIssue(file, source)) {
            result.addExcludedIssue(
                    type, severity, file, line, description, rule, DOC_STATIC_ANALYSIS, source);
            return;
        }
        result.addIssue(type, severity, file, line, description, rule, DOC_STATIC_ANALYSIS, source);
    }

    private boolean isCodeLevelExcludedIssue(String file, String source) {
        if (!CODE_LEVEL_SOURCES.contains(source)) {
            return false;
        }
        String issuePath = normalizePathForMatch(file);
        if (issuePath.isBlank()) {
            return false;
        }
        for (String excludedModule : codeLevelExcludedModuleSet()) {
            if (samePathOrUnder(issuePath, excludedModule)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> codeLevelExcludedModuleSet() {
        Set<String> modules = new HashSet<>();
        if (codeLevelExcludedModules == null || codeLevelExcludedModules.isBlank()) {
            return modules;
        }
        for (String module : codeLevelExcludedModules.split(",")) {
            String normalized = normalizePathForMatch(module);
            if (!normalized.isBlank()) {
                modules.add(normalized);
            }
        }
        return modules;
    }

    private boolean samePathOrUnder(String issuePath, String modulePath) {
        return issuePath.equals(modulePath)
                || issuePath.startsWith(modulePath + "/")
                || issuePath.contains("/" + modulePath + "/");
    }

    private String normalizePathForMatch(String path) {
        if (path == null) {
            return "";
        }
        return path.trim().replace('\\', '/').replaceAll("^\\./+", "");
    }

    private Document loadXml(Path report) throws MojoExecutionException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(report.toFile());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to parse XML report: " + report, e);
        }
    }

    private Element firstChildElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return (Element) nodes.item(0);
    }

    private String textOfFirstChild(Element parent, String tagName) {
        Element child = firstChildElement(parent, tagName);
        return child == null ? "" : child.getTextContent();
    }

    private int parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String mapPmdSeverity(String priority) {
        int level = parseInteger(priority);
        if (level <= 1) {
            return "CRITICAL";
        }
        if (level == 2) {
            return "MAJOR";
        }
        return "MINOR";
    }

    private String mapCheckstyleSeverity(String severity) {
        if ("error".equalsIgnoreCase(severity)) {
            return "MAJOR";
        }
        return "MINOR";
    }

    private String mapSpotbugsSeverity(String priority, String rank) {
        int priorityValue = parseInteger(priority);
        int rankValue = parseInteger(rank);
        if (priorityValue == 1 || (rankValue > 0 && rankValue <= 4)) {
            return "CRITICAL";
        }
        if (priorityValue == 2 || (rankValue > 0 && rankValue <= 9)) {
            return "MAJOR";
        }
        return "MINOR";
    }

    private String extractCheckstyleRule(String source) {
        if (source == null || source.isBlank()) {
            return "CHECKSTYLE";
        }
        int index = source.lastIndexOf('.');
        if (index >= 0 && index < source.length() - 1) {
            return source.substring(index + 1);
        }
        return source;
    }

    private String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private Map<String, ModuleDescriptor> loadModuleDescriptors(
            Path rootPath, List<ModuleInfoIssue> issues) {
        Map<String, ModuleDescriptor> descriptors = new LinkedHashMap<>();
        Map<String, String> modulePathOwners = new LinkedHashMap<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                throws IOException {
                            loadModuleDescriptor(file, descriptors, modulePathOwners, issues);
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            issues.add(
                    new ModuleInfoIssue(
                            "MAJOR", rootPath.toString(), "模块信息扫描失败: " + e.getMessage()));
        }
        return descriptors;
    }

    private void loadModuleDescriptor(
            Path pomFile,
            Map<String, ModuleDescriptor> descriptors,
            Map<String, String> modulePathOwners,
            List<ModuleInfoIssue> issues)
            throws IOException {
        if (isIgnoredScanPath(pomFile) || !pomFile.toString().endsWith(POM_PATH_SUFFIX)) {
            return;
        }
        String artifactId = extractArtifactId(Files.readString(pomFile));
        if (artifactId == null) {
            return;
        }
        Path moduleInfoFile = pomFile.getParent().resolve(MODULE_PROPERTIES_PATH);
        if (classifyModule(artifactId) != ModuleType.STARTER) {
            rejectUnexpectedModuleInfo(artifactId, moduleInfoFile, issues);
            return;
        }
        if (MODULE_STARTER_ARTIFACT.equals(artifactId) || !Files.exists(moduleInfoFile)) {
            return;
        }
        ModuleDescriptor descriptor = readModuleDescriptor(artifactId, moduleInfoFile, issues);
        if (descriptor == null) {
            return;
        }
        registerModuleDescriptor(descriptor, modulePathOwners, descriptors, issues);
    }

    private void rejectUnexpectedModuleInfo(
            String artifactId, Path moduleInfoFile, List<ModuleInfoIssue> issues) {
        if (Files.exists(moduleInfoFile)) {
            issues.add(
                    new ModuleInfoIssue(
                            "CRITICAL",
                            moduleInfoFile.toString(),
                            artifactId + " 不允许声明 module.properties；只有本地 starter 可以声明模块信息"));
        }
    }

    private ModuleDescriptor readModuleDescriptor(
            String artifactId, Path moduleInfoFile, List<ModuleInfoIssue> issues)
            throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(moduleInfoFile)) {
            properties.load(inputStream);
        }
        String moduleName = properties.getProperty("module-name", "").trim();
        String modulePath = normalizeModulePath(properties.getProperty("module-path", "").trim());
        if (moduleName.isEmpty()) {
            issues.add(
                    new ModuleInfoIssue(
                            "CRITICAL",
                            moduleInfoFile.toString(),
                            artifactId + " 的 module-name 不能为空"));
            return null;
        }
        if (!MODULE_NAME_PATTERN.matcher(moduleName).matches()) {
            issues.add(
                    new ModuleInfoIssue(
                            "MAJOR",
                            moduleInfoFile.toString(),
                            artifactId + " 的 module-name 只能使用小写字母、数字和中划线: " + moduleName));
        }
        if (modulePath.isEmpty()) {
            issues.add(
                    new ModuleInfoIssue(
                            "CRITICAL",
                            moduleInfoFile.toString(),
                            artifactId + " 的 module-path 不能为空"));
            return null;
        }
        return new ModuleDescriptor(artifactId, moduleName, modulePath, moduleInfoFile.toString());
    }

    private void registerModuleDescriptor(
            ModuleDescriptor descriptor,
            Map<String, String> modulePathOwners,
            Map<String, ModuleDescriptor> descriptors,
            List<ModuleInfoIssue> issues) {
        String owner = modulePathOwners.putIfAbsent(descriptor.modulePath, descriptor.artifactId);
        if (owner != null && !owner.equals(descriptor.artifactId)) {
            issues.add(
                    new ModuleInfoIssue(
                            "CRITICAL",
                            descriptor.file,
                            "module-path 冲突: "
                                    + descriptor.artifactId
                                    + " 与 "
                                    + owner
                                    + " 都声明为 "
                                    + descriptor.modulePath));
        }
        descriptors.put(descriptor.artifactId, descriptor);
    }

    private String normalizeModulePath(String modulePath) {
        if (modulePath == null || modulePath.isBlank()) {
            return "";
        }
        String normalized = modulePath.trim();
        if (!normalized.startsWith(ROOT_PATH)) {
            normalized = ROOT_PATH + normalized;
        }
        normalized = normalized.replaceAll("/+$", "");
        if (ROOT_PATH.equals(normalized) || normalized.startsWith(REVERSE_ROOT_PREFIX)) {
            return "";
        }
        return normalized;
    }

    private String reverseModulePath(String modulePath) {
        return REVERSE_ROOT_PREFIX + modulePath.substring(1);
    }

    private List<Path> starterJavaFiles(Path moduleDir) throws IOException {
        List<Path> files = new ArrayList<>();
        Path sourceRoot = moduleDir.resolve("src/main/java");
        if (!Files.exists(sourceRoot)) {
            return files;
        }
        Files.walkFileTree(
                sourceRoot,
                new SkippingFileVisitor() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.toString().endsWith(".java")) {
                            files.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        return files;
    }

    private Path siblingRemoteModule(Path starterDir, String artifactId) {
        String remoteArtifactId = artifactId + "-remote";
        Path remoteDir = starterDir.getParent().resolve(remoteArtifactId);
        if (Files.exists(remoteDir.resolve("pom.xml"))) {
            return remoteDir;
        }
        return null;
    }

    private String extractControllerRootPath(Path javaFile) throws IOException {
        String content = Files.readString(javaFile);
        if (!isControllerSource(content)) {
            return "";
        }
        Matcher matcher = REQUEST_MAPPING_PATTERN.matcher(content);
        while (matcher.find()) {
            String mapping = normalizeMappingPath(extractMappingValue(matcher.group(1)));
            if (!mapping.isEmpty()) {
                return mapping;
            }
        }
        return "";
    }

    private String extractMappingValue(String annotationBody) {
        String value = extractAnnotationAttribute(annotationBody, "value");
        if (!value.isEmpty()) {
            return value;
        }
        return extractAnnotationAttribute(annotationBody, "path");
    }

    private String extractAnnotationAttribute(String annotationBody, String attributeName) {
        if (annotationBody == null || annotationBody.isBlank()) {
            return "";
        }
        Matcher namedMatcher =
                Pattern.compile(attributeName + "\\s*=\\s*\"([^\"]+)\"").matcher(annotationBody);
        if (namedMatcher.find()) {
            return namedMatcher.group(1).trim();
        }
        if ("value".equals(attributeName)) {
            Matcher valueMatcher =
                    Pattern.compile("^\\s*\"([^\"]+)\"").matcher(annotationBody.trim());
            if (valueMatcher.find()) {
                return valueMatcher.group(1).trim();
            }
        }
        return "";
    }

    private String normalizeMappingPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = resolvePlaceholderDefault(path.trim());
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.replaceAll("/+$", "");
    }

    private String resolvePlaceholderDefault(String path) {
        if (!path.startsWith("${") || !path.endsWith("}")) {
            return path;
        }
        int separator = path.indexOf(':');
        if (separator < 0 || separator >= path.length() - 1) {
            return path;
        }
        return path.substring(separator + 1, path.length() - 1).trim();
    }

    private String artifactIdFromPath(Path file) {
        String artifactId = "";
        for (Path part : file) {
            String name = part.toString();
            if (name.startsWith("mango-")) {
                artifactId = name;
            }
        }
        return artifactId;
    }

    private boolean isControllerSource(String content) {
        return content.contains("@RestController") || content.contains("@Controller");
    }

    private void checkNaming() {
        getLog().info("Checking Mango-specific naming conventions...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<NamingIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if ("pom.xml".equals(file.getFileName().toString())) {
                                analyzeMavenModuleName(file, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (NamingIssue issue : issues) {
                result.addIssue(
                        "NAMING",
                        issue.severity,
                        issue.file,
                        issue.line,
                        issue.description,
                        "NAMING",
                        DOC_NAMING_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file);
            }
            getLog().warn("Found " + issues.size() + " naming violation(s)");
        } else {
            getLog().info("Mango-specific naming checks passed");
        }
    }

    private void analyzeMavenModuleName(Path file, List<NamingIssue> issues) {
        try {
            String content = Files.readString(file);
            String projectContent = content.replaceFirst("(?s)<parent>.*?</parent>", "");
            Matcher matcher =
                    Pattern.compile("<artifactId>\\s*([^<\\s]+)\\s*</artifactId>")
                            .matcher(projectContent);
            if (!matcher.find()) {
                return;
            }

            String artifactId = matcher.group(1).trim();
            if (!artifactId.matches("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")) {
                issues.add(
                        new NamingIssue(
                                "MAJOR",
                                file.toString(),
                                lineNumber(content, content.indexOf(artifactId)),
                                "Mango module artifactId must use kebab-case: " + artifactId));
            }
        } catch (IOException e) {
            issues.add(new NamingIssue("MAJOR", file.toString(), 0, "模块命名检查失败: " + e.getMessage()));
        }
    }

    /**
     * Rules: 1. Mango modules are evaluated in two dimensions: first-level layer (common / infra /
     * platform / app), then second-level role (api / support / core / starter / starter-remote). 2.
     * *-api cannot depend on *-support, *-core, *-starter, or any *-starter-*. 3. *-support cannot
     * depend on *-core, *-starter, or any *-starter-*. 4. *-support cannot contain persistence
     * content, Spring Boot auto-configuration, module metadata, controllers, or Feign adapters. 5.
     * *-core cannot depend on *-core, *-starter, or any *-starter-*. 6. *-starter-remote can only
     * depend on its own *-api, *-support and mango-infra-feign-starter inside io.mango modules. 7.
     * *-starter-remote must not directly depend on spring-cloud-starter-openfeign. 8. Non-resource
     * non-app modules can only depend on mango-resource-api unless explicitly excepted with a
     * reason. *-app modules are runtime assembly boundaries and may depend on resource runtime
     * starters.
     */
    private void checkDependency() {
        getLog().info("Checking module dependencies...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<DependencyIssue> issues = new ArrayList<>();
        List<Path> scopedPomFiles = resolveCheckPomFiles(rootPath);
        try {
            if (scopedPomFiles.isEmpty()) {
                Files.walkFileTree(
                        rootPath,
                        new SkippingFileVisitor() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                if (!isIgnoredScanPath(file)
                                        && file.toString().endsWith("/pom.xml")) {
                                    analyzePomDependency(file, issues);
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
            } else {
                getLog().info(
                                "Dependency check scope: "
                                        + scopedPomFiles.size()
                                        + " Maven reactor project(s)");
                for (Path pomFile : scopedPomFiles) {
                    analyzePomDependency(pomFile, issues);
                }
            }
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (DependencyIssue issue : issues) {
                result.addIssue(
                        "DEPENDENCY",
                        issue.severity,
                        issue.file,
                        0,
                        issue.description,
                        "DEPENDENCY",
                        DOC_MODULE_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file);
            }
            getLog().warn("Found " + issues.size() + " dependency violation(s)");
        } else {
            getLog().info("All dependency checks passed");
        }
    }

    private List<Path> resolveCheckPomFiles(Path rootPath) {
        if (session == null || session.getProjects() == null || session.getProjects().isEmpty()) {
            return List.of();
        }
        Path normalizedRoot = rootPath.toAbsolutePath().normalize();
        List<Path> pomFiles = new ArrayList<>();
        Set<Path> seen = new HashSet<>();
        for (MavenProject project : session.getProjects()) {
            if (project == null || project.getFile() == null) {
                continue;
            }
            Path pomFile = project.getFile().toPath().toAbsolutePath().normalize();
            if (!pomFile.startsWith(normalizedRoot) || !Files.exists(pomFile)) {
                continue;
            }
            if (seen.add(pomFile)) {
                pomFiles.add(pomFile);
            }
        }
        return pomFiles;
    }

    private void analyzePomDependency(Path pomFile, List<DependencyIssue> issues) {
        try {
            String content = Files.readString(pomFile);
            String artifactId = extractArtifactId(content);
            if (artifactId == null) {
                return;
            }

            ModuleType moduleType = classifyModule(artifactId);
            if (moduleType == ModuleType.SUPPORT) {
                analyzeSupportModuleContent(pomFile, artifactId, issues);
            }
            Map<String, String> resourceDependencyExceptions = parseResourceDependencyExceptions();
            for (String dependencyBlock : extractDependencies(content)) {
                String depArtifactId = extractArtifactIdFromDep(dependencyBlock);
                if (depArtifactId == null || depArtifactId.isEmpty()) {
                    continue;
                }
                if ("test".equals(extractScopeFromDep(dependencyBlock))) {
                    continue;
                }
                String depGroupId = extractGroupIdFromDep(dependencyBlock);
                if (moduleType == ModuleType.STARTER_REMOTE
                        && "org.springframework.cloud".equals(depGroupId)
                        && "spring-cloud-starter-openfeign".equals(depArtifactId)) {
                    DependencyIssue issue =
                            new DependencyIssue(
                                    "CRITICAL",
                                    "*-starter-remote 模块禁止直接依赖 spring-cloud-starter-openfeign，请依赖"
                                            + " mango-infra-feign-starter: "
                                            + artifactId
                                            + " -> "
                                            + depArtifactId);
                    issue.file = pomFile.toString();
                    issues.add(issue);
                    continue;
                }
                if (!isMangoGroupId(depGroupId)) {
                    continue;
                }

                DependencyIssue resourceIssue =
                        validateResourceRegistryDependency(
                                pomFile,
                                moduleType,
                                artifactId,
                                depGroupId,
                                depArtifactId,
                                resourceDependencyExceptions);
                if (resourceIssue != null) {
                    resourceIssue.file = pomFile.toString();
                    issues.add(resourceIssue);
                    continue;
                }
                if (isExplicitResourceRuntimeDependencyException(
                        pomFile,
                        artifactId,
                        depGroupId,
                        depArtifactId,
                        resourceDependencyExceptions)) {
                    continue;
                }

                DependencyIssue issue = validateDependency(moduleType, artifactId, depArtifactId);
                if (issue != null) {
                    issue.file = pomFile.toString();
                    issues.add(issue);
                }
            }
        } catch (Exception e) {
            getLog().warn("Failed to analyze " + pomFile + ": " + e.getMessage());
        }
    }

    private DependencyIssue validateResourceRegistryDependency(
            Path pomFile,
            ModuleType moduleType,
            String consumerArtifact,
            String depGroupId,
            String depArtifact,
            Map<String, String> exceptions) {
        if (!RESOURCE_GROUP_ID.equals(depGroupId)
                || !RESOURCE_RUNTIME_ARTIFACTS.contains(depArtifact)) {
            return null;
        }
        if (moduleType == ModuleType.ROOT || moduleType == ModuleType.APP) {
            return null;
        }

        if (isResourceModulePom(pomFile)) {
            return validateResourceInternalDependency(consumerArtifact, depArtifact);
        }
        if (isResourceDependencyException(consumerArtifact, exceptions)) {
            getLog().warn(
                            "Resource starter dependency exception accepted for "
                                    + consumerArtifact
                                    + ": "
                                    + exceptions.get(consumerArtifact));
            return null;
        }
        return new DependencyIssue(
                "CRITICAL",
                "非 mango-resource 模块默认只能依赖 mango-resource-api；依赖 "
                        + depArtifact
                        + " 需要人工明确确认并通过 -Dmango.check.resourceStarterDependencyExceptions="
                        + consumerArtifact
                        + "=<reason> 记录理由: "
                        + consumerArtifact
                        + " -> "
                        + depArtifact);
    }

    private boolean isExplicitResourceRuntimeDependencyException(
            Path pomFile,
            String consumerArtifact,
            String depGroupId,
            String depArtifact,
            Map<String, String> exceptions) {
        return !isResourceModulePom(pomFile)
                && RESOURCE_GROUP_ID.equals(depGroupId)
                && RESOURCE_RUNTIME_ARTIFACTS.contains(depArtifact)
                && isResourceDependencyException(consumerArtifact, exceptions);
    }

    private DependencyIssue validateResourceInternalDependency(
            String consumerArtifact, String depArtifact) {
        if (("mango-resource-starter-remote".equals(consumerArtifact)
                        || "mango-resource-sync-starter".equals(consumerArtifact))
                && ("mango-resource-core".equals(depArtifact)
                        || "mango-resource-starter".equals(depArtifact))) {
            return new DependencyIssue(
                    "CRITICAL",
                    consumerArtifact
                            + " 不能依赖本地 Resource Registry runtime: "
                            + consumerArtifact
                            + " -> "
                            + depArtifact);
        }
        return null;
    }

    private boolean isResourceModulePom(Path pomFile) {
        return pomFile.toString().replace('\\', '/').contains("/mango-resource/");
    }

    private boolean isResourceDependencyException(
            String artifactId, Map<String, String> exceptions) {
        String reason = exceptions.get(artifactId);
        return reason != null && !reason.isBlank();
    }

    private Map<String, String> parseResourceDependencyExceptions() {
        if (resourceStarterDependencyExceptions == null
                || resourceStarterDependencyExceptions.isBlank()) {
            return Map.of();
        }
        Map<String, String> exceptions = new LinkedHashMap<>();
        for (String entry : resourceStarterDependencyExceptions.split(",")) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                continue;
            }
            String artifactId = entry.substring(0, separator).trim();
            String reason = entry.substring(separator + 1).trim();
            if (!artifactId.isBlank() && !reason.isBlank()) {
                exceptions.put(artifactId, reason);
            }
        }
        return exceptions;
    }

    private void analyzeSupportModuleContent(
            Path pomFile, String artifactId, List<DependencyIssue> issues) throws IOException {
        Path moduleDir = pomFile.getParent();
        Path sourceDir = moduleDir.resolve("src/main");
        if (!Files.exists(sourceDir)) {
            return;
        }
        Files.walkFileTree(
                sourceDir,
                new SkippingFileVisitor() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        String normalized = file.toString().replace('\\', '/');
                        if (normalized.contains("/db/migration/")) {
                            issues.add(
                                    supportContentIssue(
                                            file, artifactId, "support 模块禁止包含 db/migration"));
                            return FileVisitResult.CONTINUE;
                        }
                        if (normalized.endsWith("/META-INF/mango/module.properties")) {
                            issues.add(
                                    supportContentIssue(
                                            file,
                                            artifactId,
                                            "support 模块禁止声明 module.properties，只有本地 starter"
                                                    + " 可以声明模块信息"));
                            return FileVisitResult.CONTINUE;
                        }
                        if (normalized.endsWith("AutoConfiguration.imports")) {
                            issues.add(
                                    supportContentIssue(
                                            file,
                                            artifactId,
                                            "support 模块禁止声明 Spring Boot"
                                                    + " AutoConfiguration.imports"));
                            return FileVisitResult.CONTINUE;
                        }
                        if (!normalized.endsWith(".java")) {
                            return FileVisitResult.CONTINUE;
                        }
                        String code = Files.readString(file);
                        if (code.contains("@RestController") || code.contains("@Controller")) {
                            issues.add(
                                    supportContentIssue(
                                            file,
                                            artifactId,
                                            "support 模块禁止包含 Controller，HTTP 入口必须放在 starter"));
                            return FileVisitResult.CONTINUE;
                        }
                        if (code.contains("@FeignClient")) {
                            issues.add(
                                    supportContentIssue(
                                            file,
                                            artifactId,
                                            "support 模块禁止包含 Feign adapter，远程适配必须放在"
                                                    + " starter-remote"));
                            return FileVisitResult.CONTINUE;
                        }
                        if (SUPPORT_PERSISTENCE_CONTENT_PATTERN.matcher(code).find()
                                || containsPersistenceTypeDeclaration(code)) {
                            issues.add(
                                    supportContentIssue(
                                            file,
                                            artifactId,
                                            "support 模块禁止包含持久化内容，持久化能力必须放在 api/core/starter"
                                                    + " 的对应边界内"));
                            return FileVisitResult.CONTINUE;
                        }
                        if (SUPPORT_AUTO_CONFIGURATION_PATTERN.matcher(code).find()) {
                            issues.add(
                                    supportContentIssue(
                                            file,
                                            artifactId,
                                            "support 模块禁止包含 Spring Boot 自动配置或配置属性装配"));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    private boolean containsPersistenceTypeDeclaration(String code) {
        Matcher matcher = TYPE_DECLARATION_PATTERN.matcher(code);
        while (matcher.find()) {
            String typeName = matcher.group(1);
            if (typeName.endsWith("Entity") || typeName.endsWith("Mapper")) {
                return true;
            }
        }
        return false;
    }

    private DependencyIssue supportContentIssue(Path file, String artifactId, String description) {
        DependencyIssue issue = new DependencyIssue("CRITICAL", artifactId + " " + description);
        issue.file = file.toString();
        return issue;
    }

    private ModuleType classifyModule(String artifactId) {
        for (ModuleSuffix suffix : MODULE_SUFFIXES) {
            if (artifactId.endsWith(suffix.value())) {
                return suffix.type();
            }
        }
        if (isMangoPlatformArtifact(artifactId)) {
            return ModuleType.OTHER;
        }
        return ModuleType.ROOT;
    }

    private boolean isMangoPlatformArtifact(String artifactId) {
        if (!artifactId.startsWith(MANGO_ARTIFACT_PREFIX)) {
            return false;
        }
        return !"mango".equals(artifactId) && !"mango-parent".equals(artifactId);
    }

    private DependencyIssue validateDependency(
            ModuleType consumer, String consumerArtifact, String depArtifact) {
        return switch (consumer) {
            case API -> validateApiDependency(consumerArtifact, depArtifact);
            case SUPPORT -> validateSupportDependency(consumerArtifact, depArtifact);
            case CORE -> validateCoreDependency(consumerArtifact, depArtifact);
            case STARTER_REMOTE -> validateRemoteDependency(consumerArtifact, depArtifact);
            default -> null;
        };
    }

    private DependencyIssue validateApiDependency(String consumerArtifact, String depArtifact) {
        if (depArtifact.endsWith(SUPPORT_SUFFIX)) {
            return dependencyIssue("*-api 模块不能依赖 *-support", consumerArtifact, depArtifact);
        }
        if (depArtifact.endsWith(CORE_SUFFIX)) {
            return dependencyIssue("*-api 模块不能依赖 *-core", consumerArtifact, depArtifact);
        }
        if (isStarterArtifact(depArtifact)) {
            return dependencyIssue(
                    "*-api 模块不能依赖 *-starter 或 *-starter-*", consumerArtifact, depArtifact);
        }
        return null;
    }

    private DependencyIssue validateSupportDependency(String consumerArtifact, String depArtifact) {
        if (depArtifact.endsWith(CORE_SUFFIX)) {
            return dependencyIssue("*-support 模块不能依赖 *-core", consumerArtifact, depArtifact);
        }
        if (isStarterArtifact(depArtifact)) {
            return dependencyIssue(
                    "*-support 模块不能依赖 *-starter 或 *-starter-*", consumerArtifact, depArtifact);
        }
        return null;
    }

    private DependencyIssue validateCoreDependency(String consumerArtifact, String depArtifact) {
        if (depArtifact.endsWith(CORE_SUFFIX)) {
            return dependencyIssue("*-core 模块不能依赖其它模块 *-core", consumerArtifact, depArtifact);
        }
        if (isStarterArtifact(depArtifact)) {
            return dependencyIssue(
                    "*-core 模块不能依赖 *-starter 或 *-starter-*", consumerArtifact, depArtifact);
        }
        return null;
    }

    private DependencyIssue validateRemoteDependency(String consumerArtifact, String depArtifact) {
        if (isAuthorizationRemoteSupportDependency(consumerArtifact, depArtifact)) {
            return null;
        }
        if (isRemoteInfrastructureDependency(depArtifact)) {
            return null;
        }
        String expectedApi = expectedRemoteApi(consumerArtifact);
        String expectedSupport = expectedRemoteSupport(consumerArtifact);
        if (depArtifact.equals(expectedApi) || depArtifact.equals(expectedSupport)) {
            return null;
        }
        return new DependencyIssue(
                "CRITICAL",
                "*-starter-remote 模块只能依赖本模块 api/support 或 mango-infra-feign-starter: "
                        + consumerArtifact
                        + " -> "
                        + depArtifact
                        + "，期望依赖 "
                        + expectedApi
                        + "、"
                        + expectedSupport
                        + " 或 mango-infra-feign-starter");
    }

    private DependencyIssue dependencyIssue(
            String message, String consumerArtifact, String depArtifact) {
        return new DependencyIssue(
                "CRITICAL", message + ": " + consumerArtifact + " -> " + depArtifact);
    }

    private boolean isStarterArtifact(String artifactId) {
        return artifactId.endsWith(STARTER_SUFFIX)
                || artifactId.contains(STARTER_SUFFIX + "-")
                || artifactId.endsWith(SYNC_STARTER_SUFFIX);
    }

    private boolean isAuthorizationRemoteSupportDependency(
            String consumerArtifact, String depArtifact) {
        return "mango-authorization-starter-remote".equals(consumerArtifact)
                && "mango-authorization-support".equals(depArtifact);
    }

    private boolean isRemoteInfrastructureDependency(String depArtifact) {
        return "mango-infra-feign-starter".equals(depArtifact);
    }

    private String expectedRemoteApi(String consumerArtifact) {
        if (consumerArtifact.endsWith("-starter-remote")) {
            return consumerArtifact.substring(
                            0, consumerArtifact.length() - "-starter-remote".length())
                    + "-api";
        }
        return consumerArtifact;
    }

    private String expectedRemoteSupport(String consumerArtifact) {
        if (consumerArtifact.endsWith("-starter-remote")) {
            return consumerArtifact.substring(
                            0, consumerArtifact.length() - "-starter-remote".length())
                    + "-support";
        }
        return consumerArtifact;
    }

    /**
     * Rules: 1. *-api can depend on mango-infra-web-api, but not mango-infra-web-starter. 2.
     * Modules that depend on mango-infra-web-starter must not also declare spring-boot-starter-web.
     * 3. Refactored common/infra modules must not depend directly on spring-boot-starter-web.
     */
    private void checkWebBoundary() {
        getLog().info("Checking web boundary declarations...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<WebBoundaryIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (!isIgnoredScanPath(file) && file.toString().endsWith("/pom.xml")) {
                                analyzeWebBoundaryPom(file, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (WebBoundaryIssue issue : issues) {
                result.addIssue(
                        "WEB_BOUNDARY",
                        issue.severity,
                        issue.file,
                        0,
                        issue.description,
                        "WEB_BOUNDARY",
                        DOC_MODULE_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file);
            }
            getLog().warn("Found " + issues.size() + " web boundary violation(s)");
        } else {
            getLog().info("All web boundary checks passed");
        }
    }

    private void analyzeWebBoundaryPom(Path pomFile, List<WebBoundaryIssue> issues) {
        try {
            String content = Files.readString(pomFile);
            String artifactId = extractArtifactId(content);
            if (artifactId == null) {
                return;
            }
            List<String> dependencies = extractDependencies(content);
            WebDependencies webDependencies = webDependencies(dependencies);
            checkApiWebStarter(pomFile, artifactId, webDependencies, issues);
            checkDuplicateWebStarter(pomFile, artifactId, webDependencies, issues);
            checkRefactoredWebScope(pomFile, artifactId, webDependencies, issues);
            checkInnerWebApi(pomFile, artifactId, webDependencies, issues);
        } catch (IOException e) {
            issues.add(
                    new WebBoundaryIssue(
                            "MAJOR", pomFile.toString(), "Web 边界检查失败: " + e.getMessage()));
        }
    }

    private WebDependencies webDependencies(List<String> dependencies) {
        return new WebDependencies(
                hasMangoDependency(dependencies, "mango-infra-web-api"),
                hasMangoDependency(dependencies, "mango-infra-web-starter"),
                hasDependency(dependencies, "org.springframework.boot", "spring-boot-starter-web"));
    }

    private void checkApiWebStarter(
            Path pomFile,
            String artifactId,
            WebDependencies dependencies,
            List<WebBoundaryIssue> issues) {
        if (artifactId.endsWith(API_SUFFIX) && dependencies.webStarter()) {
            issues.add(
                    new WebBoundaryIssue(
                            "CRITICAL",
                            pomFile.toString(),
                            "*-api 如需 Web 边界注解只能依赖 mango-infra-web-api，禁止依赖"
                                    + " mango-infra-web-starter"));
        }
    }

    private void checkDuplicateWebStarter(
            Path pomFile,
            String artifactId,
            WebDependencies dependencies,
            List<WebBoundaryIssue> issues) {
        if (dependencies.webStarter() && dependencies.springWebStarter()) {
            issues.add(
                    new WebBoundaryIssue(
                            "MAJOR",
                            pomFile.toString(),
                            artifactId
                                    + " 已依赖 mango-infra-web-starter，不应重复声明"
                                    + " spring-boot-starter-web"));
        }
    }

    private void checkRefactoredWebScope(
            Path pomFile,
            String artifactId,
            WebDependencies dependencies,
            List<WebBoundaryIssue> issues) {
        if (!dependencies.springWebStarter()) {
            return;
        }
        if (!isRefactoredWebScope(pomFile, artifactId)
                || "mango-infra-web-starter".equals(artifactId)) {
            return;
        }
        issues.add(
                new WebBoundaryIssue(
                        "MAJOR",
                        pomFile.toString(),
                        artifactId + " 属于已重构 Web 边界范围，不应直接依赖 spring-boot-starter-web"));
    }

    private void checkInnerWebApi(
            Path pomFile,
            String artifactId,
            WebDependencies dependencies,
            List<WebBoundaryIssue> issues)
            throws IOException {
        if (!artifactId.endsWith(API_SUFFIX) || dependencies.webApi()) {
            return;
        }
        if (sourceUsesInner(pomFile)) {
            issues.add(
                    new WebBoundaryIssue(
                            "CRITICAL",
                            pomFile.toString(),
                            artifactId + " 使用 @Inner 时必须显式依赖 mango-infra-web-api"));
        }
    }

    private boolean hasDependency(
            List<String> dependencyBlocks, String groupId, String artifactId) {
        for (String dependencyBlock : dependencyBlocks) {
            if (artifactId.equals(extractArtifactIdFromDep(dependencyBlock))
                    && groupId.equals(extractGroupIdFromDep(dependencyBlock))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMangoDependency(List<String> dependencyBlocks, String artifactId) {
        for (String dependencyBlock : dependencyBlocks) {
            if (artifactId.equals(extractArtifactIdFromDep(dependencyBlock))
                    && isMangoGroupId(extractGroupIdFromDep(dependencyBlock))) {
                return true;
            }
        }
        return false;
    }

    private boolean isMangoGroupId(String groupId) {
        return MANGO_GROUP_ID_PREFIX.equals(groupId)
                || (groupId != null && groupId.startsWith(MANGO_GROUP_ID_PREFIX + "."));
    }

    private boolean isRefactoredWebScope(Path pomFile, String artifactId) {
        String normalized = pomFile.toString().replace('\\', '/');
        return artifactId.startsWith("mango-infra-kv")
                || artifactId.startsWith("mango-infra-realtime")
                || artifactId.startsWith("mango-infra-web")
                || normalized.contains("/mango-common/");
    }

    private boolean sourceUsesInner(Path pomFile) throws IOException {
        Path sourceRoot = pomFile.getParent().resolve("src/main/java");
        if (!Files.exists(sourceRoot)) {
            return false;
        }
        final boolean[] found = {false};
        Files.walkFileTree(
                sourceRoot,
                new SkippingFileVisitor() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        if (file.toString().endsWith(".java")
                                && Files.readString(file).contains("@Inner")) {
                            found[0] = true;
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        return found[0];
    }

    /**
     * Rules: 1. Local *-starter modules must provide META-INF/mango/module.properties. 2.
     * module.properties must declare module-name, module-path. 3. module-name must be stable and
     * kebab-case. 4. module-path must be unique across starter modules. 5. starter controller root
     * path must start with module-path or /_{module-path}. 6. starter-remote reverse controller
     * root path must start with /_{module-path}.
     */
    private void checkModuleInfo() {
        getLog().info("Checking module info declarations...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<ModuleInfoIssue> issues = new ArrayList<>();
        Map<String, ModuleDescriptor> descriptors = loadModuleDescriptors(rootPath, issues);
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (!isIgnoredScanPath(file) && file.toString().endsWith("/pom.xml")) {
                                analyzeModuleInfo(file, descriptors, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (ModuleInfoIssue issue : issues) {
                result.addIssue(
                        "MODULE_INFO",
                        issue.severity,
                        issue.file,
                        0,
                        issue.description,
                        "MODULE_INFO",
                        DOC_MODULE_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file);
            }
            getLog().warn("Found " + issues.size() + " module info violation(s)");
        } else {
            getLog().info("All module info checks passed");
        }
    }

    private void analyzeModuleInfo(
            Path pomFile, Map<String, ModuleDescriptor> descriptors, List<ModuleInfoIssue> issues) {
        try {
            String content = Files.readString(pomFile);
            String artifactId = extractArtifactId(content);
            if (artifactId == null || classifyModule(artifactId) != ModuleType.STARTER) {
                return;
            }
            if ("mango-infra-module-starter".equals(artifactId)) {
                return;
            }

            Path moduleInfoFile =
                    pomFile.getParent()
                            .resolve("src/main/resources/META-INF/mango/module.properties");
            if (!Files.exists(moduleInfoFile)) {
                issues.add(
                        new ModuleInfoIssue(
                                "CRITICAL",
                                moduleInfoFile.toString(),
                                artifactId + " 缺少 META-INF/mango/module.properties"));
                return;
            }

            ModuleDescriptor descriptor = descriptors.get(artifactId);
            if (descriptor == null) {
                return;
            }

            for (Path javaFile : starterJavaFiles(pomFile.getParent())) {
                String rootPath = extractControllerRootPath(javaFile);
                if (rootPath.isEmpty()) {
                    continue;
                }
                if (!isModuleControllerPath(rootPath, descriptor.modulePath)) {
                    issues.add(
                            new ModuleInfoIssue(
                                    "CRITICAL",
                                    javaFile.toString(),
                                    artifactId
                                            + " 的 Controller 根路径必须以 module-path "
                                            + descriptor.modulePath
                                            + " 或反向 module-path "
                                            + reverseModulePath(descriptor.modulePath)
                                            + " 开头，当前为 "
                                            + rootPath));
                }
            }

            Path remoteModuleDir = siblingRemoteModule(pomFile.getParent(), artifactId);
            if (remoteModuleDir != null) {
                for (Path javaFile : starterJavaFiles(remoteModuleDir)) {
                    String rootPath = extractControllerRootPath(javaFile);
                    if (rootPath.isEmpty()) {
                        continue;
                    }
                    if (!isReverseModuleControllerPath(rootPath, descriptor.modulePath)) {
                        issues.add(
                                new ModuleInfoIssue(
                                        "CRITICAL",
                                        javaFile.toString(),
                                        remoteModuleDir.getFileName()
                                                + " 的反向 Controller 根路径必须以 "
                                                + reverseModulePaths(descriptor.modulePath)
                                                + " 开头，当前为 "
                                                + rootPath));
                    }
                }
            }
        } catch (Exception e) {
            issues.add(
                    new ModuleInfoIssue(
                            "MAJOR", pomFile.toString(), "模块信息检查失败: " + e.getMessage()));
        }
    }

    private boolean isModuleControllerPath(String rootPath, String modulePath) {
        return isAnyModulePathPrefix(rootPath, modulePath)
                || isReverseModuleControllerPath(rootPath, modulePath);
    }

    private boolean isReverseModuleControllerPath(String rootPath, String modulePath) {
        if (modulePath == null || modulePath.isBlank()) {
            return false;
        }
        for (String candidate : modulePath.split(",")) {
            String normalized = normalizeModulePath(candidate);
            if (!normalized.isEmpty() && rootPath.startsWith(reverseModulePath(normalized))) {
                return true;
            }
        }
        return false;
    }

    private String reverseModulePaths(String modulePath) {
        if (modulePath == null || modulePath.isBlank()) {
            return "";
        }
        List<String> paths = new ArrayList<>();
        for (String candidate : modulePath.split(",")) {
            String normalized = normalizeModulePath(candidate);
            if (!normalized.isEmpty()) {
                paths.add(reverseModulePath(normalized));
            }
        }
        return String.join(",", paths);
    }

    /**
     * Rules: 1. starter-remote Feign client name must use the target module-name. 2. starter-remote
     * Feign client contextId must explicitly use lowerCamelCase Feign interface name. 3.
     * starter-remote Feign client path must start with the target module-path or reverse
     * module-path. 4. One Feign client must implement exactly one XxxApi.
     */
    private void checkRemoteAdapter() {
        getLog().info("Checking remote adapter declarations...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<RemoteAdapterIssue> issues = new ArrayList<>();
        Map<String, String> feignContextOwners = new LinkedHashMap<>();
        Map<String, ModuleDescriptor> descriptors =
                loadModuleDescriptors(rootPath, new ArrayList<>());

        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isStarterRemoteJavaFile(file)) {
                                analyzeRemoteAdapter(file, descriptors, feignContextOwners, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (RemoteAdapterIssue issue : issues) {
                result.addIssue(
                        "REMOTE_ADAPTER",
                        issue.severity,
                        issue.file,
                        0,
                        issue.description,
                        "REMOTE_ADAPTER",
                        DOC_MODULE_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file);
            }
            getLog().warn("Found " + issues.size() + " remote adapter violation(s)");
        } else {
            getLog().info("All remote adapter checks passed");
        }
    }

    private boolean isStarterRemoteJavaFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return !isIgnoredScanPath(file)
                && normalized.contains(STARTER_REMOTE_SUFFIX + "/src/main/java/")
                && normalized.endsWith(JAVA_FILE_SUFFIX);
    }

    private void analyzeRemoteAdapter(
            Path file,
            Map<String, ModuleDescriptor> descriptors,
            Map<String, String> feignContextOwners,
            List<RemoteAdapterIssue> issues) {
        try {
            String content = Files.readString(file);
            Matcher matcher = FEIGN_CLIENT_PATTERN.matcher(content);
            String artifactId = artifactIdFromPath(file);
            String expectedArtifactId = expectedStarterArtifact(artifactId);
            ModuleDescriptor descriptor = descriptors.get(expectedArtifactId);
            while (matcher.find()) {
                analyzeFeignClient(
                        file, content, matcher, artifactId, descriptor, feignContextOwners, issues);
            }
        } catch (IOException e) {
            issues.add(
                    new RemoteAdapterIssue(
                            "MAJOR", file.toString(), "远程适配器检查失败: " + e.getMessage()));
        }
    }

    private String expectedStarterArtifact(String artifactId) {
        if (!artifactId.endsWith(STARTER_REMOTE_SUFFIX)) {
            return artifactId;
        }
        return artifactId.substring(0, artifactId.length() - STARTER_REMOTE_SUFFIX.length())
                + STARTER_SUFFIX;
    }

    private void analyzeFeignClient(
            Path file,
            String content,
            Matcher matcher,
            String artifactId,
            ModuleDescriptor descriptor,
            Map<String, String> contextOwners,
            List<RemoteAdapterIssue> issues) {
        String annotationBody = matcher.group(1);
        FeignMetadata metadata =
                new FeignMetadata(
                        extractAnnotationAttribute(annotationBody, "name"),
                        extractAnnotationAttribute(annotationBody, "contextId"),
                        normalizeMappingPath(extractAnnotationAttribute(annotationBody, "path")));
        FeignInterfaceDeclaration declaration =
                findFeignInterfaceDeclaration(content, matcher.end());
        if (declaration == null) {
            issues.add(
                    new RemoteAdapterIssue(
                            "CRITICAL", file.toString(), "@FeignClient 必须声明在 XxxFeignClient 接口上"));
            return;
        }
        checkFeignContextId(file, declaration, metadata.contextId(), issues);
        checkFeignContextUniqueness(file, artifactId, declaration, metadata, contextOwners, issues);
        checkFeignApiCount(file, declaration, issues);
        checkFeignDescriptor(file, artifactId, declaration, metadata, descriptor, issues);
    }

    private void checkFeignContextId(
            Path file,
            FeignInterfaceDeclaration declaration,
            String contextId,
            List<RemoteAdapterIssue> issues) {
        String expectedContextId = lowerCamelCase(declaration.name);
        if (expectedContextId.equals(contextId)) {
            return;
        }
        String actual = contextId;
        if (actual.isEmpty()) {
            actual = "<missing>";
        }
        issues.add(
                new RemoteAdapterIssue(
                        "CRITICAL",
                        file.toString(),
                        "@FeignClient contextId 必须显式使用 Feign 接口名 lowerCamelCase "
                                + expectedContextId
                                + "，当前为 "
                                + actual));
    }

    private void checkFeignContextUniqueness(
            Path file,
            String artifactId,
            FeignInterfaceDeclaration declaration,
            FeignMetadata metadata,
            Map<String, String> contextOwners,
            List<RemoteAdapterIssue> issues) {
        if (metadata.name().isEmpty() || metadata.contextId().isEmpty()) {
            return;
        }
        String contextKey = artifactId + "|" + metadata.name() + "|" + metadata.contextId();
        String owner = contextOwners.putIfAbsent(contextKey, declaration.name);
        if (owner != null && !owner.equals(declaration.name)) {
            issues.add(
                    new RemoteAdapterIssue(
                            "CRITICAL",
                            file.toString(),
                            "相同 @FeignClient name 下 contextId 必须唯一: name="
                                    + metadata.name()
                                    + ", contextId="
                                    + metadata.contextId()
                                    + ", 已被 "
                                    + owner
                                    + " 使用"));
        }
    }

    private void checkFeignApiCount(
            Path file, FeignInterfaceDeclaration declaration, List<RemoteAdapterIssue> issues) {
        int apiCount = countFeignApiContracts(declaration.extendsClause);
        if (apiCount != 1) {
            issues.add(
                    new RemoteAdapterIssue(
                            "CRITICAL",
                            file.toString(),
                            "XxxFeignClient 必须且只能继承一个 XxxApi，当前 API 数="
                                    + apiCount
                                    + ": "
                                    + declaration.name));
        }
    }

    private void checkFeignDescriptor(
            Path file,
            String artifactId,
            FeignInterfaceDeclaration declaration,
            FeignMetadata metadata,
            ModuleDescriptor descriptor,
            List<RemoteAdapterIssue> issues) {
        if (descriptor == null) {
            issues.add(
                    new RemoteAdapterIssue(
                            "CRITICAL",
                            file.toString(),
                            artifactId
                                    + " 缺少目标 starter 的 module.properties，无法校验"
                                    + " @FeignClient"));
            return;
        }
        if (!descriptor.moduleName.equals(metadata.name())) {
            issues.add(
                    new RemoteAdapterIssue(
                            "CRITICAL",
                            file.toString(),
                            "@FeignClient name 必须使用目标模块 module-name "
                                    + descriptor.moduleName
                                    + "，当前为 "
                                    + metadata.name()));
        }
        if (!isFeignModulePathPrefix(metadata.path(), descriptor.modulePath)) {
            issues.add(
                    new RemoteAdapterIssue(
                            "CRITICAL",
                            file.toString(),
                            "@FeignClient path 必须以目标模块 module-path "
                                    + descriptor.modulePath
                                    + " 或反向 module-path "
                                    + reverseModulePaths(descriptor.modulePath)
                                    + " 开头，当前为 "
                                    + metadata.path()));
        }
    }

    private FeignInterfaceDeclaration findFeignInterfaceDeclaration(
            String content, int annotationEnd) {
        Matcher matcher = FEIGN_INTERFACE_DECLARATION_PATTERN.matcher(content);
        matcher.region(annotationEnd, content.length());
        if (!matcher.find()) {
            return null;
        }
        return new FeignInterfaceDeclaration(matcher.group(1).trim(), matcher.group(2));
    }

    private int countFeignApiContracts(String extendsClause) {
        if (extendsClause == null || extendsClause.isBlank()) {
            return 0;
        }
        int extendsIndex = extendsClause.indexOf("extends");
        if (extendsIndex < 0) {
            return 0;
        }
        String apiList = extendsClause.substring(extendsIndex + "extends".length());
        int count = 0;
        for (String apiType : apiList.split(",")) {
            if (simpleTypeName(apiType).endsWith("Api")) {
                count++;
            }
        }
        return count;
    }

    private String lowerCamelCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private boolean isAnyModulePathPrefix(String path, String modulePath) {
        if (modulePath == null || modulePath.isBlank()) {
            return false;
        }
        for (String candidate : modulePath.split(",")) {
            String normalized = normalizeModulePath(candidate);
            if (!normalized.isEmpty() && path.startsWith(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFeignModulePathPrefix(String path, String modulePath) {
        return isAnyModulePathPrefix(path, modulePath)
                || isReverseModuleControllerPath(path, modulePath);
    }

    /**
     * Rules: 1. *-api source must not declare @FeignClient. 2. *-api must not contain local
     * implementation-collaboration types such as *Service/*Manager. 3. HTTP-facing contract types
     * in *-api must use XxxApi naming. 4. Controllers must not hold XxxApi fields. 5. Services must
     * not directly implement XxxApi. 6. Feign clients must implement XxxApi.
     */
    private void checkApiContract() {
        getLog().info("Checking API contracts...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<ApiContractIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isApiJavaFile(file)) {
                                analyzeApiContract(file, issues);
                            }
                            if (isMainJavaFile(file)) {
                                analyzeApiLayerContract(file, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (ApiContractIssue issue : issues) {
                result.addIssue(
                        "API_CONTRACT",
                        issue.severity,
                        issue.file,
                        0,
                        issue.description,
                        "API_CONTRACT",
                        DOC_API_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file);
            }
            getLog().warn("Found " + issues.size() + " API contract violation(s)");
        } else {
            getLog().info("All API contract checks passed");
        }
    }

    private boolean isApiJavaFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return !isIgnoredScanPath(file)
                && normalized.contains(API_SUFFIX + "/src/main/java/")
                && normalized.endsWith(JAVA_FILE_SUFFIX);
    }

    private void analyzeApiContract(Path file, List<ApiContractIssue> issues) {
        try {
            String content = Files.readString(file);
            String code = stripStringLiterals(content);
            String typeName = declaredTypeName(code, file);
            inspectApiFileDeclarations(file, code, typeName, issues);
            inspectApiMethodParameterCounts(file, code, issues);
            if (typeName.endsWith(API_TYPE_SUFFIX)) {
                inspectDeclaredApiMethods(file, code, issues);
            }
        } catch (IOException e) {
            issues.add(
                    new ApiContractIssue(
                            "MAJOR", file.toString(), "API 契约检查失败: " + e.getMessage()));
        }
    }

    private void inspectApiFileDeclarations(
            Path file, String code, String typeName, List<ApiContractIssue> issues) {
        if (code.contains(FEIGN_CLIENT_ANNOTATION)) {
            issues.add(
                    new ApiContractIssue("CRITICAL", file.toString(), "*-api 禁止声明 @FeignClient"));
        }
        if (hasForbiddenApiTypeSuffix(typeName)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "*-api 只能声明协议契约，禁止持久化、实现或适配类型: " + typeName));
        }
        if (containsApiImplementationMarker(code)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "*-api 禁止包含 Entity/Mapper/Controller 实现注解或基类: " + typeName));
        }
        if (isForbiddenLocalApiType(file, typeName)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "*-api 只允许放跨模块契约，禁止出现本地协作类型: " + typeName));
        }
        if (typeName.endsWith(API_TYPE_SUFFIX) && containsLocalImplementationWord(typeName)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "XxxApi 名称只表达跨模块能力，禁止包含 Registry/Dispatcher/Manager/Session 等内部实现词:"
                                    + " "
                                    + typeName));
        }
        if (declaresHttpContract(code) && !typeName.endsWith(API_TYPE_SUFFIX)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "Controller/远程契约接口在 *-api 中必须命名为 XxxApi: " + typeName));
        }
    }

    private boolean hasForbiddenApiTypeSuffix(String typeName) {
        return FORBIDDEN_API_TYPE_SUFFIXES.stream().anyMatch(typeName::endsWith);
    }

    private boolean containsApiImplementationMarker(String code) {
        return code.contains(TABLE_NAME_ANNOTATION)
                || code.contains("BaseMapper<")
                || code.contains(REST_CONTROLLER_ANNOTATION);
    }

    private boolean isForbiddenLocalApiType(Path file, String typeName) {
        if (!isLocalCollaborationType(typeName)) {
            return false;
        }
        return !isApiSpiJavaFile(file) && !isAllowedInfrastructureApi(typeName);
    }

    private void inspectApiMethodParameterCounts(
            Path file, String code, List<ApiContractIssue> issues) {
        Matcher matcher = API_METHOD_PATTERN.matcher(code);
        while (matcher.find()) {
            int parameterCount = countTopLevelParameters(matcher.group(METHOD_PARAMETERS_GROUP));
            if (parameterCount > MAX_DIRECT_API_PARAMETERS) {
                issues.add(
                        new ApiContractIssue(
                                "CRITICAL",
                                file.toString(),
                                "XxxApi 方法超过 2 个入参时必须收敛为 Query/Command 对象: "
                                        + matcher.group(1)
                                        + " 入参数="
                                        + parameterCount));
            }
        }
    }

    private void inspectDeclaredApiMethods(Path file, String code, List<ApiContractIssue> issues) {
        Matcher matcher = API_DECLARED_METHOD_PATTERN.matcher(code);
        while (matcher.find()) {
            inspectDeclaredApiMethod(file, matcher, issues);
        }
    }

    private void inspectDeclaredApiMethod(
            Path file, Matcher matcher, List<ApiContractIssue> issues) {
        String returnType = matcher.group(1);
        if (IGNORED_DECLARED_RETURN_TYPES.contains(returnType)) {
            return;
        }
        if (!isResultType(returnType)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "XxxApi 方法必须统一返回 R<T>: " + matcher.group(2) + " 返回 " + returnType));
        }
        inspectApiPersistenceParameters(file, declaredParameters(matcher), issues);
        if (exposesPersistenceModel(returnType)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL", file.toString(), "XxxApi 返回禁止暴露持久化模型: " + returnType));
        }
    }

    private boolean isResultType(String returnType) {
        return returnType.startsWith("R<") || returnType.startsWith("io.mango.common.result.R<");
    }

    private String declaredParameters(Matcher matcher) {
        String declaration = matcher.group();
        return declaration.substring(declaration.indexOf('(') + 1, declaration.lastIndexOf(')'));
    }

    private void inspectApiPersistenceParameters(
            Path file, String parameters, List<ApiContractIssue> issues) {
        for (String parameter : splitTopLevelParameters(parameters)) {
            String type = parameterType(parameter);
            if (exposesPersistenceModel(type)) {
                issues.add(
                        new ApiContractIssue(
                                "CRITICAL", file.toString(), "XxxApi 入参禁止暴露持久化模型 " + type));
            }
        }
    }

    private int countTopLevelParameters(String parameters) {
        if (parameters == null || parameters.isBlank()) {
            return 0;
        }
        return splitTopLevelParameters(parameters).size();
    }

    private List<String> splitTopLevelParameters(String parameters) {
        if (parameters == null || parameters.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        StringBuilder currentParameter = new StringBuilder();
        ParameterNesting nesting = new ParameterNesting();
        for (int i = 0; i < parameters.length(); i++) {
            char current = parameters.charAt(i);
            if (current == PARAMETER_SEPARATOR && nesting.isTopLevel()) {
                addParameter(result, currentParameter);
                currentParameter.setLength(0);
                continue;
            }
            nesting.accept(current);
            currentParameter.append(current);
        }
        addParameter(result, currentParameter);
        return result;
    }

    private void addParameter(List<String> parameters, StringBuilder candidate) {
        String value = candidate.toString().trim();
        if (!value.isEmpty()) {
            parameters.add(value);
        }
    }

    private String parameterType(String parameter) {
        String normalized =
                parameter
                        .replaceAll("@[A-Za-z0-9_$.]+(?:\\s*\\([^)]*\\))?", " ")
                        .replace("final ", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
        if (normalized.isEmpty()) {
            return "";
        }
        String[] parts = normalized.split("\\s+");
        if (parts.length == 1) {
            return simpleTypeName(parts[0]);
        }
        String type = parts[parts.length - 2];
        if (type.endsWith(VARARGS_SUFFIX)) {
            type = type.substring(0, type.length() - VARARGS_SUFFIX.length());
        }
        return simpleTypeName(type);
    }

    private String simpleTypeName(String type) {
        String value = type.trim();
        int genericStart = value.indexOf('<');
        if (genericStart >= 0) {
            value = value.substring(0, genericStart);
        }
        while (value.endsWith(ARRAY_SUFFIX)) {
            value = value.substring(0, value.length() - ARRAY_SUFFIX.length());
        }
        int dot = value.lastIndexOf('.');
        if (dot >= 0) {
            return value.substring(dot + 1);
        }
        return value;
    }

    private void analyzeApiLayerContract(Path file, List<ApiContractIssue> issues) {
        if (isMangoToolingFile(file)) {
            return;
        }
        try {
            String content = Files.readString(file);
            String code = stripStringLiterals(content);
            String typeName = declaredTypeName(code, file);
            ApiLayerFacts facts = apiLayerFacts(file, code, typeName);
            inspectApiLayerPlacement(file, facts, issues);
            if (code.contains(REST_CONTROLLER_ANNOTATION)) {
                inspectControllerContract(file, code, typeName, issues);
            }
            inspectServiceAndFeignContracts(file, code, facts, issues);
            if (facts.serviceImplementation()) {
                validateServicePreconditions(code, file, issues);
            }
        } catch (IOException e) {
            issues.add(
                    new ApiContractIssue(
                            "MAJOR", file.toString(), "API 分层检查失败: " + e.getMessage()));
        }
    }

    private ApiLayerFacts apiLayerFacts(Path file, String code, String typeName) {
        return new ApiLayerFacts(
                moduleTypeFromSourcePath(file),
                typeName,
                code.contains(REST_CONTROLLER_ANNOTATION)
                        || typeName.endsWith(CONTROLLER_TYPE_SUFFIX),
                code.contains(FEIGN_CLIENT_ANNOTATION)
                        || typeName.endsWith(FEIGN_CLIENT_TYPE_SUFFIX),
                typeName.endsWith(ENTITY_TYPE_SUFFIX) || code.contains(TABLE_NAME_ANNOTATION),
                typeName.endsWith(MAPPER_TYPE_SUFFIX) || code.contains("BaseMapper<"),
                code.contains("@Service") || typeName.endsWith(SERVICE_IMPL_TYPE_SUFFIX));
    }

    private void inspectApiLayerPlacement(
            Path file, ApiLayerFacts facts, List<ApiContractIssue> issues) {
        if (facts.feignClient() && facts.moduleType() != ModuleType.STARTER_REMOTE) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "Feign adapter 只能放在 *-starter-remote: " + facts.typeName()));
        }
        if (facts.controller() && !CONTROLLER_MODULE_TYPES.contains(facts.moduleType())) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "Controller 只能放在 *-starter；反向 Controller 才可放 *-starter-remote: "
                                    + facts.typeName()));
        }
        checkPersistenceTypePlacement(file, facts, issues);
        if (facts.serviceImplementation() && !CORE_MODULE_TYPES.contains(facts.moduleType())) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "业务 Service 实现只能放在 *-core: " + facts.typeName()));
        }
    }

    private void checkPersistenceTypePlacement(
            Path file, ApiLayerFacts facts, List<ApiContractIssue> issues) {
        if (!facts.entity() && !facts.mapper()) {
            return;
        }
        if (CORE_MODULE_TYPES.contains(facts.moduleType())
                || isMangoPersistenceFrameworkFile(file)) {
            return;
        }
        issues.add(
                new ApiContractIssue(
                        "CRITICAL",
                        file.toString(),
                        "Entity/Mapper 只能放在 *-core: " + facts.typeName()));
    }

    private void inspectControllerContract(
            Path file, String code, String typeName, List<ApiContractIssue> issues) {
        if (!CONTROLLER_IMPLEMENTS_API_PATTERN.matcher(code).find()) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "XxxController 必须实现本域 XxxApi: " + typeName));
        }
        if (!code.contains(VALIDATED_ANNOTATION)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "Controller 必须使用 @Validated 开启基础字段校验: " + typeName));
        }
        inspectControllerApiFields(file, code, issues);
        inspectControllerFields(file, code, issues);
        inspectControllerMethods(file, code, issues);
        inspectControllerParameters(file, code, issues);
        if (code.contains(RESULT_FAIL_CALL)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "Controller 禁止自行拼装失败 R；业务失败必须由 Service 使用 Require + BizCode 抛出"));
        }
    }

    private void inspectControllerApiFields(Path file, String code, List<ApiContractIssue> issues) {
        Matcher matcher = API_FIELD_PATTERN.matcher(code);
        while (matcher.find()) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "Controller 禁止持有 XxxApi 字段，应实现 XxxApi 并依赖 IXxxService: "
                                    + matcher.group(1)));
        }
    }

    private void inspectControllerFields(Path file, String code, List<ApiContractIssue> issues) {
        Matcher matcher = CONTROLLER_FIELD_PATTERN.matcher(code);
        while (matcher.find()) {
            String fieldType = matcher.group(1);
            if (isForbiddenControllerField(fieldType)) {
                issues.add(
                        new ApiContractIssue(
                                "CRITICAL",
                                file.toString(),
                                "Controller 只能依赖 IXxxService/等效服务接口，禁止依赖 " + fieldType));
            }
        }
    }

    private boolean isForbiddenControllerField(String fieldType) {
        if (fieldType.endsWith(MAPPER_TYPE_SUFFIX)
                || fieldType.endsWith(ENTITY_TYPE_SUFFIX)
                || fieldType.endsWith(FEIGN_CLIENT_TYPE_SUFFIX)) {
            return true;
        }
        return fieldType.endsWith(SERVICE_TYPE_SUFFIX) && !fieldType.startsWith("I");
    }

    private void inspectControllerMethods(Path file, String code, List<ApiContractIssue> issues) {
        Matcher matcher = MAPPED_METHOD_PATTERN.matcher(code);
        while (matcher.find()) {
            String returnType = matcher.group(1);
            if (!isResultType(returnType)) {
                issues.add(
                        new ApiContractIssue(
                                "CRITICAL",
                                file.toString(),
                                "Controller HTTP 方法必须统一返回 R<T>: "
                                        + matcher.group(2)
                                        + " 返回 "
                                        + returnType));
            }
            if (exposesPersistenceModel(returnType)) {
                issues.add(
                        new ApiContractIssue(
                                "CRITICAL",
                                file.toString(),
                                "Controller 禁止返回持久化模型: " + returnType));
            }
        }
    }

    private void inspectControllerParameters(
            Path file, String code, List<ApiContractIssue> issues) {
        Matcher matcher = JAVA_METHOD_PATTERN.matcher(code);
        while (matcher.find()) {
            for (String parameter :
                    splitTopLevelParameters(matcher.group(METHOD_PARAMETERS_GROUP))) {
                inspectControllerParameter(file, matcher.group(1), parameter, issues);
            }
        }
    }

    private void inspectControllerParameter(
            Path file, String methodName, String parameter, List<ApiContractIssue> issues) {
        if (parameter.contains(REQUEST_BODY_ANNOTATION) && !parameter.contains(VALID_ANNOTATION)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "Controller @RequestBody 必须使用 @Valid 做基础字段校验: " + methodName));
        }
        String type = parameterType(parameter);
        if (exposesPersistenceModel(type)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL", file.toString(), "Controller 入参禁止使用持久化模型 " + type));
        }
    }

    private void inspectServiceAndFeignContracts(
            Path file, String code, ApiLayerFacts facts, List<ApiContractIssue> issues) {
        Matcher serviceMatcher = SERVICE_IMPLEMENTS_API_PATTERN.matcher(code);
        if (serviceMatcher.find()) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "XxxService 禁止直接实现 XxxApi，应实现 IXxxService: "
                                    + serviceMatcher.group(1)));
        }
        if (code.contains(FEIGN_CLIENT_ANNOTATION)
                && !FEIGN_EXTENDS_API_PATTERN.matcher(code).find()) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL", file.toString(), "XxxFeignClient 必须继承本域 XxxApi"));
        }
        if (facts.serviceImplementation() && usesResultContract(code)) {
            issues.add(
                    new ApiContractIssue(
                            "CRITICAL",
                            file.toString(),
                            "Service 禁止返回或拼装 R；R<T> 只属于 API/Controller/Feign 协议边界"));
        }
    }

    private boolean usesResultContract(String code) {
        return SERVICE_R_RETURN_PATTERN.matcher(code).find()
                || code.contains(RESULT_OK_CALL)
                || code.contains(RESULT_FAIL_CALL);
    }

    private ModuleType moduleTypeFromSourcePath(Path file) {
        String normalized = file.toString().replace('\\', '/');
        if (normalized.contains(STARTER_REMOTE_SUFFIX + JAVA_SOURCE_PATH)) {
            return ModuleType.STARTER_REMOTE;
        }
        if (normalized.contains(STARTER_SUFFIX + JAVA_SOURCE_PATH)) {
            return ModuleType.STARTER;
        }
        if (normalized.contains(CORE_SUFFIX + JAVA_SOURCE_PATH)) {
            return ModuleType.CORE;
        }
        if (normalized.contains(API_SUFFIX + JAVA_SOURCE_PATH)) {
            return ModuleType.API;
        }
        return ModuleType.OTHER;
    }

    private void validateServicePreconditions(
            String code, Path file, List<ApiContractIssue> issues) {
        Matcher matcher = SERVICE_IMPL_PUBLIC_METHOD_PATTERN.matcher(code);
        while (matcher.find()) {
            String methodName = matcher.group(1);
            if (!isBusinessActionMethod(methodName)
                    || splitTopLevelParameters(matcher.group(2)).isEmpty()) {
                continue;
            }
            String body = methodBody(code, matcher.end() - 1);
            if (body == null || !body.contains("Require.")) {
                issues.add(
                        new ApiContractIssue(
                                "CRITICAL",
                                file.toString(),
                                "Service 业务动作必须使用 Require 执行业务前置条件校验: " + methodName));
                continue;
            }
            if (SERVICE_DIRECT_THROW_PATTERN.matcher(body).find()) {
                issues.add(
                        new ApiContractIssue(
                                "CRITICAL",
                                file.toString(),
                                "Service 业务动作禁止直接 if/throw 或裸业务异常，必须使用 Require + BizCode: "
                                        + methodName));
            }
            Matcher requireMatcher = REQUIRE_CALL_PATTERN.matcher(body);
            while (requireMatcher.find()) {
                if (!BIZ_CODE_REFERENCE_PATTERN.matcher(requireMatcher.group(1)).find()) {
                    issues.add(
                            new ApiContractIssue(
                                    "CRITICAL",
                                    file.toString(),
                                    "Service Require 必须使用模块 BizCode/ErrorCode，禁止裸数字或错误字符串: "
                                            + methodName));
                }
            }
        }
    }

    private boolean isBusinessActionMethod(String methodName) {
        String normalized = methodName.toLowerCase(Locale.ROOT);
        return BUSINESS_ACTION_PREFIXES.stream().anyMatch(normalized::startsWith);
    }

    private String methodBody(String code, int openingBraceIndex) {
        if (openingBraceIndex < 0
                || openingBraceIndex >= code.length()
                || code.charAt(openingBraceIndex) != '{') {
            openingBraceIndex = code.indexOf('{', Math.max(0, openingBraceIndex));
        }
        if (openingBraceIndex < 0) {
            return null;
        }
        int depth = 0;
        for (int index = openingBraceIndex; index < code.length(); index++) {
            char current = code.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return code.substring(openingBraceIndex + 1, index);
            }
        }
        return null;
    }

    private String declaredTypeName(String content, Path file) {
        Matcher matcher = TYPE_DECLARATION_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        String fileName = file.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        if (index > 0) {
            return fileName.substring(0, index);
        }
        return fileName;
    }

    private boolean isLocalCollaborationType(String typeName) {
        return LOCAL_COLLABORATION_SUFFIXES.stream().anyMatch(typeName::endsWith);
    }

    private boolean isApiSpiJavaFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("-api/src/main/java/") && normalized.contains("/api/spi/");
    }

    private boolean isMangoToolingFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("/mango-tools/mango-maven-plugin/src/main/java/");
    }

    private boolean isMangoPersistenceFrameworkFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("/mango-infra/mango-infra-persistence/");
    }

    private boolean isAllowedInfrastructureApi(String typeName) {
        return "ModuleInfoRegistry".equals(typeName);
    }

    private String stripStringLiterals(String content) {
        StringBuilder result = new StringBuilder(content.length());
        boolean inString = false;
        boolean inTextBlock = false;
        boolean escaped = false;
        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            if (inTextBlock) {
                if (i + 2 < content.length()
                        && content.charAt(i) == '"'
                        && content.charAt(i + 1) == '"'
                        && content.charAt(i + 2) == '"') {
                    result.append("   ");
                    i += 2;
                    inTextBlock = false;
                } else {
                    result.append(current == '\n' ? '\n' : ' ');
                }
                continue;
            }
            if (inString) {
                if (current == '\n') {
                    result.append('\n');
                    inString = false;
                    escaped = false;
                    continue;
                }
                result.append(' ');
                if (current == '"' && !escaped) {
                    inString = false;
                }
                escaped = current == '\\' && !escaped;
                if (current != '\\') {
                    escaped = false;
                }
                continue;
            }
            if (i + 2 < content.length()
                    && content.charAt(i) == '"'
                    && content.charAt(i + 1) == '"'
                    && content.charAt(i + 2) == '"') {
                result.append("   ");
                i += 2;
                inTextBlock = true;
                continue;
            }
            if (current == '"') {
                result.append(' ');
                inString = true;
                escaped = false;
                continue;
            }
            result.append(current);
        }
        return result.toString();
    }

    private boolean containsLocalImplementationWord(String typeName) {
        return LOCAL_IMPLEMENTATION_WORDS.stream().anyMatch(typeName::contains);
    }

    private boolean declaresHttpContract(String content) {
        return HTTP_CONTRACT_ANNOTATIONS.stream().anyMatch(content::contains);
    }

    /**
     * Rules: 1. KV annotation key must not hardcode mango:kv prefix. 2. Dynamic key must use SpEL
     * template syntax such as user:#{#userId}.
     */
    private void checkKvKey() {
        getLog().info("Checking KV key declarations...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<KvKeyIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isMainJavaFile(file)) {
                                analyzeKvKey(file, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (KvKeyIssue issue : issues) {
                result.addIssue(
                        "KV_KEY",
                        issue.severity,
                        issue.file,
                        issue.line,
                        issue.description,
                        "KV_KEY",
                        DOC_NAMING_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file);
            }
            getLog().warn("Found " + issues.size() + " KV key violation(s)");
        } else {
            getLog().info("All KV key checks passed");
        }
    }

    private boolean isMainJavaFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return !isIgnoredScanPath(file)
                && normalized.contains("/src/main/java/")
                && normalized.endsWith(".java");
    }

    private boolean exposesPersistenceModel(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        Matcher matcher = TYPE_TOKEN_PATTERN.matcher(type);
        while (matcher.find()) {
            String token = matcher.group();
            String simpleName = token.substring(token.lastIndexOf('.') + 1);
            if (isPersistenceModelType(simpleName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPersistenceModelType(String simpleName) {
        if (NON_PERSISTENCE_ENTITY_TYPES.contains(simpleName)) {
            return false;
        }
        return simpleName.endsWith(ENTITY_TYPE_SUFFIX) || simpleName.endsWith("PO");
    }

    private boolean isIgnoredScanPath(Path file) {
        if (file == null) {
            return false;
        }
        for (Path segmentPath : file.normalize()) {
            String segment = segmentPath.toString();
            if (IGNORED_SCAN_SEGMENTS.contains(segment)
                    || ".venv".equals(segment)
                    || segment.startsWith(".venv-")) {
                return true;
            }
        }
        return false;
    }

    /** 禁止 HTTP API 使用 /{id} 这类路径变量。统一使用 query 参数或 command/query 对象。 */
    private void checkPathParam() {
        getLog().info("Checking path parameter usage...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<PathParamIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isMainJavaFile(file)) {
                                analyzePathParam(file, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (PathParamIssue issue : issues) {
                result.addIssue(
                        "PATH_PARAM",
                        issue.severity,
                        issue.file,
                        issue.line,
                        issue.description,
                        "PATH_PARAM",
                        DOC_API_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file
                                        + ":"
                                        + issue.line);
            }
            getLog().warn("Found " + issues.size() + " path parameter violation(s)");
        } else {
            getLog().info("All path parameter checks passed");
        }
    }

    private void analyzePathParam(Path file, List<PathParamIssue> issues) {
        try {
            String content = Files.readString(file);
            int pathVariableIndex = content.indexOf(PATH_VARIABLE_ANNOTATION);
            if (pathVariableIndex >= 0) {
                issues.add(
                        new PathParamIssue(
                                "CRITICAL",
                                file.toString(),
                                lineNumber(content, pathVariableIndex),
                                "HTTP API 禁止使用 "
                                        + PATH_VARIABLE_ANNOTATION
                                        + "，请改为 @RequestParam 或 Query/Command 对象"));
            }
            Matcher matcher = REQUEST_MAPPING_PATTERN.matcher(content);
            while (matcher.find()) {
                String mapping = extractMappingValue(matcher.group(1));
                if (usesPathTemplateVariable(mapping)) {
                    issues.add(
                            new PathParamIssue(
                                    "CRITICAL",
                                    file.toString(),
                                    lineNumber(content, matcher.start()),
                                    "HTTP API 禁止使用路径模板变量 " + mapping + "，请改为 query 参数"));
                }
            }
        } catch (IOException e) {
            issues.add(
                    new PathParamIssue("MAJOR", file.toString(), 0, "路径参数检查失败: " + e.getMessage()));
        }
    }

    private boolean usesPathTemplateVariable(String mapping) {
        if (mapping == null || mapping.isBlank()) {
            return false;
        }
        String resolved = resolvePlaceholderDefault(mapping.trim());
        Matcher matcher = Pattern.compile("\\{([^}:]+)}").matcher(resolved);
        return matcher.find();
    }

    /** PERMISSION 接口必须声明明确 permission code，运行时只信任服务端同步的资源权限码。 */
    private void checkPermissionParam() {
        getLog().info("Checking permission access declarations...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<PathParamIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isMainJavaFile(file)
                                    && !isMangoToolingFile(file)
                                    && !isPermissionAccessAnnotation(file)) {
                                analyzePermissionParam(file, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (PathParamIssue issue : issues) {
                result.addIssue(
                        "PERMISSION_PARAM",
                        issue.severity,
                        issue.file,
                        issue.line,
                        issue.description,
                        "PERMISSION_PARAM",
                        DOC_API_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file
                                        + ":"
                                        + issue.line);
            }
            getLog().warn("Found " + issues.size() + " permission declaration violation(s)");
        } else {
            getLog().info("All permission access declaration checks passed");
        }
    }

    private boolean isPermissionAccessAnnotation(Path file) {
        return file.toString().replace('\\', '/').endsWith("/api/annotation/PermissionAccess.java");
    }

    private void analyzePermissionParam(Path file, List<PathParamIssue> issues) {
        try {
            String content = Files.readString(file);
            Matcher apiAccessMatcher =
                    Pattern.compile("@ApiAccess\\s*\\((.*?)\\)", Pattern.DOTALL).matcher(content);
            while (apiAccessMatcher.find()) {
                String body = apiAccessMatcher.group(1);
                if (body.contains("ApiResourceAccessMode.PERMISSION")
                        && !Pattern.compile("\\bpermission\\s*=\\s*\"[^\"]+\"")
                                .matcher(body)
                                .find()) {
                    issues.add(
                            new PathParamIssue(
                                    "CRITICAL",
                                    file.toString(),
                                    lineNumber(content, apiAccessMatcher.start()),
                                    "@ApiAccess PERMISSION 必须声明 permission，运行时只信任服务端同步的资源权限码"));
                }
            }
            Matcher permissionAccessMatcher =
                    Pattern.compile("@PermissionAccess(?:\\s*\\((.*?)\\))?", Pattern.DOTALL)
                            .matcher(content);
            while (permissionAccessMatcher.find()) {
                String body = permissionAccessMatcher.group(1);
                if (body == null || !Pattern.compile("\"[^\"]+\"").matcher(body).find()) {
                    issues.add(
                            new PathParamIssue(
                                    "CRITICAL",
                                    file.toString(),
                                    lineNumber(content, permissionAccessMatcher.start()),
                                    "@PermissionAccess 必须声明非空权限码"));
                }
            }
        } catch (IOException e) {
            issues.add(
                    new PathParamIssue("MAJOR", file.toString(), 0, "权限参数检查失败: " + e.getMessage()));
        }
    }

    private void analyzeKvKey(Path file, List<KvKeyIssue> issues) {
        try {
            String content = Files.readString(file);
            Pattern annotationPattern =
                    Pattern.compile(
                            "@(Cacheable|Locker|RateLimit|Idempotent)\\s*\\([\\s\\S]*?\\bkey\\s*=\\s*\"([^\"]*)\"");
            Matcher matcher = annotationPattern.matcher(content);
            while (matcher.find()) {
                String key = matcher.group(2).trim();
                int line = lineNumber(content, matcher.start(2));
                if (key.startsWith("mango:kv:")) {
                    issues.add(
                            new KvKeyIssue(
                                    "CRITICAL",
                                    file.toString(),
                                    line,
                                    "KV 注解 key 不得手写 mango:kv 前缀，应由 capability 自动补齐"));
                }
                if (usesInlinePlaceholder(key, "#")) {
                    issues.add(
                            new KvKeyIssue(
                                    "CRITICAL",
                                    file.toString(),
                                    line,
                                    "KV 注解 key 不支持 user:#userId 写法，应使用 user:#{#userId}"));
                }
                if (usesInlinePlaceholder(key, "@")) {
                    issues.add(
                            new KvKeyIssue(
                                    "CRITICAL",
                                    file.toString(),
                                    line,
                                    "KV 注解 key 不支持 user:@bean 写法，应使用 user:#{@bean.method()}"));
                }
            }
        } catch (IOException e) {
            issues.add(
                    new KvKeyIssue("MAJOR", file.toString(), 0, "KV key 检查失败: " + e.getMessage()));
        }
    }

    /**
     * Rules: 1. Migration CREATE TABLE statements must include audit and tenant columns. 2. Tables
     * managed by external infrastructure can opt out with an explicit mango-check comment.
     */
    private void checkPersistenceSchema() throws MojoExecutionException {
        getLog().info("Checking persistence schema declarations...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<Path> migrationFiles = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isMigrationSqlFile(file)) {
                                migrationFiles.add(file);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        Set<String> globalTables =
                Set.copyOf(
                        GlobalEntityManifestLoader.load(rootPath, globalEntityManifest).values());
        Set<String> discoveredTables = new LinkedHashSet<>();
        List<PersistenceSchemaIssue> issues =
                analyzePersistenceSchemaMigrations(
                        migrationFiles, globalTables, rootPath, discoveredTables);
        Map<String, Path> tableOwners = collectMigrationTableOwners(migrationFiles, issues);
        analyzeEntityTableMappings(rootPath, discoveredTables, tableOwners, issues);
        analyzeMapperXmlTableOwnership(rootPath, tableOwners, issues);
        if (!issues.isEmpty()) {
            for (PersistenceSchemaIssue issue : issues) {
                result.addIssue(
                        "PERSISTENCE_SCHEMA",
                        issue.severity,
                        issue.file,
                        issue.line,
                        issue.description,
                        "PERSISTENCE_SCHEMA",
                        DOC_PERSISTENCE_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file);
            }
            getLog().warn("Found " + issues.size() + " persistence schema violation(s)");
        } else {
            getLog().info("All persistence schema checks passed");
        }
    }

    private boolean isMigrationSqlFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("/src/main/resources/db/migration/")
                && normalized.endsWith(".sql");
    }

    private List<PersistenceSchemaIssue> analyzePersistenceSchemaMigrations(
            List<Path> migrationFiles,
            Set<String> globalTables,
            Path rootPath,
            Set<String> discoveredTables) {
        List<PersistenceSchemaIssue> issues = new ArrayList<>();
        Map<Path, List<Path>> filesByDirectory = new LinkedHashMap<>();
        for (Path file : migrationFiles) {
            filesByDirectory
                    .computeIfAbsent(file.getParent(), ignored -> new ArrayList<>())
                    .add(file);
        }
        for (List<Path> files : filesByDirectory.values()) {
            files.sort(this::compareMigrationFiles);
            analyzePersistenceSchemaDirectory(files, globalTables, discoveredTables, issues);
        }
        for (String globalTable : globalTables) {
            if (!discoveredTables.contains(globalTable)) {
                issues.add(
                        new PersistenceSchemaIssue(
                                "CRITICAL",
                                rootPath.toString(),
                                0,
                                "全局实体例外表未在 migration 中创建: " + globalTable));
            }
        }
        return issues;
    }

    private void analyzeEntityTableMappings(
            Path rootPath,
            Set<String> discoveredTables,
            Map<String, Path> tableOwners,
            List<PersistenceSchemaIssue> issues) {
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (!isMainJavaFile(file)) {
                                return FileVisitResult.CONTINUE;
                            }
                            try {
                                String content = Files.readString(file);
                                Matcher matcher = TABLE_NAME_ANNOTATION_PATTERN.matcher(content);
                                while (matcher.find()) {
                                    Matcher valueMatcher =
                                            TABLE_NAME_LITERAL_VALUE_PATTERN.matcher(
                                                    matcher.group(1));
                                    if (!valueMatcher.matches()) {
                                        issues.add(
                                                new PersistenceSchemaIssue(
                                                        "CRITICAL",
                                                        file.toString(),
                                                        lineNumber(content, matcher.start()),
                                                        "@TableName 必须直接使用小写 snake_case 字符串字面量"));
                                        continue;
                                    }
                                    String tableName = valueMatcher.group(1);
                                    if (!discoveredTables.contains(tableName)) {
                                        issues.add(
                                                new PersistenceSchemaIssue(
                                                        "CRITICAL",
                                                        file.toString(),
                                                        lineNumber(content, matcher.start()),
                                                        "实体 @TableName 对应表未在 migration 中创建: "
                                                                + tableName));
                                        continue;
                                    }
                                    Path tableOwner = tableOwners.get(tableName);
                                    Path entityOwner = sourceModuleRoot(file);
                                    if (tableOwner != null
                                            && entityOwner != null
                                            && !tableOwner.equals(entityOwner)) {
                                        issues.add(
                                                new PersistenceSchemaIssue(
                                                        "CRITICAL",
                                                        file.toString(),
                                                        lineNumber(content, matcher.start()),
                                                        "实体禁止绑定其它模块拥有的表 "
                                                                + tableName
                                                                + "; owner="
                                                                + tableOwner));
                                    }
                                }
                            } catch (IOException e) {
                                issues.add(
                                        new PersistenceSchemaIssue(
                                                "MAJOR",
                                                file.toString(),
                                                0,
                                                "实体与 migration 一致性检查失败: " + e.getMessage()));
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            issues.add(
                    new PersistenceSchemaIssue(
                            "MAJOR",
                            rootPath.toString(),
                            0,
                            "实体与 migration 一致性扫描失败: " + e.getMessage()));
        }
    }

    private Map<String, Path> collectMigrationTableOwners(
            List<Path> migrationFiles, List<PersistenceSchemaIssue> issues) {
        Map<String, Path> owners = new LinkedHashMap<>();
        for (Path file : migrationFiles) {
            try {
                String content = Files.readString(file);
                Matcher matcher = CREATE_TABLE_PATTERN.matcher(content);
                while (matcher.find()) {
                    if (isSqlIgnoredOffset(content, matcher.start(), true)) {
                        continue;
                    }
                    String tableName = normalizeSqlName(matcher.group(2));
                    Path owner = sourceModuleRoot(file);
                    Path existing = owners.putIfAbsent(tableName, owner);
                    if (existing != null && owner != null && !existing.equals(owner)) {
                        issues.add(
                                new PersistenceSchemaIssue(
                                        "CRITICAL",
                                        file.toString(),
                                        lineNumber(content, matcher.start()),
                                        "表 "
                                                + tableName
                                                + " 不得由多个模块重复创建; owners="
                                                + existing
                                                + ", "
                                                + owner));
                    }
                }
            } catch (IOException e) {
                issues.add(
                        new PersistenceSchemaIssue(
                                "MAJOR",
                                file.toString(),
                                0,
                                "migration 表归属检查失败: " + e.getMessage()));
            }
        }
        return owners;
    }

    private void analyzeMapperXmlTableOwnership(
            Path rootPath, Map<String, Path> tableOwners, List<PersistenceSchemaIssue> issues) {
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isMapperXmlResource(file)) {
                                analyzeMapperXmlTableOwnershipFile(file, tableOwners, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            issues.add(
                    new PersistenceSchemaIssue(
                            "MAJOR",
                            rootPath.toString(),
                            0,
                            "Mapper XML 表归属扫描失败: " + e.getMessage()));
        }
    }

    private boolean isMapperXmlResource(Path file) {
        String normalized = file.toString().replace('\\', '/');
        if (!normalized.contains(MAIN_RESOURCES_PATH) || !normalized.endsWith(XML_FILE_SUFFIX)) {
            return false;
        }
        return normalized.contains(MAPPER_DIRECTORY)
                || file.getFileName().toString().endsWith(MAPPER_XML_SUFFIX);
    }

    private void analyzeMapperXmlTableOwnershipFile(
            Path file, Map<String, Path> tableOwners, List<PersistenceSchemaIssue> issues) {
        try {
            String content = Files.readString(file).replaceAll("(?s)<!--.*?-->", "");
            if (content.contains(MYBATIS_RAW_SUBSTITUTION)) {
                issues.add(
                        new PersistenceSchemaIssue(
                                "CRITICAL", file.toString(), 0, "Mapper XML 禁止动态拼接表名"));
            }
            inspectMapperXmlTableReferences(file, content, tableOwners, issues);
        } catch (IOException e) {
            issues.add(
                    new PersistenceSchemaIssue(
                            "MAJOR", file.toString(), 0, "Mapper XML 表归属检查失败: " + e.getMessage()));
        }
    }

    private void inspectMapperXmlTableReferences(
            Path file,
            String content,
            Map<String, Path> tableOwners,
            List<PersistenceSchemaIssue> issues) {
        Path mapperOwner = sourceModuleRoot(file);
        Matcher matcher = SQL_TABLE_REFERENCE_PATTERN.matcher(content);
        while (matcher.find()) {
            String tableName = normalizeSqlName(matcher.group(1));
            Path tableOwner = tableOwners.get(tableName);
            if (isCrossModuleTableAccess(mapperOwner, tableOwner)) {
                issues.add(
                        new PersistenceSchemaIssue(
                                "CRITICAL",
                                file.toString(),
                                lineNumber(content, matcher.start()),
                                "Mapper XML 禁止跨模块访问表 " + tableName + "; owner=" + tableOwner));
            }
        }
    }

    private boolean isCrossModuleTableAccess(Path mapperOwner, Path tableOwner) {
        return tableOwner != null && mapperOwner != null && !tableOwner.equals(mapperOwner);
    }

    private Path sourceModuleRoot(Path sourceFile) {
        Path current = sourceFile.toAbsolutePath().normalize();
        while (current != null) {
            if (current.getFileName() != null && "src".equals(current.getFileName().toString())) {
                return current.getParent();
            }
            current = current.getParent();
        }
        return null;
    }

    private int compareMigrationFiles(Path left, Path right) {
        List<Integer> leftVersion = extractMigrationVersion(left);
        List<Integer> rightVersion = extractMigrationVersion(right);
        int max = Math.max(leftVersion.size(), rightVersion.size());
        for (int i = 0; i < max; i++) {
            int leftPart = i < leftVersion.size() ? leftVersion.get(i) : 0;
            int rightPart = i < rightVersion.size() ? rightVersion.get(i) : 0;
            int compared = Integer.compare(leftPart, rightPart);
            if (compared != 0) {
                return compared;
            }
        }
        return left.getFileName().toString().compareToIgnoreCase(right.getFileName().toString());
    }

    private List<Integer> extractMigrationVersion(Path file) {
        String fileName = file.getFileName().toString();
        Matcher matcher = MIGRATION_VERSION_PATTERN.matcher(fileName);
        if (!matcher.find()) {
            return List.of();
        }
        List<Integer> parts = new ArrayList<>();
        for (String part : matcher.group(1).split(MIGRATION_VERSION_SEPARATOR_REGEX)) {
            if (!part.isBlank()) {
                parts.add(Integer.parseInt(part));
            }
        }
        return parts;
    }

    private void analyzePersistenceSchemaDirectory(
            List<Path> files,
            Set<String> globalTables,
            Set<String> discoveredTables,
            List<PersistenceSchemaIssue> issues) {
        Map<String, PersistenceTableSchema> tables = new LinkedHashMap<>();
        for (Path file : files) {
            analyzePersistenceSchemaFile(file, tables, discoveredTables, issues);
        }
        for (PersistenceTableSchema table : tables.values()) {
            validatePersistenceTableSchema(table, globalTables, issues);
        }
    }

    private void analyzePersistenceSchemaFile(
            Path file,
            Map<String, PersistenceTableSchema> tables,
            Set<String> discoveredTables,
            List<PersistenceSchemaIssue> issues) {
        try {
            String content = Files.readString(file);
            applyCreateTableStatements(file, content, tables, discoveredTables);
            applyAlterTableStatements(content, tables);
        } catch (IOException e) {
            issues.add(
                    new PersistenceSchemaIssue(
                            "MAJOR", file.toString(), 0, "持久化表结构检查失败: " + e.getMessage()));
        }
    }

    private void applyCreateTableStatements(
            Path file,
            String content,
            Map<String, PersistenceTableSchema> tables,
            Set<String> discoveredTables) {
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            if (isSqlIgnoredOffset(content, matcher.start(), true)) {
                continue;
            }
            String tableName = normalizeSqlName(matcher.group(2));
            discoveredTables.add(tableName);
            int line = lineNumber(content, matcher.start());
            if (shouldSkipPersistenceTable(tableName, file)) {
                continue;
            }
            int statementEnd = findStatementEnd(content, matcher.end());
            String statement = content.substring(matcher.start(), statementEnd);
            Map<String, String> columnDefinitions = extractSqlColumnDefinitions(statement);
            tables.putIfAbsent(
                    tableName,
                    new PersistenceTableSchema(
                            tableName,
                            file.toString(),
                            line,
                            columnDefinitions,
                            hasIdPrimaryKey(statement, columnDefinitions.get("id"))));
        }
    }

    private void applyAlterTableStatements(
            String content, Map<String, PersistenceTableSchema> tables) {
        Matcher matcher = ALTER_TABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            if (isSqlIgnoredOffset(
                    content, matcher.start(), isBusinessProject(resolveBasePath()))) {
                continue;
            }
            String tableName = normalizeSqlName(matcher.group(2));
            PersistenceTableSchema table = tables.get(tableName);
            if (table == null) {
                continue;
            }
            int statementEnd = findStatementEnd(content, matcher.end());
            applyAlterTableStatement(content.substring(matcher.start(), statementEnd), table);
        }
    }

    private boolean isSqlIgnoredOffset(String content, int offset, boolean ignoreStringLiterals) {
        SqlLexicalState state = new SqlLexicalState();
        int index = 0;
        while (index < offset) {
            index = state.advance(content, index, offset);
        }
        return state.isIgnored(ignoreStringLiterals);
    }

    private void applyAlterTableStatement(String statement, PersistenceTableSchema table) {
        for (String clause : splitAlterTableClauses(statement)) {
            applyAlterTableClause(clause, table);
        }
    }

    private List<String> splitAlterTableClauses(String statement) {
        Matcher matcher = ALTER_TABLE_PATTERN.matcher(statement);
        if (!matcher.find()) {
            return List.of(statement);
        }
        String clauses = statement.substring(matcher.end()).strip();
        if (clauses.endsWith(SQL_STATEMENT_TERMINATOR)) {
            clauses = clauses.substring(0, clauses.length() - 1).strip();
        }
        List<String> result = new ArrayList<>();
        int start = 0;
        SqlClauseState state = new SqlClauseState();
        for (int i = 0; i < clauses.length(); i++) {
            state.accept(clauses, i);
            if (state.shouldSkipNext()) {
                i++;
            }
            if (state.isDelimiter()) {
                addAlterTableClause(result, clauses.substring(start, i));
                start = i + 1;
            }
        }
        addAlterTableClause(result, clauses.substring(start));
        return result;
    }

    private void addAlterTableClause(List<String> clauses, String clause) {
        String trimmed = clause.strip();
        if (!trimmed.isEmpty()) {
            clauses.add(trimmed);
        }
    }

    private void applyAlterTableClause(String clause, PersistenceTableSchema table) {
        applyPrimaryKeyAlter(clause, table);
        if (applyDropColumn(clause, table)) {
            return;
        }
        if (applyRenameColumn(clause, table)) {
            return;
        }
        if (applyAddColumn(clause, table)) {
            return;
        }
        applyModifyColumn(clause, table);
    }

    private void applyPrimaryKeyAlter(String clause, PersistenceTableSchema table) {
        if (DROP_PRIMARY_KEY_CLAUSE_PATTERN.matcher(clause).find()) {
            table.idPrimaryKey = false;
        }
        if (ADD_PRIMARY_KEY_CLAUSE_PATTERN.matcher(clause).find()) {
            table.idPrimaryKey = true;
        }
    }

    private boolean applyDropColumn(String clause, PersistenceTableSchema table) {
        Matcher dropColumnMatcher = DROP_COLUMN_CLAUSE_PATTERN.matcher(clause);
        if (!dropColumnMatcher.find()) {
            return false;
        }
        String columnName = normalizeSqlName(dropColumnMatcher.group(1));
        table.columnDefinitions.remove(columnName);
        return true;
    }

    private boolean applyRenameColumn(String clause, PersistenceTableSchema table) {
        Matcher renameMatcher = RENAME_COLUMN_CLAUSE_PATTERN.matcher(clause);
        if (!renameMatcher.find()) {
            return false;
        }
        String oldName = normalizeSqlName(renameMatcher.group(1));
        String newName = normalizeSqlName(renameMatcher.group(2));
        String definition = table.columnDefinitions.remove(oldName);
        if (definition != null) {
            table.columnDefinitions.put(newName, definition);
        }
        return true;
    }

    private boolean applyAddColumn(String clause, PersistenceTableSchema table) {
        Matcher addMatcher = ADD_COLUMN_CLAUSE_PATTERN.matcher(clause);
        if (!addMatcher.find()) {
            return false;
        }
        String columnName = normalizeSqlName(addMatcher.group(1));
        boolean added = !table.columnDefinitions.containsKey(columnName);
        table.columnDefinitions.putIfAbsent(columnName, addMatcher.group(0));
        if (added && ID_COLUMN.equals(columnName)) {
            table.idPrimaryKey = hasIdPrimaryKey(clause, addMatcher.group(0));
        }
        return true;
    }

    private void applyModifyColumn(String clause, PersistenceTableSchema table) {
        Matcher modifyMatcher = MODIFY_COLUMN_CLAUSE_PATTERN.matcher(clause);
        if (modifyMatcher.find()) {
            String columnName = normalizeSqlName(modifyMatcher.group(1));
            if (table.columnDefinitions.containsKey(columnName)) {
                table.columnDefinitions.put(columnName, modifyMatcher.group(0));
            }
            if (ID_COLUMN.equals(columnName) && hasIdPrimaryKey(clause, modifyMatcher.group(0))) {
                table.idPrimaryKey = true;
            }
        }
    }

    private void validatePersistenceTableSchema(
            PersistenceTableSchema table,
            Set<String> globalTables,
            List<PersistenceSchemaIssue> issues) {
        validateStandardIdColumn(
                table.tableName,
                table.columnDefinitions,
                table.idPrimaryKey,
                table.file,
                table.line,
                issues);
        Set<String> columns = table.columnDefinitions.keySet();
        for (String requiredColumn : REQUIRED_PERSISTENCE_COLUMNS) {
            if (globalTables.contains(table.tableName)
                    && GLOBAL_ENTITY_OPTIONAL_COLUMNS.contains(requiredColumn)) {
                continue;
            }
            if (!columns.contains(requiredColumn)) {
                issues.add(
                        new PersistenceSchemaIssue(
                                "CRITICAL",
                                table.file,
                                table.line,
                                "表 " + table.tableName + " 缺少标准持久化字段 " + requiredColumn));
            }
        }
    }

    /**
     * Rules: 1. Business main code must not access relational persistence through direct JDBC. 2.
     * Tests are excluded; production code must use Mango persistence/MyBatis mapper boundaries.
     */
    private void checkPersistenceAccess() {
        getLog().info("Checking persistence access style...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<PersistenceStyleIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isMainJavaFile(file) && !isMangoToolingFile(file)) {
                                analyzePersistenceAccess(file, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (PersistenceStyleIssue issue : issues) {
                result.addIssue(
                        "PERSISTENCE_ACCESS",
                        issue.severity,
                        issue.file,
                        issue.line,
                        issue.description,
                        "PERSISTENCE_ACCESS",
                        DOC_PERSISTENCE_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file
                                        + ":"
                                        + issue.line);
            }
            getLog().warn("Found " + issues.size() + " persistence access violation(s)");
        } else {
            getLog().info("All persistence access checks passed");
        }
    }

    private void analyzePersistenceAccess(Path file, List<PersistenceStyleIssue> issues) {
        try {
            String content = Files.readString(file);
            String code = stripStringLiterals(content);
            int jdbcTemplateIndex = code.indexOf("org.springframework.jdbc.core.JdbcTemplate");
            if (jdbcTemplateIndex < 0) {
                jdbcTemplateIndex = findWord(code, "JdbcTemplate");
            }
            if (jdbcTemplateIndex >= 0) {
                issues.add(
                        new PersistenceStyleIssue(
                                "CRITICAL",
                                file.toString(),
                                lineNumber(content, jdbcTemplateIndex),
                                "业务代码禁止直接使用 JdbcTemplate，请通过 mango-infra-persistence、Mapper XML 或"
                                        + " Mango CRUD 基线访问数据库"));
                return;
            }
            for (String type : DIRECT_JDBC_TYPES) {
                int index = findDirectJdbcType(code, type);
                if (index >= 0) {
                    issues.add(
                            new PersistenceStyleIssue(
                                    "CRITICAL",
                                    file.toString(),
                                    lineNumber(content, index),
                                    "业务代码禁止直接使用 java.sql."
                                            + type
                                            + "，请通过 mango-infra-persistence、Mapper XML 或 Mango CRUD"
                                            + " 基线访问数据库"));
                    return;
                }
            }
        } catch (IOException e) {
            issues.add(
                    new PersistenceStyleIssue(
                            "MAJOR", file.toString(), 0, "持久化访问检查失败: " + e.getMessage()));
        }
    }

    /**
     * Rules: 1. Business CRUD services use MangoCrudServiceImpl instead of direct MyBatis-Plus
     * ServiceImpl. 2. Business code does not hand-roll ordinary pagination or tenant
     * filling/filtering.
     */
    private void checkPersistenceCrudBaseline() {
        getLog().info("Checking persistence CRUD baseline...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<PersistenceStyleIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isMainJavaFile(file)
                                    && !isMangoToolingFile(file)
                                    && !isMangoPersistenceFrameworkFile(file)) {
                                analyzePersistenceCrudBaseline(file, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (PersistenceStyleIssue issue : issues) {
                result.addIssue(
                        "PERSISTENCE_CRUD_BASELINE",
                        issue.severity,
                        issue.file,
                        issue.line,
                        issue.description,
                        "PERSISTENCE_CRUD_BASELINE",
                        DOC_PERSISTENCE_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file
                                        + ":"
                                        + issue.line);
            }
            getLog().warn("Found " + issues.size() + " persistence CRUD baseline violation(s)");
        } else {
            getLog().info("All persistence CRUD baseline checks passed");
        }
    }

    private void analyzePersistenceCrudBaseline(Path file, List<PersistenceStyleIssue> issues) {
        try {
            String content = Files.readString(file);
            String code = stripStringLiterals(content);
            addPatternIssue(
                    file,
                    content,
                    code,
                    DIRECT_MYBATIS_SERVICE_IMPL_PATTERN,
                    issues,
                    "普通 CRUD Service 禁止直接继承 MyBatis-Plus ServiceImpl，请继承 MangoCrudServiceImpl");
            addPatternIssue(
                    file,
                    content,
                    code,
                    MYBATIS_SELECT_PAGE_PATTERN,
                    issues,
                    "普通分页查询禁止手写 mapper.selectPage，请复用 MangoCrudServiceImpl.pageByQuery 或标准查询入口");
            addPatternIssue(
                    file,
                    content,
                    code,
                    MYBATIS_NEW_PAGE_PATTERN,
                    issues,
                    "普通分页查询禁止手写 MyBatis-Plus Page，请复用 MangoCrudServiceImpl.pageByQuery 或标准查询入口");
            addPatternIssue(
                    file,
                    content,
                    content,
                    TENANT_CONDITION_PATTERN,
                    issues,
                    "普通租户表查询禁止手写 tenant_id 条件，请依赖租户拦截器或显式建模例外场景");
            addPatternIssue(
                    file,
                    content,
                    content,
                    DATA_SCOPE_CONDITION_PATTERN,
                    issues,
                    "普通数据权限查询禁止手写 created_by/org_id 条件，请通过 DataScopeApplier 统一追加数据范围");
            addPatternIssue(
                    file,
                    content,
                    code,
                    SET_TENANT_ID_PATTERN,
                    issues,
                    "插入普通租户实体禁止手工 setTenantId，请依赖 PersistenceAuditMetaObjectHandler 自动填充");
        } catch (IOException e) {
            issues.add(
                    new PersistenceStyleIssue(
                            "MAJOR", file.toString(), 0, "持久化 CRUD 基线检查失败: " + e.getMessage()));
        }
    }

    private void addPatternIssue(
            Path file,
            String content,
            String code,
            Pattern pattern,
            List<PersistenceStyleIssue> issues,
            String description) {
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            issues.add(
                    new PersistenceStyleIssue(
                            "CRITICAL",
                            file.toString(),
                            lineNumber(content, matcher.start()),
                            description));
        }
    }

    private int findDirectJdbcType(String code, String type) {
        int qualifiedIndex = code.indexOf("java.sql." + type);
        if (qualifiedIndex >= 0) {
            return qualifiedIndex;
        }
        boolean imported =
                Pattern.compile("(?m)^\\s*import\\s+java\\.sql\\." + type + "\\s*;")
                                .matcher(code)
                                .find()
                        || Pattern.compile("(?m)^\\s*import\\s+java\\.sql\\.\\*\\s*;")
                                .matcher(code)
                                .find();
        return imported ? findWord(code, type) : -1;
    }

    private int findWord(String content, String word) {
        Matcher matcher =
                Pattern.compile("(?<![A-Za-z0-9_$])" + Pattern.quote(word) + "(?![A-Za-z0-9_$])")
                        .matcher(content);
        return matcher.find() ? matcher.start() : -1;
    }

    /**
     * Rules: 1. Business mapper SQL must live in mapper.xml. 2. Mapper method annotation SQL is
     * forbidden in main code.
     */
    private void checkMapperSqlStyle() {
        getLog().info("Checking mapper SQL style...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<PersistenceStyleIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isMainJavaFile(file) && isMapperJavaFile(file)) {
                                analyzeMapperSqlStyle(file, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (PersistenceStyleIssue issue : issues) {
                result.addIssue(
                        "MAPPER_SQL_STYLE",
                        issue.severity,
                        issue.file,
                        issue.line,
                        issue.description,
                        "MAPPER_SQL_STYLE",
                        DOC_PERSISTENCE_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file
                                        + ":"
                                        + issue.line);
            }
            getLog().warn("Found " + issues.size() + " mapper SQL style violation(s)");
        } else {
            getLog().info("All mapper SQL style checks passed");
        }
    }

    private boolean isMapperJavaFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.endsWith("Mapper.java");
    }

    private void analyzeMapperSqlStyle(Path file, List<PersistenceStyleIssue> issues) {
        try {
            String content = Files.readString(file);
            String code = stripStringLiterals(content);
            Matcher matcher = MAPPER_SQL_ANNOTATION_PATTERN.matcher(code);
            while (matcher.find()) {
                issues.add(
                        new PersistenceStyleIssue(
                                "CRITICAL",
                                file.toString(),
                                lineNumber(content, matcher.start()),
                                "业务 Mapper 禁止使用 @" + matcher.group(1) + " 注解 SQL，请迁移到 mapper.xml"));
            }
            Matcher methodMatcher = JAVA_METHOD_PATTERN.matcher(code);
            while (methodMatcher.find()) {
                for (String parameter : splitTopLevelParameters(methodMatcher.group(2))) {
                    String type = parameterType(parameter);
                    if (isApiProtocolModel(type)) {
                        issues.add(
                                new PersistenceStyleIssue(
                                        "CRITICAL",
                                        file.toString(),
                                        lineNumber(content, methodMatcher.start(1)),
                                        "Mapper 入参禁止使用 API 协议模型 "
                                                + type
                                                + "，Service 应先转换为 Entity、id、Wrapper、分页对象或 core"
                                                + " 内部持久化查询对象"));
                    }
                }
            }
        } catch (IOException e) {
            issues.add(
                    new PersistenceStyleIssue(
                            "MAJOR", file.toString(), 0, "Mapper SQL 风格检查失败: " + e.getMessage()));
        }
    }

    private boolean isApiProtocolModel(String type) {
        return type.endsWith("Command")
                || type.endsWith("Query")
                || type.endsWith("VO")
                || type.endsWith("Request");
    }

    /**
     * Rules: 1. I*Service and *ServiceImpl public business methods must not expand more than two
     * business parameters. 2. Complex create/update/query/batch actions must use Command, Query or
     * request objects.
     */
    private void checkServiceContract() {
        getLog().info("Checking service method contracts...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<ServiceContractIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isMainJavaFile(file)
                                    && isServiceContractFile(file)
                                    && !isMangoToolingFile(file)) {
                                analyzeServiceContract(file, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (ServiceContractIssue issue : issues) {
                result.addIssue(
                        "SERVICE_CONTRACT",
                        issue.severity,
                        issue.file,
                        issue.line,
                        issue.description,
                        "SERVICE_CONTRACT",
                        DOC_API_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file
                                        + ":"
                                        + issue.line);
            }
            getLog().warn("Found " + issues.size() + " service contract violation(s)");
        } else {
            getLog().info("All service contract checks passed");
        }
    }

    private boolean isServiceContractFile(Path file) {
        String fileName = file.getFileName().toString();
        String normalized = file.toString().replace('\\', '/');
        return (fileName.matches("I[A-Za-z0-9_]+Service\\.java")
                        || fileName.endsWith("ServiceImpl.java"))
                && !normalized.contains("/src/main/java/io/mango/plugin/");
    }

    private boolean isServiceImplFile(Path file) {
        return file.getFileName().toString().endsWith("ServiceImpl.java");
    }

    private void analyzeServiceContract(Path file, List<ServiceContractIssue> issues) {
        try {
            String content = Files.readString(file);
            String code = stripStringLiterals(content);
            Pattern pattern = serviceContractPattern(file);
            Matcher matcher = pattern.matcher(code);
            while (matcher.find()) {
                String methodName = matcher.group(1);
                if (isIgnorableServiceMethod(methodName)) {
                    continue;
                }
                List<String> parameters = splitTopLevelParameters(matcher.group(2));
                int businessParameterCount = 0;
                for (String parameter : parameters) {
                    if (!parameter.isBlank() && !isInfrastructureParameter(parameter)) {
                        businessParameterCount++;
                    }
                }
                if (businessParameterCount > 2 && !hasContractObjectParameter(parameters)) {
                    issues.add(
                            new ServiceContractIssue(
                                    "CRITICAL",
                                    file.toString(),
                                    lineNumber(content, matcher.start(1)),
                                    "Service 方法超过 2 个业务入参时必须收敛为 Command/Query/Request 对象: "
                                            + methodName
                                            + " 入参数="
                                            + businessParameterCount));
                }
            }
        } catch (IOException e) {
            issues.add(
                    new ServiceContractIssue(
                            "MAJOR", file.toString(), 0, "Service 入参契约检查失败: " + e.getMessage()));
        }
    }

    private Pattern serviceContractPattern(Path file) {
        if (isServiceImplFile(file)) {
            return SERVICE_IMPL_PUBLIC_METHOD_PATTERN;
        }
        return SERVICE_INTERFACE_METHOD_PATTERN;
    }

    private boolean isIgnorableServiceMethod(String methodName) {
        return Set.of("equals", "hashCode", "toString").contains(methodName);
    }

    private boolean hasContractObjectParameter(List<String> parameters) {
        for (String parameter : parameters) {
            String type = parameterType(parameter);
            if (type.endsWith("Command") || type.endsWith("Query") || type.endsWith("Request")) {
                return true;
            }
        }
        return false;
    }

    private boolean isInfrastructureParameter(String parameter) {
        String type = parameterType(parameter);
        return Set.of(
                        "HttpServletRequest",
                        "HttpServletResponse",
                        "ServletRequest",
                        "ServletResponse")
                .contains(type);
    }

    private int findStatementEnd(String content, int offset) {
        int index = content.indexOf(';', offset);
        if (index >= 0) {
            return index + 1;
        }
        return content.length();
    }

    private boolean shouldSkipPersistenceTable(String tableName, Path migrationFile) {
        if (isBusinessProject(resolveBasePath())) {
            return false;
        }
        String requiredModulePath = FRAMEWORK_PERSISTENCE_EXCLUDED_TABLES.get(tableName);
        if (requiredModulePath == null) {
            return false;
        }
        String normalized =
                migrationFile.toAbsolutePath().normalize().toString().replace('\\', '/');
        return normalized.contains(requiredModulePath);
    }

    private void validateStandardIdColumn(
            String tableName,
            Map<String, String> columnDefinitions,
            boolean idPrimaryKey,
            String file,
            int line,
            List<PersistenceSchemaIssue> issues) {
        String idDefinition = columnDefinitions.get("id");
        if (idDefinition == null) {
            issues.add(
                    new PersistenceSchemaIssue(
                            "CRITICAL", file, line, "表 " + tableName + " 必须使用标准主键字段 id"));
            return;
        }
        String normalizedDefinition = idDefinition.toLowerCase(Locale.ROOT);
        if (!normalizedDefinition.matches("(?s).*\\bbigint(?:\\s*\\(\\s*20\\s*\\))?\\b.*")) {
            issues.add(
                    new PersistenceSchemaIssue(
                            "CRITICAL", file, line, "表 " + tableName + " 标准主键 id 必须为 BIGINT"));
        }
        if (normalizedDefinition.contains("auto_increment")) {
            issues.add(
                    new PersistenceSchemaIssue(
                            "CRITICAL",
                            file,
                            line,
                            "表 " + tableName + " 标准主键 id 必须使用雪花算法，不允许 AUTO_INCREMENT"));
        }
        if (!idPrimaryKey) {
            issues.add(
                    new PersistenceSchemaIssue(
                            "CRITICAL", file, line, "表 " + tableName + " 必须以 id 作为主键"));
        }
    }

    private boolean hasIdPrimaryKey(String statement, String idDefinition) {
        if (idDefinition == null) {
            return false;
        }
        if (idDefinition.toLowerCase(Locale.ROOT).contains("primary key")) {
            return true;
        }
        return ID_PRIMARY_KEY_PATTERN.matcher(statement).find();
    }

    private Set<String> extractSqlColumns(String statement) {
        Set<String> columns = new HashSet<>();
        Matcher matcher = SQL_COLUMN_PATTERN.matcher(statement);
        while (matcher.find()) {
            String column = normalizeSqlName(matcher.group(1));
            if (!isSqlConstraintKeyword(column)) {
                columns.add(column);
            }
        }
        return columns;
    }

    private Map<String, String> extractSqlColumnDefinitions(String statement) {
        Map<String, String> columns = new LinkedHashMap<>();
        Matcher matcher = SQL_COLUMN_DEFINITION_PATTERN.matcher(statement);
        while (matcher.find()) {
            String column = normalizeSqlName(matcher.group(1));
            if (!isSqlConstraintKeyword(column)) {
                columns.put(column, matcher.group(0));
            }
        }
        return columns;
    }

    private boolean isSqlConstraintKeyword(String value) {
        return Set.of(
                        "create",
                        "primary",
                        "key",
                        "unique",
                        "constraint",
                        "index",
                        "foreign",
                        "fulltext",
                        "spatial",
                        "check")
                .contains(value);
    }

    private String normalizeSqlName(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("`", "").trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Rules: 1. Redis*Test must not use MemoryKvStore or JdbcKvStore as its core store fixture. 2.
     * Jdbc*Test must not use MemoryKvStore or RedisKvStore as its core store fixture. 3.
     * Memory*Test must not use RedisKvStore or JdbcKvStore as its core store fixture.
     */
    private void checkTestFixture() {
        getLog().info("Checking test fixture consistency...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<TestFixtureIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isTestJavaFile(file)) {
                                analyzeTestFixture(file, issues);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            getLog().error("Error walking file tree", e);
        }

        if (!issues.isEmpty()) {
            for (TestFixtureIssue issue : issues) {
                result.addIssue(
                        "TEST_FIXTURE",
                        issue.severity,
                        issue.file,
                        issue.line,
                        issue.description,
                        "TEST_FIXTURE",
                        DOC_TEST_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file);
            }
            getLog().warn("Found " + issues.size() + " test fixture violation(s)");
        } else {
            getLog().info("All test fixture checks passed");
        }
    }

    private boolean isTestJavaFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("/src/test/java/") && normalized.endsWith("Test.java");
    }

    private void analyzeTestFixture(Path file, List<TestFixtureIssue> issues) {
        String fileName = file.getFileName().toString();
        try {
            String content = Files.readString(file);
            String expected = implementationPrefix(fileName);
            if (expected == null) {
                return;
            }

            for (String candidate : List.of("MemoryKvStore", "RedisKvStore", "JdbcKvStore")) {
                if (!candidate.startsWith(expected) && content.contains(candidate)) {
                    issues.add(
                            new TestFixtureIssue(
                                    "CRITICAL",
                                    file.toString(),
                                    lineNumber(content, content.indexOf(candidate)),
                                    fileName
                                            + " 表示测试 "
                                            + expected
                                            + " 实现，但测试物料出现 "
                                            + candidate
                                            + "；实现类型测试必须使用同名实现，通用能力测试应按能力命名并参数化注入"));
                }
            }
        } catch (IOException e) {
            issues.add(
                    new TestFixtureIssue(
                            "MAJOR", file.toString(), 0, "测试物料一致性检查失败: " + e.getMessage()));
        }
    }

    private String implementationPrefix(String fileName) {
        if (fileName.startsWith("Redis")) {
            return "Redis";
        }
        if (fileName.startsWith("Jdbc")) {
            return "Jdbc";
        }
        if (fileName.startsWith("Memory")) {
            return "Memory";
        }
        return null;
    }

    private void checkResourceRegistry() {
        getLog().info("Checking resource registry declarations...");
        Path rootPath = resolveBasePath();
        if (rootPath == null) {
            return;
        }

        List<ResourceRegistryIssue> issues = new ArrayList<>();
        List<ResourceDeclarationRecord> declarations = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isMainResourceDeclarationFile(file)) {
                                declarations.addAll(loadResourceDeclarations(file, issues));
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            issues.add(
                    new ResourceRegistryIssue(
                            "MAJOR", rootPath.toString(), 0, "资源声明扫描失败: " + e.getMessage()));
        }

        validateResourceDeclarationUniqueness(declarations, issues);

        if (!issues.isEmpty()) {
            for (ResourceRegistryIssue issue : issues) {
                result.addIssue(
                        "RESOURCE_REGISTRY",
                        issue.severity,
                        issue.file,
                        issue.line,
                        issue.description,
                        "RESOURCE_REGISTRY",
                        DOC_MODULE_RULES,
                        SOURCE_MANGO_CHECK);
                getLog().warn(
                                "  ["
                                        + issue.severity
                                        + "] "
                                        + issue.description
                                        + " at "
                                        + issue.file);
            }
            getLog().warn("Found " + issues.size() + " resource registry declaration violation(s)");
        } else {
            getLog().info("All resource registry declaration checks passed");
        }
    }

    private void checkModuleMenu() {
        getLog().info("Checking module menu declarations...");
        Path rootPath = resolveModuleMenuScanPath();
        if (rootPath == null) {
            return;
        }
        Set<String> scopeFiles = resolveModuleMenuChangedFiles(rootPath);
        if (!validateModuleMenuScope(rootPath, scopeFiles)) {
            return;
        }
        List<ModuleMenuIssue> issues = scanModuleMenuFiles(rootPath);
        getLog().info("Module menu scope: " + moduleMenuScopeSummary(rootPath, scopeFiles));
        if (issues.isEmpty()) {
            getLog().info("All module menu declaration checks passed");
            return;
        }
        ModuleMenuCounts counts = recordModuleMenuIssues(issues, rootPath, scopeFiles);
        reportModuleMenuCounts(counts);
    }

    private boolean validateModuleMenuScope(Path rootPath, Set<String> scopeFiles) {
        if (!changedOnly || !scopeFiles.isEmpty()) {
            return true;
        }
        result.addIssue(
                "MODULE_MENU",
                "MAJOR",
                rootPath.toString(),
                0,
                "module-menu changedOnly scope is empty; set -Dmango.check.changedFiles=<paths>"
                        + " or -Dmango.check.baseRef=<ref>",
                "MODULE_MENU",
                DOC_MODULE_MENU_RULES,
                SOURCE_MANGO_CHECK);
        return false;
    }

    private List<ModuleMenuIssue> scanModuleMenuFiles(Path rootPath) {
        List<ModuleMenuIssue> issues = new ArrayList<>();
        try {
            Files.walkFileTree(
                    rootPath,
                    new SkippingFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            analyzeModuleMenuFile(file, issues);
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            issues.add(
                    new ModuleMenuIssue(
                            "MAJOR", rootPath.toString(), 0, "菜单声明扫描失败: " + e.getMessage()));
        }
        return issues;
    }

    private ModuleMenuCounts recordModuleMenuIssues(
            List<ModuleMenuIssue> issues, Path rootPath, Set<String> scopeFiles) {
        int blockingIssueCount = 0;
        int excludedIssueCount = 0;
        for (ModuleMenuIssue issue : issues) {
            if (isModuleMenuIssueInScope(issue, rootPath, scopeFiles)) {
                blockingIssueCount++;
                recordBlockingModuleMenuIssue(issue);
            } else {
                excludedIssueCount++;
                recordExcludedModuleMenuIssue(issue);
            }
        }
        return new ModuleMenuCounts(blockingIssueCount, excludedIssueCount);
    }

    private void recordBlockingModuleMenuIssue(ModuleMenuIssue issue) {
        result.addIssue(
                "MODULE_MENU",
                issue.severity,
                issue.file,
                issue.line,
                issue.description,
                "MODULE_MENU",
                DOC_MODULE_MENU_RULES,
                SOURCE_MANGO_CHECK);
        getLog().warn("  [" + issue.severity + "] " + issue.description + " at " + issue.file);
    }

    private void recordExcludedModuleMenuIssue(ModuleMenuIssue issue) {
        result.addExcludedIssue(
                "MODULE_MENU",
                issue.severity,
                issue.file,
                issue.line,
                "out-of-scope existing issue: " + issue.description,
                "MODULE_MENU",
                DOC_MODULE_MENU_RULES,
                SOURCE_MANGO_CHECK);
        getLog().info(
                        "  [OUT-OF-SCOPE]["
                                + issue.severity
                                + "] "
                                + issue.description
                                + " at "
                                + issue.file);
    }

    private void reportModuleMenuCounts(ModuleMenuCounts counts) {
        if (counts.blocking() > 0) {
            getLog().warn(
                            "Found "
                                    + counts.blocking()
                                    + " in-scope module menu declaration violation(s)");
        }
        if (counts.excluded() > 0) {
            getLog().info("Excluded " + counts.excluded() + " out-of-scope module menu issue(s)");
        }
    }

    private Set<String> resolveModuleMenuChangedFiles(Path rootPath) {
        if (!changedOnly) {
            return Set.of();
        }
        if (changedFiles != null && !changedFiles.isBlank()) {
            return changedFilesFromParameter(changedFiles);
        }
        if (baseRef == null || baseRef.isBlank()) {
            return Set.of();
        }
        return changedFilesFromGit(rootPath, baseRef);
    }

    private Set<String> changedFilesFromParameter(String files) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String file : files.split(COMMA_DELIMITER)) {
            addNormalizedScopeFile(result, file);
        }
        return result;
    }

    private Set<String> changedFilesFromGit(Path rootPath, String ref) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        ProcessBuilder processBuilder =
                new ProcessBuilder("git", "diff", "--name-only", ref.trim() + "...HEAD");
        processBuilder.directory(rootPath.toFile());
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                getLog().warn(
                                "Failed to resolve module-menu changed files from "
                                        + ref
                                        + ": "
                                        + output.trim());
                return Set.of();
            }
            for (String line : output.split(LINE_SEPARATOR_REGEX)) {
                addNormalizedScopeFile(result, line);
            }
            return result;
        } catch (IOException e) {
            getLog().warn("Failed to resolve module-menu changed files: " + e.getMessage());
            return Set.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            getLog().warn("Interrupted while resolving module-menu changed files");
            return Set.of();
        }
    }

    private void addNormalizedScopeFile(Set<String> files, String file) {
        String normalized = normalizeScopePath(file);
        if (!normalized.isBlank()) {
            files.add(normalized);
        }
    }

    private String moduleMenuScopeSummary(Path rootPath, Set<String> scopeFiles) {
        if (!changedOnly) {
            return "baseDir=" + rootPath;
        }
        return "changedOnly=true, changedFiles=" + scopeFiles.size() + ", baseDir=" + rootPath;
    }

    private boolean isModuleMenuIssueInScope(
            ModuleMenuIssue issue, Path rootPath, Set<String> scopeFiles) {
        if (!changedOnly) {
            return true;
        }
        String issueFile = normalizeIssuePath(issue.file, rootPath);
        for (String scopeFile : scopeFiles) {
            if (sameScopePath(issueFile, scopeFile)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeIssuePath(String file, Path rootPath) {
        if (file == null || file.isBlank()) {
            return "";
        }
        Path issuePath = Paths.get(file);
        if (issuePath.isAbsolute()) {
            Path normalizedRoot = rootPath.toAbsolutePath().normalize();
            Path normalizedIssue = issuePath.toAbsolutePath().normalize();
            if (normalizedIssue.startsWith(normalizedRoot)) {
                return normalizeScopePath(normalizedRoot.relativize(normalizedIssue).toString());
            }
        }
        return normalizeScopePath(file);
    }

    private boolean sameScopePath(String issueFile, String changedFile) {
        if (issueFile.equals(changedFile)) {
            return true;
        }
        if (issueFile.endsWith(ROOT_PATH + changedFile)) {
            return true;
        }
        return changedFile.endsWith(ROOT_PATH + issueFile);
    }

    private String normalizeScopePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith(CURRENT_DIRECTORY_PREFIX)) {
            normalized = normalized.substring(CURRENT_DIRECTORY_PREFIX.length());
        }
        return normalized;
    }

    private Path resolveModuleMenuScanPath() {
        if (hasExplicitBaseDir()) {
            return resolveBasePath();
        }
        if (project != null && project.getBasedir() != null) {
            Path projectPath = project.getBasedir().toPath();
            if (Files.exists(projectPath)) {
                return projectPath;
            }
        }
        return resolveBasePath();
    }

    private boolean hasExplicitBaseDir() {
        String explicitBaseDir = System.getProperty("baseDir");
        return explicitBaseDir != null && !explicitBaseDir.isBlank();
    }

    private void analyzeModuleMenuFile(Path file, List<ModuleMenuIssue> issues) {
        String normalized = file.toString().replace('\\', '/');
        if (normalized.contains("/target/") || !normalized.contains("/src/main/")) {
            return;
        }
        try {
            if (isLegacyResourceManifestFile(normalized)) {
                analyzeLegacyMenuManifest(file, issues);
                return;
            }
            if (normalized.contains("/db/migration/") && normalized.endsWith(".sql")) {
                analyzeMenuMigrationSql(file, issues);
                return;
            }
            if (isModuleMenuResourceFile(file)) {
                analyzeModuleMenuResource(file, issues);
            }
        } catch (IOException e) {
            issues.add(
                    new ModuleMenuIssue(
                            "MAJOR", file.toString(), 0, "菜单规则检查失败: " + e.getMessage()));
        }
    }

    private boolean isLegacyResourceManifestFile(String normalized) {
        return normalized.endsWith("/META-INF/mango/resource-manifest.json")
                || normalized.contains("/META-INF/mango/resource-manifests/");
    }

    private void analyzeLegacyMenuManifest(Path file, List<ModuleMenuIssue> issues)
            throws IOException {
        String content = Files.readString(file);
        if (content.contains("\"menus\"") || content.contains("\"menuCode\"")) {
            issues.add(
                    new ModuleMenuIssue(
                            "CRITICAL",
                            file.toString(),
                            lineNumber(
                                    content,
                                    Math.max(
                                            content.indexOf("\"menus\""),
                                            content.indexOf("\"menuCode\""))),
                            "新增菜单禁止使用旧 resource-manifest；请改用"
                                + " META-INF/mango/resources/{module}-common-menu.{json,yml,yaml} 的"
                                + " AUTH_MENU 资源"));
        }
    }

    private void analyzeMenuMigrationSql(Path file, List<ModuleMenuIssue> issues)
            throws IOException {
        String content = Files.readString(file);
        String lower = content.toLowerCase(Locale.ROOT);
        if (!lower.contains("authorization_menu")
                && !lower.contains("frontend_menu_runtime_config")
                && !lower.contains("authorization_menu_package_item")
                && !lower.contains("authorization_role_menu")) {
            return;
        }
        String menuTables =
                "authorization_menu|frontend_menu_runtime_config"
                        + "|authorization_menu_package_item|authorization_role_menu";
        Matcher matcher =
                Pattern.compile(
                                "(?i)\\b(?:insert\\s+into|update)\\s+`?("
                                        + menuTables
                                        + ")`?\\b"
                                        + "|\\bdelete\\s+(?:from\\s+`?("
                                        + menuTables
                                        + ")`?\\b"
                                        + "|(?:`?\\w+`?\\s*,\\s*)*`?\\w+`?\\s+from\\s+`?("
                                        + menuTables
                                        + ")`?\\b)")
                        .matcher(content);
        while (matcher.find()) {
            issues.add(
                    new ModuleMenuIssue(
                            "CRITICAL",
                            file.toString(),
                            lineNumber(content, matcher.start()),
                            "禁止用 Flyway SQL 维护菜单、按钮权限、运行时配置、套餐授权或默认角色授权数据；请使用 AUTH_MENU 资源注入"));
        }
    }

    private boolean isModuleMenuResourceFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        String fileName = file.getFileName().toString();
        return normalized.contains("/META-INF/mango/resources/")
                && (fileName.endsWith("-common-menu.json")
                        || fileName.endsWith("-common-menu.yml")
                        || fileName.endsWith("-common-menu.yaml"));
    }

    private void analyzeModuleMenuResource(Path file, List<ModuleMenuIssue> issues)
            throws IOException {
        String content = Files.readString(file);
        if (!content.contains("AUTH_MENU")) {
            issues.add(
                    new ModuleMenuIssue(
                            "CRITICAL", file.toString(), 0, "菜单资源文件必须声明 AUTH_MENU 资源类型"));
        }
        if (!content.contains("menus")) {
            issues.add(
                    new ModuleMenuIssue("CRITICAL", file.toString(), 0, "菜单资源文件必须提供 menus 菜单树字段"));
        }
        if (!content.contains("appCode") && !content.contains("app-code")) {
            issues.add(
                    new ModuleMenuIssue("CRITICAL", file.toString(), 0, "菜单资源文件必须提供 appCode 字段"));
        }
    }

    private boolean isMainResourceDeclarationFile(Path file) {
        String normalized = file.toString().replace('\\', '/');
        return normalized.contains("/src/main/resources/META-INF/mango/resources/")
                && (normalized.endsWith(".yml")
                        || normalized.endsWith(".yaml")
                        || normalized.endsWith(".json"));
    }

    private List<ResourceDeclarationRecord> loadResourceDeclarations(
            Path file, List<ResourceRegistryIssue> issues) {
        try {
            String content = Files.readString(file);
            String fileName = file.getFileName().toString();
            if (fileName.endsWith(".json")) {
                return parseJsonResourceDeclarations(file, content, issues);
            }
            return parseYamlResourceDeclarations(file, content, issues);
        } catch (IOException e) {
            issues.add(
                    new ResourceRegistryIssue(
                            "MAJOR", file.toString(), 0, "资源声明读取失败: " + e.getMessage()));
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceDeclarationRecord> parseJsonResourceDeclarations(
            Path file, String content, List<ResourceRegistryIssue> issues) {
        try {
            Map<String, Object> root = objectMapper.readValue(content, Map.class);
            Map<String, Object> mango = asMap(root.get("mango"));
            Map<String, Object> resource = asMap(mango.get("resource"));
            Map<String, Object> declarationGroups = asMap(resource.get("declarations"));
            List<ResourceDeclarationRecord> records = new ArrayList<>();
            for (Map.Entry<String, Object> group : declarationGroups.entrySet()) {
                if (!(group.getValue() instanceof List<?> declarations)) {
                    continue;
                }
                for (Object value : declarations) {
                    if (!(value instanceof Map<?, ?> declaration)) {
                        continue;
                    }
                    String id = stringValue(declaration.get("id"));
                    String bizKey = stringValue(declaration.get("bizKey"));
                    if (bizKey == null) {
                        bizKey = stringValue(declaration.get("biz-key"));
                    }
                    records.add(
                            new ResourceDeclarationRecord(
                                    id, group.getKey(), bizKey, file.toString(), 0));
                }
            }
            return records;
        } catch (Exception e) {
            issues.add(
                    new ResourceRegistryIssue(
                            "MAJOR", file.toString(), 0, "JSON 资源声明解析失败: " + e.getMessage()));
            return List.of();
        }
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return Map.of();
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private List<ResourceDeclarationRecord> parseYamlResourceDeclarations(
            Path file, String content, List<ResourceRegistryIssue> issues) {
        List<ResourceDeclarationRecord> records = new ArrayList<>();
        String currentResourceType = null;
        ResourceDeclarationRecord current = null;
        String[] lines = content.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            Matcher typeMatcher = YAML_RESOURCE_TYPE_PATTERN.matcher(line);
            if (typeMatcher.matches()) {
                currentResourceType = typeMatcher.group(1);
                current = null;
                continue;
            }

            Matcher idMatcher = YAML_RESOURCE_ID_PATTERN.matcher(line);
            if (idMatcher.matches()) {
                current =
                        new ResourceDeclarationRecord(
                                idMatcher.group(1),
                                currentResourceType,
                                null,
                                file.toString(),
                                index + 1);
                records.add(current);
                continue;
            }

            Matcher bizKeyMatcher = YAML_RESOURCE_BIZ_KEY_PATTERN.matcher(line);
            if (bizKeyMatcher.matches() && current != null) {
                current.bizKey = unquote(bizKeyMatcher.group(1));
            }
        }

        for (ResourceDeclarationRecord declaration : records) {
            if (declaration.resourceType == null || declaration.resourceType.isBlank()) {
                issues.add(
                        new ResourceRegistryIssue(
                                "CRITICAL",
                                declaration.file,
                                declaration.line,
                                "资源声明缺少 resourceType 分组: id=" + declaration.id));
            }
        }
        return records;
    }

    private String unquote(String value) {
        String trimmed = "";
        if (value != null) {
            trimmed = value.trim();
        }
        if (isQuoted(trimmed)) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private boolean isQuoted(String value) {
        if (value.length() < MIN_QUOTED_LENGTH) {
            return false;
        }
        return hasMatchingQuote(value, "\"") || hasMatchingQuote(value, "'");
    }

    private boolean hasMatchingQuote(String value, String quote) {
        return value.startsWith(quote) && value.endsWith(quote);
    }

    private void validateResourceDeclarationUniqueness(
            List<ResourceDeclarationRecord> declarations, List<ResourceRegistryIssue> issues) {
        Map<String, ResourceDeclarationRecord> ids = new LinkedHashMap<>();
        Map<String, ResourceDeclarationRecord> bizKeys = new LinkedHashMap<>();
        for (ResourceDeclarationRecord declaration : declarations) {
            if (!validateResourceIdPresence(declaration, issues)) {
                continue;
            }
            validateResourceIdFormat(declaration, issues);
            validateUniqueResourceId(declaration, ids, issues);
            if (!validateResourceKeyPresence(declaration, issues)) {
                continue;
            }
            validateUniqueResourceKey(declaration, bizKeys, issues);
        }
    }

    private boolean validateResourceIdPresence(
            ResourceDeclarationRecord declaration, List<ResourceRegistryIssue> issues) {
        if (declaration.id != null && !declaration.id.isBlank()) {
            return true;
        }
        issues.add(
                new ResourceRegistryIssue(
                        "CRITICAL", declaration.file, declaration.line, "资源声明缺少 id"));
        return false;
    }

    private void validateResourceIdFormat(
            ResourceDeclarationRecord declaration, List<ResourceRegistryIssue> issues) {
        if (!declaration.id.matches(DIGITS_REGEX)) {
            issues.add(
                    new ResourceRegistryIssue(
                            "CRITICAL",
                            declaration.file,
                            declaration.line,
                            "资源声明 id 必须是雪花数字字符串: " + declaration.id));
        }
    }

    private void validateUniqueResourceId(
            ResourceDeclarationRecord declaration,
            Map<String, ResourceDeclarationRecord> ids,
            List<ResourceRegistryIssue> issues) {
        ResourceDeclarationRecord previous = ids.putIfAbsent(declaration.id, declaration);
        if (previous != null) {
            issues.add(
                    new ResourceRegistryIssue(
                            "CRITICAL",
                            declaration.file,
                            declaration.line,
                            "资源声明 id 重复: "
                                    + declaration.id
                                    + "，已存在于 "
                                    + previous.file
                                    + ":"
                                    + previous.line));
        }
    }

    private boolean validateResourceKeyPresence(
            ResourceDeclarationRecord declaration, List<ResourceRegistryIssue> issues) {
        if (!isBlank(declaration.resourceType) && !isBlank(declaration.bizKey)) {
            return true;
        }
        issues.add(
                new ResourceRegistryIssue(
                        "CRITICAL",
                        declaration.file,
                        declaration.line,
                        "资源声明缺少 resourceType 或 biz-key: id=" + declaration.id));
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateUniqueResourceKey(
            ResourceDeclarationRecord declaration,
            Map<String, ResourceDeclarationRecord> bizKeys,
            List<ResourceRegistryIssue> issues) {
        String bizKey = declaration.resourceType + ":" + declaration.bizKey;
        ResourceDeclarationRecord previous = bizKeys.putIfAbsent(bizKey, declaration);
        if (previous != null) {
            issues.add(
                    new ResourceRegistryIssue(
                            "CRITICAL",
                            declaration.file,
                            declaration.line,
                            "资源声明 resourceType + biz-key 重复: "
                                    + bizKey
                                    + "，已存在于 "
                                    + previous.file
                                    + ":"
                                    + previous.line));
        }
    }

    private String extractSignature(String line) {
        int parenStart = line.indexOf('(');
        int parenEnd = line.lastIndexOf(')');
        if (parenStart > 0 && parenEnd > parenStart) {
            int braceStart = line.indexOf('{');
            String signature = line.substring(0, parenEnd + 1).trim();
            if (braceStart > 0) {
                signature = line.substring(0, braceStart).trim();
            }
            return signature.replaceAll("\\s+", " ");
        }
        return null;
    }

    private Path resolveBasePath() {
        String effectiveBaseDir = baseDir;
        if (effectiveBaseDir == null || effectiveBaseDir.isEmpty()) {
            if (session != null && session.getExecutionRootDirectory() != null) {
                effectiveBaseDir = session.getExecutionRootDirectory();
            } else {
                getLog().error("Cannot determine base directory. Please set -DbaseDir=<path>");
                return null;
            }
        }

        Path rootPath = Paths.get(effectiveBaseDir);
        if (!Files.exists(rootPath)) {
            getLog().error("Base directory does not exist: " + effectiveBaseDir);
            return null;
        }
        return rootPath;
    }

    private boolean isBusinessProject(Path rootPath) {
        if (rootPath == null) {
            return false;
        }
        return Files.exists(rootPath.resolve("business-pmo"))
                || Files.exists(rootPath.resolve("backend"))
                || Files.exists(rootPath.resolve("../business-pmo").normalize());
    }

    private String extractArtifactId(String content) {
        String projectContent = content.replaceFirst("(?s)<parent>.*?</parent>", "");
        int start = projectContent.indexOf("<artifactId>");
        if (start == -1) {
            return null;
        }
        start += "<artifactId>".length();
        int end = projectContent.indexOf("</artifactId>", start);
        if (end == -1) {
            return null;
        }
        return projectContent.substring(start, end).trim();
    }

    private List<String> extractDependencies(String content) {
        List<String> dependencies = new ArrayList<>();
        String runtimeContent =
                content.replaceAll("(?s)<dependencyManagement>.*?</dependencyManagement>", "");
        int depStart = 0;
        while ((depStart = runtimeContent.indexOf("<dependencies>", depStart)) != -1) {
            int depEnd = runtimeContent.indexOf("</dependencies>", depStart);
            if (depEnd == -1) {
                break;
            }

            String depsSection = runtimeContent.substring(depStart, depEnd);
            int marker = 0;
            while ((marker = depsSection.indexOf("<dependency>", marker)) != -1) {
                int end = depsSection.indexOf("</dependency>", marker);
                if (end == -1) {
                    break;
                }
                dependencies.add(depsSection.substring(marker, end + "</dependency>".length()));
                marker = end + "</dependency>".length();
            }
            depStart = depEnd + "</dependencies>".length();
        }
        return dependencies;
    }

    private String extractArtifactIdFromDep(String dependencyBlock) {
        int start = dependencyBlock.indexOf("<artifactId>");
        if (start == -1) {
            return "";
        }
        start += "<artifactId>".length();
        int end = dependencyBlock.indexOf("</artifactId>", start);
        if (end == -1) {
            return "";
        }
        return dependencyBlock.substring(start, end).trim();
    }

    private String extractGroupIdFromDep(String dependencyBlock) {
        int start = dependencyBlock.indexOf("<groupId>");
        if (start == -1) {
            return "";
        }
        start += "<groupId>".length();
        int end = dependencyBlock.indexOf("</groupId>", start);
        if (end == -1) {
            return "";
        }
        return dependencyBlock.substring(start, end).trim();
    }

    private String extractScopeFromDep(String dependencyBlock) {
        int start = dependencyBlock.indexOf("<scope>");
        if (start == -1) {
            return "";
        }
        start += "<scope>".length();
        int end = dependencyBlock.indexOf("</scope>", start);
        if (end == -1) {
            return "";
        }
        return dependencyBlock.substring(start, end).trim();
    }

    private int lineNumber(String content, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private boolean usesInlinePlaceholder(String key, String token) {
        if (!key.contains(token) || key.startsWith(token)) {
            return false;
        }
        return !key.contains("#{" + token);
    }

    private CheckGateFinalizer gateFinalizer(Path basePath) {
        return new CheckGateFinalizer(
                objectMapper,
                new CheckGateOptions(
                        basePath,
                        changedFiles,
                        baseRef,
                        baselineFile,
                        gate,
                        staticFailurePolicy,
                        changedOnly),
                getLog()::warn);
    }

    private void outputText() {
        getLog().info("=== Check Result ===");
        getLog().info("Status: " + result.gateStatus);
        getLog().info("Gate: " + result.gate);
        getLog().info("Issues: " + result.issues.size());
        getLog().info("New issues: " + result.newIssues.size());
        getLog().info("Baseline issues: " + result.baselineIssues.size());
        getLog().info("Excluded issues: " + result.excludedIssues.size());
        getLog().info("Tool failures: " + result.toolFailures.size());
        for (String message : result.gateMessages) {
            getLog().warn("  [GATE] " + message);
        }
        for (ToolFailure failure : result.toolFailures) {
            getLog().warn("  [TOOL][" + failure.goal + "] " + failure.message);
        }

        for (CheckIssue issue : result.issues) {
            getLog().warn("  [" + issue.severity + "][" + issue.source + "] " + issue.description);
            if (issue.file != null) {
                getLog().warn("    at: " + issue.file + ":" + issue.line);
            }
        }
    }

    private void outputJson() {
        try {
            String json = objectMapper.writeValueAsString(result);
            getLog().info(json);
        } catch (Exception e) {
            getLog().error("Failed to output JSON", e);
        }
    }

    private void saveReport() {
        try {
            File report = new File(reportFile);
            File parent = report.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            String json = objectMapper.writeValueAsString(result);
            Files.writeString(report.toPath(), json);
            getLog().info("Report saved to: " + reportFile);
        } catch (Exception e) {
            getLog().error("Failed to save report", e);
        }
    }

    private static class NamingIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private NamingIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class DependencyIssue {
        private final String severity;
        private final String description;
        private String file;

        private DependencyIssue(String severity, String description) {
            this.severity = severity;
            this.description = description;
        }
    }

    private static class WebBoundaryIssue {
        private final String severity;
        private final String file;
        private final String description;

        private WebBoundaryIssue(String severity, String file, String description) {
            this.severity = severity;
            this.file = file;
            this.description = description;
        }
    }

    private static class ModuleInfoIssue {
        private final String severity;
        private final String file;
        private final String description;

        private ModuleInfoIssue(String severity, String file, String description) {
            this.severity = severity;
            this.file = file;
            this.description = description;
        }
    }

    private static class ModuleDescriptor {
        private final String artifactId;
        private final String moduleName;
        private final String modulePath;
        private final String file;

        private ModuleDescriptor(
                String artifactId, String moduleName, String modulePath, String file) {
            this.artifactId = artifactId;
            this.moduleName = moduleName;
            this.modulePath = modulePath;
            this.file = file;
        }
    }

    private static class RemoteAdapterIssue {
        private final String severity;
        private final String file;
        private final String description;

        private RemoteAdapterIssue(String severity, String file, String description) {
            this.severity = severity;
            this.file = file;
            this.description = description;
        }
    }

    private static class FeignInterfaceDeclaration {
        private final String name;
        private final String extendsClause;

        private FeignInterfaceDeclaration(String name, String extendsClause) {
            this.name = name;
            if (extendsClause == null) {
                this.extendsClause = "";
            } else {
                this.extendsClause = extendsClause;
            }
        }
    }

    private static class ApiContractIssue {
        private final String severity;
        private final String file;
        private final String description;

        private ApiContractIssue(String severity, String file, String description) {
            this.severity = severity;
            this.file = file;
            this.description = description;
        }
    }

    private static class PathParamIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private PathParamIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class KvKeyIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private KvKeyIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class PersistenceSchemaIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private PersistenceSchemaIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class PersistenceTableSchema {
        private final String tableName;
        private final String file;
        private final int line;
        private final Map<String, String> columnDefinitions;
        private boolean idPrimaryKey;

        private PersistenceTableSchema(
                String tableName,
                String file,
                int line,
                Map<String, String> columnDefinitions,
                boolean idPrimaryKey) {
            this.tableName = tableName;
            this.file = file;
            this.line = line;
            this.columnDefinitions = columnDefinitions;
            this.idPrimaryKey = idPrimaryKey;
        }
    }

    private static class PersistenceStyleIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private PersistenceStyleIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class ServiceContractIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private ServiceContractIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class TestFixtureIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private TestFixtureIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class ResourceRegistryIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private ResourceRegistryIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private static class ResourceDeclarationRecord {
        private final String id;
        private final String resourceType;
        private String bizKey;
        private final String file;
        private final int line;

        private ResourceDeclarationRecord(
                String id, String resourceType, String bizKey, String file, int line) {
            this.id = id;
            this.resourceType = resourceType;
            this.bizKey = bizKey;
            this.file = file;
            this.line = line;
        }
    }

    private static class ModuleMenuIssue {
        private final String severity;
        private final String file;
        private final int line;
        private final String description;

        private ModuleMenuIssue(String severity, String file, int line, String description) {
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
        }
    }

    private record ModuleSuffix(String value, ModuleType type) {}

    private record WebDependencies(boolean webApi, boolean webStarter, boolean springWebStarter) {}

    private record FeignMetadata(String name, String contextId, String path) {}

    private record ModuleMenuCounts(int blocking, int excluded) {}

    private record ApiLayerFacts(
            ModuleType moduleType,
            String typeName,
            boolean controller,
            boolean feignClient,
            boolean entity,
            boolean mapper,
            boolean serviceImplementation) {}

    private static class ParameterNesting {
        private int angleDepth;
        private int parenthesisDepth;

        private void accept(char current) {
            if (current == OPEN_ANGLE_BRACKET) {
                angleDepth++;
            } else if (current == CLOSE_ANGLE_BRACKET) {
                angleDepth = Math.max(0, angleDepth - 1);
            } else if (current == OPEN_PARENTHESIS) {
                parenthesisDepth++;
            } else if (current == CLOSE_PARENTHESIS) {
                parenthesisDepth = Math.max(0, parenthesisDepth - 1);
            }
        }

        private boolean isTopLevel() {
            return angleDepth == 0 && parenthesisDepth == 0;
        }
    }

    private static class SqlLexicalState {
        private boolean inBlockComment;
        private boolean inLineComment;
        private boolean inSingleQuote;
        private boolean inDoubleQuote;

        private int advance(String content, int index, int offset) {
            char current = content.charAt(index);
            char next = nextCharacter(content, index);
            if (inBlockComment) {
                return advanceBlockComment(current, next, index);
            }
            if (inSingleQuote) {
                return advanceSingleQuote(current, next, index);
            }
            if (inDoubleQuote) {
                return advanceDoubleQuote(current, index);
            }
            return advanceCode(content, current, next, index, offset);
        }

        private char nextCharacter(String content, int index) {
            if (index + 1 < content.length()) {
                return content.charAt(index + 1);
            }
            return '\0';
        }

        private int advanceBlockComment(char current, char next, int index) {
            if (current == ASTERISK && next == FORWARD_SLASH) {
                inBlockComment = false;
                return index + 2;
            }
            return index + 1;
        }

        private int advanceSingleQuote(char current, char next, int index) {
            if (current == SINGLE_QUOTE && next == SINGLE_QUOTE) {
                return index + 2;
            }
            if (current == SINGLE_QUOTE) {
                inSingleQuote = false;
            }
            return index + 1;
        }

        private int advanceDoubleQuote(char current, int index) {
            if (current == DOUBLE_QUOTE) {
                inDoubleQuote = false;
            }
            return index + 1;
        }

        private int advanceCode(String content, char current, char next, int index, int offset) {
            if (current == HYPHEN && next == HYPHEN) {
                return advanceLineComment(content, index, offset);
            }
            if (current == FORWARD_SLASH && next == ASTERISK) {
                inBlockComment = true;
                return index + 2;
            }
            if (current == SINGLE_QUOTE) {
                inSingleQuote = true;
            } else if (current == DOUBLE_QUOTE) {
                inDoubleQuote = true;
            }
            return index + 1;
        }

        private int advanceLineComment(String content, int index, int offset) {
            int lineEnd = content.indexOf('\n', index + 2);
            if (lineEnd < 0 || lineEnd >= offset) {
                inLineComment = true;
                return offset;
            }
            return lineEnd + 1;
        }

        private boolean isIgnored(boolean ignoreStringLiterals) {
            if (inLineComment || inBlockComment) {
                return true;
            }
            return ignoreStringLiterals && isInsideStringLiteral();
        }

        private boolean isInsideStringLiteral() {
            return inSingleQuote || inDoubleQuote;
        }
    }

    private static class SqlClauseState {
        private int parenthesisDepth;
        private boolean inSingleQuote;
        private boolean skipNext;
        private boolean delimiter;

        private void accept(String content, int index) {
            skipNext = false;
            delimiter = false;
            char current = content.charAt(index);
            if (current == SINGLE_QUOTE) {
                acceptQuote(content, index);
                return;
            }
            if (inSingleQuote) {
                return;
            }
            if (current == OPEN_PARENTHESIS) {
                parenthesisDepth++;
            } else if (current == CLOSE_PARENTHESIS) {
                parenthesisDepth = Math.max(0, parenthesisDepth - 1);
            } else if (current == PARAMETER_SEPARATOR && parenthesisDepth == 0) {
                delimiter = true;
            }
        }

        private void acceptQuote(String content, int index) {
            if (inSingleQuote
                    && index + 1 < content.length()
                    && content.charAt(index + 1) == SINGLE_QUOTE) {
                skipNext = true;
                return;
            }
            inSingleQuote = !inSingleQuote;
        }

        private boolean shouldSkipNext() {
            return skipNext;
        }

        private boolean isDelimiter() {
            return delimiter;
        }
    }

    private class SkippingFileVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (isIgnoredScanPath(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }
    }

    private class StaticReportFileVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            Path name = dir.getFileName();
            if (name == null || TARGET_DIRECTORY_NAME.equals(name.toString())) {
                return FileVisitResult.CONTINUE;
            }
            if (IGNORED_SCAN_SEGMENTS.contains(name.toString())) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }
    }

    private enum ModuleType {
        ROOT,
        OTHER,
        APP,
        API,
        SUPPORT,
        CORE,
        STARTER,
        SYNC_STARTER,
        STARTER_REMOTE,
    }
}
