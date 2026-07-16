package io.mango.identity.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量查询成员请求")
public class ListTenantMembersRequest {

    @NotEmpty(message = "成员ID不能为空")
    @Schema(description = "成员ID列表")
    private List<@NotNull(message = "成员ID不能为空") Long> memberIds;

    public List<Long> getMemberIds() {
        if (memberIds == null) {
            return null;
        }
        return List.copyOf(memberIds);
    }

    public void setMemberIds(List<Long> memberIds) {
        if (memberIds == null) {
            this.memberIds = null;
            return;
        }
        this.memberIds = List.copyOf(memberIds);
    }
}
