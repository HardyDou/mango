package io.mango.architecture;

import io.mango.architecture.pmd.MangoJavaArchitectureRule;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.reporting.Report;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Programmatic PMD 7 runner that fails closed on parser and configuration errors. */
public final class MangoPmdChecker {

    private static final String LEGACY_VERSION_PREFIX = "1.";

    public List<ArchitectureIssue> check(Collection<Path> sourceDirectories, String javaVersion) {
        return check(sourceDirectories, javaVersion, List.of());
    }

    public List<ArchitectureIssue> check(
            Collection<Path> sourceDirectories,
            String javaVersion,
            Collection<Path> auxiliaryClasspath) {
        if (sourceDirectories == null || sourceDirectories.isEmpty()) {
            throw new IllegalStateException(
                    "MANGO-ARCH-ENGINE-005 no Java source directories configured");
        }
        PMDConfiguration configuration = new PMDConfiguration();
        configuration.setDefaultLanguageVersion(
                LanguageRegistry.PMD.getLanguageVersionById(
                        "java", normalizeJavaVersion(javaVersion)));
        configuration.setIgnoreIncrementalAnalysis(true);
        if (auxiliaryClasspath != null && !auxiliaryClasspath.isEmpty()) {
            configuration.prependAuxClasspath(
                    auxiliaryClasspath.stream()
                            .map(path -> path.toAbsolutePath().normalize().toString())
                            .collect(Collectors.joining(File.pathSeparator)));
        }
        try (PmdAnalysis analysis = PmdAnalysis.create(configuration)) {
            analysis.addRuleSet(RuleSet.forSingleRule(new MangoJavaArchitectureRule()));
            for (Path sourceDirectory : sourceDirectories) {
                analysis.files().addDirectory(sourceDirectory.toAbsolutePath().normalize(), true);
            }
            Report report = analysis.performAnalysisAndCollectReport();
            if (!report.getConfigurationErrors().isEmpty()
                    || !report.getProcessingErrors().isEmpty()) {
                String processingDetails =
                        report.getProcessingErrors().stream()
                                .map(
                                        error ->
                                                error.getFileId().getOriginalPath()
                                                        + ": "
                                                        + error.getMsg())
                                .collect(Collectors.joining("; "));
                String configurationDetails =
                        report.getConfigurationErrors().stream()
                                .map(error -> error.rule().getName() + ": " + error.issue())
                                .collect(Collectors.joining("; "));
                throw new IllegalStateException(
                        "MANGO-ARCH-ENGINE-006 PMD analysis failed: configurationErrors="
                                + report.getConfigurationErrors().size()
                                + ", processingErrors="
                                + report.getProcessingErrors().size()
                                + ", details="
                                + configurationDetails
                                + processingDetails);
            }
            List<ArchitectureIssue> issues = new ArrayList<>();
            report.getViolations().forEach(violation -> addViolation(issues, violation, false));
            report.getSuppressedViolations()
                    .forEach(
                            suppressed ->
                                    addViolation(issues, suppressed.getRuleViolation(), true));
            issues.sort(
                    Comparator.comparing(ArchitectureIssue::ruleId)
                            .thenComparing(ArchitectureIssue::subject));
            return List.copyOf(issues);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("MANGO-ARCH-ENGINE-006 PMD analysis failed", exception);
        }
    }

    private void addViolation(
            List<ArchitectureIssue> issues,
            net.sourceforge.pmd.reporting.RuleViolation violation,
            boolean suppressed) {
        String description = violation.getDescription();
        String ruleId = parseRuleId(description);
        String subject = violation.getFileId().getOriginalPath() + ":" + violation.getBeginLine();
        String message = violationMessage(description, suppressed);
        issues.add(new ArchitectureIssue(ruleId, subject, message));
    }

    private String parseRuleId(String description) {
        int separator = description.indexOf(' ');
        if (separator > 0) {
            return description.substring(0, separator);
        }
        return description;
    }

    private String violationMessage(String description, boolean suppressed) {
        if (suppressed) {
            return description + " (PMD suppression is forbidden for Mango architecture rules)";
        }
        return description;
    }

    private String normalizeJavaVersion(String javaVersion) {
        if (javaVersion == null || javaVersion.isBlank()) {
            return "17";
        }
        String normalized = javaVersion.trim();
        if (normalized.startsWith(LEGACY_VERSION_PREFIX)) {
            return normalized.substring(LEGACY_VERSION_PREFIX.length());
        }
        return normalized;
    }
}
