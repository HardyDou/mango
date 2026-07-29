package io.mango.plugin.baseline;

import org.apache.maven.plugin.MojoExecutionException;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BaselineObjectOwnership {

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?is)\\bcreate\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?"
                    + "(?:`?[a-zA-Z0-9_]+`?\\s*\\.\\s*)?`?([a-zA-Z0-9_]+)`?");
    private static final Pattern CREATE_VIEW = Pattern.compile(
            "(?is)\\bcreate\\s+(?:or\\s+replace\\s+)?(?:algorithm\\s*=\\s*\\w+\\s+)?"
                    + "(?:definer\\s*=\\s*[^\\s]+\\s+)?(?:sql\\s+security\\s+\\w+\\s+)?view\\s+"
                    + "(?:`?[a-zA-Z0-9_]+`?\\s*\\.\\s*)?`?([a-zA-Z0-9_]+)`?");

    private final Map<String, String> tableOwners;
    private final Map<String, String> viewOwners;

    private BaselineObjectOwnership(
            Map<String, String> tableOwners,
            Map<String, String> viewOwners) {
        this.tableOwners = Map.copyOf(tableOwners);
        this.viewOwners = Map.copyOf(viewOwners);
    }

    static BaselineObjectOwnership analyze(BaselineMigrationCatalog catalog)
            throws MojoExecutionException {
        Map<String, String> tables = new LinkedHashMap<>();
        Map<String, String> views = new LinkedHashMap<>();
        for (BaselineMigrationCatalog.MigrationResource migration : catalog.allMigrations()) {
            String sql = new String(migration.content(), StandardCharsets.UTF_8);
            String masked = maskCommentsAndLiterals(sql);
            collect(CREATE_TABLE, masked, migration, tables, "table");
            collect(CREATE_VIEW, masked, migration, views, "view");
        }
        if (tables.isEmpty()) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-011 migrations do not declare any CREATE TABLE ownership");
        }
        return new BaselineObjectOwnership(tables, views);
    }

    String tableOwner(String table) {
        return tableOwners.get(normalize(table));
    }

    String viewOwner(String view) {
        return viewOwners.get(normalize(view));
    }

    Map<String, String> tables() {
        return tableOwners;
    }

    Map<String, String> views() {
        return viewOwners;
    }

    private static void collect(
            Pattern pattern,
            String maskedSql,
            BaselineMigrationCatalog.MigrationResource migration,
            Map<String, String> owners,
            String objectType) throws MojoExecutionException {
        Matcher matcher = pattern.matcher(maskedSql);
        while (matcher.find()) {
            String objectName = normalize(matcher.group(1));
            String previous = owners.putIfAbsent(objectName, migration.module());
            if (previous != null && !previous.equals(migration.module())) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-012 cross-module " + objectType + " ownership conflict for "
                                + objectName + ": " + previous + " and " + migration.module());
            }
        }
    }

    private static String maskCommentsAndLiterals(String sql) {
        StringBuilder masked = new StringBuilder(sql.length());
        State state = State.SQL;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
            switch (state) {
                case SQL -> {
                    if (current == '\'') {
                        state = State.SINGLE_QUOTE;
                        masked.append(' ');
                    } else if (current == '"') {
                        state = State.DOUBLE_QUOTE;
                        masked.append(' ');
                    } else if (current == '/' && next == '*') {
                        state = State.BLOCK_COMMENT;
                        masked.append("  ");
                        index++;
                    } else if (current == '#') {
                        state = State.LINE_COMMENT;
                        masked.append(' ');
                    } else if (current == '-' && next == '-'
                            && (index + 2 >= sql.length() || Character.isWhitespace(sql.charAt(index + 2)))) {
                        state = State.LINE_COMMENT;
                        masked.append("  ");
                        index++;
                    } else {
                        masked.append(current);
                    }
                }
                case SINGLE_QUOTE -> {
                    masked.append(current == '\n' ? '\n' : ' ');
                    if (current == '\\' && index + 1 < sql.length()) {
                        masked.append(' ');
                        index++;
                    } else if (current == '\'' && next == '\'') {
                        masked.append(' ');
                        index++;
                    } else if (current == '\'') {
                        state = State.SQL;
                    }
                }
                case DOUBLE_QUOTE -> {
                    masked.append(current == '\n' ? '\n' : ' ');
                    if (current == '\\' && index + 1 < sql.length()) {
                        masked.append(' ');
                        index++;
                    } else if (current == '"' && next == '"') {
                        masked.append(' ');
                        index++;
                    } else if (current == '"') {
                        state = State.SQL;
                    }
                }
                case LINE_COMMENT -> {
                    masked.append(current == '\n' ? '\n' : ' ');
                    if (current == '\n') {
                        state = State.SQL;
                    }
                }
                case BLOCK_COMMENT -> {
                    if (current == '*' && next == '/') {
                        masked.append("  ");
                        index++;
                        state = State.SQL;
                    } else {
                        masked.append(current == '\n' ? '\n' : ' ');
                    }
                }
                default -> throw new IllegalStateException("Unknown SQL masking state: " + state);
            }
        }
        return masked.toString();
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private enum State {
        SQL,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        LINE_COMMENT,
        BLOCK_COMMENT
    }
}
