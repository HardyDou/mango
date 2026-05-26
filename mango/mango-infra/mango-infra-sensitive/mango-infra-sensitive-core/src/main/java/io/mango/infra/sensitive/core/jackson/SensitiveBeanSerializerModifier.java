package io.mango.infra.sensitive.core.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import io.mango.infra.sensitive.api.annotation.Sensitive;

import java.util.List;

/**
 * Assigns sensitive serializers to annotated string properties.
 */
public class SensitiveBeanSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        for (BeanPropertyWriter writer : beanProperties) {
            Sensitive sensitive = writer.getAnnotation(Sensitive.class);
            if (sensitive != null && String.class.isAssignableFrom(writer.getType().getRawClass())) {
                writer.assignSerializer(new SensitiveStringSerializer(sensitive));
            }
        }
        return beanProperties;
    }
}
