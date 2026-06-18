package io.mango.i18n.api.resource;

import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.builder.ResourceDeclarationBuilder;
import io.mango.resource.api.model.ResourceDeclaration;

/**
 * Utility for declaring sys_i18n resource entries.
 */
public final class I18nMessageResourceDeclarations {

    private static final String TARGET_MODULE = "system";
    private static final String DEFAULT_TENANT_ID = "1";

    private I18nMessageResourceDeclarations() {
    }

    public static ResourceDeclaration message(I18nMessageSpec spec) {
        return ResourceDeclarationBuilder.create(ResourceTypes.I18N_MESSAGE)
                .id(String.valueOf(spec.id()))
                .version(spec.version())
                .module(spec.moduleCode(), spec.moduleName())
                .bizKey(spec.moduleCode() + ".i18n." + spec.name())
                .name(spec.name())
                .targetModule(TARGET_MODULE)
                .longValue("i18nId", spec.id())
                .string("tenantId", DEFAULT_TENANT_ID)
                .string("name", spec.name())
                .string("zhCn", spec.zhCn())
                .string("en", spec.en())
                .string("description", spec.description())
                .build();
    }

    public record I18nMessageSpec(
            long id,
            int version,
            String moduleCode,
            String moduleName,
            String name,
            String zhCn,
            String en,
            String description
    ) {
    }
}
