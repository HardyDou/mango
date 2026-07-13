package io.mango.architecture;

import java.util.List;
import java.util.Locale;

/** Architectural role inferred from a Maven artifact id, never from an absolute path. */
public enum ModuleRole {
    /** Public API contract module. */
    API,
    /** Domain implementation module. */
    CORE,
    /** Framework-neutral shared support module. */
    SUPPORT,
    /** Remote client adapter module. */
    STARTER_REMOTE,
    /** Runtime adapter module. */
    STARTER,
    /** Deployable application module. */
    APP,
    /** Module without a governed suffix. */
    OTHER;

    private static final String STARTER_REMOTE_SUFFIX = "-starter-remote";
    private static final String STARTER_SUFFIX = "-starter";
    private static final String SUPPORT_SUFFIX = "-support";
    private static final String CORE_SUFFIX = "-core";
    private static final String API_SUFFIX = "-api";
    private static final String APP_SUFFIX = "-app";
    private static final List<String> DOMAIN_SUFFIXES =
            List.of(
                    STARTER_REMOTE_SUFFIX,
                    STARTER_SUFFIX,
                    SUPPORT_SUFFIX,
                    CORE_SUFFIX,
                    API_SUFFIX,
                    APP_SUFFIX);

    public static ModuleRole fromArtifactId(String artifactId) {
        String value = normalizeArtifactId(artifactId);
        if (value.endsWith(STARTER_REMOTE_SUFFIX)) {
            return STARTER_REMOTE;
        }
        if (value.endsWith(API_SUFFIX)) {
            return API;
        }
        if (value.endsWith(CORE_SUFFIX)) {
            return CORE;
        }
        if (value.endsWith(SUPPORT_SUFFIX)) {
            return SUPPORT;
        }
        if (value.endsWith(STARTER_SUFFIX)) {
            return STARTER;
        }
        if (value.endsWith(APP_SUFFIX)) {
            return APP;
        }
        return OTHER;
    }

    public static String domainOf(String artifactId) {
        String value = artifactId;
        if (value == null) {
            value = "";
        }
        for (String suffix : DOMAIN_SUFFIXES) {
            if (value.endsWith(suffix)) {
                return value.substring(0, value.length() - suffix.length());
            }
        }
        return value;
    }

    private static String normalizeArtifactId(String artifactId) {
        if (artifactId == null) {
            return "";
        }
        return artifactId.toLowerCase(Locale.ROOT);
    }
}
