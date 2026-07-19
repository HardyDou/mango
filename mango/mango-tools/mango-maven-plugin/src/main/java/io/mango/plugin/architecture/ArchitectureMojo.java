package io.mango.plugin.architecture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.mango.architecture.ArchitectureIssue;
import io.mango.architecture.MangoArchUnitChecker;
import io.mango.architecture.MangoPmdChecker;
import io.mango.architecture.MavenDependencyChecker;
import io.mango.architecture.ModuleRole;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/** Aggregates all local architecture engines into one fail-closed Maven verify goal. */
@Mojo(
    name = "architecture",
    defaultPhase = LifecyclePhase.VERIFY,
    aggregator = true,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public final class ArchitectureMojo extends AbstractMojo {

  private static final int ARCHITECTURE_REPORT_SCHEMA_VERSION = 2;
  private static final int DEBT_BASELINE_SCHEMA_VERSION = 4;
  private static final int LEGACY_DEBT_BASELINE_SCHEMA_VERSION = 3;
  private static final String SCHEMA_VERSION_FIELD = "schemaVersion";
  private static final String FULL_REACTOR_SCOPE = "full-reactor";
  private static final String PARTIAL_REACTOR_SCOPE = "partial-reactor";
  private static final String ALL_DETECTED_ISSUES = "all-detected-issues";
  private static final String FULL_MODE = "full";
  private static final long NANOS_PER_MILLISECOND = 1_000_000L;
  private static final int DEFAULT_JAVA_FEATURE = 17;
  private static final String UNRESOLVED_PROPERTY_PREFIX = "${";
  private static final String LEGACY_JAVA_VERSION_PREFIX = "1.";
  private static final String JAVA_SOURCE_MARKER = "/src/main/java/";
  private static final String JAVA_EXTENSION = ".java";
  private static final String FORWARD_SLASH = "/";
  private static final String BACKSLASH = "\\";
  private static final String POM_FILE = "pom.xml";
  private static final String MODULE_PROPERTIES =
      "src/main/resources/META-INF/mango/module.properties";
  private static final String ENTITY_RULE_PREFIX = "MANGO-ARCH-ENTITY-";
  private static final String GENERATED_SOURCE_MARKER = "/target/generated-sources/";
  private static final String DEPENDENCY_ARROW = " ->";
  private static final String REFERENCE_ARROW = "-> ";
  private static final List<String> DEFAULT_GIT_BASES = List.of("main", "origin/main");
  private static final List<String> CLASS_REFERENCE_SUFFIXES = List.of("$", ".", "#", "|");

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter(
      defaultValue = "${maven.multiModuleProjectDirectory}/target/mango-architecture-report.json")
  private File reportFile;

  @Parameter(
      defaultValue = "${maven.multiModuleProjectDirectory}",
      readonly = true,
      required = true)
  private File rootDirectory;

  @Parameter(property = "mango.architecture.base")
  private String gitBase;

  @Parameter(
      property = "mango.architecture.debtBaselineFile",
      defaultValue =
          "${maven.multiModuleProjectDirectory}/../mango-pmo/baselines/architecture/debt-budget.json")
  private File debtBaselineFile;

  @Parameter(property = "mango.architecture.mode", defaultValue = "changed")
  private String mode;

  @Parameter private List<String> excludedModules = List.of();

  @Parameter private List<String> allowedReverseControllers = List.of();

  @Parameter private List<String> requiredReactorArtifacts = List.of();

  @Parameter(property = "mango.architecture.requireFullReactor", defaultValue = "true")
  private boolean requireFullReactor;

  @Parameter(property = "mango.architecture.skip", defaultValue = "false")
  private boolean skip;

  @Parameter private File globalEntityManifest;

  @Parameter private List<String> businessGroupPrefixes = List.of();

  @Parameter(defaultValue = "false")
  private boolean lockFullReactor;

  /** POM-only lock that prevents command-line mode downgrades in governed verification. */
  @Parameter(defaultValue = "false")
  private boolean lockFullMode;

  private String resolvedGitBase;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    validateConfiguration();
    long startedAt = System.nanoTime();
    ReactorInputs inputs = collectReactorInputs();
    validateReservedNamespaces(inputs.sourceDirectories());
    Map<Path, MangoArchUnitChecker.ModuleContract> moduleContracts =
        moduleContracts(inputs.classDirectoryArtifacts());
    List<ArchitectureIssue> bytecodeIssues = checkBytecode(inputs, moduleContracts);
    ClassOwnership classOwnership =
        collectClassOwnership(inputs.classDirectoryArtifacts(), inputs.classDirectoryModules());
    Map<String, String> classArtifacts = classOwnership.artifacts();
    List<ArchitectureIssue> sourceIssues = checkSources(inputs);
    List<ArchitectureIssue> allIssues =
        combineIssues(inputs.dependencyIssues(), bytecodeIssues, sourceIssues);
    List<ArchitectureIssue> blockingIssues = blockingIssues(allIssues, classArtifacts);
    List<ReactorModuleDescriptor> moduleDescriptors = reactorModuleDescriptors();
    long durationMillis = (System.nanoTime() - startedAt) / NANOS_PER_MILLISECOND;
    String inventoryScope = PARTIAL_REACTOR_SCOPE;
    if (requireFullReactor) {
      inventoryScope = FULL_REACTOR_SCOPE;
    }
    ArchitectureReport report =
        new ArchitectureReport(
            ARCHITECTURE_REPORT_SCHEMA_VERSION,
            moduleDescriptors.stream().map(ReactorModuleDescriptor::reportedModule).toList(),
            reportIssues(inputs.dependencyIssues(), moduleDescriptors, classOwnership.modules()),
            reportIssues(bytecodeIssues, moduleDescriptors, classOwnership.modules()),
            reportIssues(sourceIssues, moduleDescriptors, classOwnership.modules()),
            reportIssues(blockingIssues, moduleDescriptors, classOwnership.modules()),
            mode,
            inventoryScope,
            ALL_DETECTED_ISSUES,
            session.getProjects().size(),
            session.getAllProjects().size(),
            durationMillis);
    writeReport(report);
    logReport(report, durationMillis);
    failOnIssues(report);
  }

  private void validateConfiguration() throws MojoExecutionException {
    if (skip) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-015 mango.architecture.skip is forbidden in governed"
              + " verification");
    }
    if (!excludedModules.isEmpty()) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-013 excludedModules is forbidden; architecture redlines"
              + " cannot be skipped");
    }
    if (lockFullReactor && !requireFullReactor) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-016 requireFullReactor=false is forbidden in governed"
              + " verification");
    }
    if (lockFullMode && !FULL_MODE.equalsIgnoreCase(mode)) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-018 architecture mode must remain full in governed" + " verification");
    }
  }

  private ReactorInputs collectReactorInputs() throws MojoExecutionException {
    List<ArchitectureIssue> dependencyIssues = new ArrayList<>();
    Map<Path, ModuleRole> classDirectories = new LinkedHashMap<>();
    Map<Path, String> classDirectoryArtifacts = new LinkedHashMap<>();
    Map<Path, String> classDirectoryModules = new LinkedHashMap<>();
    Set<Path> contractContextDirectories = new LinkedHashSet<>();
    List<Path> sourceDirectories = new ArrayList<>();
    Set<String> reactorArtifactIds =
        session.getProjects().stream().map(MavenProject::getArtifactId).collect(Collectors.toSet());
    Set<String> governedGroupPrefixes = new LinkedHashSet<>(businessGroupPrefixes);
    session.getProjects().stream()
        .map(MavenProject::getGroupId)
        .filter(groupId -> groupId != null && !groupId.isBlank())
        .forEach(governedGroupPrefixes::add);
    Set<String> allArtifactIds =
        session.getAllProjects().stream()
            .map(MavenProject::getArtifactId)
            .filter(artifactId -> !excludedModules.contains(artifactId))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    validateReactorScope(reactorArtifactIds, allArtifactIds);
    for (MavenProject reactorProject : session.getProjects()) {
      dependencyIssues.addAll(
          new MavenDependencyChecker()
              .check(
                  reactorProject.getArtifactId(),
                  reactorProject.getDependencies(),
                  reactorArtifactIds,
                  governedGroupPrefixes));
      collectJavaInputs(
          reactorProject,
          classDirectories,
          classDirectoryArtifacts,
          classDirectoryModules,
          sourceDirectories);
    }
    collectFeignApiContractInputs(reactorArtifactIds, contractContextDirectories);
    if (sourceDirectories.isEmpty()) {
      getLog().info("Reactor contains no Java sources; dependency architecture remains enforced");
    }
    return new ReactorInputs(
        dependencyIssues,
        classDirectories,
        classDirectoryArtifacts,
        classDirectoryModules,
        contractContextDirectories,
        sourceDirectories);
  }

  private void collectFeignApiContractInputs(
      Set<String> reactorArtifactIds, Set<Path> contractContextDirectories)
      throws MojoExecutionException {
    Set<String> remoteDomains =
        session.getProjects().stream()
            .filter(
                project ->
                    ModuleRole.fromArtifactId(project.getArtifactId()) == ModuleRole.STARTER_REMOTE)
            .map(project -> ModuleRole.domainOf(project.getArtifactId()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (remoteDomains.isEmpty()) {
      return;
    }

    List<Path> contractSourceDirectories = new ArrayList<>();
    Map<Path, ModuleRole> contractClassDirectories = new LinkedHashMap<>();
    for (MavenProject project : session.getAllProjects()) {
      String artifactId = project.getArtifactId();
      if (reactorArtifactIds.contains(artifactId)
          || ModuleRole.fromArtifactId(artifactId) != ModuleRole.API
          || !remoteDomains.contains(ModuleRole.domainOf(artifactId))) {
        continue;
      }
      collectJavaInputs(
          project,
          contractClassDirectories,
          new LinkedHashMap<>(),
          new LinkedHashMap<>(),
          contractSourceDirectories);
    }
    contractContextDirectories.addAll(contractClassDirectories.keySet());
    if (!contractSourceDirectories.isEmpty()) {
      getLog()
          .info(
              "ArchUnit Feign contract context: "
                  + contractSourceDirectories.size()
                  + " API source directorie(s)");
    }
  }

  private Map<Path, MangoArchUnitChecker.ModuleContract> moduleContracts(
      Map<Path, String> classDirectoryArtifacts) throws MojoExecutionException {
    Set<String> requiredDomains =
        classDirectoryArtifacts.values().stream()
            .map(ModuleRole::domainOf)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<String, ModuleIdentity> moduleIdentities = loadModuleIdentities(requiredDomains);
    Map<Path, MangoArchUnitChecker.ModuleContract> moduleContracts = new LinkedHashMap<>();
    for (Map.Entry<Path, String> entry : classDirectoryArtifacts.entrySet()) {
      String artifactId = entry.getValue();
      ModuleIdentity identity = moduleIdentities.get(ModuleRole.domainOf(artifactId));
      moduleContracts.put(entry.getKey(), moduleContract(artifactId, identity));
    }
    return moduleContracts;
  }

  private MangoArchUnitChecker.ModuleContract moduleContract(
      String artifactId, ModuleIdentity identity) {
    if (identity == null) {
      return new MangoArchUnitChecker.ModuleContract(artifactId, "", "");
    }
    return new MangoArchUnitChecker.ModuleContract(
        artifactId, identity.moduleName(), identity.modulePath());
  }

  private List<ArchitectureIssue> checkBytecode(
      ReactorInputs inputs, Map<Path, MangoArchUnitChecker.ModuleContract> moduleContracts)
      throws MojoExecutionException {
    if (inputs.classDirectories().isEmpty()) {
      getLog().info("ArchUnit: no Reactor bytecode inputs");
      return List.of();
    }
    MangoArchUnitChecker checker =
        new MangoArchUnitChecker(
            Set.copyOf(allowedReverseControllers),
            GlobalEntityManifestLoader.load(rootDirectory.toPath(), toPath(globalEntityManifest)));
    return checker.check(
        inputs.classDirectories(), moduleContracts, inputs.contractContextDirectories());
  }

  private List<ArchitectureIssue> checkSources(ReactorInputs inputs) throws MojoExecutionException {
    if (inputs.sourceDirectories().isEmpty()) {
      getLog().info("PMD architecture: no Reactor Java source inputs");
      return List.of();
    }
    String javaVersion = resolveJavaVersion(session.getAllProjects());
    getLog().info("PMD Java language version: " + javaVersion);
    Set<Path> auxiliaryClasspath = collectAuxiliaryClasspath(inputs.classDirectories().keySet());
    return new MangoPmdChecker().check(inputs.sourceDirectories(), javaVersion, auxiliaryClasspath);
  }

  private List<ArchitectureIssue> combineIssues(
      List<ArchitectureIssue> dependencyIssues,
      List<ArchitectureIssue> bytecodeIssues,
      List<ArchitectureIssue> sourceIssues) {
    List<ArchitectureIssue> allIssues = new ArrayList<>();
    allIssues.addAll(dependencyIssues);
    allIssues.addAll(bytecodeIssues);
    allIssues.addAll(sourceIssues);
    return allIssues;
  }

  private List<ReactorModuleDescriptor> reactorModuleDescriptors() throws MojoExecutionException {
    Map<String, ReactorModuleDescriptor> byModuleKey = new LinkedHashMap<>();
    Set<String> coordinates = new LinkedHashSet<>();
    for (MavenProject project : session.getProjects()) {
      validateReactorProject(project);
      String key = moduleKey(project);
      String coordinatesKey = project.getGroupId() + ":" + project.getArtifactId();
      if (!coordinates.add(coordinatesKey)) {
        throw new MojoExecutionException(
            "MANGO-ARCH-ENGINE-026 duplicate Reactor coordinates: " + coordinatesKey);
      }
      ReactorModuleDescriptor descriptor =
          new ReactorModuleDescriptor(
              key,
              project.getGroupId(),
              project.getArtifactId(),
              project.getBasedir().toPath().toAbsolutePath().normalize());
      if (byModuleKey.putIfAbsent(key, descriptor) != null) {
        throw new MojoExecutionException(
            "MANGO-ARCH-ENGINE-026 duplicate Reactor moduleKey: " + key);
      }
    }
    return byModuleKey.values().stream()
        .sorted((left, right) -> left.moduleKey().compareTo(right.moduleKey()))
        .toList();
  }

  private void validateReactorProject(MavenProject project) throws MojoExecutionException {
    if (project == null) {
      throw invalidReactorProject();
    }
    boolean missingCoordinates = project.getGroupId() == null || project.getArtifactId() == null;
    if (missingCoordinates) {
      throw invalidReactorProject();
    }
    if (project.getGroupId().isBlank() || project.getArtifactId().isBlank()) {
      throw invalidReactorProject();
    }
    if (project.getBasedir() == null) {
      throw invalidReactorProject();
    }
  }

  private MojoExecutionException invalidReactorProject() {
    return new MojoExecutionException(
        "MANGO-ARCH-ENGINE-026 Reactor project is missing module coordinates");
  }

  private String moduleKey(MavenProject project) throws MojoExecutionException {
    if (project.getBasedir() == null) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-026 Reactor project has no base directory: "
              + project.getArtifactId());
    }
    Path root = rootDirectory.toPath().toAbsolutePath().normalize();
    Path base = project.getBasedir().toPath().toAbsolutePath().normalize();
    if (!base.startsWith(root)) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-026 Reactor project is outside the Maven root: " + base);
    }
    String relative = root.relativize(base).toString().replace('\\', '/');
    if (relative.isBlank()) {
      return ".";
    }
    return relative;
  }

  private List<ReportedArchitectureIssue> reportIssues(
      List<ArchitectureIssue> issues,
      List<ReactorModuleDescriptor> modules,
      Map<String, String> classModules)
      throws MojoExecutionException {
    List<ReportedArchitectureIssue> reported = new ArrayList<>();
    for (ArchitectureIssue issue : issues) {
      reported.add(
          new ReportedArchitectureIssue(
              issue.ruleId(),
              issue.subject(),
              issue.message(),
              issueModuleKey(issue, modules, classModules)));
    }
    return List.copyOf(reported);
  }

  private String issueModuleKey(
      ArchitectureIssue issue,
      List<ReactorModuleDescriptor> modules,
      Map<String, String> classModules)
      throws MojoExecutionException {
    String subject = issue.subject().replace('\\', '/');
    String pathModule = pathSubjectModule(subject, modules);
    if (pathModule != null) {
      return pathModule;
    }

    int dependencySeparator = subject.indexOf(DEPENDENCY_ARROW);
    if (dependencySeparator > 0) {
      String sourceArtifact = subject.substring(0, dependencySeparator).trim();
      Set<String> sourceModules =
          modules.stream()
              .filter(module -> module.artifactId().equals(sourceArtifact))
              .map(ReactorModuleDescriptor::moduleKey)
              .collect(Collectors.toCollection(LinkedHashSet::new));
      if (sourceModules.size() == 1) {
        return sourceModules.iterator().next();
      }
      if (sourceModules.size() > 1) {
        throw ambiguousIssueModule(issue, sourceModules);
      }
    }

    String leadingClassModule = leadingClassModule(subject, classModules);
    if (leadingClassModule != null) {
      return leadingClassModule;
    }

    Set<String> referencedModules =
        classModules.entrySet().stream()
            .filter(entry -> referencesClass(subject, entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (referencedModules.size() == 1) {
      return referencedModules.iterator().next();
    }
    if (referencedModules.size() > 1) {
      throw ambiguousIssueModule(issue, referencedModules);
    }
    throw new MojoExecutionException(
        "MANGO-ARCH-ENGINE-027 cannot attribute architecture issue to a Reactor module: "
            + issue.ruleId()
            + " "
            + issue.subject());
  }

  private MojoExecutionException ambiguousIssueModule(
      ArchitectureIssue issue, Set<String> modules) {
    return new MojoExecutionException(
        "MANGO-ARCH-ENGINE-027 architecture issue has ambiguous module ownership: "
            + issue.ruleId()
            + " "
            + issue.subject()
            + " -> "
            + String.join(", ", modules));
  }

  private String pathSubjectModule(String subject, List<ReactorModuleDescriptor> modules) {
    if (!isSourcePathSubject(subject)) {
      return null;
    }
    String source = subject.replaceFirst(":\\d+$", "");
    try {
      Path candidate = Path.of(source);
      if (!candidate.isAbsolute()) {
        candidate = rootDirectory.toPath().resolve(candidate);
      }
      candidate = candidate.toAbsolutePath().normalize();
      ReactorModuleDescriptor owner = null;
      int ownerDepth = -1;
      for (ReactorModuleDescriptor module : modules) {
        if (candidate.startsWith(module.baseDirectory())) {
          int depth = module.baseDirectory().getNameCount();
          if (depth > ownerDepth) {
            owner = module;
            ownerDepth = depth;
          }
        }
      }
      if (owner == null) {
        return null;
      }
      return owner.moduleKey();
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private boolean isSourcePathSubject(String subject) {
    boolean hasPathSeparator = subject.contains(FORWARD_SLASH) || subject.contains(BACKSLASH);
    if (!hasPathSeparator) {
      return false;
    }
    return subject.contains(JAVA_EXTENSION) || subject.contains(GENERATED_SOURCE_MARKER);
  }

  private String leadingClassModule(String subject, Map<String, String> classModules) {
    int end = subject.length();
    for (char delimiter : new char[] {'|', '#', ' ', '('}) {
      int candidate = subject.indexOf(delimiter);
      if (candidate >= 0 && candidate < end) {
        end = candidate;
      }
    }
    String candidate = subject.substring(0, end);
    while (!candidate.isBlank()) {
      String module = classModules.get(candidate);
      if (module != null) {
        return module;
      }
      int methodSeparator = candidate.lastIndexOf('.');
      if (methodSeparator < 0) {
        return null;
      }
      candidate = candidate.substring(0, methodSeparator);
    }
    return null;
  }

  private List<ArchitectureIssue> blockingIssues(
      List<ArchitectureIssue> allIssues, Map<String, String> classArtifacts)
      throws MojoExecutionException {
    if (FULL_MODE.equalsIgnoreCase(mode)) {
      return List.copyOf(allIssues);
    }
    return newIssuesAgainstDebtBaseline(changedIssues(allIssues, classArtifacts));
  }

  private List<ArchitectureIssue> newIssuesAgainstDebtBaseline(
      List<ArchitectureIssue> changedIssues) throws MojoExecutionException {
    String baseBudget = readFileFromGitBase(debtBaselineFile.toPath());
    if (baseBudget.isBlank()) {
      return changedIssues;
    }
    Map<String, Integer> remainingHistoricalIdentities = readDebtIdentityCounts(baseBudget);
    List<ArchitectureIssue> newIssues = new ArrayList<>();
    for (ArchitectureIssue issue : changedIssues) {
      String identity = issueIdentity(issue);
      int remaining = remainingHistoricalIdentities.getOrDefault(identity, 0);
      if (remaining > 0) {
        remainingHistoricalIdentities.put(identity, remaining - 1);
      } else {
        newIssues.add(issue);
      }
    }
    return List.copyOf(newIssues);
  }

  private Map<String, Integer> readDebtIdentityCounts(String source) throws MojoExecutionException {
    try {
      var baseline = new ObjectMapper().readTree(source);
      int schemaVersion = baseline.path(SCHEMA_VERSION_FIELD).asInt(-1);
      if (schemaVersion != DEBT_BASELINE_SCHEMA_VERSION
          && schemaVersion != LEGACY_DEBT_BASELINE_SCHEMA_VERSION) {
        throw new MojoExecutionException(
            "MANGO-ARCH-ENGINE-019 architecture debt baseline schemaVersion must be "
                + LEGACY_DEBT_BASELINE_SCHEMA_VERSION
                + " or "
                + DEBT_BASELINE_SCHEMA_VERSION
                + ": "
                + debtBaselineFile);
      }
      var identities = baseline.path("identities");
      if (!identities.isObject()) {
        throw new MojoExecutionException(
            "MANGO-ARCH-ENGINE-020 architecture debt baseline identities are missing: "
                + debtBaselineFile);
      }
      Map<String, Integer> counts = new LinkedHashMap<>();
      Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields =
          identities.fields();
      while (fields.hasNext()) {
        Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> field = fields.next();
        if (!field.getKey().matches("[a-f0-9]{64}")
            || !field.getValue().canConvertToInt()
            || field.getValue().asInt() < 0) {
          throw new MojoExecutionException(
              "MANGO-ARCH-ENGINE-021 invalid architecture debt identity: " + field.getKey());
        }
        counts.put(field.getKey(), field.getValue().asInt());
      }
      return counts;
    } catch (IOException exception) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-022 unable to parse base architecture debt baseline "
              + resolvedGitBase,
          exception);
    }
  }

  private String issueIdentity(ArchitectureIssue issue) throws MojoExecutionException {
    return sha256(
        issue.ruleId() + "\0" + normalizedIssueSubject(issue.subject()) + "\0" + issue.message());
  }

  private String normalizedIssueSubject(String subject) throws MojoExecutionException {
    String normalized = subject.replace('\\', '/');
    int separator = normalized.lastIndexOf(':');
    if (separator > 0
        && normalized.substring(0, separator).endsWith(JAVA_EXTENSION)
        && normalized.substring(separator + 1).matches("[0-9]+")) {
      Path source = Path.of(normalized.substring(0, separator)).toAbsolutePath().normalize();
      int line = Integer.parseInt(normalized.substring(separator + 1));
      try {
        List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        if (line < 1 || line > lines.size()) {
          throw new MojoExecutionException(
              "MANGO-ARCH-ENGINE-023 architecture issue source line is outside the file: "
                  + subject);
        }
        String sourceLine = lines.get(line - 1).trim().replaceAll("\\s+", " ");
        return repositoryRelative(source) + "|source-line-sha256=" + sha256(sourceLine);
      } catch (IOException exception) {
        throw new MojoExecutionException(
            "MANGO-ARCH-ENGINE-024 unable to read architecture issue source " + source, exception);
      }
    }
    Path repositoryRoot = repositoryRoot();
    String prefix = repositoryRoot.toString().replace('\\', '/') + "/";
    if (normalized.startsWith(prefix)) {
      return normalized.substring(prefix.length());
    }
    return normalized;
  }

  private String repositoryRelative(Path source) {
    Path repositoryRoot = repositoryRoot();
    if (source.startsWith(repositoryRoot)) {
      return repositoryRoot.relativize(source).toString().replace('\\', '/');
    }
    return source.toString().replace('\\', '/');
  }

  private Path repositoryRoot() {
    Path normalizedRoot = rootDirectory.toPath().toAbsolutePath().normalize();
    if (normalizedRoot.getFileName() != null
        && "mango".equals(normalizedRoot.getFileName().toString())
        && normalizedRoot.getParent() != null) {
      return normalizedRoot.getParent();
    }
    return normalizedRoot;
  }

  private String sha256(String value) throws MojoExecutionException {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new MojoExecutionException("MANGO-ARCH-ENGINE-025 SHA-256 is unavailable", exception);
    }
  }

  private void logReport(ArchitectureReport report, long durationMillis) {
    getLog()
        .info(
            "Mango architecture: dependency="
                + report.dependencyIssues().size()
                + ", archunit="
                + report.archUnitIssues().size()
                + ", pmd="
                + report.pmdIssues().size()
                + ", blocking="
                + report.blockingIssues().size()
                + ", mode="
                + mode
                + ", durationMs="
                + durationMillis);
  }

  private void failOnIssues(ArchitectureReport report) throws MojoFailureException {
    int issueCount = report.issueCount();
    if (issueCount > 0) {
      throw new MojoFailureException(
          "Mango architecture gate found " + issueCount + " violation(s); report: " + reportFile);
    }
  }

  void validateReservedNamespaces(Collection<Path> sourceDirectories)
      throws MojoExecutionException {
    if (businessGroupPrefixes == null || businessGroupPrefixes.isEmpty()) {
      return;
    }
    List<String> reservedPrefixes =
        List.of(
            "io.mango.",
            "org.springframework.",
            "org.apache.ibatis.",
            "com.baomidou.mybatisplus.",
            "jakarta.validation.");
    for (Path sourceDirectory : sourceDirectories) {
      if (!Files.isDirectory(sourceDirectory)) {
        continue;
      }
      try (var paths = Files.walk(sourceDirectory)) {
        for (Path source :
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .toList()) {
          String content = Files.readString(source);
          var matcher =
              java.util.regex.Pattern.compile(
                      "(?m)^\\s*package\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;")
                  .matcher(content);
          if (!matcher.find()) {
            throw new MojoExecutionException(
                "MANGO-ARCH-ENGINE-017 business Java source requires an explicit"
                    + " package: "
                    + source);
          }
          String packageName = matcher.group(1) + ".";
          if (reservedPrefixes.stream().anyMatch(packageName::startsWith)) {
            throw new MojoExecutionException(
                "MANGO-ARCH-ENGINE-017 business source must not shadow a reserved"
                    + " namespace: "
                    + matcher.group(1)
                    + " at "
                    + source);
          }
        }
      } catch (MojoExecutionException exception) {
        throw exception;
      } catch (IOException exception) {
        throw new MojoExecutionException(
            "MANGO-ARCH-ENGINE-017 failed to validate business source namespaces", exception);
      }
    }
  }

  String resolveJavaVersion(Collection<MavenProject> reactorProjects) {
    int highest =
        reactorProjects.stream()
            .flatMap(
                reactorProject ->
                    List.of(
                        reactorProject.getProperties().getProperty("maven.compiler.release", ""),
                        reactorProject.getProperties().getProperty("maven.compiler.source", ""))
                        .stream())
            .mapToInt(this::parseJavaFeature)
            .max()
            .orElse(0);
    if (highest == 0) {
      highest =
          parseJavaFeature(
              System.getProperty(
                  "java.specification.version", Integer.toString(DEFAULT_JAVA_FEATURE)));
    }
    if (highest == 0) {
      highest = DEFAULT_JAVA_FEATURE;
    }
    return Integer.toString(highest);
  }

  private int parseJavaFeature(String value) {
    if (value == null || value.isBlank() || value.contains(UNRESOLVED_PROPERTY_PREFIX)) {
      return 0;
    }
    String normalized = value.trim();
    if (normalized.startsWith(LEGACY_JAVA_VERSION_PREFIX)) {
      normalized = normalized.substring(LEGACY_JAVA_VERSION_PREFIX.length());
    }
    int separator = normalized.indexOf('.');
    if (separator >= 0) {
      normalized = normalized.substring(0, separator);
    }
    try {
      return Integer.parseInt(normalized);
    } catch (NumberFormatException ignored) {
      return 0;
    }
  }

  void validateReactorScope(Set<String> reactorArtifactIds, Set<String> allArtifactIds)
      throws MojoExecutionException {
    Set<String> expectedArtifacts = new LinkedHashSet<>(requiredReactorArtifacts);
    if (requireFullReactor) {
      expectedArtifacts.addAll(allArtifactIds);
    }
    Set<String> missingReactorArtifacts =
        expectedArtifacts.stream()
            .filter(required -> !reactorArtifactIds.contains(required))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (!missingReactorArtifacts.isEmpty()) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-011 incomplete Reactor; missing required artifacts: "
              + String.join(", ", missingReactorArtifacts));
    }
  }

  private List<ArchitectureIssue> changedIssues(
      List<ArchitectureIssue> allIssues, Map<String, String> classArtifacts)
      throws MojoExecutionException {
    Set<String> changedPaths = gitChangedPaths();
    if (changedPaths.isEmpty()) {
      return List.of();
    }
    Set<String> relevantPomPaths = architectureRelevantPomPaths(changedPaths);
    Set<String> changedArtifacts = impactedArtifactsForChangedPoms(relevantPomPaths);
    Set<String> directChangedArtifacts = directArtifactsForChangedPoms(relevantPomPaths);
    Set<String> changedDomains =
        session.getProjects().stream()
            .filter(reactorProject -> projectContractChanged(reactorProject, changedPaths))
            .map(MavenProject::getArtifactId)
            .map(ModuleRole::domainOf)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Set<String> changedArtifactClasses =
        impactedArtifactClasses(classArtifacts, directChangedArtifacts, changedDomains);
    Set<String> changedClasses = changedJavaClasses(changedPaths);
    Set<String> changedGlobalEntities = changedGlobalEntitySubjects(changedPaths);
    return allIssues.stream()
        .filter(
            issue ->
                isChangedIssue(
                    issue,
                    changedPaths,
                    changedArtifacts,
                    changedDomains,
                    changedClasses,
                    changedArtifactClasses,
                    changedGlobalEntities))
        .toList();
  }

  private Set<String> changedJavaClasses(Set<String> changedPaths) {
    return changedPaths.stream()
        .filter(path -> path.endsWith(JAVA_EXTENSION) && path.contains(JAVA_SOURCE_MARKER))
        .map(this::javaClassName)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private String javaClassName(String path) {
    int classNameStart = path.indexOf(JAVA_SOURCE_MARKER) + JAVA_SOURCE_MARKER.length();
    int classNameEnd = path.length() - JAVA_EXTENSION.length();
    return path.substring(classNameStart, classNameEnd).replace('/', '.');
  }

  Set<String> impactedArtifactsForChangedPoms(Set<String> changedPaths)
      throws MojoExecutionException {
    if (session == null || session.getProjects() == null) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-011 Maven session is required to attribute changed POMs");
    }
    List<MavenProject> projects =
        session.getProjects().stream()
            .filter(project -> project != null && project.getArtifactId() != null)
            .toList();
    List<MavenProject> changedProjects =
        projects.stream()
            .filter(project -> project.getFile() != null)
            .filter(project -> changedPaths.contains(relativePath(project.getFile().toPath())))
            .toList();
    Set<String> impacted = new LinkedHashSet<>();
    for (MavenProject candidate : projects) {
      if (changedProjects.stream()
          .anyMatch(changed -> sameOrDescendantProject(candidate, changed))) {
        impacted.add(candidate.getArtifactId());
      }
    }
    return impacted;
  }

  private Set<String> directArtifactsForChangedPoms(Set<String> changedPaths)
      throws MojoExecutionException {
    if (session == null || session.getProjects() == null) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-011 Maven session is required to attribute changed POMs");
    }
    return session.getProjects().stream()
        .filter(project -> project != null && project.getFile() != null)
        .filter(project -> changedPaths.contains(relativePath(project.getFile().toPath())))
        .map(MavenProject::getArtifactId)
        .filter(artifactId -> artifactId != null && !artifactId.isBlank())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<String> architectureRelevantPomPaths(Set<String> changedPaths)
      throws MojoExecutionException {
    Set<String> relevant =
        changedPaths.stream()
            .filter(path -> POM_FILE.equals(path) || path.endsWith("/" + POM_FILE))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (relevant.isEmpty()) {
      return relevant;
    }
    Map<String, MavenProject> projectsByPom =
        session.getProjects().stream()
            .filter(project -> project != null && project.getFile() != null)
            .collect(
                Collectors.toMap(
                    project -> relativePath(project.getFile().toPath()),
                    project -> project,
                    (left, right) -> left,
                    LinkedHashMap::new));
    for (String changedPath : List.copyOf(relevant)) {
      MavenProject project = projectsByPom.get(changedPath);
      if (project != null && !architectureRelevantPomChanged(project.getFile().toPath())) {
        relevant.remove(changedPath);
      }
    }
    return relevant;
  }

  private boolean architectureRelevantPomChanged(Path pom) throws MojoExecutionException {
    if (resolvedGitBase == null || resolvedGitBase.isBlank()) {
      return true;
    }
    List<String> roots = runGit("rev-parse", "--show-toplevel");
    if (roots.isEmpty()) {
      return true;
    }
    Path repositoryRoot = canonicalPath(Path.of(roots.get(0)));
    Path comparablePom = canonicalPath(pom);
    if (!comparablePom.startsWith(repositoryRoot) || !Files.isRegularFile(pom)) {
      return true;
    }
    String repositoryPath = repositoryRoot.relativize(comparablePom).toString().replace('\\', '/');
    try {
      String previous =
          String.join(
              "\n", runGitAt(repositoryRoot, "show", resolvedGitBase + ":" + repositoryPath));
      String current = Files.readString(pom);
      return architectureRelevantPomChange(previous, current);
    } catch (MojoExecutionException | IOException exception) {
      getLog()
          .debug(
              "Treat POM as architecture-relevant because its base version is"
                  + " unavailable: "
                  + repositoryPath,
              exception);
      return true;
    }
  }

  boolean architectureRelevantPomChange(String previous, String current)
      throws MojoExecutionException {
    return !architecturePomFingerprint(previous).equals(architecturePomFingerprint(current));
  }

  private String architecturePomFingerprint(String source) throws MojoExecutionException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setExpandEntityReferences(false);
      Element project =
          factory
              .newDocumentBuilder()
              .parse(new InputSource(new StringReader(source)))
              .getDocumentElement();
      StringBuilder fingerprint = new StringBuilder();
      for (String tag :
          List.of(
              "parent",
              "groupId",
              "artifactId",
              "packaging",
              "modules",
              "dependencyManagement",
              "dependencies")) {
        appendDirectElements(project, tag, fingerprint);
      }
      return fingerprint.toString();
    } catch (Exception exception) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-008 unable to parse architecture-relevant POM content", exception);
    }
  }

  private void appendDirectElements(Element parent, String tag, StringBuilder target) {
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element element && tag.equals(element.getTagName())) {
        appendElementFingerprint(element, target);
      }
    }
  }

  private void appendElementFingerprint(Element element, StringBuilder target) {
    target.append('<').append(element.getTagName()).append('>');
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element nested) {
        appendElementFingerprint(nested, target);
      } else if (child.getNodeType() == Node.TEXT_NODE) {
        String text = child.getTextContent().trim().replaceAll("\\s+", " ");
        if (!text.isEmpty()) {
          target.append(text);
        }
      }
    }
    target.append("</").append(element.getTagName()).append('>');
  }

  private boolean sameOrDescendantProject(MavenProject candidate, MavenProject ancestor) {
    if (candidate == ancestor) {
      return true;
    }
    if (candidate.getBasedir() == null || ancestor.getBasedir() == null) {
      return false;
    }
    Path candidateRoot = candidate.getBasedir().toPath().toAbsolutePath().normalize();
    Path ancestorRoot = ancestor.getBasedir().toPath().toAbsolutePath().normalize();
    return candidateRoot.startsWith(ancestorRoot);
  }

  Set<String> impactedArtifactClasses(
      Map<String, String> classArtifacts,
      Set<String> changedArtifacts,
      Set<String> changedDomains) {
    return classArtifacts.entrySet().stream()
        .filter(
            entry ->
                changedArtifacts.contains(entry.getValue())
                    || changedDomains.contains(ModuleRole.domainOf(entry.getValue())))
        .map(Map.Entry::getKey)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private boolean projectContractChanged(MavenProject reactorProject, Set<String> changedPaths) {
    String projectRoot = relativePath(reactorProject.getBasedir().toPath());
    String contractPath = MODULE_PROPERTIES;
    if (!projectRoot.isEmpty()) {
      contractPath = projectRoot + "/" + MODULE_PROPERTIES;
    }
    return changedPaths.contains(contractPath);
  }

  boolean isChangedIssue(
      ArchitectureIssue issue,
      Set<String> changedPaths,
      Set<String> changedArtifacts,
      Set<String> changedDomains,
      Set<String> changedClasses,
      Set<String> changedArtifactClasses) {
    return isChangedIssue(
        issue,
        changedPaths,
        changedArtifacts,
        changedDomains,
        changedClasses,
        changedArtifactClasses,
        Set.of());
  }

  boolean isChangedIssue(
      ArchitectureIssue issue,
      Set<String> changedPaths,
      Set<String> changedArtifacts,
      Set<String> changedDomains,
      Set<String> changedClasses,
      Set<String> changedArtifactClasses,
      Set<String> changedGlobalEntities) {
    if (issue.ruleId().startsWith(ENTITY_RULE_PREFIX)
        && changedGlobalEntities.stream()
            .anyMatch(entity -> referencesClass(issue.subject(), entity))) {
      return true;
    }
    String subject = issue.subject().replace('\\', '/');
    if (subject.contains(GENERATED_SOURCE_MARKER)) {
      return true;
    }
    if (changedPaths.stream()
        .anyMatch(
            path ->
                subject.startsWith(
                    rootDirectory
                        .toPath()
                        .resolve(path)
                        .toAbsolutePath()
                        .normalize()
                        .toString()
                        .replace('\\', '/')))) {
      return true;
    }
    if (changedArtifacts.stream()
        .anyMatch(artifact -> subject.startsWith(artifact + DEPENDENCY_ARROW))) {
      return true;
    }
    int artifactSeparator = subject.indexOf(DEPENDENCY_ARROW);
    if (artifactSeparator > 0
        && changedDomains.contains(ModuleRole.domainOf(subject.substring(0, artifactSeparator)))) {
      return true;
    }
    if (changedClasses.stream().anyMatch(className -> referencesClass(subject, className))) {
      return true;
    }
    return changedArtifactClasses.stream()
        .anyMatch(className -> referencesClass(subject, className));
  }

  private boolean referencesClass(String subject, String className) {
    if (subject.equals(className)) {
      return true;
    }
    if (CLASS_REFERENCE_SUFFIXES.stream().map(className::concat).anyMatch(subject::startsWith)) {
      return true;
    }
    return subject.contains("|" + className + "#") || subject.contains(REFERENCE_ARROW + className);
  }

  private Set<String> gitChangedPaths() throws MojoExecutionException {
    String base = gitBase;
    if (base == null || base.isBlank()) {
      base = resolveDefaultBase();
    }
    resolvedGitBase = base;
    Set<String> paths =
        new LinkedHashSet<>(
            runGit("diff", "--name-only", "--relative", "--diff-filter=ACMRD", base));
    paths.addAll(runGit("ls-files", "--others", "--exclude-standard"));
    addExternalManifestChanges(base, paths);
    return paths.stream()
        .filter(path -> !path.isBlank())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<String> changedGlobalEntitySubjects(Set<String> changedPaths)
      throws MojoExecutionException {
    Set<String> changedEntities = new LinkedHashSet<>();
    for (Path candidate : globalEntityManifestCandidates()) {
      if (!changedPaths.contains(relativePath(candidate))) {
        continue;
      }
      Map<String, String> previous = readGlobalEntityEntries(candidate, true);
      Map<String, String> current = readGlobalEntityEntries(candidate, false);
      Set<String> entities = new LinkedHashSet<>(previous.keySet());
      entities.addAll(current.keySet());
      for (String entity : entities) {
        if (!java.util.Objects.equals(previous.get(entity), current.get(entity))) {
          changedEntities.add(entity);
        }
      }
    }
    return changedEntities;
  }

  private Map<String, String> readGlobalEntityEntries(Path manifest, boolean fromBase)
      throws MojoExecutionException {
    String source;
    if (fromBase) {
      source = readFileFromGitBase(manifest);
    } else if (Files.isRegularFile(manifest)) {
      try {
        source = Files.readString(manifest);
      } catch (IOException exception) {
        throw new MojoExecutionException(
            "MANGO-ARCH-ENGINE-014 unable to read global entity manifest: " + manifest, exception);
      }
    } else {
      source = "";
    }
    if (source.isBlank()) {
      return Map.of();
    }
    try {
      var root = new ObjectMapper().readTree(source);
      var entries = root.path("exceptions");
      if (!entries.isArray()) {
        return Map.of();
      }
      Map<String, String> result = new LinkedHashMap<>();
      for (var entry : entries) {
        String entity = entry.path("entity").asText("").trim();
        String table = entry.path("table").asText("").trim();
        if (!entity.isEmpty()) {
          result.put(entity, table);
        }
      }
      return result;
    } catch (IOException exception) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-014 unable to compare global entity manifest: " + manifest, exception);
    }
  }

  private String readFileFromGitBase(Path file) throws MojoExecutionException {
    if (resolvedGitBase == null || resolvedGitBase.isBlank()) {
      return "";
    }
    List<String> roots = runGit("rev-parse", "--show-toplevel");
    if (roots.isEmpty()) {
      return "";
    }
    Path repositoryRoot = canonicalPath(Path.of(roots.get(0)));
    Path comparableFile = canonicalPath(file);
    if (!comparableFile.startsWith(repositoryRoot)) {
      return "";
    }
    String repositoryPath = repositoryRoot.relativize(comparableFile).toString().replace('\\', '/');
    try {
      return String.join(
          "\n", runGitAt(repositoryRoot, "show", resolvedGitBase + ":" + repositoryPath));
    } catch (MojoExecutionException missingAtBase) {
      getLog().debug("Global entity manifest did not exist at Git base: " + repositoryPath);
      return "";
    }
  }

  private void addExternalManifestChanges(String base, Set<String> changedPaths)
      throws MojoExecutionException {
    List<String> roots = runGit("rev-parse", "--show-toplevel");
    if (roots.isEmpty()) {
      return;
    }
    Path repositoryRoot = canonicalPath(Path.of(roots.get(0)));
    Path mavenRoot = canonicalPath(rootDirectory.toPath());
    Set<String> repositoryChanges =
        new LinkedHashSet<>(
            runGitAt(repositoryRoot, "diff", "--name-only", "--diff-filter=ACMRD", base));
    repositoryChanges.addAll(
        runGitAt(repositoryRoot, "ls-files", "--others", "--exclude-standard"));
    for (Path manifest : globalEntityManifestCandidates()) {
      Path normalized = manifest.toAbsolutePath().normalize();
      Path comparableManifest = canonicalPath(normalized);
      if (comparableManifest.startsWith(mavenRoot)
          || !comparableManifest.startsWith(repositoryRoot)) {
        continue;
      }
      String repositoryPath =
          repositoryRoot.relativize(comparableManifest).toString().replace('\\', '/');
      if (repositoryChanges.contains(repositoryPath)) {
        changedPaths.add(relativePath(normalized));
      }
    }
  }

  private Path canonicalPath(Path path) throws MojoExecutionException {
    Path absolute = path.toAbsolutePath().normalize();
    try {
      Path existing = absolute;
      List<Path> missingSegments = new ArrayList<>();
      while (existing != null && !Files.exists(existing)) {
        missingSegments.add(existing.getFileName());
        existing = existing.getParent();
      }
      if (existing == null) {
        return absolute;
      }
      Path canonical = existing.toRealPath();
      for (int index = missingSegments.size() - 1; index >= 0; index--) {
        canonical = canonical.resolve(missingSegments.get(index));
      }
      return canonical;
    } catch (IOException exception) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-008 unable to canonicalize Git path: " + absolute, exception);
    }
  }

  private List<Path> globalEntityManifestCandidates() {
    if (globalEntityManifest != null) {
      Path configured = globalEntityManifest.toPath();
      if (!configured.isAbsolute()) {
        configured = rootDirectory.toPath().resolve(configured);
      }
      return List.of(configured.toAbsolutePath().normalize());
    }
    return List.of(
        rootDirectory.toPath().resolve("business-pmo/global-entity-exceptions.json"),
        rootDirectory.toPath().resolve("../business-pmo/global-entity-exceptions.json"),
        rootDirectory.toPath().resolve("../mango-pmo/contracts/global-entity-exceptions.json"));
  }

  private String resolveDefaultBase() throws MojoExecutionException {
    for (String candidate : DEFAULT_GIT_BASES) {
      try {
        List<String> result = runGit("merge-base", "HEAD", candidate);
        if (!result.isEmpty()) {
          return result.get(0);
        }
      } catch (MojoExecutionException ignored) {
        getLog().debug("Git base candidate unavailable: " + candidate);
      }
    }
    List<String> parent = runGit("rev-parse", "HEAD^");
    if (parent.isEmpty()) {
      throw new MojoExecutionException("MANGO-ARCH-ENGINE-008 unable to resolve Git base");
    }
    return parent.get(0);
  }

  private List<String> runGit(String... arguments) throws MojoExecutionException {
    return runGitAt(rootDirectory.toPath().toAbsolutePath().normalize(), arguments);
  }

  private List<String> runGitAt(Path directory, String... arguments) throws MojoExecutionException {
    List<String> command = new ArrayList<>();
    command.add("git");
    command.addAll(List.of(arguments));
    ProcessBuilder builder =
        new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);
    try {
      Process process = builder.start();
      List<String> output;
      try (var reader = process.inputReader()) {
        output = reader.lines().toList();
      }
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new MojoExecutionException(
            "MANGO-ARCH-ENGINE-008 git command failed ("
                + exitCode
                + "): "
                + String.join(" ", command)
                + "\n"
                + String.join("\n", output));
      }
      return output;
    } catch (IOException exception) {
      throw new MojoExecutionException("MANGO-ARCH-ENGINE-008 unable to execute Git", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException("MANGO-ARCH-ENGINE-008 Git command interrupted", exception);
    }
  }

  private String relativePath(Path path) {
    return rootDirectory
        .toPath()
        .toAbsolutePath()
        .normalize()
        .relativize(path.toAbsolutePath().normalize())
        .toString()
        .replace('\\', '/');
  }

  private void collectJavaInputs(
      MavenProject reactorProject,
      Map<Path, ModuleRole> classDirectories,
      Map<Path, String> classDirectoryArtifacts,
      Map<Path, String> classDirectoryModules,
      List<Path> sourceDirectories)
      throws MojoExecutionException {
    if (excludedModules.contains(reactorProject.getArtifactId())) {
      return;
    }
    Set<Path> compileSourceDirectories =
        reactorProject.getCompileSourceRoots().stream()
            .map(Path::of)
            .map(path -> path.toAbsolutePath().normalize())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    String buildSourceDirectory = reactorProject.getBuild().getSourceDirectory();
    if (buildSourceDirectory != null && !buildSourceDirectory.isBlank()) {
      compileSourceDirectories.add(Path.of(buildSourceDirectory).toAbsolutePath().normalize());
    }
    Set<Path> javaSourceDirectories = new LinkedHashSet<>();
    for (Path sourceDirectory : compileSourceDirectories) {
      if (containsJava(sourceDirectory)) {
        javaSourceDirectories.add(sourceDirectory);
      }
    }
    if (javaSourceDirectories.isEmpty()) {
      return;
    }
    Path classDirectory =
        Path.of(reactorProject.getBuild().getOutputDirectory()).toAbsolutePath().normalize();
    if (!Files.isDirectory(classDirectory)) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-003 missing compiled classes for "
              + reactorProject.getArtifactId()
              + ": "
              + classDirectory);
    }
    sourceDirectories.addAll(javaSourceDirectories);
    classDirectories.put(classDirectory, ModuleRole.fromArtifactId(reactorProject.getArtifactId()));
    classDirectoryArtifacts.put(classDirectory, reactorProject.getArtifactId());
    classDirectoryModules.put(classDirectory, moduleKey(reactorProject));
  }

  private Set<Path> collectAuxiliaryClasspath(Collection<Path> reactorClassDirectories)
      throws MojoExecutionException {
    Set<Path> classpath = new LinkedHashSet<>();
    classpath.addAll(reactorClassDirectories);
    for (MavenProject reactorProject : session.getProjects()) {
      Set<Artifact> artifacts = reactorProject.getArtifacts();
      if (artifacts == null) {
        continue;
      }
      for (Artifact artifact : artifacts) {
        if (artifact.getFile() == null) {
          if (!"pom".equals(artifact.getType())) {
            throw new MojoExecutionException(
                "MANGO-ARCH-ENGINE-010 unresolved PMD classpath artifact: " + artifact);
          }
          continue;
        }
        Path artifactPath = artifact.getFile().toPath().toAbsolutePath().normalize();
        if (!Files.exists(artifactPath)) {
          throw new MojoExecutionException(
              "MANGO-ARCH-ENGINE-010 missing PMD classpath artifact: " + artifactPath);
        }
        classpath.add(artifactPath);
      }
    }
    return classpath;
  }

  private ClassOwnership collectClassOwnership(
      Map<Path, String> artifactDirectories, Map<Path, String> moduleDirectories)
      throws MojoExecutionException {
    Map<String, String> artifacts = new LinkedHashMap<>();
    Map<String, String> modules = new LinkedHashMap<>();
    for (Map.Entry<Path, String> entry : artifactDirectories.entrySet()) {
      String moduleKey = moduleDirectories.get(entry.getKey());
      if (moduleKey == null || moduleKey.isBlank()) {
        throw new MojoExecutionException(
            "MANGO-ARCH-ENGINE-026 missing module ownership for class directory " + entry.getKey());
      }
      try (var files = Files.walk(entry.getKey())) {
        for (Path file :
            files
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".class"))
                .toList()) {
          String relative = entry.getKey().relativize(file).toString().replace('\\', '/');
          String className =
              relative.substring(0, relative.length() - ".class".length()).replace('/', '.');
          artifacts.put(className, entry.getValue());
          modules.put(className, moduleKey);
        }
      } catch (IOException exception) {
        throw new MojoExecutionException(
            "MANGO-ARCH-ENGINE-012 unable to map Reactor classes to artifacts", exception);
      }
    }
    return new ClassOwnership(Map.copyOf(artifacts), Map.copyOf(modules));
  }

  private Map<String, ModuleIdentity> loadModuleIdentities(Set<String> requiredDomains)
      throws MojoExecutionException {
    Map<String, ModuleIdentity> identities = new LinkedHashMap<>();
    for (MavenProject reactorProject : session.getAllProjects()) {
      if (!isStarterProject(reactorProject)) {
        continue;
      }
      String domain = ModuleRole.domainOf(reactorProject.getArtifactId());
      if (!requiredDomains.contains(domain)) {
        continue;
      }
      Path propertiesFile = modulePropertiesFile(reactorProject);
      if (propertiesFile == null) {
        continue;
      }
      registerModuleIdentity(identities, domain, readModuleIdentity(propertiesFile));
    }
    return identities;
  }

  private boolean isStarterProject(MavenProject reactorProject) {
    if (excludedModules.contains(reactorProject.getArtifactId())) {
      return false;
    }
    return ModuleRole.fromArtifactId(reactorProject.getArtifactId()) == ModuleRole.STARTER;
  }

  private Path modulePropertiesFile(MavenProject reactorProject) {
    String outputDirectory =
        reactorProject.getBuild() == null ? null : reactorProject.getBuild().getOutputDirectory();
    if (outputDirectory != null && !outputDirectory.isBlank()) {
      Path generated =
          Path.of(outputDirectory)
              .resolve("META-INF/mango/module.properties")
              .toAbsolutePath()
              .normalize();
      if (Files.isRegularFile(generated)) {
        return generated;
      }
    }
    Path source =
        reactorProject
            .getBasedir()
            .toPath()
            .resolve(MODULE_PROPERTIES)
            .toAbsolutePath()
            .normalize();
    if (Files.isRegularFile(source)) {
      return source;
    }
    return null;
  }

  private ModuleIdentity readModuleIdentity(Path propertiesFile) throws MojoExecutionException {
    Properties properties = new Properties();
    try (var input = Files.newInputStream(propertiesFile)) {
      properties.load(input);
    } catch (IOException exception) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-009 unable to read " + propertiesFile, exception);
    }
    String moduleName = properties.getProperty("module-name", "").trim();
    String modulePath = properties.getProperty("module-path", "").trim();
    if (moduleName.isEmpty() || modulePath.isEmpty()) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-009 module.properties requires module-name and"
              + " module-path: "
              + propertiesFile);
    }
    return new ModuleIdentity(moduleName, modulePath);
  }

  private void registerModuleIdentity(
      Map<String, ModuleIdentity> identities, String domain, ModuleIdentity identity)
      throws MojoExecutionException {
    ModuleIdentity existing = identities.putIfAbsent(domain, identity);
    if (existing != null && !existing.equals(identity)) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-009 conflicting module.properties for domain " + domain);
    }
  }

  private boolean containsJava(Path sourceDirectory) throws MojoExecutionException {
    if (!Files.isDirectory(sourceDirectory)) {
      return false;
    }
    try (var files = Files.walk(sourceDirectory)) {
      return files.anyMatch(
          path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"));
    } catch (IOException exception) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-005 unable to inspect source directory " + sourceDirectory, exception);
    }
  }

  private void writeReport(ArchitectureReport report) throws MojoExecutionException {
    try {
      Files.createDirectories(reportFile.toPath().toAbsolutePath().normalize().getParent());
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(reportFile, report);
    } catch (IOException exception) {
      throw new MojoExecutionException(
          "MANGO-ARCH-ENGINE-007 unable to write report " + reportFile, exception);
    }
  }

  private static Path toPath(File file) {
    if (file == null) {
      return null;
    }
    return file.toPath();
  }

  public record ArchitectureReport(
      int schemaVersion,
      List<ReportedReactorModule> modules,
      List<ReportedArchitectureIssue> dependencyIssues,
      List<ReportedArchitectureIssue> archUnitIssues,
      List<ReportedArchitectureIssue> pmdIssues,
      List<ReportedArchitectureIssue> blockingIssues,
      String mode,
      String inventoryScope,
      String issueInventory,
      int reactorProjectCount,
      int expectedProjectCount,
      long durationMillis) {

    public int issueCount() {
      return blockingIssues.size();
    }
  }

  public record ReportedReactorModule(String moduleKey, String groupId, String artifactId) {}

  public record ReportedArchitectureIssue(
      String ruleId, String subject, String message, String moduleKey) {}

  private record ModuleIdentity(String moduleName, String modulePath) {}

  private record ReactorModuleDescriptor(
      String moduleKey, String groupId, String artifactId, Path baseDirectory) {

    ReportedReactorModule reportedModule() {
      return new ReportedReactorModule(moduleKey, groupId, artifactId);
    }
  }

  private record ClassOwnership(Map<String, String> artifacts, Map<String, String> modules) {}

  private record ReactorInputs(
      List<ArchitectureIssue> dependencyIssues,
      Map<Path, ModuleRole> classDirectories,
      Map<Path, String> classDirectoryArtifacts,
      Map<Path, String> classDirectoryModules,
      Set<Path> contractContextDirectories,
      List<Path> sourceDirectories) {}
}
