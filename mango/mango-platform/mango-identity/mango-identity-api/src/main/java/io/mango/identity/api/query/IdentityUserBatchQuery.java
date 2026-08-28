package io.mango.identity.api.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前租户身份用户批量查询。
 */
@Data
public class IdentityUserBatchQuery {

    public static final int MAX_IDENTIFIERS = 200;

    /** 用户 ID。 */
    @Size(max = MAX_IDENTIFIERS)
    private List<@NotNull Long> userIds;

    /** 用户名。 */
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
