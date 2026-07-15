package io.mango.calendar.starter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarResourceDeclarationContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void flywayV1_containsDdlOnly() throws IOException {
        String sql = readText("db/migration/calendar/V1__init_calendar.sql");
        String normalized = sql.toUpperCase(java.util.Locale.ROOT);

        assertThat(normalized).contains("CREATE TABLE IF NOT EXISTS `CALENDAR`")
                .contains("CREATE TABLE IF NOT EXISTS `CALENDAR_DAY`")
                .doesNotContain("INSERT INTO")
                .doesNotContain("UPDATE `")
                .doesNotContain("DELETE FROM");
    }

    @Test
    void requiredAndDemoDeclarations_areSeparatedAndComplete() throws IOException {
        JsonNode required = readJson("META-INF/mango/resources/calendar-common-definition.json");
        JsonNode demo = readJson("META-INF/mango/demo/calendar-demo-cn-standard-years.json");

        JsonNode definitions = required.at("/mango/resource/declarations/CALENDAR_DEFINITION");
        JsonNode years = demo.at("/mango/resource/declarations/CALENDAR_YEAR");
        assertThat(definitions).hasSize(1);
        assertThat(definitions.get(0).path("syncMode").asText()).isEqualTo("INIT_ONLY");
        assertThat(definitions.get(0).at("/fields/calendarCode/value").asText()).isEqualTo("CN_STANDARD");
        assertThat(years).hasSize(2);
        assertThat(years.get(0).at("/fields/year/value").asInt()).isEqualTo(2025);
        assertThat(years.get(0).at("/fields/items/value")).hasSize(33);
        assertThat(years.get(1).at("/fields/year/value").asInt()).isEqualTo(2026);
        assertThat(years.get(1).at("/fields/items/value")).hasSize(38);
    }

    private JsonNode readJson(String path) throws IOException {
        try (InputStream input = requiredResource(path)) {
            return objectMapper.readTree(input);
        }
    }

    private String readText(String path) throws IOException {
        try (InputStream input = requiredResource(path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private InputStream requiredResource(String path) {
        InputStream input = getClass().getClassLoader().getResourceAsStream(path);
        if (input == null) {
            throw new IllegalStateException("Missing classpath resource: " + path);
        }
        return input;
    }
}
