package io.mango.notice.api.resource;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.builder.ResourceDeclarationBuilder;
import io.mango.resource.api.model.ResourceDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for declaring mango-notice message template resources.
 */
public final class NoticeMessageTemplateResourceDeclarations {

    private static final String TARGET_MODULE = "notice";
    private static final String DEFAULT_TENANT_ID = "1";
    private static final long OPERATOR_ID = 1L;

    private NoticeMessageTemplateResourceDeclarations() {
    }

    public static List<ResourceDeclaration> fourChannels(MessageTemplateSpec spec) {
        List<ResourceDeclaration> declarations = new ArrayList<>(4);
        declarations.add(template(spec, 0, NoticeChannelType.SITE, "系统消息", spec.siteTitle(), spec.siteContent()));
        declarations.add(template(spec, 1, NoticeChannelType.EMAIL, "邮件", spec.emailTitle(), spec.emailContent()));
        declarations.add(template(spec, 2, NoticeChannelType.WECOM, "企业微信", spec.wecomTitle(), spec.wecomContent()));
        declarations.add(template(spec, 3, NoticeChannelType.SMS, "短信", spec.smsTitle(), spec.smsContent()));
        return declarations;
    }

    private static ResourceDeclaration template(MessageTemplateSpec spec, int channelIndex, NoticeChannelType channelType,
                                                String channelName, String title, String content) {
        long resourceId = spec.resourceIdBase() + channelIndex;
        long channelTemplateId = spec.channelTemplateIdBase() + channelIndex;
        return ResourceDeclarationBuilder.create(ResourceTypes.MESSAGE_TEMPLATE)
                .id(String.valueOf(resourceId))
                .version(spec.version())
                .module(spec.moduleCode(), spec.moduleName())
                .bizKey(spec.moduleCode() + ".message." + spec.bizType().replace('.', '-') + "-"
                        + channelType.name().toLowerCase())
                .name(spec.bizName() + channelName + "模板")
                .targetModule(TARGET_MODULE)
                .longValue("businessTypeId", spec.businessTypeId())
                .longValue("configVersionId", spec.configVersionId())
                .longValue("channelTemplateId", channelTemplateId)
                .string("tenantId", DEFAULT_TENANT_ID)
                .string("bizType", spec.bizType())
                .string("bizName", spec.bizName())
                .string("bizGroup", spec.bizGroup())
                .string("domainCode", spec.domainCode())
                .string("description", spec.description())
                .json("paramsSchema", spec.paramsSchema())
                .bool("enabled", spec.enabled())
                .string("defaultPriority", spec.defaultPriority().name())
                .string("idempotentStrategy", spec.idempotentStrategy())
                .intValue("version", spec.version())
                .string("versionStatus", "ACTIVE")
                .string("channelType", channelType.name())
                .string("templateName", spec.bizName() + channelName + "模板")
                .string("titleTemplate", title)
                .string("contentTemplate", content)
                .longValue("operatorId", OPERATOR_ID)
                .build();
    }

    public record MessageTemplateSpec(
            String moduleCode,
            String moduleName,
            long resourceIdBase,
            long businessTypeId,
            long configVersionId,
            long channelTemplateIdBase,
            int version,
            String bizType,
            String bizName,
            String bizGroup,
            String domainCode,
            String description,
            String paramsSchema,
            NoticePriority defaultPriority,
            String idempotentStrategy,
            boolean enabled,
            String siteTitle,
            String siteContent,
            String emailTitle,
            String emailContent,
            String wecomTitle,
            String wecomContent,
            String smsTitle,
            String smsContent
    ) {
    }
}
