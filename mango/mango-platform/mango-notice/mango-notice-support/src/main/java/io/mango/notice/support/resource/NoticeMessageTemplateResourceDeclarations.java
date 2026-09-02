package io.mango.notice.support.resource;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.builder.ResourceDeclarationBuilder;
import io.mango.resource.support.model.ResourceDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for declaring mango-notice message template resources.
 */
public final class NoticeMessageTemplateResourceDeclarations {

    private static final String TARGET_MODULE = "notice";
    private static final String DEFAULT_TENANT_ID = "1";
    private static final long OPERATOR_ID = 1L;
    private static final int CHANNEL_COUNT = 4;
    private static final int SITE_CHANNEL_INDEX = 0;
    private static final int EMAIL_CHANNEL_INDEX = 1;
    private static final int WECOM_CHANNEL_INDEX = 2;
    private static final int SMS_CHANNEL_INDEX = 3;

    private NoticeMessageTemplateResourceDeclarations() {
    }

    public static List<ResourceDeclaration> fourChannels(MessageTemplateSpec spec) {
        List<ResourceDeclaration> declarations = new ArrayList<>(CHANNEL_COUNT);
        declarations.add(template(spec, SITE_CHANNEL_INDEX, NoticeChannelType.SITE, "系统消息",
                spec.siteTitle(), spec.siteContent()));
        declarations.add(template(spec, EMAIL_CHANNEL_INDEX, NoticeChannelType.EMAIL, "邮件",
                spec.emailTitle(), spec.emailContent()));
        declarations.add(template(spec, WECOM_CHANNEL_INDEX, NoticeChannelType.WECOM, "企业微信",
                spec.wecomTitle(), spec.wecomContent()));
        declarations.add(template(spec, SMS_CHANNEL_INDEX, NoticeChannelType.SMS, "短信",
                spec.smsTitle(), spec.smsContent()));
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
                .bool("channelEnabled", defaultChannelEnabled(channelType))
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

    private static boolean defaultChannelEnabled(NoticeChannelType channelType) {
        return channelType == NoticeChannelType.SITE || channelType == NoticeChannelType.WECOM;
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
