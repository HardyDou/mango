package io.mango.resource.support.declaration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDeclarationCanonicalizerTest {

    @Test
    void canonicalIdentityDoesNotDependOnHostLongSerialization() {
        ObjectMapper numericHostMapper = new ObjectMapper();
        SimpleModule longAsString = new SimpleModule()
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(Long.TYPE, ToStringSerializer.instance);
        ObjectMapper webHostMapper = new ObjectMapper().registerModule(longAsString);
        ResourceDeclaration declaration = declarationWithLongField();

        ResourceDeclarationCanonicalizer bootstrapCanonicalizer =
                new ResourceDeclarationCanonicalizer(numericHostMapper);
        ResourceDeclarationCanonicalizer runtimeCanonicalizer =
                new ResourceDeclarationCanonicalizer(webHostMapper);

        assertThat(runtimeCanonicalizer.canonicalBytes(declaration))
                .isEqualTo(bootstrapCanonicalizer.canonicalBytes(declaration));
        assertThat(runtimeCanonicalizer.fingerprint(declaration))
                .isEqualTo(bootstrapCanonicalizer.fingerprint(declaration));
        assertThat(new String(runtimeCanonicalizer.canonicalBytes(declaration), StandardCharsets.UTF_8))
                .contains("\"value\":9007199254740993")
                .doesNotContain("\"value\":\"9007199254740993\"");
    }

    @Test
    void canonicalIdentitySupportsJavaTimeWithoutHostModules() {
        ResourceDeclaration declaration = declarationWithLongField();
        ResourceField date = new ResourceField();
        date.setType(ResourceFieldType.DATE);
        date.setValue(LocalDate.of(2026, 8, 3));
        declaration.putField("businessDate", date);
        ResourceField dateTime = new ResourceField();
        dateTime.setType(ResourceFieldType.DATETIME);
        dateTime.setValue(LocalDateTime.of(2026, 8, 3, 3, 30, 45));
        declaration.putField("publishedAt", dateTime);

        String canonicalJson = new String(
                new ResourceDeclarationCanonicalizer(new ObjectMapper()).canonicalBytes(declaration),
                StandardCharsets.UTF_8);

        assertThat(canonicalJson)
                .contains("\"value\":\"2026-08-03\"")
                .contains("\"value\":\"2026-08-03T03:30:45\"");
    }

    private static ResourceDeclaration declarationWithLongField() {
        ResourceField field = new ResourceField();
        field.setType(ResourceFieldType.LONG);
        field.setValue(9_007_199_254_740_993L);
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("resource-1");
        declaration.setVersion(1);
        declaration.setResourceType("TEST");
        declaration.setModuleCode("test");
        declaration.setBizKey("test.long");
        declaration.setName("Long identity");
        declaration.setTargetModule("test");
        declaration.putField("tenantId", field);
        return declaration;
    }
}
