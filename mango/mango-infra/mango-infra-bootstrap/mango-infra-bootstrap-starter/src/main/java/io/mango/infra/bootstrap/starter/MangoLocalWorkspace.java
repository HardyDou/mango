package io.mango.infra.bootstrap.starter;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class MangoLocalWorkspace {

    private static final int URL_AUTHORITY_OFFSET = 3;
    private static final String SOURCE_NAME = "mangoLocalWorkspace";
    private static final Pattern WORKSPACE_DATABASE = Pattern.compile("mango_dev_[A-Za-z0-9_]+");

    private MangoLocalWorkspace() {
    }

    static Map<String, Object> loadProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        Path envFile = findWorkspaceEnv();
        if (envFile != null) {
            properties.putAll(readEnvFile(envFile));
        }
        if (!hasConfiguredValue("MANGO_DB_URL", properties)) {
            String host = configuredValue("MANGO_DB_HOST", properties, "127.0.0.1");
            String port = configuredValue("MANGO_DB_PORT", properties, "3306");
            String database = configuredValue("MANGO_DB_NAME", properties, "");
            if (!database.isBlank()) {
                properties.put("MANGO_DB_URL", "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useUnicode=true&characterEncoding=utf8&useSSL=false"
                        + "&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai");
            }
        }
        return properties;
    }

    static void addPropertySource(ConfigurableEnvironment environment, Map<String, Object> properties) {
        MutablePropertySources sources = environment.getPropertySources();
        if (sources.contains(SOURCE_NAME)) {
            sources.remove(SOURCE_NAME);
        }
        String anchor = sources.contains("systemEnvironment") ? "systemEnvironment" : "systemProperties";
        sources.addAfter(anchor, new MapPropertySource(SOURCE_NAME, properties));
    }

    static void validateDatabase(Map<String, Object> properties, String[] springArguments) {
        String database = configuredValue("MANGO_DB_NAME", properties, "");
        if (!WORKSPACE_DATABASE.matcher(database).matches()) {
            throw new IllegalStateException(
                    "MANGO_LOCAL_DATABASE_REJECTED: expected a database named mango_dev_*, actual=" + database);
        }
        String configuredUrl = configuredValue("MANGO_DB_URL", properties, "");
        assertDatabaseUrl(database, configuredUrl);
        for (String argument : springArguments) {
            if (argument.startsWith("--spring.datasource.url=")) {
                assertDatabaseUrl(database, argument.substring("--spring.datasource.url=".length()));
            }
        }
    }

    static void prepareDatabase(Map<String, Object> properties) {
        if (!Boolean.parseBoolean(configuredValue("MANGO_DB_AUTO_CREATE", properties, "false"))) {
            return;
        }
        String database = configuredValue("MANGO_DB_NAME", properties, "");
        String host = configuredValue("MANGO_DB_HOST", properties, "127.0.0.1");
        String port = configuredValue("MANGO_DB_PORT", properties, "3306");
        String username = configuredValue("MANGO_DB_USERNAME", properties, "root");
        String password = configuredValue("MANGO_DB_PASSWORD", properties, "");
        String url = "jdbc:mysql://" + host + ":" + port + "/?useUnicode=true&characterEncoding=utf8"
                + "&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        String escapedDatabase = database.replace("`", "``");
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + escapedDatabase
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException exception) {
            throw new IllegalStateException("MANGO_LOCAL_DATABASE_CREATE_FAILED: database=" + database, exception);
        }
    }

    static String[] additionalArguments(Map<String, Object> properties) {
        String value = configuredValue("MANGO_BACKEND_ADDITIONAL_ARGS", properties, "").trim();
        if (value.isEmpty()) {
            return new String[0];
        }
        return tokenize(value).toArray(String[]::new);
    }

    private static Path findWorkspaceEnv() {
        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(".mango/dev-workspace.env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private static Map<String, Object> readEnvFile(Path path) {
        Map<String, Object> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if ((value.startsWith("'") && value.endsWith("'"))
                        || (value.startsWith("\"") && value.endsWith("\""))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
            return values;
        } catch (IOException exception) {
            throw new IllegalStateException("MANGO_LOCAL_WORKSPACE_ENV_READ_FAILED: path=" + path, exception);
        }
    }

    private static void assertDatabaseUrl(String expectedDatabase, String url) {
        String actualDatabase = databaseFromUrl(url);
        if (!expectedDatabase.equals(actualDatabase)) {
            throw new IllegalStateException(
                    "MANGO_LOCAL_DATABASE_REJECTED: datasource must target " + expectedDatabase
                            + ", actual=" + (actualDatabase.isBlank() ? url : actualDatabase));
        }
    }

    private static String databaseFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return "";
        }
        int authorityEnd = url.indexOf('/', schemeEnd + URL_AUTHORITY_OFFSET);
        if (authorityEnd < 0) {
            return "";
        }
        int queryStart = url.indexOf('?', authorityEnd + 1);
        String database = url.substring(authorityEnd + 1, queryStart < 0 ? url.length() : queryStart);
        return database.contains("/") ? "" : database;
    }

    private static boolean hasConfiguredValue(String key, Map<String, Object> properties) {
        return System.getProperty(key) != null || System.getenv(key) != null
                || (properties.containsKey(key) && !String.valueOf(properties.get(key)).isBlank());
    }

    private static String configuredValue(String key, Map<String, Object> properties, String fallback) {
        String system = System.getProperty(key);
        if (system != null) {
            return system;
        }
        String environment = System.getenv(key);
        if (environment != null) {
            return environment;
        }
        Object configured = properties.get(key);
        return configured == null ? fallback : String.valueOf(configured);
    }

    private static List<String> tokenize(String value) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        char quote = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (quote != 0) {
                if (character == quote) {
                    quote = 0;
                } else {
                    token.append(character);
                }
            } else if (character == '\'' || character == '"') {
                quote = character;
            } else if (Character.isWhitespace(character)) {
                if (!token.isEmpty()) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
            } else {
                token.append(character);
            }
        }
        if (quote != 0) {
            throw new IllegalArgumentException("MANGO_BACKEND_ADDITIONAL_ARGS has an unterminated quote");
        }
        if (!token.isEmpty()) {
            tokens.add(token.toString());
        }
        return List.copyOf(tokens);
    }
}
