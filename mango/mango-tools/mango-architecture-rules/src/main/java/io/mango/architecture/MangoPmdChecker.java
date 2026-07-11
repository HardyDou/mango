package io.mango.architecture;

import io.mango.architecture.pmd.MangoJavaArchitectureRule;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.reporting.Report;

/** Programmatic PMD 7 runner that fails closed on parser and configuration errors. */
public final class MangoPmdChecker {

    public List<ArchitectureIssue> check(Collection<Path> sourceDirectories, String javaVersion) {
        return check(sourceDirectories, javaVersion, List.of());
    }

    public List<ArchitectureIssue> check(
            Collection<Path> sourceDirectories,
            String javaVersion,
            Collection<Path> auxiliaryClasspath) {
        if (sourceDirectories == null || sourceDirectories.isEmpty()) {
            throw new IllegalStateException("MANGO-ARCH-ENGINE-005 no Java source directories configured");
        }
        PMDConfiguration configuration = new PMDConfiguration();
        configuration.setDefaultLanguageVersion(
                LanguageRegistry.PMD.getLanguageVersionById("java", normalizeJavaVersion(javaVersion)));
        configuration.setIgnoreIncrementalAnalysis(true);
        if (auxiliaryClasspath != null && !auxiliaryClasspath.isEmpty()) {
            configuration.prependAuxClasspath(auxiliaryClasspath.stream()
                    .map(path -> path.toAbsolutePath().normalize().toString())
                    .collect(Collectors.joining(File.pathSeparator)));
        }
        try (PmdAnalysis analysis = PmdAnalysis.create(configuration)) {
            analysis.addRuleSet(RuleSet.forSingleRule(new MangoJavaArchitectureRule()));
            for (Path sourceDirectory : sourceDirectories) {
                analysis.files().addDirectory(sourceDirectory.toAbsolutePath().normalize(), true);
            }
            Report report = analysis.performAnalysisAndCollectReport();
            if (!report.getConfigurationErrors().isEmpty() || !report.getProcessingErrors().isEmpty()) {
                String processingDetails = report.getProcessingErrors().stream()
                        .map(error -> error.getFileId().getOriginalPath() + ": " + error.getMsg())
                        .collect(Collectors.joining("; "));
                String configurationDetails = report.getConfigurationErrors().stream()
                        .map(error -> error.rule().getName() + ": " + error.issue())
                        .collect(Collectors.joining("; "));
                throw new IllegalStateException(
                        "MANGO-ARCH-ENGINE-006 PMD analysis failed: configurationErrors="
                                + report.getConfigurationErrors().size() + ", processingErrors="
                                + report.getProcessingErrors().size() + ", details="
                                + configurationDetails + processingDetails);
            }
            List<ArchitectureIssue> issues = new ArrayList<>();
            report.getViolations().forEach(violation -> {
                String description = violation.getDescription();
                int separator = description.indexOf(' ');
                String ruleId = separator > 0 ? description.substring(0, separator) : description;
                String subject = violation.getFileId().getOriginalPath() + ":" + violation.getBeginLine();
                issues.add(new ArchitectureIssue(ruleId, subject, description));
            });
            issues.sort(Comparator.comparing(ArchitectureIssue::ruleId)
                    .thenComparing(ArchitectureIssue::subject));
            return List.copyOf(issues);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("MANGO-ARCH-ENGINE-006 PMD analysis failed", exception);
        }
    }

    private String normalizeJavaVersion(String javaVersion) {
        if (javaVersion == null || javaVersion.isBlank()) {
            return "17";
        }
        String normalized = javaVersion.trim();
        return normalized.startsWith("1.") ? normalized.substring(2) : normalized;
    }
}
