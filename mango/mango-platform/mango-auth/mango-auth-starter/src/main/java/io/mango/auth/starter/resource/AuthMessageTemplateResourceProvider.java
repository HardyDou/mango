package io.mango.auth.starter.resource;

import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.resource.NoticeMessageTemplateResourceDeclarations;
import io.mango.notice.api.resource.NoticeMessageTemplateResourceDeclarations.MessageTemplateSpec;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.model.ResourceDeclaration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Auth module message template resources.
 */
@Component
public class AuthMessageTemplateResourceProvider implements ResourceProvider {

    @Override
    public List<String> moduleCodes() {
        return List.of("auth");
    }

    @Override
    public List<ResourceDeclaration> provide() {
        List<ResourceDeclaration> declarations = new ArrayList<>();
        declarations.addAll(NoticeMessageTemplateResourceDeclarations.fourChannels(spec(
                2026061900100010000L,
                2060000000001010000L,
                "auth.login.locked",
                "登录尝试锁定",
                "登录失败次数达到限制后通知账号持有人或安全管理员。",
                NoticePriority.HIGH,
                "{\"type\":\"object\",\"properties\":{\"username\":{\"type\":\"string\",\"title\":\"用户名\"},\"clientIp\":{\"type\":\"string\",\"title\":\"客户端IP\"},\"remainingMinutes\":{\"type\":\"number\",\"title\":\"剩余锁定分钟\"},\"loginTime\":{\"type\":\"string\",\"title\":\"登录时间\"}},\"required\":[\"username\",\"clientIp\",\"remainingMinutes\"]}",
                "账号登录已被临时锁定：{{username}}",
                "账号 {{username}} 因连续登录失败已被临时锁定，客户端 IP：{{clientIp}}，剩余锁定时间：{{remainingMinutes}} 分钟。",
                "【Mango】账号登录已被临时锁定：{{username}}",
                "账号 {{username}} 因连续登录失败已被临时锁定，客户端 IP：{{clientIp}}，剩余锁定时间：{{remainingMinutes}} 分钟。请确认是否本人操作。",
                "账号登录已被临时锁定：{{username}}，IP：{{clientIp}}，剩余 {{remainingMinutes}} 分钟。",
                "账号 {{username}} 登录已被临时锁定，剩余 {{remainingMinutes}} 分钟。")));
        declarations.addAll(NoticeMessageTemplateResourceDeclarations.fourChannels(spec(
                2026061900100010100L,
                2060000000001010100L,
                "auth.login.success",
                "登录成功",
                "用户成功登录后发送低优先级提醒。",
                NoticePriority.LOW,
                "{\"type\":\"object\",\"properties\":{\"username\":{\"type\":\"string\",\"title\":\"用户名\"},\"clientIp\":{\"type\":\"string\",\"title\":\"客户端IP\"},\"loginTime\":{\"type\":\"string\",\"title\":\"登录时间\"},\"appCode\":{\"type\":\"string\",\"title\":\"应用编码\"}},\"required\":[\"username\",\"loginTime\"]}",
                "登录成功：{{username}}",
                "账号 {{username}} 已成功登录 {{appCode}}，客户端 IP：{{clientIp}}，时间：{{loginTime}}。",
                "【Mango】登录成功：{{username}}",
                "账号 {{username}} 已成功登录 {{appCode}}，客户端 IP：{{clientIp}}，时间：{{loginTime}}。",
                "登录成功：{{username}}，IP：{{clientIp}}。",
                "账号 {{username}} 已成功登录。")));
        return declarations;
    }

    private MessageTemplateSpec spec(long resourceBase, long targetBase, String bizType, String bizName,
                                     String description, NoticePriority priority, String paramsSchema,
                                     String siteTitle, String siteContent, String emailTitle, String emailContent,
                                     String wecomContent, String smsContent) {
        return new MessageTemplateSpec(
                "auth",
                "认证授权",
                resourceBase,
                targetBase,
                targetBase + 1,
                targetBase + 2,
                1,
                bizType,
                bizName,
                "AUTH",
                "AUTH",
                description,
                paramsSchema,
                priority,
                "BIZ_ID",
                true,
                siteTitle,
                siteContent,
                emailTitle,
                emailContent,
                siteTitle,
                wecomContent,
                siteTitle,
                smsContent);
    }
}
