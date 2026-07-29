package io.mango.plugin.baseline;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

record MySqlJdbcUrl(String serverPrefix, String parameters) {

    private static final Pattern URL = Pattern.compile("^(jdbc:mysql://[^,/?#]+)(?:/[^?]*)?(\\?.*)?$");

    static MySqlJdbcUrl parse(String jdbcUrl) throws MojoExecutionException {
        Matcher matcher = URL.matcher(jdbcUrl == null ? "" : jdbcUrl.trim());
        if (!matcher.matches()) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-013 jdbcUrl must be a direct jdbc:mysql://host:port/database URL");
        }
        return new MySqlJdbcUrl(matcher.group(1), matcher.group(2) == null ? "" : matcher.group(2));
    }

    String database(String database) {
        return serverPrefix + "/" + database + parameters;
    }
}
