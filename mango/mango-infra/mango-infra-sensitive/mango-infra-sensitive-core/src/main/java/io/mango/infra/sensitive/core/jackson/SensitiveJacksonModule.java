package io.mango.infra.sensitive.core.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson module that applies Mango sensitive field masking.
 */
public class SensitiveJacksonModule extends SimpleModule {

    public SensitiveJacksonModule() {
        super("mango-sensitive");
        setSerializerModifier(new SensitiveBeanSerializerModifier());
    }
}
