package io.mango.plugin.baseline;

import org.apache.maven.plugin.MojoExecutionException;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BaselineObjectOwnership {

    private static final char SINGLE_QUOTE_CHARACTER = '\'';
    private static final char DOUBLE_QUOTE_CHARACTER = '"';
    private static final char HASH_CHARACTER = '#';
    private static final char ESCAPE_CHARACTER = '\\';
    private static final char LINE_FEED_CHARACTER = '\n';
    private static final char ASTERISK_CHARACTER = '*';
    private static final char SLASH_CHARACTER = '/';
    private static final char DASH_CHARACTER = '-';
    private static final char END_OF_INPUT_CHARACTER = '\0';
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
            String module = migration.module();
            String previous = owners.putIfAbsent(objectName, module);
            if (previous == null || previous.equals(module)) {
                continue;
            }
            throw new MojoExecutionException(
                    "MANGO-BASELINE-012 cross-module " + objectType + " ownership conflict for "
                            + objectName + ": " + previous + " and " + module);
        }
    }

    private static String maskCommentsAndLiterals(String sql) {
        return new SqlMasker(sql).mask();
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private enum State {
        /** Executable SQL text. */
        SQL,
        /** Single-quoted string literal. */
        SINGLE_QUOTE,
        /** Double-quoted string literal. */
        DOUBLE_QUOTE,
        /** Hash or double-dash line comment. */
        LINE_COMMENT,
        /** Slash-star block comment. */
        BLOCK_COMMENT
    }

    private static final class SqlMasker {

        private final String sql;
        private final StringBuilder masked;
        private State state = State.SQL;
        private int index;

        private SqlMasker(String sql) {
            this.sql = sql;
            this.masked = new StringBuilder(sql.length());
        }

        private String mask() {
            while (index < sql.length()) {
                if (state == State.SQL) {
                    maskSql();
                } else if (state == State.SINGLE_QUOTE) {
                    maskQuoted(SINGLE_QUOTE_CHARACTER);
                } else if (state == State.DOUBLE_QUOTE) {
                    maskQuoted(DOUBLE_QUOTE_CHARACTER);
                } else if (state == State.LINE_COMMENT) {
                    maskLineComment();
                } else if (state == State.BLOCK_COMMENT) {
                    maskBlockComment();
                } else {
                    throw new IllegalStateException("Unknown SQL masking state: " + state);
                }
                index++;
            }
            return masked.toString();
        }

        private void maskSql() {
            char current = current();
            if (current == SINGLE_QUOTE_CHARACTER) {
                beginMaskedState(State.SINGLE_QUOTE);
            } else if (current == DOUBLE_QUOTE_CHARACTER) {
                beginMaskedState(State.DOUBLE_QUOTE);
            } else if (isBlockCommentStart()) {
                beginTwoCharacterMaskedState(State.BLOCK_COMMENT);
            } else if (current == HASH_CHARACTER) {
                beginMaskedState(State.LINE_COMMENT);
            } else if (isDashCommentStart()) {
                beginTwoCharacterMaskedState(State.LINE_COMMENT);
            } else {
                masked.append(current);
            }
        }

        private void maskQuoted(char quote) {
            char current = current();
            appendMasked(current);
            if (current == ESCAPE_CHARACTER && hasNext()) {
                masked.append(' ');
                index++;
            } else if (current == quote && next() == quote) {
                masked.append(' ');
                index++;
            } else if (current == quote) {
                state = State.SQL;
            }
        }

        private void maskLineComment() {
            char current = current();
            appendMasked(current);
            if (current == LINE_FEED_CHARACTER) {
                state = State.SQL;
            }
        }

        private void maskBlockComment() {
            if (current() == ASTERISK_CHARACTER && next() == SLASH_CHARACTER) {
                masked.append("  ");
                index++;
                state = State.SQL;
            } else {
                appendMasked(current());
            }
        }

        private void beginMaskedState(State nextState) {
            state = nextState;
            masked.append(' ');
        }

        private void beginTwoCharacterMaskedState(State nextState) {
            state = nextState;
            masked.append("  ");
            index++;
        }

        private boolean isBlockCommentStart() {
            return current() == SLASH_CHARACTER && next() == ASTERISK_CHARACTER;
        }

        private boolean isDashCommentStart() {
            if (current() != DASH_CHARACTER || next() != DASH_CHARACTER) {
                return false;
            }
            int followingIndex = index + 2;
            return followingIndex >= sql.length()
                    || Character.isWhitespace(sql.charAt(followingIndex));
        }

        private void appendMasked(char current) {
            masked.append(current == LINE_FEED_CHARACTER ? LINE_FEED_CHARACTER : ' ');
        }

        private char current() {
            return sql.charAt(index);
        }

        private char next() {
            return hasNext() ? sql.charAt(index + 1) : END_OF_INPUT_CHARACTER;
        }

        private boolean hasNext() {
            return index + 1 < sql.length();
        }
    }
}
