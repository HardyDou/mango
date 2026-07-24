package io.mango.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "通知渠道或标签的模板引用影响")
public class NoticeChannelReferenceImpactVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "引用模板数量")
    private Integer referenceCount;

    @Schema(description = "引用该渠道或标签的业务模板名称")
    private List<String> businessTemplateNames;

    public List<String> getBusinessTemplateNames() {
        return businessTemplateNames == null ? null : List.copyOf(businessTemplateNames);
    }

    public void setBusinessTemplateNames(List<String> businessTemplateNames) {
        this.businessTemplateNames =
                businessTemplateNames == null ? null : List.copyOf(businessTemplateNames);
    }
}
