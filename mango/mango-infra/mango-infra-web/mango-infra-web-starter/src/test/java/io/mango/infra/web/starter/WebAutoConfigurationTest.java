package io.mango.infra.web.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebAutoConfigurationTest {

    @Test
    void longValuesShouldSerializeAsStringAndDeserializeFromString() throws Exception {
        Jackson2ObjectMapperBuilderCustomizer customizer = new WebAutoConfiguration(new MangoWebProperties())
                .mangoLongToStringJacksonCustomizer();
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        customizer.customize(builder);
        ObjectMapper objectMapper = builder.build();

        String json = objectMapper.writeValueAsString(new LongPayload(2055465090113392600L, 1L));

        assertEquals("{\"id\":\"2055465090113392600\",\"primitiveId\":\"1\"}", json);
        assertEquals(2055465090113392600L, objectMapper.readValue("{\"id\":\"2055465090113392600\"}", LongPayload.class).id);
    }

    @Test
    void existingObjectMapperBeanShouldSerializeLongValuesAsString() throws Exception {
        BeanPostProcessor processor = new WebAutoConfiguration(new MangoWebProperties())
                .mangoLongToStringObjectMapperPostProcessor();
        ObjectMapper objectMapper = (ObjectMapper) processor.postProcessAfterInitialization(new ObjectMapper(), "objectMapper");

        String json = objectMapper.writeValueAsString(new LongPayload(2055465090113392600L, 1L));

        assertEquals("{\"id\":\"2055465090113392600\",\"primitiveId\":\"1\"}", json);
    }

    static class LongPayload {
        public Long id;
        public long primitiveId;

        LongPayload() {
        }

        LongPayload(Long id, long primitiveId) {
            this.id = id;
            this.primitiveId = primitiveId;
        }
    }
}
