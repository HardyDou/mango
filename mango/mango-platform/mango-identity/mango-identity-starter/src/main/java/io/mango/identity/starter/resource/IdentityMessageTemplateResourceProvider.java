package io.mango.identity.starter.resource;

import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.resource.NoticeMessageTemplateResourceDeclarations;
import io.mango.notice.api.resource.NoticeMessageTemplateResourceDeclarations.MessageTemplateSpec;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.model.ResourceDeclaration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Identity module message template resources.
 */
@Component
public class IdentityMessageTemplateResourceProvider implements ResourceProvider {

    @Override
    public List<String> moduleCodes() {
        return List.of("identity");
    }

    @Override
    public List<ResourceDeclaration> provide() {
        List<ResourceDeclaration> declarations = new ArrayList<>();
        declarations.addAll(NoticeMessageTemplateResourceDeclarations.fourChannels(spec(
                2026061900200010000L,
                2060000000001020000L,
                "identity.user.created",
                "账号开通",
                "管理员创建成员账号后通知账号持有人。",
                NoticePriority.NORMAL,
                "{\"type\":\"object\",\"properties\":{\"userId\":{\"type\":\"string\",\"title\":\"用户ID\"},\"username\":{\"type\":\"string\",\"title\":\"用户名\"},\"nickname\":{\"type\":\"string\",\"title\":\"昵称\"},\"tenantId\":{\"type\":\"string\",\"title\":\"租户ID\"},\"createdAt\":{\"type\":\"string\",\"title\":\"开通时间\"}},\"required\":[\"userId\",\"username\"]}",
                "账号已开通：{{username}}",
                "账号 {{username}} 已开通，请按管理员提供的登录方式访问系统。",
                "【Mango】账号已开通：{{username}}",
                "账号 {{username}} 已开通，请按管理员提供的登录方式访问系统。如非本人申请，请联系管理员。",
                "账号已开通：{{username}}",
                "账号 {{username}} 已开通，请及时登录确认。",
                "账号 {{username}} 已开通。")));
        declarations.addAll(NoticeMessageTemplateResourceDeclarations.fourChannels(spec(
                2026061900200010100L,
                2060000000001020100L,
                "identity.password.reset",
                "密码重置",
                "管理员重置成员密码后通知账号持有人。",
                NoticePriority.HIGH,
                "{\"type\":\"object\",\"properties\":{\"userId\":{\"type\":\"string\",\"title\":\"用户ID\"},\"username\":{\"type\":\"string\",\"title\":\"用户名\"},\"resetAt\":{\"type\":\"string\",\"title\":\"重置时间\"}},\"required\":[\"userId\",\"username\"]}",
                "账号密码已重置：{{username}}",
                "账号 {{username}} 的登录密码已被管理员重置。如非本人申请，请立即联系管理员。",
                "【Mango】账号密码已重置：{{username}}",
                "账号 {{username}} 的登录密码已被管理员重置。如非本人申请，请立即联系管理员。",
                "账号密码已重置：{{username}}",
                "账号 {{username}} 的登录密码已被管理员重置。",
                "账号 {{username}} 的密码已重置。")));
        declarations.addAll(NoticeMessageTemplateResourceDeclarations.fourChannels(spec(
                2026061900200010200L,
                2060000000001020200L,
                "auth.wecom.login.bound",
                "企业微信账号绑定",
                "成员企业微信身份绑定成功后发送通知。",
                NoticePriority.NORMAL,
                "{\"type\":\"object\",\"properties\":{\"userId\":{\"type\":\"string\",\"title\":\"用户ID\"},\"username\":{\"type\":\"string\",\"title\":\"用户名\"},\"corpId\":{\"type\":\"string\",\"title\":\"企业ID\"},\"externalUserId\":{\"type\":\"string\",\"title\":\"企业微信用户ID\"},\"bindTime\":{\"type\":\"string\",\"title\":\"绑定时间\"}},\"required\":[\"userId\",\"username\",\"externalUserId\"]}",
                "企业微信账号已绑定：{{username}}",
                "账号 {{username}} 已绑定企业微信身份 {{externalUserId}}。",
                "【Mango】企业微信账号已绑定：{{username}}",
                "账号 {{username}} 已绑定企业微信身份 {{externalUserId}}。如非本人操作，请联系管理员。",
                "企业微信账号已绑定：{{username}}",
                "账号 {{username}} 已绑定企业微信身份 {{externalUserId}}。",
                "账号 {{username}} 已绑定企业微信。")));
        declarations.addAll(NoticeMessageTemplateResourceDeclarations.fourChannels(spec(
                2026061900200010300L,
                2060000000001020300L,
                "auth.wecom.login.unbound",
                "企业微信账号解绑",
                "成员企业微信身份解绑后发送通知。",
                NoticePriority.NORMAL,
                "{\"type\":\"object\",\"properties\":{\"userId\":{\"type\":\"string\",\"title\":\"用户ID\"},\"username\":{\"type\":\"string\",\"title\":\"用户名\"},\"corpId\":{\"type\":\"string\",\"title\":\"企业ID\"},\"externalUserId\":{\"type\":\"string\",\"title\":\"企业微信用户ID\"},\"unbindTime\":{\"type\":\"string\",\"title\":\"解绑时间\"}},\"required\":[\"userId\",\"username\",\"externalUserId\"]}",
                "企业微信账号已解绑：{{username}}",
                "账号 {{username}} 已解绑企业微信身份 {{externalUserId}}。",
                "【Mango】企业微信账号已解绑：{{username}}",
                "账号 {{username}} 已解绑企业微信身份 {{externalUserId}}。如非本人操作，请联系管理员。",
                "企业微信账号已解绑：{{username}}",
                "账号 {{username}} 已解绑企业微信身份 {{externalUserId}}。",
                "账号 {{username}} 已解绑企业微信。")));
        return declarations;
    }

    private MessageTemplateSpec spec(long resourceBase, long targetBase, String bizType, String bizName,
                                     String description, NoticePriority priority, String paramsSchema,
                                     String siteTitle, String siteContent, String emailTitle, String emailContent,
                                     String wecomTitle, String wecomContent, String smsContent) {
        return new MessageTemplateSpec("identity", "身份中心", resourceBase, targetBase, targetBase + 1,
                targetBase + 2, 1, bizType, bizName, "IDENTITY", "IDENTITY", description, paramsSchema,
                priority, "BIZ_ID", true, siteTitle, siteContent, emailTitle, emailContent, wecomTitle,
                wecomContent, siteTitle, smsContent);
    }
}
