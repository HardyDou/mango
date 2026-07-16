package io.mango.identity.api.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量移除租户成员命令。
 */
@Data
public class BatchDeleteIdentityUserCommand {

    /**
     * 用户 ID 列表。
     */
    @NotEmpty(message = "用户ID不能为空")
    @Schema(description = "用户ID列表")
    private List<@NotNull(message = "用户ID不能为空") Long> userIds;

    public List<Long> getUserIds() {
        if (userIds == null) {
            return null;
        }
        return List.copyOf(userIds);
    }

    public void setUserIds(List<Long> userIds) {
        if (userIds == null) {
            this.userIds = null;
            return;
        }
        this.userIds = List.copyOf(userIds);
    }
}
