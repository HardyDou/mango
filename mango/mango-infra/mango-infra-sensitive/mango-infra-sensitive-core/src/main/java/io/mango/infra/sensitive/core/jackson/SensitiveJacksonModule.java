package io.mango.infra.sensitive.core.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.mango.infra.sensitive.api.ISensitiveMaskingService;

import java.util.Objects;

/**
 * Jackson module that applies Mango sensitive field masking.
 */
public class SensitiveJacksonModule extends SimpleModule {

    public SensitiveJacksonModule() {
        super("mango-sensitive");
        setSerializerModifier(new SensitiveBeanSerializerModifier());
    }

    /**
     * Creates a module with an isolated masking policy.
     *
     * @param maskingService masking policy used by serializers created by this module
     */
    public SensitiveJacksonModule(ISensitiveMaskingService maskingService) {
        super("mango-sensitive");
        setSerializerModifier(new SensitiveBeanSerializerModifier(
                Objects.requireNonNull(maskingService, "maskingService must not be null")));
    }
}
