package io.mango.identity.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前租户身份用户批量查询请求。
 */
@Data
public class IdentityUserBatchRequest {

    public static final int MAX_IDENTIFIERS = 200;

    /** 用户 ID。 */
    @Schema(description = "用户 ID 列表")
    @Size(max = MAX_IDENTIFIERS)
    private List<@NotNull Long> userIds;

    /** 用户名。 */
    @Schema(description = "用户名列表")
    @Size(max = MAX_IDENTIFIERS)
    private List<@NotBlank String> usernames;

    public List<Long> getUserIds() {
        return userIds == null ? null : new ArrayList<>(userIds);
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds == null ? null : new ArrayList<>(userIds);
    }

    public List<String> getUsernames() {
        return usernames == null ? null : new ArrayList<>(usernames);
    }

    public void setUsernames(List<String> usernames) {
        this.usernames = usernames == null ? null : new ArrayList<>(usernames);
    }
}
