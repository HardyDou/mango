package io.mango.plugin.architecture;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.maven.plugin.MojoExecutionException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads exact, approved exceptions for non-tenant global entities and tables. */
public final class GlobalEntityManifestLoader {

    private static final String CONTRACT_ID = "global-entity-exceptions";
    private static final int SCHEMA_REVISION = 1;
    private static final int CONTRACT_VERSION = 1;
    private static final int MIN_REASON_LENGTH = 10;

    private GlobalEntityManifestLoader() {}

    public static Map<String, String> load(Path mavenRoot, Path configuredManifest)
            throws MojoExecutionException {
        Path manifest = resolve(mavenRoot, configuredManifest);
        if (manifest == null) {
            return Map.of();
        }
        try {
            Manifest parsed = new ObjectMapper().readValue(manifest.toFile(), Manifest.class);
            if (!CONTRACT_ID.equals(parsed.contractId())
                    || parsed.schemaRevision() != SCHEMA_REVISION
                    || parsed.version() != CONTRACT_VERSION
                    || parsed.exceptions() == null) {
                throw invalid(
                        "requires contractId=global-entity-exceptions, schemaRevision=1, "
                                + "version=1 and exceptions[]: "
                                + manifest);
            }
            Map<String, String> tables = new LinkedHashMap<>();
            for (ExceptionEntry entry : parsed.exceptions()) {
                validate(entry, manifest);
                String existing = tables.putIfAbsent(entry.entity(), entry.table());
                if (existing != null) {
                    throw invalid("duplicate entity: " + entry.entity());
                }
            }
            return Map.copyOf(tables);
        } catch (MojoExecutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MojoExecutionException(
                    "MANGO-ARCH-ENGINE-014 invalid global entity manifest: " + manifest, exception);
        }
    }

    private static Path resolve(Path mavenRoot, Path configuredManifest)
            throws MojoExecutionException {
        if (configuredManifest != null) {
            Path configured = configuredManifest.toAbsolutePath().normalize();
            if (!Files.isRegularFile(configured)) {
                throw invalid("configured manifest does not exist: " + configured);
            }
            return configured;
        }
        for (Path candidate :
                List.of(
                        mavenRoot.resolve("business-pmo/global-entity-exceptions.json"),
                        mavenRoot.resolve("../business-pmo/global-entity-exceptions.json"),
                        mavenRoot.resolve(
                                "../mango-pmo/contracts/global-entity-exceptions.json"))) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private static void validate(ExceptionEntry entry, Path manifest)
            throws MojoExecutionException {
        validateRequiredFields(entry, manifest);
        if (entry.reason().trim().length() < MIN_REASON_LENGTH) {
            throw invalid("reason is too short: " + entry.entity());
        }
        validateExpiry(entry);
    }

    private static void validateRequiredFields(ExceptionEntry entry, Path manifest)
            throws MojoExecutionException {
        if (entry == null) {
            throw invalid(
                    "entry requires entity, table, owner, reason, approvalRef, approvedBy and"
                            + " expiresOn: "
                            + manifest);
        }
        String[] requiredFields = {
            entry.entity(),
            entry.table(),
            entry.owner(),
            entry.reason(),
            entry.approvalRef(),
            entry.approvedBy(),
            entry.expiresOn()
        };
        for (String requiredField : requiredFields) {
            if (isBlank(requiredField)) {
                throw invalid(
                        "entry requires entity, table, owner, reason, approvalRef, approvedBy and"
                                + " expiresOn: "
                                + manifest);
            }
        }
    }

    private static void validateExpiry(ExceptionEntry entry) throws MojoExecutionException {
        LocalDate expiry;
        try {
            expiry = LocalDate.parse(entry.expiresOn());
        } catch (RuntimeException invalidDate) {
            throw invalid("invalid expiresOn for " + entry.entity(), invalidDate);
        }
        if (expiry.isBefore(LocalDate.now())) {
            throw invalid("expired exception: " + entry.entity());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static MojoExecutionException invalid(String message) {
        return new MojoExecutionException(
                "MANGO-ARCH-ENGINE-014 global entity exception " + message);
    }

    private static MojoExecutionException invalid(String message, Throwable cause) {
        return new MojoExecutionException(
                "MANGO-ARCH-ENGINE-014 global entity exception " + message, cause);
    }

    private record Manifest(
            String contractId, int schemaRevision, int version, List<ExceptionEntry> exceptions) {}

    private record ExceptionEntry(
            String entity,
            String table,
            String owner,
            String reason,
            String approvalRef,
            String approvedBy,
            String expiresOn) {}
}
