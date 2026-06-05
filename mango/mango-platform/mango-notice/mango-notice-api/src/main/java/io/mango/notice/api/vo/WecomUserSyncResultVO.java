package io.mango.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "企业微信用户同步结果")
public class WecomUserSyncResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "企业微信返回成员总数")
    private int totalCount;

    @Schema(description = "企业微信返回部门总数")
    private int departmentTotalCount;

    @Schema(description = "新建部门数量")
    private int departmentCreatedCount;

    @Schema(description = "更新部门数量")
    private int departmentUpdatedCount;

    @Schema(description = "跳过部门数量")
    private int departmentSkippedCount;

    @Schema(description = "匹配到已有成员数量")
    private int matchedCount;

    @Schema(description = "新建成员数量")
    private int createdCount;

    @Schema(description = "更新成员资料数量")
    private int updatedCount;

    @Schema(description = "绑定企业微信接收账户数量")
    private int boundAccountCount;

    @Schema(description = "跳过数量")
    private int skippedCount;

    @Schema(description = "数据未变化跳过数量")
    private int unchangedCount;

    @Schema(description = "失败数量")
    private int failedCount;

    @Schema(description = "失败或跳过明细")
    private List<String> messages = new ArrayList<>();

    public void addMessage(String message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
    }
}
