package io.mango.infra.web.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebAutoConfigurationTest {

    @Test
    void longValuesShouldSerializeAsStringAndDeserializeFromString() throws Exception {
        Jackson2ObjectMapperBuilderCustomizer customizer = new WebAutoConfiguration(new MangoWebProperties())
                .mangoLongToStringJacksonCustomizer();
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        customizer.customize(builder);
        ObjectMapper objectMapper = builder.build();

        String json = objectMapper.writeValueAsString(new LongPayload(
                2055465090113392600L,
                1L,
                LocalDateTime.of(2026, 5, 17, 17, 29, 31)));

        assertEquals("{\"id\":\"2055465090113392600\",\"primitiveId\":\"1\",\"createdTime\":\"2026-05-17 17:29:31\"}", json);
        LongPayload payload = objectMapper.readValue(
                "{\"id\":\"2055465090113392600\",\"createdTime\":\"2026-05-17 17:29:31\"}",
                LongPayload.class);
        assertEquals(2055465090113392600L, payload.id);
        assertEquals(LocalDateTime.of(2026, 5, 17, 17, 29, 31), payload.createdTime);
    }

    @Test
    void existingObjectMapperBeanShouldSerializeLongValuesAsString() throws Exception {
        BeanPostProcessor processor = new WebAutoConfiguration(new MangoWebProperties())
                .mangoLongToStringObjectMapperPostProcessor();
        ObjectMapper objectMapper = (ObjectMapper) processor.postProcessAfterInitialization(new ObjectMapper(), "objectMapper");

        String json = objectMapper.writeValueAsString(new LongPayload(
                2055465090113392600L,
                1L,
                LocalDateTime.of(2026, 5, 17, 17, 29, 31)));

        assertEquals("{\"id\":\"2055465090113392600\",\"primitiveId\":\"1\",\"createdTime\":\"2026-05-17 17:29:31\"}", json);
    }

    static class LongPayload {
        public Long id;
        public long primitiveId;
        public LocalDateTime createdTime;

        LongPayload() {
        }

        LongPayload(Long id, long primitiveId, LocalDateTime createdTime) {
            this.id = id;
            this.primitiveId = primitiveId;
            this.createdTime = createdTime;
        }
    }
}
