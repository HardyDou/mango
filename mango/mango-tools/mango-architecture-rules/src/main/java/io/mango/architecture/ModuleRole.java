package io.mango.architecture;

import java.util.Locale;

/** Architectural role inferred from a Maven artifact id, never from an absolute path. */
public enum ModuleRole {
    API,
    CORE,
    SUPPORT,
    STARTER_REMOTE,
    STARTER,
    OTHER;

    public static ModuleRole fromArtifactId(String artifactId) {
        String value = artifactId == null ? "" : artifactId.toLowerCase(Locale.ROOT);
        if (value.endsWith("-starter-remote")) {
            return STARTER_REMOTE;
        }
        if (value.endsWith("-api")) {
            return API;
        }
        if (value.endsWith("-core")) {
            return CORE;
        }
        if (value.endsWith("-support")) {
            return SUPPORT;
        }
        if (value.endsWith("-starter")) {
            return STARTER;
        }
        return OTHER;
    }

    public static String domainOf(String artifactId) {
        String value = artifactId == null ? "" : artifactId;
        for (String suffix : new String[] {"-starter-remote", "-starter", "-support", "-core", "-api"}) {
            if (value.endsWith(suffix)) {
                return value.substring(0, value.length() - suffix.length());
            }
        }
        return value;
    }
}
