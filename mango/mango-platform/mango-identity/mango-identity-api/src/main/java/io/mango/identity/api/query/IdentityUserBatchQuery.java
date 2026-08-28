package io.mango.identity.api.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 当前租户身份用户批量查询。
 */
@Data
public class IdentityUserBatchQuery {

    /** 用户 ID。 */
    @Size(max = 200)
    private List<@NotNull Long> userIds;

    /** 用户名。 */
    @Size(max = 200)
    private List<@NotBlank String> usernames;
}
