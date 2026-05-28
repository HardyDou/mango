package io.mango.infra.sensitive.core.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.mango.infra.sensitive.api.SensitiveMaskingContext;
import io.mango.infra.sensitive.api.annotation.Sensitive;
import io.mango.infra.sensitive.core.SensitiveMasker;
import io.mango.infra.sensitive.core.SensitiveMaskingRuntime;

import java.io.IOException;

/**
 * Serializes annotated strings with output masking.
 */
public class SensitiveStringSerializer extends JsonSerializer<Object> {

    private final Sensitive sensitive;

    public SensitiveStringSerializer(Sensitive sensitive) {
        this.sensitive = sensitive;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String origin = (String) value;
        if (origin == null || origin.isBlank()) {
            gen.writeString(origin);
            return;
        }
        if (SensitiveMaskingContext.isMaskingDisabled()
                || !SensitiveMaskingRuntime.getMaskingService().shouldMask(sensitive)) {
            gen.writeString(origin);
            return;
        }
        gen.writeString(SensitiveMasker.mask(sensitive, origin));
    }
}
