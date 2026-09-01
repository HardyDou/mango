package io.mango.authorization.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量查询成员直接角色请求。
 */
@Data
@Schema(description = "批量查询成员直接角色请求")
public class SubjectRoleBatchRequest {

    @NotEmpty(message = "成员ID不能为空")
    @Size(max = 200, message = "成员ID不能超过200个")
    @Schema(description = "成员ID列表，最多200个")
    private List<@Positive(message = "成员ID必须大于0") Long> subjectIds;

    public List<Long> getSubjectIds() {
        return subjectIds == null ? null : List.copyOf(subjectIds);
    }

    public void setSubjectIds(List<Long> subjectIds) {
        this.subjectIds = subjectIds == null ? null : List.copyOf(subjectIds);
    }
}
