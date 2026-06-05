package io.mango.identity.api.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    private List<@NotNull(message = "用户ID不能为空") Long> userIds;
}
