package io.mango.information.api.vo;

import lombok.Data;

/** 提交结果。 */
@Data
public class InformationSubmissionResultVO {
    /** 采集记录主键，业务方据此查询详情。 */
    private Long id;
    private String message;
}
