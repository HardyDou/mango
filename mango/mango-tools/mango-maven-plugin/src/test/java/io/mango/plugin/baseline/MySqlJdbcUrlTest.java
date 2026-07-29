package io.mango.plugin.baseline;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MySqlJdbcUrlTest {

    @Test
    void replacesOnlyDatabaseAndPreservesParameters() throws Exception {
        MySqlJdbcUrl url = MySqlJdbcUrl.parse(
                "jdbc:mysql://127.0.0.1:3306/mysql?useSSL=false&serverTimezone=Asia/Shanghai");

        assertEquals(
                "jdbc:mysql://127.0.0.1:3306/mango_baseline?useSSL=false&serverTimezone=Asia/Shanghai",
                url.database("mango_baseline"));
    }

    @Test
    void rejectsNonMysqlAndMultiHostUrls() {
        assertThrows(MojoExecutionException.class,
                () -> MySqlJdbcUrl.parse("jdbc:postgresql://127.0.0.1:5432/postgres"));
        assertThrows(MojoExecutionException.class,
                () -> MySqlJdbcUrl.parse("jdbc:mysql:loadbalance://host-a,host-b/mysql"));
        assertThrows(MojoExecutionException.class,
                () -> MySqlJdbcUrl.parse("jdbc:mysql://host-a,host-b/mysql"));
    }
}
