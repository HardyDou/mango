package io.mango.infra.sensitive.core.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.mango.infra.sensitive.api.ISensitiveMaskingService;
import io.mango.infra.sensitive.api.SensitiveMaskingContext;
import io.mango.infra.sensitive.api.annotation.Sensitive;
import io.mango.infra.sensitive.core.DefaultSensitiveMaskingService;
import io.mango.infra.sensitive.core.SensitiveMasker;

import java.io.IOException;
import java.util.Objects;

/**
 * Serializes annotated strings with output masking.
 */
public class SensitiveStringSerializer extends JsonSerializer<Object> {

    private final Sensitive sensitive;

    private final ISensitiveMaskingService maskingService;

    public SensitiveStringSerializer(Sensitive sensitive) {
        this(sensitive, new DefaultSensitiveMaskingService());
    }

    public SensitiveStringSerializer(Sensitive sensitive, ISensitiveMaskingService maskingService) {
        this.sensitive = Objects.requireNonNull(sensitive, "sensitive must not be null");
        this.maskingService = Objects.requireNonNull(maskingService, "maskingService must not be null");
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String origin = (String) value;
        if (origin == null || origin.isBlank()) {
            gen.writeString(origin);
            return;
        }
        if (SensitiveMaskingContext.isMaskingDisabled()
                || !maskingService.shouldMask(sensitive)) {
            gen.writeString(origin);
            return;
        }
        gen.writeString(SensitiveMasker.mask(sensitive, origin));
    }
}
