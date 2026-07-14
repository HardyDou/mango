package io.mango.notice.support.channel;

import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.command.NoticeSiteMessageActionCommand;
import io.mango.notice.api.command.NoticeSiteMessageSubjectCommand;
import io.mango.notice.api.command.NoticeSiteMessageTargetCommand;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class NoticeChannelMessage {

    private Long taskId;

    private Long sendRecordId;

    private Long userId;

    private String recipientName;

    private String mobile;

    private String email;

    private String wechatOpenid;

    private String wecomUserId;

    private String dingtalkUserId;

    private String title;

    private String content;

    private String messageScene;

    private NoticeSiteMessageSubjectCommand messageSubject;

    private NoticeSiteMessageTargetCommand messageTarget;

    private Map<String, Object> messageData;

    private List<NoticeSiteMessageActionCommand> messageActions;

    private LocalDateTime messageExpireTime;

    private List<Long> attachmentFileIds;

    private NoticePriority priority;

    private String bizType;

    private String bizId;

    private Map<String, Object> params;

    private Long channelConfigId;

    private String channelProviderCode;

    private String channelConfigName;

    private String channelConfigJson;

    private String channelTemplateId;

    private String variableMapping;
}
