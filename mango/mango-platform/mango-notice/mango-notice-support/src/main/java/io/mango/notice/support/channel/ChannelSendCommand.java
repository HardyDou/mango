package io.mango.notice.support.channel;

import io.mango.notice.api.enums.NoticePriority;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChannelSendCommand {

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
