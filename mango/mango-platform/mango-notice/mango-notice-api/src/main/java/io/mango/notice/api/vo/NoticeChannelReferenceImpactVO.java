package io.mango.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "通知渠道或标签的模板引用影响")
public class NoticeChannelReferenceImpactVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer referenceCount;
    private List<String> businessTemplateNames;
}
